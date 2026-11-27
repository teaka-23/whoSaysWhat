package ru.tac.whosayswhat.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.tac.whosayswhat.assetFile
import ru.tac.whosayswhat.audio.SPOTLIGHT_MIN_MS
import ru.tac.whosayswhat.audio.spotlightFloorMs
import ru.tac.whosayswhat.audio.spotlightMaxWaitMs
import ru.tac.whosayswhat.audio.wavDurationMs
import ru.tac.whosayswhat.loadTestCatalog
import ru.tac.whosayswhat.pngHeaderOf
import java.io.File
import java.security.MessageDigest

/**
 * Крупный план доступен у каждого животного, а не только у коровы со
 * скриншота.
 *
 * Окно собирается из трёх частей: увеличенная иллюстрация, подпись
 * («Му-у-у!») и звук. Не хватает любой — ребёнок видит пустую карточку или не
 * слышит голос, а заметно это будет уже в магазине. Поэтому проверяется весь
 * каталог целиком: данные, файлы в assets и нижняя граница показа.
 */
class AnimalCloseUpCoverageTest {

    private val animals = loadTestCatalog().animals

    @Test
    fun `every animal is announced as its name and voice`() {
        assertTrue("В каталоге меньше 22 животных: ${animals.size}", animals.size >= 22)

        animals.forEach { animal ->
            val onomatopoeia = animal.closeUpOnomatopoeia
            assertNotNull("Нет подписи «кто как говорит» у ${animal.id}", onomatopoeia)

            val announcement = closeUpAnnouncement(animal.name, onomatopoeia)
            assertEquals(
                "TalkBack читает не «название и звук» у ${animal.id}",
                "${animal.name}. $onomatopoeia",
                announcement,
            )
            // Незаполненное поле в каталоге превратилось бы в «Корова..» или
            // «Корова. » — такую подпись TalkBack прочитает с лишней паузой.
            assertEquals(
                "Подпись склеена не одним «. » у ${animal.id}: $announcement",
                2,
                announcement.split(". ").size,
            )
        }
    }

    @Test
    fun `every animal has a full size portrait`() {
        animals.forEach { animal ->
            val path = animal.image
            assertTrue("Нет картинки у ${animal.id}", !path.isNullOrBlank())

            val header = pngHeaderOf(path!!)
            assertNotNull("$path у ${animal.id} не PNG или не читается", header)
            // Крупный план — единственное место, где картинка рисуется почти
            // во всю ширину карточки: 512 там размывались бы на экранах с
            // высокой плотностью.
            assertEquals("Ширина $path мала для крупного плана", 1024, header!!.width)
            assertEquals("Высота $path мала для крупного плана", 1024, header.height)
            assertTrue(
                "У $path нет прозрачного фона (colorType=${header.colorType})",
                header.colorType == 6 || header.colorType == 4,
            )
        }
    }

    @Test
    fun `animals do not share one placeholder picture`() {
        assertTrue("В каталоге нет животных", animals.isNotEmpty())
        val seen = mutableMapOf<String, String>()

        animals.forEach { animal ->
            val path = animal.image?.trim()
            assertTrue("Нет картинки у ${animal.id}", !path.isNullOrBlank())

            val twin = seen.put(checksum(assetFile(path!!)), animal.id)
            // Одинаковый файл под разными именами — обычно забытая заглушка:
            // ребёнок видит корову там, где нажал на волка.
            assertNull("${animal.id} и $twin используют одну картинку $path", twin)
        }
    }

    @Test
    fun `every animal keeps the close-up until its own sound ends`() {
        animals.forEach { animal ->
            val file = assetFile(animal.soundFile)
            assertTrue("Нет звука ${animal.soundFile} у ${animal.id}", file.isFile)

            val bytes = file.readBytes()
            val soundMs = wavDurationMs(bytes, bytes.size.toLong())
            assertNotNull("Не читается WAV ${animal.soundFile} у ${animal.id}", soundMs)
            assertTrue(
                "Неправдоподобная длительность ${animal.soundFile}: $soundMs мс",
                soundMs!! in 100L..10_000L,
            )

            val floorMs = spotlightFloorMs(soundMs)
            assertTrue(
                "Крупный план ${animal.id} исчезнет раньше звука: $floorMs < $soundMs",
                floorMs >= soundMs,
            )
            assertTrue(
                "Крупный план ${animal.id} короче минимума: $floorMs",
                floorMs >= SPOTLIGHT_MIN_MS,
            )
            assertTrue(
                "Нет предела ожидания конца фразы TTS у ${animal.id}",
                spotlightMaxWaitMs(floorMs) > floorMs,
            )
        }
    }

    private fun checksum(file: File): String =
        MessageDigest.getInstance("SHA-256")
            .digest(file.readBytes())
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
}
