package ru.tac.whosayswhat.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Длительность PCM WAV по началу файла. Все звуки приложения — несжатый
 * RIFF/WAVE PCM, поэтому хватает заголовка: длительность равна размеру
 * data-чанка, делённому на байты в секунду.
 *
 * Возвращает `null`, если файл не WAV или заголовок обрезан: тогда название
 * животного просто не озвучивается, а звук играет как обычно.
 */
internal fun wavDurationMs(header: ByteArray, fileLength: Long): Long? {
    if (header.size < 12) return null
    if (!header.isAscii(0, "RIFF") || !header.isAscii(8, "WAVE")) return null

    val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
    var offset = 12
    var byteRate = 0L

    while (offset + 8 <= header.size) {
        val id = String(header, offset, 4, Charsets.US_ASCII)
        val size = buffer.getInt(offset + 4).toLong() and 0xFFFFFFFFL

        when (id) {
            // fmt: format(2) channels(2) sampleRate(4) byteRate(4) ...
            "fmt " -> if (offset + 24 <= header.size) {
                byteRate = buffer.getInt(offset + 16).toLong() and 0xFFFFFFFFL
            }

            "data" -> {
                if (byteRate <= 0L) return null
                val dataSize = when (size) {
                    0L, UNKNOWN_SIZE ->
                        (fileLength - (offset + 8)).coerceAtLeast(0L)
                    else -> size
                }
                return (dataSize * 1000L / byteRate).takeIf { it > 0L }
            }
        }

        if (size == UNKNOWN_SIZE) return null
        val padded = size + (size and 1L) // чанки выравниваются по чётному байту
        offset = (offset + 8 + padded).toInt()
    }
    return null
}

/**
 * Задержка перед названием животного: сначала доигрывает запись, потом
 * короткая пауза, чтобы ребёнок успел «услышать» тишину и связать звук
 * с названием. Нулевая длительность заменяется запасом, слишком длинная —
 * ограничивается сверху, чтобы название не откладывалось бесконечно.
 */
internal fun narrationDelayMs(soundDurationMs: Long): Long {
    val sound = when {
        soundDurationMs <= 0L -> FALLBACK_SOUND_MS
        soundDurationMs > MAX_SOUND_MS -> MAX_SOUND_MS
        else -> soundDurationMs
    }
    return sound + NARRATION_PAUSE_MS
}

internal const val NARRATION_PAUSE_MS = 600L
private const val FALLBACK_SOUND_MS = 2_000L
private const val MAX_SOUND_MS = 10_000L
private const val UNKNOWN_SIZE = 0xFFFFFFFFL

private fun ByteArray.isAscii(offset: Int, text: String): Boolean {
    if (offset + text.length > size) return false
    return String(this, offset, text.length, Charsets.US_ASCII) == text
}
