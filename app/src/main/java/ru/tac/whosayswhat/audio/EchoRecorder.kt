package ru.tac.whosayswhat.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File

/** Максимальная длина записи в режиме «Эхо». */
const val ECHO_MAX_DURATION_MS = 5_000L

/** Проверка разрешения на микрофон: используется и в UI, и перед стартом записи. */
fun Context.hasRecordAudioPermission(): Boolean =
    ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

/**
 * Запись короткого «эха» ребёнка и воспроизведение её обратно.
 *
 * Файл создаётся только в cacheDir, никуда не отправляется и удаляется при
 * закрытии диалога. Если процесс завершился аварийно, [cleanup] удалит
 * оставшиеся файлы при следующем запуске. Приложение не имеет разрешения
 * INTERNET, поэтому отправка записи технически невозможна.
 */
class EchoRecorder(context: Context) {

    private val appContext = context.applicationContext
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var outputFile: File? = null

    var isRecording: Boolean = false
        private set

    /** Начинает запись. Возвращает false, если нет разрешения или микрофон занят. */
    @SuppressLint("MissingPermission") // разрешение проверяется ниже и запрашивается в UI
    fun start(): Boolean {
        if (isRecording) return true
        if (!appContext.hasRecordAudioPermission()) return false

        stopPlayback()
        releaseRecorder()
        outputFile?.delete()
        outputFile = null

        val dir = echoDir()
        // На старте записи в папке не должно быть чужих/старых файлов:
        // после аварийного завершения процесса они могли остаться.
        EchoFiles.deleteAll(dir)
        val file = File(dir, "echo_${System.currentTimeMillis()}.m4a")
        val mediaRecorder = createRecorder()

        return try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setAudioChannels(1)
            mediaRecorder.setAudioSamplingRate(44_100)
            mediaRecorder.setAudioEncodingBitRate(96_000)
            mediaRecorder.setMaxDuration(ECHO_MAX_DURATION_MS.toInt())
            mediaRecorder.setOutputFile(file.absolutePath)
            mediaRecorder.prepare()
            mediaRecorder.start()

            recorder = mediaRecorder
            outputFile = file
            isRecording = true
            true
        } catch (error: Exception) {
            Log.w(TAG, "Не удалось начать запись", error)
            runCatching { mediaRecorder.release() }
            file.delete()
            false
        }
    }

    /**
     * Останавливает запись. Возвращает файл или null, если запись слишком
     * короткая/не началась. Файл живёт до [release] или следующего старта.
     */
    fun stop(): File? {
        val mediaRecorder = recorder ?: return null
        recorder = null
        isRecording = false
        val file = outputFile

        val stopped = runCatching { mediaRecorder.stop() }
            .onFailure { Log.w(TAG, "Запись не сохранилась (слишком короткая?)", it) }
            .isSuccess
        runCatching { mediaRecorder.release() }

        return if (stopped && file?.isFile == true) {
            file
        } else {
            file?.delete()
            outputFile = null
            null
        }
    }

    /** Воспроизводит запись. [onFinished] вызывается на главном потоке. */
    fun play(file: File, onFinished: () -> Unit = {}): Boolean {
        stopPlayback()
        if (!file.isFile) return false

        val mediaPlayer = MediaPlayer()
        return try {
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            mediaPlayer.setDataSource(file.absolutePath)
            mediaPlayer.setOnCompletionListener {
                player = null
                it.release()
                onFinished()
            }
            mediaPlayer.setOnErrorListener { failed, what, extra ->
                Log.w(TAG, "Ошибка воспроизведения what=$what extra=$extra")
                player = null
                failed.release()
                onFinished()
                true
            }
            mediaPlayer.prepare()
            mediaPlayer.start()
            player = mediaPlayer
            true
        } catch (error: Exception) {
            Log.w(TAG, "Не удалось воспроизвести запись", error)
            runCatching { mediaPlayer.release() }
            false
        }
    }

    fun stopPlayback() {
        player?.let { mediaPlayer ->
            player = null
            runCatching { if (mediaPlayer.isPlaying) mediaPlayer.stop() }
            runCatching { mediaPlayer.release() }
        }
    }

    /** Полная очистка: останавливает всё и удаляет запись. */
    fun release() {
        releaseRecorder()
        stopPlayback()
        outputFile?.delete()
        outputFile = null
    }

    private fun releaseRecorder() {
        recorder?.let { mediaRecorder ->
            runCatching { mediaRecorder.stop() }
            runCatching { mediaRecorder.release() }
        }
        recorder = null
        isRecording = false
    }

    private fun createRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(appContext)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

    private fun echoDir(): File = echoDir(appContext)

    companion object {
        /**
         * Удаляет все временные записи. Вызывается при старте приложения и при
         * выключении «Эхо»: [release] из onDispose не выполняется, если процесс
         * был убит системой.
         */
        fun cleanup(context: Context): Int =
            EchoFiles.deleteAll(echoDir(context.applicationContext))

        private fun echoDir(context: Context): File =
            File(context.cacheDir, "echo").apply { mkdirs() }

        private const val TAG = "EchoRecorder"
    }
}
