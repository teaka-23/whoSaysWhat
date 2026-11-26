package ru.tac.whosayswhat.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Что TalkBack читает на карточке животного.
 *
 * Без этой проверки легко вернуться к прежнему поведению, где подпись кнопки
 * склеивалась с именем («Выбрать Корова» — с ошибкой в падеже), а подсветка
 * менялась молча: слабовидящий ребёнок не узнавал, верный ли был ответ.
 */
class GuessCardSemanticsTest {

    @Test
    fun `name is announced apart from the action`() {
        val semantics = guessCardSemantics(name = "Корова", feedback = GuessFeedback.None)

        assertEquals("Корова", semantics.name)
    }

    @Test
    fun `idle card announces nothing`() {
        val semantics = guessCardSemantics(name = "Корова", feedback = GuessFeedback.None)

        assertNull(semantics.outcome)
        assertFalse(semantics.announceOutcome)
    }

    @Test
    fun `correct answer is announced`() {
        val semantics = guessCardSemantics(name = "Корова", feedback = GuessFeedback.Correct)

        assertEquals("Верно", semantics.outcome)
        assertTrue(semantics.announceOutcome)
    }

    @Test
    fun `wrong answer is announced gently`() {
        val semantics = guessCardSemantics(name = "Корова", feedback = GuessFeedback.Wrong)

        assertEquals("Не то, попробуй ещё", semantics.outcome)
        assertTrue(semantics.announceOutcome)
    }
}
