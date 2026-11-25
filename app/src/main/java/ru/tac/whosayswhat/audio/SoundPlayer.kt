package ru.tac.whosayswhat.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.SoundPool
import android.util.Log

/**
 * Короткие звуки через SoundPool: один звук за раз, предыдущий обрывается.
 * Громкость и фокус аудио — чтобы не «перекрикивать» другие приложения,
 * а в детском приложении не пугать резким стартом.
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

    private val sampleIds = mutableMapOf<String, Int>()
    private val sampleToPath = mutableMapOf<Int, String>()
    private val loadedPaths = mutableSetOf<String>()
    private val failedPaths = mutableSetOf<String>()
    private val durations = mutableMapOf<String, Long>()
    private var pendingPath: String? = null
    private var focusRequest: AudioFocusRequest? = null
    private var activeStreamId = 0

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
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

    /** Предзагрузка на старте приложения, чтобы первый тап не был тихим. */
    fun preload(paths: Collection<String>) {
        paths.asSequence().filter { it.isNotBlank() }.distinct().forEach(::load)
    }

    fun play(path: String) {
        if (path.isBlank() || path in failedPaths) return
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
        if (path in sampleIds || path in failedPaths) return
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

    private fun playNow(sampleId: Int) {
        requestFocus()
        stop()
        activeStreamId = soundPool.play(sampleId, VOLUME, VOLUME, PRIORITY, 0, 1f)
    }

    /** Останавливает текущий звук. Нужен перед записью голоса в режиме «Эхо». */
    fun stop() {
        pendingPath = null
        if (activeStreamId != 0) {
            soundPool.stop(activeStreamId)
            activeStreamId = 0
        }
    }

    private fun requestFocus() {
        if (focusRequest != null) return
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .build()
        if (audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            focusRequest = request
        }
    }

    fun release() {
        stop()
        focusRequest?.let(audioManager::abandonAudioFocusRequest)
        focusRequest = null
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
