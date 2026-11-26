package ru.tac.whosayswhat.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Проверяет подсветку карточек в игре «Угадай»: верный ответ должен быть виден
 * до перехода к следующему раунду, но подсвечена всегда не больше одной
 * карточки — иначе ребёнок видит подряд два «правильных».
 */
class GuessFeedbackTest {

    @Test
    fun `correct card is highlighted`() {
        assertEquals(
            GuessFeedback.Correct,
            guessFeedback(animalId = "cow", correctId = "cow", wrongId = null),
        )
    }

    @Test
    fun `wrong card only shakes`() {
        assertEquals(
            GuessFeedback.Wrong,
            guessFeedback(animalId = "cat", correctId = null, wrongId = "cat"),
        )
    }

    @Test
    fun `untouched card has no feedback`() {
        assertEquals(
            GuessFeedback.None,
            guessFeedback(animalId = "cat", correctId = "cow", wrongId = "dog"),
        )
    }

    @Test
    fun `correct wins when both ids point at the same card`() {
        assertEquals(
            GuessFeedback.Correct,
            guessFeedback(animalId = "cow", correctId = "cow", wrongId = "cow"),
        )
    }

    @Test
    fun `no ids means no feedback`() {
        assertEquals(
            GuessFeedback.None,
            guessFeedback(animalId = "cow", correctId = null, wrongId = null),
        )
    }
}
