package ru.tac.whosayswhat.ui

/**
 * Пауза после верного ответа: ребёнок успевает увидеть подсветку карточки,
 * прежде чем появится новый раунд.
 */
internal const val CORRECT_ANSWER_PAUSE_MS = 900L

/**
 * Что делать после верного ответа.
 *
 * Похвалу играет экран награды, поэтому последний верный ответ завершает
 * сессию сразу. Если бы игра в этот момент ещё и выдерживала паузу со своим
 * `praise.wav`, ребёнок услышал бы один и тот же файл дважды подряд с обрывом.
 */
internal data class CorrectAnswerStep(
    val finishSession: Boolean,
    val pauseMs: Long,
)

internal fun correctAnswerStep(
    solved: Int,
    roundsPerSession: Int = ROUNDS_PER_SESSION,
): CorrectAnswerStep =
    if (solved >= roundsPerSession) {
        CorrectAnswerStep(finishSession = true, pauseMs = 0L)
    } else {
        CorrectAnswerStep(finishSession = false, pauseMs = CORRECT_ANSWER_PAUSE_MS)
    }
