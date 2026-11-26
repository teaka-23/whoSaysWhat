package ru.tac.whosayswhat.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Каталог, из которого нельзя собрать раунд, должен показываться как ошибка:
 * SessionEngine требует минимум двух разных животных и иначе падает.
 */
class CatalogPlayableTest {

    @Test
    fun `two different animals are enough to play`() {
        assertTrue(Catalog(animals = listOf(animal("cow"), animal("cat"))).isPlayable())
    }

    @Test
    fun `single animal cannot form a round`() {
        assertFalse(Catalog(animals = listOf(animal("cow"))).isPlayable())
    }

    @Test
    fun `duplicate ids count as one animal`() {
        assertFalse(Catalog(animals = listOf(animal("cow"), animal("cow"))).isPlayable())
    }

    @Test
    fun `empty catalog is not playable`() {
        assertFalse(Catalog().isPlayable())
    }

    private fun animal(id: String) = Animal(id = id, name = id, soundFile = "sounds/$id.wav")
}
