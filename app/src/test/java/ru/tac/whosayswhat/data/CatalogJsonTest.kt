package ru.tac.whosayswhat.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

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
    fun `artwork is 1024 square with transparent background`() {
        // 1024 — размер для крупного плана: на карточке картинка рисуется
        // мельче, но в увеличенном окне 512 размывалась бы на экранах с
        // высокой плотностью.
        val catalog = json.decodeFromString<Catalog>(
            File(assetsDir, "catalog.json").readText(),
        )
        val checked = mutableSetOf<String>()

        val pngSignature = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        )

        catalog.animals.forEach { animal ->
            val path = animal.image ?: return@forEach
            if (!checked.add(path)) return@forEach
            val bytes = File(assetsDir, path).readBytes()
            assertTrue("$path не PNG", bytes.size > 29)
            assertTrue(
                "$path не PNG",
                bytes.copyOfRange(0, 8).contentEquals(pngSignature),
            )
            val header = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            assertEquals("Ширина $path", 1024, header.getInt(16))
            assertEquals("Высота $path", 1024, header.getInt(20))
            val colorType = bytes[25].toInt() and 0xFF
            assertTrue(
                "У $path нет альфа-канала (colorType=$colorType)",
                colorType == 6 || colorType == 4,
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
