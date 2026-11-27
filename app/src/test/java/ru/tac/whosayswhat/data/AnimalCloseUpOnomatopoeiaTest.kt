package ru.tac.whosayswhat.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Подпись «Му-у-у!» в крупном плане животного. Полное звукоподражание
 * читается ребёнку вслух взрослым, поэтому оно берётся из каталога как есть;
 * короткое «Му» — только запасной вариант, а пустая строка не должна
 * превращаться в пустое место под названием.
 */
class AnimalCloseUpOnomatopoeiaTest {

    private fun animal(
        onomatopoeia: String = "",
        simple: String? = null,
    ) = Animal(
        id = "x",
        name = "Корова",
        soundFile = "sounds/x.wav",
        onomatopoeiaText = onomatopoeia,
        simpleOnomatopoeia = simple,
    )

    @Test
    fun `uses the full onomatopoeia from the catalog`() {
        assertEquals("Му-у-у!", animal("Му-у-у!", "Му").closeUpOnomatopoeia)
    }

    @Test
    fun `falls back to the simple onomatopoeia`() {
        assertEquals("Му", animal(simple = "Му").closeUpOnomatopoeia)
        assertEquals("Му", animal("   ", "  Му  ").closeUpOnomatopoeia)
    }

    @Test
    fun `blank onomatopoeia is not shown at all`() {
        assertNull(animal().closeUpOnomatopoeia)
        assertNull(animal("   ", "  ").closeUpOnomatopoeia)
    }
}
