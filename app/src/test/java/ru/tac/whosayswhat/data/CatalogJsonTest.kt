package ru.tac.whosayswhat.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Проверяет, что каталог в assets валиден и все заявленные звуки реально
 * лежат в APK. Ловит опечатки в путях до запуска на устройстве.
 */
class CatalogJsonTest {

    private val assetsDir = File("src/main/assets")
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `catalog parses and references existing sounds`() {
        val catalog = json.decodeFromString<Catalog>(
            File(assetsDir, "catalog.json").readText(),
        )

        assertTrue("В каталоге меньше двух животных", catalog.animals.size >= 2)
        assertEquals(
            "id животных должны быть уникальны",
            catalog.animals.size,
            catalog.animals.map { it.id }.toSet().size,
        )

        catalog.animals.forEach { animal ->
            assertTrue("Пустой id", animal.id.isNotBlank())
            assertTrue("Пустое имя у ${animal.id}", animal.name.isNotBlank())
            assertTrue(
                "Нет файла ${animal.soundFile} у ${animal.id}",
                File(assetsDir, animal.soundFile).isFile,
            )
            animal.image?.let { path ->
                assertTrue("Нет картинки $path у ${animal.id}", File(assetsDir, path).isFile)
            }
            animal.imageTransparent?.let { path ->
                assertTrue("Нет прозрачной картинки $path у ${animal.id}", File(assetsDir, path).isFile)
            }
            animal.onomatopoeiaFile?.let { path ->
                assertTrue("Нет файла озвучки $path у ${animal.id}", File(assetsDir, path).isFile)
            }
        }
    }

    @Test
    fun `game sound falls back to animal sound`() {
        val plain = Animal(id = "x", name = "X", soundFile = "a.wav")
        assertEquals("a.wav", plain.gameSoundFile)

        val voiced = plain.copy(onomatopoeiaFile = "b.wav")
        assertEquals("b.wav", voiced.gameSoundFile)
    }
}
