package ru.tac.whosayswhat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.tac.whosayswhat.data.Animal
import ru.tac.whosayswhat.data.AnimalProgress
import ru.tac.whosayswhat.ui.theme.Cream
import ru.tac.whosayswhat.ui.theme.Ink
import ru.tac.whosayswhat.ui.theme.Mint
import ru.tac.whosayswhat.ui.theme.Peach
import ru.tac.whosayswhat.ui.theme.Sky
import ru.tac.whosayswhat.ui.theme.Sunny

private const val TAP_WINDOW_MS = 1_500L

/**
 * Атрибуция записей с Wikimedia Commons. Файлы входят в APK, поэтому credits
 * должны быть доступны пользователю без интернета — показываем их в панели
 * родителей. Полные тексты лицензий лежат в assets/licenses/animal-sounds.md.
 */
private val SOUND_CREDITS = """
    Корова — MichaeltheFox8621, CC BY-SA 4.0
    commons.wikimedia.org/wiki/File:Single_Cow_Moo.ogg
    creativecommons.org/licenses/by-sa/4.0

    Кошка — freemaster2, CC0 1.0
    commons.wikimedia.org/wiki/File:Meow_of_a_Siamese_cat_-_freemaster2.wav
    creativecommons.org/publicdomain/zero/1.0

    Собака — Amada44, CC BY-SA 3.0
    commons.wikimedia.org/wiki/File:Barking_of_a_dog.ogg
    creativecommons.org/licenses/by-sa/3.0

    Утка — Ganesh Mohan T, CC BY-SA 4.0
    commons.wikimedia.org/wiki/File:Domestic_duck_sound_01.wav
    creativecommons.org/licenses/by-sa/4.0

    Лошадь — Hü, общественное достояние
    commons.wikimedia.org/wiki/File:Wiehern.ogg

    Петух — alys, общественное достояние
    commons.wikimedia.org/wiki/File:Medium_rooster_crowing.ogg

    Записи перекодированы в моно 44,1 кГц, обрезаны и нормализованы.
    Полные условия лицензий: creativecommons.org/licenses
""".trimIndent()

/**
 * Родительский гейт без математики: три быстрых нажатия по замку.
 * Кнопка маленькая и полупрозрачная, чтобы ребёнок не наткнулся случайно.
 */
@Composable
fun ParentGateButton(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val taps = remember { mutableStateListOf<Long>() }

    Box(
        modifier = modifier
            .size(56.dp)
            .clickable {
                val now = System.currentTimeMillis()
                taps.removeAll { now - it > TAP_WINDOW_MS }
                taps.add(now)
                if (taps.size >= 3) {
                    taps.clear()
                    onOpen()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "🔒", fontSize = 22.sp, modifier = Modifier.alpha(0.35f))
    }
}

/**
 * Экран для родителей: прогресс по каждому животному, звёзды, настройки игры
 * и включатель режима «Эхо». Открывается только через гейт, ребёнку не виден.
 */
@Composable
fun ParentPanel(
    animals: List<Animal>,
    progress: Map<String, AnimalProgress>,
    sessions: Int,
    level: Int,
    echoEnabled: Boolean,
    onLevelChange: (Int) -> Unit,
    onEchoEnabledChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmReset by remember { mutableStateOf(false) }
    var showSoundLicenses by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = Cream) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Для родителей",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismiss) {
                    Text(text = "Закрыть", fontSize = 16.sp)
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    SummaryCard(animals = animals, progress = progress, sessions = sessions)
                }

                item { SectionTitle("Прогресс по животным") }

                if (animals.isEmpty()) {
                    item {
                        Text(
                            text = "Каталог ещё загружается…",
                            fontSize = 15.sp,
                            color = Ink.copy(alpha = 0.6f),
                        )
                    }
                }

                items(animals, key = { it.id }) { animal ->
                    AnimalProgressRow(
                        animal = animal,
                        progress = progress[animal.id] ?: AnimalProgress.EMPTY,
                    )
                }

                item { SectionTitle("Настройки игры") }

                item { LevelCard(level = level, onLevelChange = onLevelChange) }

                item {
                    EchoCard(
                        echoEnabled = echoEnabled,
                        onEchoEnabledChange = onEchoEnabledChange,
                    )
                }

                item { SectionTitle("Приватность") }

                item { PrivacyCard() }

                item {
                    TextButton(onClick = { showSoundLicenses = true }) {
                        Text(text = "Звуки животных: лицензии", fontSize = 15.sp)
                    }
                }

                item {
                    TextButton(
                        onClick = { confirmReset = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFB3261E),
                        ),
                    ) {
                        Text(text = "Сбросить прогресс", fontSize = 15.sp)
                    }
                }

                item {
                    Text(
                        text = "Прогресс хранится только на этом устройстве. " +
                            "Без аккаунта, рекламы и аналитики.",
                        fontSize = 12.sp,
                        color = Ink.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Сбросить прогресс?") },
            text = {
                Text(
                    "Звёзды, статистика и число сессий обнулятся. " +
                        "Выбранный уровень и режим «Эхо» останутся.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    onReset()
                }) {
                    Text(text = "Сбросить", color = Color(0xFFB3261E))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text("Отмена")
                }
            },
        )
    }

    if (showSoundLicenses) {
        AlertDialog(
            onDismissRequest = { showSoundLicenses = false },
            title = { Text("Звуки животных") },
            text = {
                Text(
                    text = SOUND_CREDITS,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Ink,
                    modifier = Modifier
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(onClick = { showSoundLicenses = false }) {
                    Text("Закрыть")
                }
            },
        )
    }
}

@Composable
private fun SummaryCard(
    animals: List<Animal>,
    progress: Map<String, AnimalProgress>,
    sessions: Int,
) {
    val mastered = animals.count { progress[it.id]?.isMastered == true }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SummaryValue(value = sessions.toString(), label = sessionsLabel(sessions))
            SummaryValue(value = "$mastered / ${animals.size}", label = "освоено")
        }
    }
}

private fun sessionsLabel(count: Int): String {
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..14 -> "сессий"
        mod10 == 1 -> "сессия"
        mod10 in 2..4 -> "сессии"
        else -> "сессий"
    }
}

@Composable
private fun SummaryValue(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )
        Text(
            text = label,
            fontSize = 13.sp,
            color = Ink.copy(alpha = 0.55f),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 6.dp),
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Ink.copy(alpha = 0.65f),
    )
}

@Composable
private fun AnimalProgressRow(animal: Animal, progress: AnimalProgress) {
    val subtitle = if (progress.attempts == 0) {
        "Пока нет ответов"
    } else {
        "Верно ${progress.correct} из ${progress.attempts}"
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimalArtwork(animal = animal, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = animal.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Ink,
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Ink.copy(alpha = 0.55f),
                )
            }
            StarRow(stars = progress.stars)
        }
    }
}

@Composable
private fun StarRow(stars: Int) {
    Row {
        repeat(AnimalProgress.MAX_STARS) { index ->
            Text(
                text = "★",
                fontSize = 18.sp,
                color = if (index < stars) Color(0xFFFFB300) else Ink.copy(alpha = 0.12f),
            )
        }
    }
}

@Composable
private fun LevelCard(level: Int, onLevelChange: (Int) -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "Сколько карточек показывать",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Ink,
            )
            listOf(
                1 to "2 карточки",
                2 to "3 карточки",
                3 to "4 карточки",
            ).forEach { (value, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLevelChange(value) }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = level == value,
                        onClick = { onLevelChange(value) },
                    )
                    Text(text = label, fontSize = 16.sp, color = Ink)
                }
            }
        }
    }
}

@Composable
private fun PrivacyCard() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrivacyRow(
                title = "Работает офлайн",
                text = "Все звуки и картинки уже внутри приложения.",
                accent = Mint,
            )
            PrivacyRow(
                title = "Без рекламы и аналитики",
                text = "Нет аккаунта, покупок и сбора данных.",
                accent = Sky,
            )
            PrivacyRow(
                title = "Нет доступа в интернет",
                text = "Приложение не может отправить данные в сеть.",
                accent = Peach,
            )
            PrivacyRow(
                title = "Микрофон — только для «Эхо»",
                text = "Запись остаётся на устройстве и удаляется после закрытия.",
                accent = Sunny,
            )
        }
    }
}

@Composable
private fun PrivacyRow(title: String, text: String, accent: Color) {
    Row {
        Text(
            text = "●",
            modifier = Modifier.padding(end = 10.dp, top = 1.dp),
            fontSize = 14.sp,
            color = accent,
        )
        Column {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Ink,
            )
            Text(
                text = text,
                fontSize = 12.sp,
                color = Ink.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun EchoCard(echoEnabled: Boolean, onEchoEnabledChange: (Boolean) -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Режим «Эхо»",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Ink,
                )
                Text(
                    text = if (echoEnabled) {
                        "Ребёнок повторяет звук в микрофон и слышит себя. " +
                            "Запись не покидает устройство."
                    } else {
                        "Выключен — микрофон не используется."
                    },
                    fontSize = 12.sp,
                    color = Ink.copy(alpha = 0.55f),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(checked = echoEnabled, onCheckedChange = onEchoEnabledChange)
        }
    }
}

