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
