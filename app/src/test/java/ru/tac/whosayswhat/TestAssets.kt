package ru.tac.whosayswhat

import kotlinx.serialization.json.Json
import ru.tac.whosayswhat.data.Catalog
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Доступ к assets из unit-тестов.
 *
 * Тесты запускаются из каталога модуля `app`, поэтому настоящий `assets`
 * читается по относительному пути: проверки видят ровно те файлы, которые
 * попадут в APK, а не копию в тестовых ресурсах.
 */
internal val testAssetsDir = File("src/main/assets")

/** Каталог животных из assets, разобранный тем же сериализатором, что и в приложении. */
internal fun loadTestCatalog(): Catalog =
    testJson.decodeFromString<Catalog>(assetFile("catalog.json").readText())

// Один формат на все тесты: сериализатор неизменяемый, а настройка
// `ignoreUnknownKeys` нужна та же, что в приложении.
private val testJson = Json { ignoreUnknownKeys = true }

/** Файл из assets по пути из каталога: префикс `assets/` допускается. */
internal fun assetFile(path: String): File =
    File(testAssetsDir, path.trim().removePrefix("assets/"))

/** Ширина, высота и тип цвета из заголовка PNG. */
internal data class PngHeader(val width: Int, val height: Int, val colorType: Int)

/**
 * Заголовок PNG или null, если файла нет или это не PNG.
 *
 * Ошибка чтения не пробрасывается: сообщение в тесте должно говорить, какой
 * именно файл плохой, а не тонуть в стектрейсе `FileNotFoundException`.
 */
internal fun pngHeaderOf(path: String): PngHeader? = runCatching {
    readPngHeader(assetFile(path).readBytes())
}.getOrNull()

private val PNG_SIGNATURE = byteArrayOf(
    0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
)

private fun readPngHeader(bytes: ByteArray): PngHeader? {
    if (bytes.size < 29) return null
    if (!bytes.copyOfRange(0, PNG_SIGNATURE.size).contentEquals(PNG_SIGNATURE)) return null
    // Ширина и высота — первые два поля IHDR сразу после сигнатуры и длины
    // чанка; цветность лежит в байте 25 и включает альфа-канал.
    val header = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
    return PngHeader(
        width = header.getInt(16),
        height = header.getInt(20),
        colorType = bytes[25].toInt() and 0xFF,
    )
}
