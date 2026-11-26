package ru.tac.whosayswhat.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Проверяет, как игра «Угадай» завершает ответ.
 *
 * Похвалу играет экран награды, поэтому последний верный ответ не должен ещё
 * и выдерживать паузу с тем же `praise.wav` в игре: иначе один и тот же файл
 * звучит дважды подряд с обрывом.
 */
class GuessSessionTest {

    @Test
    fun `correct answer before the limit moves to the next round`() {
        val step = correctAnswerStep(solved = 1)

        assertFalse(step.finishSession)
        assertEquals(CORRECT_ANSWER_PAUSE_MS, step.pauseMs)
    }

    @Test
    fun `pause keeps the highlight visible before the next round`() {
        // Подсветка верной карточки имеет смысл только если её успели увидеть.
        assertTrue(correctAnswerStep(solved = 1).pauseMs > 0L)
    }

    @Test
    fun `last correct answer finishes the session`() {
        assertTrue(correctAnswerStep(solved = ROUNDS_PER_SESSION).finishSession)
    }

    @Test
    fun `finished session does not pause before the reward`() {
        // Награда открывается сразу и сама играет похвалу — паузы быть не
        // должно, иначе ребёнок слышит тишину, а потом тот же файл.
        assertEquals(0L, correctAnswerStep(solved = ROUNDS_PER_SESSION).pauseMs)
    }

    @Test
    fun `single round session finishes on the first answer`() {
        assertTrue(correctAnswerStep(solved = 1, roundsPerSession = 1).finishSession)
    }

    @Test
    fun `answers above the limit still finish`() {
        assertTrue(correctAnswerStep(solved = ROUNDS_PER_SESSION + 1).finishSession)
    }

    @Test
    fun `default limit matches the session length`() {
        assertFalse(correctAnswerStep(solved = ROUNDS_PER_SESSION - 1).finishSession)
    }
}
