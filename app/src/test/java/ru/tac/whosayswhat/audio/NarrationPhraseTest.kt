package ru.tac.whosayswhat.audio

import org.junit.Assert.assertEquals
import org.junit.Test

/** Фраза озвучки должна звучать тепло: «Это слоник!». */
class NarrationPhraseTest {

    @Test
    fun `adds exclamation and lowercases common noun`() {
        assertEquals("Это слоник!", narrationPhrase("Слоник"))
        assertEquals("Это петушок!", narrationPhrase("Петушок"))
        assertEquals("Это мышка!", narrationPhrase("Мышка"))
    }

    @Test
    fun `trims surrounding spaces and keeps inner words`() {
        assertEquals("Это морская свинка!", narrationPhrase("  Морская свинка  "))
    }

    @Test
    fun `blank name produces empty phrase`() {
        assertEquals("", narrationPhrase("   "))
    }
}
