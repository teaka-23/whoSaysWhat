package ru.tac.whosayswhat.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.tac.whosayswhat.assetFile
import ru.tac.whosayswhat.loadTestCatalog
import ru.tac.whosayswhat.pngHeaderOf

/**
 * Проверяет, что каталог в assets валиден и все заявленные звуки реально
 * лежат в APK. Ловит опечатки в путях до запуска на устройстве.
 */
class CatalogJsonTest {

    @Test
    fun `catalog parses and references existing sounds`() {
        val catalog = loadTestCatalog()

        assertTrue("В каталоге меньше 22 животных", catalog.animals.size >= 22)
        assertEquals(
            "id животных должны быть уникальны",
            catalog.animals.size,
            catalog.animals.map { it.id }.toSet().size,
        )

        catalog.animals.forEach { animal ->
            assertTrue("Пустой id", animal.id.isNotBlank())
            assertTrue("Пустое имя у ${animal.id}", animal.name.isNotBlank())
            assertTrue(
                "Пустое ласковое имя у ${animal.id}",
                !animal.affectionateName.isNullOrBlank(),
            )
            assertTrue("Нет картинки у ${animal.id}", !animal.image.isNullOrBlank())
            assertTrue(
                "Нет файла ${animal.soundFile} у ${animal.id}",
                assetFile(animal.soundFile).isFile,
            )
            animal.image?.let { path ->
                assertTrue("Нет картинки $path у ${animal.id}", assetFile(path).isFile)
            }
            animal.imageTransparent?.let { path ->
                assertTrue("Нет прозрачной картинки $path у ${animal.id}", assetFile(path).isFile)
            }
            animal.onomatopoeiaFile?.let { path ->
                assertTrue("Нет файла озвучки $path у ${animal.id}", assetFile(path).isFile)
            }
        }
    }

    @Test
    fun `artwork is 1024 square with transparent background`() {
        // 1024 — размер для крупного плана: на карточке картинка рисуется
        // мельче, но в увеличенном окне 512 размывалась бы на экранах с
        // высокой плотностью.
        val catalog = loadTestCatalog()
        val checked = mutableSetOf<String>()

        catalog.animals.forEach { animal ->
            val path = animal.image ?: return@forEach
            if (!checked.add(path)) return@forEach
            val header = pngHeaderOf(path)
            assertNotNull("$path не PNG или не читается", header)
            assertEquals("Ширина $path", 1024, header!!.width)
            assertEquals("Высота $path", 1024, header.height)
            assertTrue(
                "У $path нет альфа-канала (colorType=${header.colorType})",
                header.colorType == 6 || header.colorType == 4,
            )
        }

        assertTrue("Не проверено ни одной картинки", checked.isNotEmpty())
    }

    @Test
    fun `game sound falls back to animal sound`() {
        val plain = Animal(id = "x", name = "X", soundFile = "a.wav")
        assertEquals("a.wav", plain.gameSoundFile)

        val voiced = plain.copy(onomatopoeiaFile = "b.wav")
        assertEquals("b.wav", voiced.gameSoundFile)
    }
}
