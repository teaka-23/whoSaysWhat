package ru.tac.whosayswhat.audio

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

/**
 * Называет животное после того, как доиграл его звук: в песочнице ребёнок
 * слышит связку «образ — звук — название» и запоминает слово.
 *
 * Фраза строится как «Это слон»: так она звучит по-человечески и мягче,
 * чем одно слово. Голос — системный синтезатор речи: приложение по-прежнему
 * не просит `INTERNET` и ничего не отправляет наружу. Если движка или русского
 * голоса в системе нет, озвучка тихо пропускается, а звук животного играет как
 * обычно. Новый тап отменяет и предыдущую фразу, и текущую.
 */
class SpeechAnnouncer(context: Context) : TextToSpeech.OnInitListener {

    private val handler = Handler(Looper.getMainLooper())
    // Просим движок синтезировать без сети: у приложения нет INTERNET,
    // а название должно звучать и в самолётном режиме.
    private val speakParams = Bundle().apply {
        putString(TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS, "true")
    }
    private var tts: TextToSpeech? = null
    private var pendingPhrase: String? = null
    private var initialized = false
    private var ready = false
    private var released = false

    private val speakTask = object : Runnable {
        override fun run() {
            if (released) return
            val phrase = pendingPhrase ?: return
            val engine = tts ?: return
            if (!initialized) {
                // Движок ещё поднимается: фраза подождёт, звук уже играет.
                handler.postDelayed(this, READY_RETRY_MS)
                return
            }
            if (!ready) {
                pendingPhrase = null
                return
            }
            pendingPhrase = null
            engine.speak(phrase, TextToSpeech.QUEUE_FLUSH, speakParams, UTTERANCE_ID)
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
        // Среди офлайн-голосов предпочитаем женский: он обычно звучит мягче
        // и дружелюбнее для ребёнка. Если такого нет — остаётся лучший по
        // качеству.
        runCatching {
            engine.voices
                ?.asSequence()
                ?.filter { voice ->
                    voice.locale.language == RUSSIAN.language &&
                        !voice.isNetworkConnectionRequired
                }
                ?.maxByOrNull(::softVoiceRank)
                ?.let { offlineVoice -> engine.voice = offlineVoice }
        }.onFailure { error ->
            // Сторонний движок может не отдать список голосов — остаётся
            // язык, выставленный через setLanguage().
            Log.w(TAG, "Не удалось выбрать офлайн-голос", error)
        }
        // Звук названия идёт через медиапоток, как и записи животных:
        // громкость регулируется теми же кнопками, а не отдельным каналом
        // специальных возможностей.
        engine.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        engine.setSpeechRate(SPEECH_RATE)
        engine.setPitch(SPEECH_PITCH)
        Log.i(TAG, "Голос: ${engine.voice?.name ?: "по умолчанию"}")
        ready = true
    }

    /**
     * Планирует «Это <животное>» сразу после его звука и короткой паузы.
     * Если ребёнок нажал другую карточку, предыдущая фраза отменяется.
     */
    fun announceAfter(soundDurationMs: Long, name: String) {
        if (name.isBlank() || released) return
        val engine = tts ?: return
        handler.removeCallbacks(speakTask)
        pendingPhrase = narrationPhrase(name)
        engine.stop()
        handler.postDelayed(speakTask, narrationDelayMs(soundDurationMs))
    }

    /** Отменяет отложенную фразу и обрывает текущую. */
    fun stop() {
        pendingPhrase = null
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
        const val UTTERANCE_ID = "animal-intro"
        const val READY_RETRY_MS = 150L
        const val SPEECH_RATE = 0.92f
        const val SPEECH_PITCH = 1.08f

        /**
         * Мягкий голос важнее «самого нового»: у Google TTS женский русский
         * голос обычно содержит `ruf`, мужской — `ruc`. Если движок называет
         * голоса иначе, решают качество и офлайн-доступность.
         */
        fun softVoiceRank(voice: Voice): Int {
            val name = voice.name.lowercase(Locale.ROOT)
            var score = voice.quality
            if ("ruf" in name || "female" in name || "woman" in name) score += 120
            if ("ruc" in name || "male" in name || "man" in name) score -= 40
            if ("local" in name || "offline" in name) score += 10
            return score
        }
    }
}

/**
 * Фраза для озвучки: «Это слон», «Это петух». Названия в каталоге —
 * нарицательные, поэтому после «Это» они пишутся со строчной буквы.
 */
internal fun narrationPhrase(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return ""
    return "Это " + trimmed.lowercase(Locale.ROOT)
}
