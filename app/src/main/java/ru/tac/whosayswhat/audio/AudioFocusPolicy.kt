package ru.tac.whosayswhat.audio

/**
 * Хвост после расчётного конца звука: держим фокус ещё немного, чтобы короткая
 * пауза между «му» и названием не отдавала фокус туда-обратно.
 */
internal const val AUDIO_FOCUS_TAIL_MS = 200L

/**
 * Запасное удержание, если длительность записи неизвестна: SoundPool не
 * сообщает конец воспроизведения, а WAV-заголовок может не читаться.
 */
internal const val AUDIO_FOCUS_UNKNOWN_MS = 2_000L

/** Сколько держать фокус после старта звука [durationMs]. */
internal fun audioFocusHoldMs(durationMs: Long): Long =
    (if (durationMs > 0L) durationMs else AUDIO_FOCUS_UNKNOWN_MS) + AUDIO_FOCUS_TAIL_MS

/**
 * Учёт «наш ли сейчас аудиофокус».
 *
 * Фокус запрашивается на время (`AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`) и должен
 * возвращаться, как только звук доиграл: иначе приложение приглушает чужую
 * музыку до самого закрытия. Класс не знает про Android — только про сроки,
 * поэтому проверяется обычными unit-тестами.
 */
internal class AudioFocusHold {

    /** Фокус у нас: следующий звук не должен запрашивать его заново. */
    var isHeld: Boolean = false
        private set

    /** Когда пора вернуть фокус; null — звук ещё не стартовал. */
    private var releaseAtMs: Long? = null

    fun needsRequest(): Boolean = !isHeld

    fun onFocusGranted() {
        isHeld = true
    }

    /** Новый звук сдвигает срок возврата фокуса. */
    fun onPlaybackStarted(nowMs: Long, durationMs: Long) {
        releaseAtMs = nowMs + audioFocusHoldMs(durationMs)
    }

    fun shouldRelease(nowMs: Long): Boolean {
        val releaseAt = releaseAtMs ?: return false
        return nowMs >= releaseAt
    }

    /** Сколько ещё ждать возврата фокуса; 0 — возвращать нечего. */
    fun msUntilRelease(nowMs: Long): Long {
        val releaseAt = releaseAtMs ?: return 0L
        return (releaseAt - nowMs).coerceAtLeast(0L)
    }

    /** Фокус вернули сами. */
    fun onReleased() {
        isHeld = false
        releaseAtMs = null
    }

    /** Фокус забрали (звонок, другое приложение) — вернуть его мы уже не можем. */
    fun onFocusLost() {
        onReleased()
    }
}
