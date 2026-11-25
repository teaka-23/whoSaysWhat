package ru.tac.whosayswhat.audio

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Называет животное после того, как доиграл его звук: в песочнице ребёнок
 * слышит связку «образ — звук — название» и запоминает слово.
 *
 * Голос — системный синтезатор речи: приложение по-прежнему не просит
 * `INTERNET` и ничего не отправляет наружу. Если движка или русского голоса
 * в системе нет, озвучка тихо пропускается, а звук животного играет как
 * обычно. Новый тап отменяет и предыдущее название, и текущую фразу.
 */
class SpeechAnnouncer(context: Context) : TextToSpeech.OnInitListener {

    private val handler = Handler(Looper.getMainLooper())
    // Просим движок синтезировать без сети: у приложения нет INTERNET,
    // а название должно звучать и в самолётном режиме.
    private val speakParams = Bundle().apply {
        putString(TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS, "true")
    }
    private var tts: TextToSpeech? = null
    private var pendingName: String? = null
    private var initialized = false
    private var ready = false
    private var released = false

    private val speakTask = object : Runnable {
        override fun run() {
            val name = pendingName ?: return
            val engine = tts ?: return
            if (!initialized) {
                // Движок ещё поднимается: название подождёт, звук уже играет.
                handler.postDelayed(this, READY_RETRY_MS)
                return
            }
            if (!ready) {
                pendingName = null
                return
            }
            pendingName = null
            engine.speak(name, TextToSpeech.QUEUE_FLUSH, speakParams, UTTERANCE_ID)
        }
    }

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        initialized = true
        val engine = tts ?: return
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "Синтезатор речи недоступен (status=$status)")
            return
        }
        val language = engine.setLanguage(RUSSIAN)
        val supported = language == TextToSpeech.LANG_AVAILABLE ||
            language == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
            language == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
        if (!supported) {
            Log.w(TAG, "Русский голос не установлен (result=$language)")
            return
        }
        // Если движок предлагает офлайн-голос, берём его: сетевой голос
        // в самолётном режиме промолчит, а embedded-синтез работает всегда.
        runCatching {
            engine.voices
                ?.filter { voice ->
                    voice.locale.language == RUSSIAN.language &&
                        !voice.isNetworkConnectionRequired
                }
                ?.maxByOrNull { voice -> voice.quality }
                ?.let { offlineVoice -> engine.voice = offlineVoice }
        }.onFailure { error ->
            // Сторонний движок может не отдать список голосов — остаётся
            // язык, выставленный через setLanguage().
            Log.w(TAG, "Не удалось выбрать офлайн-голос", error)
        }
        engine.setSpeechRate(SPEECH_RATE)
        engine.setPitch(SPEECH_PITCH)
        ready = true
    }

    /**
     * Планирует название животного сразу после его звука и короткой паузы.
     * Если ребёнок нажал другую карточку, предыдущее название отменяется.
     */
    fun announceAfter(soundDurationMs: Long, name: String) {
        if (name.isBlank() || released) return
        val engine = tts ?: return
        handler.removeCallbacks(speakTask)
        pendingName = name
        engine.stop()
        handler.postDelayed(speakTask, narrationDelayMs(soundDurationMs))
    }

    /** Отменяет отложенное название и обрывает текущую фразу. */
    fun stop() {
        pendingName = null
        handler.removeCallbacks(speakTask)
        tts?.stop()
    }

    fun release() {
        if (released) return
        released = true
        stop()
        tts?.shutdown()
        tts = null
        ready = false
    }

    private companion object {
        val RUSSIAN: Locale = Locale.forLanguageTag("ru-RU")
        const val TAG = "SpeechAnnouncer"
        const val UTTERANCE_ID = "animal-name"
        const val READY_RETRY_MS = 150L
        const val SPEECH_RATE = 0.9f
        const val SPEECH_PITCH = 1.0f
    }
}
