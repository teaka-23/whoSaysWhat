package ru.tac.whosayswhat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(name = "progress")

/**
 * Локальный прогресс: без аккаунта, без сети и без аналитики.
 * Храним статистику ответов по каждому животному, число сессий, уровень,
 * настройку режима «Эхо» и выбранное для него животное. Отдельного «облака»
 * нет и быть не может — в приложении нет разрешения INTERNET.
 */
class ProgressStore(private val context: Context) {

    private object Keys {
        val SESSIONS = intPreferencesKey("completed_sessions")
        val LEVEL = intPreferencesKey("level")
        val ANIMAL_PROGRESS = stringPreferencesKey("animal_progress")
        val ECHO_ENABLED = booleanPreferencesKey("echo_enabled")
        val ECHO_ANIMAL_ID = stringPreferencesKey("echo_animal_id")

        /** Ключ старой версии; нужен только чтобы вычистить его при сбросе. */
        val LEGACY_MASTERED = stringSetPreferencesKey("mastered_animals")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val animalProgress: Flow<Map<String, AnimalProgress>> =
        context.progressDataStore.data.map { prefs -> decode(prefs[Keys.ANIMAL_PROGRESS]) }

    val completedSessions: Flow<Int> =
        context.progressDataStore.data.map { it[Keys.SESSIONS] ?: 0 }

    val level: Flow<Int> =
        context.progressDataStore.data.map { it[Keys.LEVEL] ?: 1 }

    val echoEnabled: Flow<Boolean> =
        context.progressDataStore.data.map { it[Keys.ECHO_ENABLED] ?: false }

    /** null — автоматический выбор: животное, которое ребёнок нажал последним. */
    val echoAnimalId: Flow<String?> =
        context.progressDataStore.data.map { prefs ->
            prefs[Keys.ECHO_ANIMAL_ID]?.takeIf { it.isNotBlank() }
        }

    /**
     * Учитывает одну попытку ответа. [animalId] — животное, которое загадали
     * в раунде, а не карточка, по которой ошибочно нажали: так прогресс
     * показывает, насколько ребёнок узнаёт конкретный звук.
     */
    suspend fun recordAnswer(animalId: String, correct: Boolean) {
        if (animalId.isBlank()) return
        context.progressDataStore.edit { prefs ->
            val updated = decode(prefs[Keys.ANIMAL_PROGRESS]).toMutableMap()
            val current = updated[animalId] ?: AnimalProgress.EMPTY
            updated[animalId] = current.withAnswer(correct)
            prefs[Keys.ANIMAL_PROGRESS] = json.encodeToString(updated)
        }
    }

    suspend fun completeSession() {
        context.progressDataStore.edit { prefs ->
            prefs[Keys.SESSIONS] = (prefs[Keys.SESSIONS] ?: 0) + 1
        }
    }

    suspend fun setLevel(level: Int) {
        context.progressDataStore.edit { prefs ->
            prefs[Keys.LEVEL] = level.coerceIn(1, 3)
        }
    }

    suspend fun setEchoEnabled(enabled: Boolean) {
        context.progressDataStore.edit { prefs -> prefs[Keys.ECHO_ENABLED] = enabled }
    }

    suspend fun setEchoAnimalId(animalId: String?) {
        context.progressDataStore.edit { prefs ->
            if (animalId.isNullOrBlank()) {
                prefs.remove(Keys.ECHO_ANIMAL_ID)
            } else {
                prefs[Keys.ECHO_ANIMAL_ID] = animalId
            }
        }
    }

    /** Сбрасывает прогресс, но оставляет настройки: уровень, «Эхо» и его цель. */
    suspend fun resetProgress() {
        context.progressDataStore.edit { prefs ->
            prefs.remove(Keys.SESSIONS)
            prefs.remove(Keys.ANIMAL_PROGRESS)
            prefs.remove(Keys.LEGACY_MASTERED)
        }
    }

    private fun decode(raw: String?): Map<String, AnimalProgress> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            json.decodeFromString<Map<String, AnimalProgress>>(raw)
        }.getOrDefault(emptyMap())
    }
}
