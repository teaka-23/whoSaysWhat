package ru.tac.whosayswhat.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Кэш иллюстраций: сколько памяти ему отдавать и как приводить путь из
 * каталога к имени файла в assets.
 *
 * Без кэша каждая прокрутка песочницы декодировала PNG заново: 512×512
 * в ARGB_8888 — это 1 МБ на картинку и заметные подвисания на бюджетном
 * телефоне. Но и «сколько дать» нельзя брать с потолка: на слабом устройстве
 * кэш не должен съедать всю кучу.
 */
class AnimalImagesTest {

    @Test
    fun `cache takes an eighth of the heap`() {
        assertEquals(32 * 1024 * 1024, imageCacheBytes(256L * 1024 * 1024))
    }

    @Test
    fun `small heap still gets eight megabytes`() {
        assertEquals(8 * 1024 * 1024, imageCacheBytes(16L * 1024 * 1024))
    }

    @Test
    fun `huge heap does not get more than sixty-four megabytes`() {
        assertEquals(64 * 1024 * 1024, imageCacheBytes(4L * 1024 * 1024 * 1024))
    }

    @Test
    fun `catalog path loses the assets prefix`() {
        assertEquals("images/cow_512.png", animalAssetPath("assets/images/cow_512.png"))
    }

    @Test
    fun `path without a prefix stays as is`() {
        assertEquals("images/cow_512.png", animalAssetPath("images/cow_512.png"))
    }

    @Test
    fun `blank path means no artwork`() {
        assertNull(animalAssetPath(null))
        assertNull(animalAssetPath(""))
        assertNull(animalAssetPath("   "))
        assertNull(animalAssetPath("assets/"))
    }
}
