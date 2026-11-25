package ru.tac.whosayswhat.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Проверяет разбор длительности WAV и расчёт паузы: по этим числам
 * приложение решает, когда озвучить название животного после его звука.
 */
class WavDurationTest {

    private val assetsDir = File("src/main/assets")

    @Test
    fun `pcm duration comes from data chunk and byte rate`() {
        val header = wavHeader(dataSize = 2L * 44100 * 2) // две секунды
        assertEquals(2000L, wavDurationMs(header, header.size.toLong()))
    }

    @Test
    fun `extra chunk before data is skipped`() {
        val header = wavHeader(dataSize = 44100L * 2, metadata = true)
        assertEquals(1000L, wavDurationMs(header, header.size.toLong()))
    }

    @Test
    fun `unknown data size falls back to file length`() {
        val header = wavHeader(dataSize = UNKNOWN_SIZE)
        assertEquals(
            1000L,
            wavDurationMs(header, header.size + 44100L * 2),
        )
    }

    @Test
    fun `non wav or truncated header returns null`() {
        assertNull(wavDurationMs(ByteArray(3), 3))
        assertNull(wavDurationMs("RIFF\u0000\u0000\u0000\u0000AVI ".toByteArray(), 12))
        assertNull(wavDurationMs(wavHeader(dataSize = 44100L * 2).copyOf(20), 20))
    }

    @Test
    fun `narration delay adds a pause and clamps unknown lengths`() {
        assertEquals(750L + NARRATION_PAUSE_MS, narrationDelayMs(750))
        // Нулевая или неверная длительность — безопасный запас, а не речь
        // поверх ещё играющего звука.
        assertEquals(2000L + NARRATION_PAUSE_MS, narrationDelayMs(0))
        assertEquals(10_000L + NARRATION_PAUSE_MS, narrationDelayMs(60_000))
    }

    @Test
    fun `every bundled sound has a plausible duration`() {
        val sounds = File(assetsDir, "sounds")
            .listFiles { file -> file.extension == "wav" }
            .orEmpty()
            .toList()
        assertTrue("В assets/sounds нет WAV", sounds.isNotEmpty())

        sounds.forEach { file ->
            val bytes = file.readBytes()
            val duration = wavDurationMs(bytes, bytes.size.toLong())
            assertTrue(
                "${file.name}: длительность $duration мс",
                duration != null && duration in 100..10_000,
            )
        }
    }

    private fun wavHeader(dataSize: Long, metadata: Boolean = false): ByteArray {
        val fmtSize = 16
        val metaSize = if (metadata) 12 else 0
        val headerSize = 12 + (8 + fmtSize) + (if (metadata) 8 + metaSize else 0) + 8
        val buffer = ByteBuffer.allocate(headerSize).order(ByteOrder.LITTLE_ENDIAN)

        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt((headerSize - 8 + dataSize).toInt())
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(fmtSize)
        buffer.putShort(1) // PCM
        buffer.putShort(1) // моно
        buffer.putInt(44100) // частота дискретизации
        buffer.putInt(44100 * 2) // байт в секунду
        buffer.putShort(2) // выравнивание блока
        buffer.putShort(16) // бит на сэмпл
        if (metadata) {
            buffer.put("LIST".toByteArray(Charsets.US_ASCII))
            buffer.putInt(metaSize)
            buffer.put("INFOtesttest".toByteArray(Charsets.US_ASCII))
        }
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(dataSize.toInt())
        return buffer.array()
    }

    private companion object {
        const val UNKNOWN_SIZE = 0xFFFFFFFFL
    }
}
