package ru.tac.whosayswhat.ui

import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap

/** Доля кучи, которую отдаём под иллюстрации. */
private const val CACHE_HEAP_FRACTION = 8L

/** Меньше этого кэш бесполезен: не помещается даже несколько картинок. */
private const val CACHE_MIN_BYTES = 8L * 1024 * 1024

/** Больше этого незачем: 22 иллюстрации по 1 МБ влезают целиком. */
private const val CACHE_MAX_BYTES = 64L * 1024 * 1024

private const val ASSET_PREFIX = "assets/"

/**
 * Сколько байт отдать кэшу иллюстраций при куче [maxHeapBytes].
 *
 * Восьмая часть кучи — привычная доля для картинок: на устройстве с 256 МБ
 * это 32 МБ, чего хватает на весь каталог, а на слабом устройстве с 64 МБ —
 * 8 МБ, иначе кэш сам стал бы причиной нехватки памяти.
 */
internal fun imageCacheBytes(maxHeapBytes: Long): Int =
    (maxHeapBytes / CACHE_HEAP_FRACTION)
        .coerceIn(CACHE_MIN_BYTES, CACHE_MAX_BYTES)
        .toInt()

/**
 * Путь к PNG из каталога в виде, который понимают assets: без префикса
 * `assets/`. null и пустая строка означают «иллюстрации нет».
 */
internal fun animalAssetPath(path: String?): String? =
    path?.trim()
        ?.removePrefix(ASSET_PREFIX)
        ?.takeIf { it.isNotBlank() }

/**
 * Кэш декодированных иллюстраций.
 *
 * Раньше каждая прокрутка песочницы декодировала PNG заново: 512×512 в
 * ARGB_8888 — это 1 МБ на картинку, и на бюджетном телефоне сетка заметно
 * подвисала. Кэш держит последние картинки в памяти, а [LruCache] сам
 * вытесняет самые давние, если места больше нет.
 */
internal object AnimalImages {

    private val cache = object : LruCache<String, ImageBitmap>(
        imageCacheBytes(Runtime.getRuntime().maxMemory()),
    ) {
        // Ключ — путь, а размер считаем по пикселям: PNG весит по-разному,
        // а в памяти картинка всегда занимает width × height × 4 байта.
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            value.width * value.height * 4
    }

    fun get(assetPath: String): ImageBitmap? = cache.get(assetPath)

    fun put(assetPath: String, bitmap: ImageBitmap) {
        cache.put(assetPath, bitmap)
    }
}
