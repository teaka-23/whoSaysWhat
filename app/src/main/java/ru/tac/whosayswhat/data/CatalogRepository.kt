package ru.tac.whosayswhat.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Читает каталог животных из assets. В MVP сеть не используется:
 * весь контент лежит внутри APK, приложение работает офлайн.
 */
class CatalogRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun load(): Catalog = withContext(Dispatchers.IO) {
        context.assets.open(CATALOG_ASSET).bufferedReader().use { reader ->
            json.decodeFromString<Catalog>(reader.readText())
        }
    }

    private companion object {
        const val CATALOG_ASSET = "catalog.json"
    }
}

/** Минимум разных животных, иначе раунду не из чего выбирать. */
const val MIN_ANIMALS_TO_PLAY = 2

/**
 * Пригоден ли каталог для игры. [ru.tac.whosayswhat.game.SessionEngine] требует
 * минимум двух разных животных, поэтому недостаточный каталог показывается как
 * ошибка загрузки, а не роняет приложение исключением.
 */
fun Catalog.isPlayable(): Boolean =
    animals.distinctBy { it.id }.size >= MIN_ANIMALS_TO_PLAY

