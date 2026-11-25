package ru.tac.whosayswhat.audio

import org.junit.Assert.assertEquals
import org.junit.Test

/** Фраза озвучки должна звучать по-человечески: «Это слон», а не «Слон». */
class NarrationPhraseTest {

    @Test
    fun `adds friendly prefix and lowercases common noun`() {
        assertEquals("Это слон", narrationPhrase("Слон"))
        assertEquals("Это петух", narrationPhrase("Петух"))
        assertEquals("Это мышка", narrationPhrase("Мышка"))
    }

    @Test
    fun `trims surrounding spaces and keeps inner words`() {
        assertEquals("Это морская свинка", narrationPhrase("  Морская свинка  "))
    }

    @Test
    fun `blank name produces empty phrase`() {
        assertEquals("", narrationPhrase("   "))
    }
}
