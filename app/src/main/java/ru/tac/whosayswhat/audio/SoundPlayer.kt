package ru.tac.whosayswhat.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Короткие звуки через SoundPool: один звук за раз, предыдущий обрывается.
 * Громкость и фокус аудио — чтобы не «перекрикивать» другие приложения,
 * а в детском приложении не пугать резким стартом.
 *
 * Фокус берётся только на время звука: короткое удержание, затем
 * `abandonAudioFocusRequest`, иначе приложение приглушало бы чужую музыку всё
 * время работы. Сроки считает [AudioFocusHold].
 *
 * Использовать с главного потока: так приходят колбэки SoundPool и таймер
 * возврата фокуса.
 */
class SoundPlayer(context: Context) {

    private val appContext = context.applicationContext
    private val audioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(attributes)
        .build()

    private val sampleIds = ConcurrentHashMap<String, Int>()
    private val sampleToPath = ConcurrentHashMap<Int, String>()
    private val loadedPaths = ConcurrentHashMap.newKeySet<String>()
    private val failedPaths = ConcurrentHashMap.newKeySet<String>()
    private val durations = ConcurrentHashMap<String, Long>()
    @Volatile
    private var pendingPath: String? = null
    private var focusRequest: AudioFocusRequest? = null
    private var activeStreamId = 0

    /** Предзагрузка идёт в фоне; после [release] её надо остановить. */
    @Volatile
    private var released = false
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Один загрузчик на путь: предзагрузка и тап могут прийти одновременно. */
    private val loadLock = Any()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val focusHold = AudioFocusHold()

    /**
     * Фокус отобрали — сразу замолкаем: поверх звонка или чужой игры наш звук
     * звучал бы невпопад. Следующий звук запросит фокус заново.
     */
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
            -> {
                stopStream()
                focusHold.onFocusLost()
            }
        }
    }

    /** Возвращает фокус, когда звук доиграл и пауза не заполнилась новым. */
    private val releaseFocusRunnable = Runnable {
        if (focusHold.shouldRelease(SystemClock.uptimeMillis())) {
            releaseFocus()
        } else {
            // Обработчик сработал на пару миллисекунд раньше срока.
            scheduleFocusRelease()
        }
    }

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (released) return@setOnLoadCompleteListener
            val path = sampleToPath[sampleId]
            if (path != null) {
                if (status == 0) {
                    loadedPaths += path
                    if (pendingPath == path) {
                        pendingPath = null
                        playNow(sampleId)
                    }
                } else {
                    failedPaths += path
                    Log.w(TAG, "SoundPool не смог декодировать $path (status=$status)")
                }
            }
        }
    }

    /**
     * Предзагрузка на старте приложения, чтобы первый тап не был тихим.
     *
     * Открытие assets и чтение WAV-заголовков идут в фоне: в каталоге больше
     * сорока звуков, и на медленном устройстве это заметно подвешивало первый
     * кадр после запуска. Сам `SoundPool.load` асинхронный, повторную загрузку
     * одного и того же файла отсекает [load].
     */
    fun preload(paths: Collection<String>) {
        val unique = paths.asSequence().filter { it.isNotBlank() }.distinct().toList()
        ioScope.launch {
            unique.forEach { path ->
                if (released) return@launch
                load(path)
                // Длительность нужна сразу при старте звука: по ней считается,
                // сколько держать аудиофокус. Читаем заголовок заранее, чтобы не
                // открывать assets на каждый тап.
                durationMs(path)
            }
        }
    }

    fun play(path: String) {
        if (released || path.isBlank() || path in failedPaths) return
        val sampleId = sampleIds[path] ?: run {
            load(path)
            sampleIds[path] ?: return
        }
        if (path in loadedPaths) {
            playNow(sampleId)
        } else {
            pendingPath = path
        }
    }

    /**
     * Длительность звука в миллисекундах по заголовку WAV. Нужна, чтобы
     * назвать животное ровно после того, как запись доиграет. 0 — файл не
     * WAV или заголовок не читается; вызывающий код подставит запас.
     */
    fun durationMs(path: String): Long {
        durations[path]?.let { return it }
        val duration = runCatching {
            appContext.assets.openFd(path).use { descriptor ->
                appContext.assets.open(path).use { stream ->
                    val header = ByteArray(HEADER_BYTES)
                    var read = 0
                    while (read < header.size) {
                        val count = stream.read(header, read, header.size - read)
                        if (count <= 0) break
                        read += count
                    }
                    wavDurationMs(header.copyOf(read), descriptor.length)
                }
            }
        }.getOrNull() ?: 0L
        durations[path] = duration
        return duration
    }

    private fun load(path: String) {
        if (released) return
        synchronized(loadLock) {
            if (sampleIds.containsKey(path) || path in failedPaths) return
            runCatching {
                appContext.assets.openFd(path).use { descriptor ->
                    soundPool.load(descriptor, PRIORITY)
                }
            }.onSuccess { sampleId ->
                sampleIds[path] = sampleId
                sampleToPath[sampleId] = path
            }.onFailure { error ->
                failedPaths += path
                Log.w(TAG, "Не удалось загрузить звук $path", error)
            }
        }
    }

    private fun playNow(sampleId: Int) {
        if (released) return
        // Предыдущий звук обрываем, но фокус не отдаём: он нужен новому.
        stopStream()
        requestFocusIfNeeded()
        activeStreamId = soundPool.play(sampleId, VOLUME, VOLUME, PRIORITY, 0, 1f)
        if (activeStreamId == 0) {
            // SoundPool не принял звук — держать фокус незачем.
            releaseFocus()
            return
        }
        val soundDurationMs = sampleToPath[sampleId]?.let(::durationMs) ?: 0L
        focusHold.onPlaybackStarted(SystemClock.uptimeMillis(), soundDurationMs)
        scheduleFocusRelease()
    }

    /**
     * Останавливает текущий звук и возвращает аудиофокус. Нужен перед записью
     * голоса в режиме «Эхо»: держать фокус во время записи незачем.
     */
    fun stop() {
        stopStream()
        releaseFocus()
    }

    private fun stopStream() {
        pendingPath = null
        if (activeStreamId != 0) {
            soundPool.stop(activeStreamId)
            activeStreamId = 0
        }
    }

    private fun requestFocusIfNeeded() {
        if (!focusHold.needsRequest()) return
        val request = focusRequest ?: AudioFocusRequest
            .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(focusListener, mainHandler)
            .build()
            .also { focusRequest = it }
        val result = audioManager.requestAudioFocus(request)
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            focusHold.onFocusGranted()
        }
    }

    private fun scheduleFocusRelease() {
        mainHandler.removeCallbacks(releaseFocusRunnable)
        val delayMs = focusHold.msUntilRelease(SystemClock.uptimeMillis())
        if (delayMs > 0L) mainHandler.postDelayed(releaseFocusRunnable, delayMs)
    }

    private fun releaseFocus() {
        mainHandler.removeCallbacks(releaseFocusRunnable)
        val request = focusRequest ?: return
        audioManager.abandonAudioFocusRequest(request)
        focusHold.onReleased()
    }

    fun release() {
        released = true
        ioScope.cancel()
        stop()
        mainHandler.removeCallbacksAndMessages(null)
        soundPool.release()
        sampleIds.clear()
        sampleToPath.clear()
        loadedPaths.clear()
        durations.clear()
    }

    private companion object {
        const val TAG = "SoundPlayer"
        const val VOLUME = 1f
        const val PRIORITY = 1
        const val HEADER_BYTES = 4096
    }
}
