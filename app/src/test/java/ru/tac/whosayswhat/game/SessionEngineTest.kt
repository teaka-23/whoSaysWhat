package ru.tac.whosayswhat.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.tac.whosayswhat.data.Animal
import kotlin.math.abs
import kotlin.random.Random

class SessionEngineTest {

    private fun animal(id: String) = Animal(
        id = id,
        name = id,
        soundFile = "sounds/$id.wav",
    )

    private fun pool(size: Int = 6) = List(size) { animal("animal-$it") }

    @Test
    fun `round contains target and unique options`() {
        val engine = SessionEngine(pool(), requestedOptions = 2, random = Random(1))

        repeat(50) {
            val round = engine.start()
            assertEquals(2, round.options.size)
            assertEquals(round.options.size, round.options.map { it.id }.toSet().size)
            assertTrue(round.options.contains(round.target))
            engine.answer(round.target.id)
        }
    }

    @Test
    fun `wrong answer does not advance the round`() {
        val engine = SessionEngine(pool(), requestedOptions = 2, random = Random(2))
        val round = engine.start()
        val wrong = round.options.first { it.id != round.target.id }

        val result = engine.answer(wrong.id)

        assertTrue(result is Answer.Wrong)
        assertSame(round, engine.current)
        assertEquals(0, engine.correctCount)
    }

    @Test
    fun `correct answer advances to a new round`() {
        val engine = SessionEngine(pool(), requestedOptions = 2, random = Random(3))
        val round = engine.start()

        val result = engine.answer(round.target.id)

        assertTrue(result is Answer.Correct)
        assertNull(engine.current)
        assertEquals(1, engine.correctCount)

        val next = engine.start()
        assertEquals(2, next.options.size)
        assertTrue(next.options.contains(next.target))
    }

    @Test
    fun `target does not repeat twice in a row`() {
        val engine = SessionEngine(pool(), requestedOptions = 2, random = Random(4))
        val targets = mutableListOf<String>()

        repeat(30) {
            val round = engine.start()
            targets += round.target.id
            engine.answer(round.target.id)
        }

        targets.zipWithNext().forEach { (first, second) ->
            assertTrue("Правильный ответ повторился: $first -> $second", first != second)
        }
    }

    @Test
    fun `target position is balanced`() {
        val engine = SessionEngine(pool(), requestedOptions = 2, random = Random(5))
        val positions = IntArray(2)

        repeat(40) {
            val round = engine.start()
            positions[round.targetPosition]++
            engine.answer(round.target.id)
        }

        assertTrue("Позиции перекошены: ${positions.toList()}", abs(positions[0] - positions[1]) <= 1)
    }

    @Test
    fun `option set changes between rounds when pool allows`() {
        val engine = SessionEngine(pool(6), requestedOptions = 2, random = Random(6))
        val sets = mutableSetOf<Set<String>>()

        repeat(10) {
            val round = engine.start()
            sets += round.options.map { it.id }.toSet()
            engine.answer(round.target.id)
        }

        assertTrue("Набор карточек не менялся", sets.size > 1)
    }

    @Test
    fun `mastered requires two correct answers`() {
        val engine = SessionEngine(pool(2), requestedOptions = 2, random = Random(7))

        repeat(6) {
            val round = engine.start()
            engine.answer(round.target.id)
        }

        assertEquals(setOf("animal-0", "animal-1"), engine.masteredIds(minCorrect = 2))
    }
}
