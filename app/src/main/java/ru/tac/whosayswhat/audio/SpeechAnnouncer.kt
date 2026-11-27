package ru.tac.whosayswhat.audio

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

/**
 * Называет животное после того, как доиграл его звук: в песочнице ребёнок
 * слышит связку «образ — звук — название» и запоминает слово.
 *
 * Фраза строится как «Это слоник!»: ласковое имя из каталога делает её
 * теплее, а замедленная речь помогает ребёнку разобрать название почти по
 * слогам. Голос — системный синтезатор речи: приложение по-прежнему не
 * просит `INTERNET` и ничего не отправляет наружу. Если движка или русского
 * голоса в системе нет, озвучка тихо пропускается, а звук животного играет
 * как обычно. Новый тап отменяет и предыдущую фразу, и текущую.
 *
 * О конце фразы класс сообщает колбэком: по нему закрывается крупный план
 * животного. Колбэк приходит и тогда, когда фразы не будет вовсе, — иначе
 * увеличенная картинка ждала бы таймаута без причины.
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

    /**
     * Кого позвать, когда фраза закончилась (или стало ясно, что её не будет).
     * По этому сигналу крупный план животного закрывается: сам он не знает,
     * сколько продлится озвучка названия на конкретном голосе.
     */
    private var onPhraseFinished: (() -> Unit)? = null

    /** Сколько раз уже ждали поднимающийся движок для текущей фразы. */
    private var readyAttempts = 0

    /**
     * Номер текущей фразы: у каждого «Это кошечка!» свой идентификатор, иначе
     * запоздалый колбэк от прошлой фразы закрыл бы крупный план слишком рано.
     */
    private var utteranceSeq = 0
    private var currentUtteranceId: String? = null

    private val speakTask = object : Runnable {
        override fun run() {
            if (released) return
            val phrase = pendingPhrase ?: return
            val engine = tts ?: return
            if (!initialized) {
                // Движок ещё поднимается: фраза подождёт, звук уже играет.
                // Но ждём ограниченно — см. [shouldKeepWaitingForTts].
                if (!shouldKeepWaitingForTts(readyAttempts)) {
                    pendingPhrase = null
                    Log.w(TAG, "Синтезатор речи не поднялся, фраза пропущена")
                    finishPhrase()
                    return
                }
                readyAttempts++
                handler.postDelayed(this, TTS_READY_RETRY_MS)
                return
            }
            readyAttempts = 0
            if (!ready) {
                // Русского голоса в системе нет — фразы не будет.
                pendingPhrase = null
                finishPhrase()
                return
            }
            pendingPhrase = null
            val utteranceId = "$UTTERANCE_ID-${++utteranceSeq}"
            currentUtteranceId = utteranceId
            engine.speak(phrase, TextToSpeech.QUEUE_FLUSH, speakParams, utteranceId)
        }
    }

    init {
        tts = TextToSpeech(context.applicationContext, this)
        // Колбэк синтезатора приходит на binder-потоке: возвращаем его на
        // главный поток, чтобы крупный план закрывался в обычном порядке
        // Compose, а не из чужого потока.
        runCatching {
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    if (isCurrentUtterance(utteranceId)) handler.post { finishPhrase() }
                }

                override fun onError(utteranceId: String?) {
                    if (isCurrentUtterance(utteranceId)) handler.post { finishPhrase() }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    if (isCurrentUtterance(utteranceId)) handler.post { finishPhrase() }
                }
            })
        }.onFailure { error ->
            Log.w(TAG, "Не удалось подписаться на окончание фразы", error)
        }
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
     * Планирует «Это <ласковое имя>!» сразу после звука и короткой паузы.
     * Если ребёнок нажал другую карточку, предыдущая фраза отменяется.
     *
     * [onFinished] вызывается, когда фраза договорена, и в тех случаях, когда
     * её не будет вовсе (нет движка, нет русского голоса, синтез не удался).
     * Крупный план животного закрывается именно по этому сигналу, поэтому
     * «тихий пропуск» озвучки не должен оставлять картинку на экране.
     */
    fun announceAfter(
        soundDurationMs: Long,
        narrationName: String,
        onFinished: (() -> Unit)? = null,
    ) {
        // Прошлое ожидание больше не нужно: его ожидающий не должен ждать
        // таймаута, если новый тап перекрыл фразу.
        finishPhrase()
        if (narrationName.isBlank() || released) {
            onFinished?.invoke()
            return
        }
        val engine = tts
        if (engine == null) {
            onFinished?.invoke()
            return
        }
        handler.removeCallbacks(speakTask)
        pendingPhrase = narrationPhrase(narrationName)
        onPhraseFinished = onFinished
        readyAttempts = 0
        engine.stop()
        handler.postDelayed(speakTask, narrationDelayMs(soundDurationMs))
    }

    /** Отменяет отложенную фразу и обрывает текущую. */
    fun stop() {
        pendingPhrase = null
        handler.removeCallbacks(speakTask)
        tts?.stop()
        // Ожидающий крупный план закрывается: фразы не будет.
        finishPhrase()
    }

    fun release() {
        if (released) return
        released = true
        stop()
        tts?.shutdown()
        tts = null
        ready = false
    }

    /**
     * Колбэк синтезатора относится к текущей фразе? Запоздалый «готово» от
     * прошлой фразы не должен закрывать крупный план новой.
     */
    private fun isCurrentUtterance(utteranceId: String?): Boolean =
        utteranceId != null && utteranceId == currentUtteranceId

    /** Сообщает ожидающему, что фраза закончилась или не состоится. */
    private fun finishPhrase() {
        val callback = onPhraseFinished ?: return
        onPhraseFinished = null
        callback()
    }

    private companion object {
        val RUSSIAN: Locale = Locale.forLanguageTag("ru-RU")
        const val TAG = "SpeechAnnouncer"
        const val UTTERANCE_ID = "animal-intro"
        // Заметно медленнее обычного: название звучит почти по слогам,
        // но фраза не растягивается до невнятности.
        const val SPEECH_RATE = 0.70f
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
 * Тёплая фраза для озвучки: «Это слоник!», «Это петушок!». Названия в
 * каталоге — нарицательные, поэтому после «это» они пишутся со строчной
 * буквы. Восклицание помогает синтезатору сказать фразу живее.
 */
internal fun narrationPhrase(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return ""
    return "Это " + trimmed.lowercase(Locale.ROOT) + "!"
}
