package ru.tac.whosayswhat.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimalProgressTest {

    @Test
    fun `new animal starts without stars`() {
        val progress = AnimalProgress.EMPTY

        assertEquals(0, progress.attempts)
        assertEquals(0, progress.correct)
        assertEquals(0, progress.stars)
        assertEquals(0f, progress.accuracy, 0.001f)
        assertFalse(progress.isMastered)
    }

    @Test
    fun `wrong answer counts attempt but not correct`() {
        val progress = AnimalProgress.EMPTY.withAnswer(correctAnswer = false)

        assertEquals(1, progress.attempts)
        assertEquals(0, progress.correct)
        assertEquals(0, progress.stars)
    }

    @Test
    fun `stars grow at one three and five correct answers`() {
        assertEquals(0, progressWith(correct = 0).stars)
        assertEquals(1, progressWith(correct = 1).stars)
        assertEquals(1, progressWith(correct = 2).stars)
        assertEquals(2, progressWith(correct = 3).stars)
        assertEquals(2, progressWith(correct = 4).stars)
        assertEquals(3, progressWith(correct = 5).stars)
        assertEquals(3, progressWith(correct = 9).stars)
    }

    @Test
    fun `mastered after three correct answers`() {
        assertFalse(progressWith(correct = 2).isMastered)
        assertTrue(progressWith(correct = 3).isMastered)
    }

    @Test
    fun `accuracy accounts for all attempts`() {
        val progress = AnimalProgress(attempts = 4, correct = 1)

        assertEquals(0.25f, progress.accuracy, 0.001f)
    }

    private fun progressWith(correct: Int): AnimalProgress =
        AnimalProgress(attempts = correct, correct = correct)
}
