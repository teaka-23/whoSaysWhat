package ru.tac.whosayswhat.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Ласковое имя используется только для озвучки. Карточки, игра и подписи
 * продолжают показывать каноническое название, чтобы ребёнок учил слово.
 */
class AnimalNarrationNameTest {

    private fun animal(name: String, affectionateName: String? = null) = Animal(
        id = "x",
        name = name,
        affectionateName = affectionateName,
        soundFile = "sounds/x.wav",
    )

    @Test
    fun `prefers affectionate name for narration`() {
        assertEquals("слоник", animal("Слон", "слоник").narrationName)
        assertEquals("кошечка", animal("Кошка", "  кошечка  ").narrationName)
    }

    @Test
    fun `falls back to canonical name when affectionate name is missing or blank`() {
        assertEquals("Слон", animal("Слон").narrationName)
        assertEquals("Слон", animal("Слон", "").narrationName)
        assertEquals("Слон", animal("Слон", "   ").narrationName)
    }
}
