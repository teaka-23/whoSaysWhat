package ru.tac.whosayswhat.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.tac.whosayswhat.audio.hasRecordAudioPermission
import ru.tac.whosayswhat.audio.supportsRecordAudioPermissionRevocation
import ru.tac.whosayswhat.data.Animal
import ru.tac.whosayswhat.data.AnimalProgress
import ru.tac.whosayswhat.ui.theme.Cream
import ru.tac.whosayswhat.ui.theme.Ink
import ru.tac.whosayswhat.ui.theme.Mint
import ru.tac.whosayswhat.ui.theme.Peach
import ru.tac.whosayswhat.ui.theme.Sky
import ru.tac.whosayswhat.ui.theme.Sunny

private const val TAP_WINDOW_MS = 1_500L
private const val PRIVACY_POLICY_URL =
    "https://teaka-23.github.io/whoSaysWhat/privacy-policy.html"

/**
 * Атрибуция записей с Wikimedia Commons. Файлы входят в APK, поэтому credits
 * должны быть доступны пользователю без интернета — показываем их в панели
 * родителей. Полные тексты лицензий лежат в assets/licenses/animal-sounds.md.
 */
private val SOUND_CREDITS = """
    Корова — Secretlondon, CC BY-SA 3.0
    commons.wikimedia.org/wiki/File:Mudchute_cow_1.ogg
    creativecommons.org/licenses/by-sa/3.0

    Кошка — Dan Crosby, CC BY-SA 3.0
    commons.wikimedia.org/wiki/File:Meow.ogg
    creativecommons.org/licenses/by-sa/3.0

    Собака — Amada44, CC BY-SA 3.0
    commons.wikimedia.org/wiki/File:Barking_of_a_dog.ogg
    creativecommons.org/licenses/by-sa/3.0

    Утка — Jonathon Jongsma, CC BY-SA 3.0
    commons.wikimedia.org/wiki/File:Anas_platyrhynchos_-_Mallard_-_XC62258.ogg
    creativecommons.org/licenses/by-sa/3.0

    Лошадь — Hü, общественное достояние
    commons.wikimedia.org/wiki/File:Wiehern.ogg

    Петух — alys, общественное достояние
    commons.wikimedia.org/wiki/File:Medium_rooster_crowing.ogg

    Овца — earthcalling, общественное достояние
    commons.wikimedia.org/wiki/File:Sheep_bleating.ogg

    Свинья — Secretlondon, CC BY-SA 3.0
    commons.wikimedia.org/wiki/File:Mudchute_pig_1.ogg
    commons.wikimedia.org/wiki/File:Mudchute_pig_2.ogg
    commons.wikimedia.org/wiki/File:Mudchute_pig_3.ogg
    creativecommons.org/licenses/by-sa/3.0

    Коза — stephan, общественное достояние
    commons.wikimedia.org/wiki/File:Herd_of_goats_bleating.ogg

    Лягушка — MichaeltheFox8621, CC BY-SA 4.0
    commons.wikimedia.org/wiki/File:Single_Frog_Croak.oga
    creativecommons.org/licenses/by-sa/4.0

    Гусь — U.S. Fish and Wildlife Service, общественное достояние
    commons.wikimedia.org/wiki/File:Geese_Honking_(loud).ogg

    Осёл — felix-blume, CC0 1.0
    commons.wikimedia.org/wiki/File:157763_felix-blume_a-donkey-is-braying-in-his-enclosure-in-south-of-france.wav
    creativecommons.org/publicdomain/zero/1.0

    Сова — Vianney Bajart, CC BY-SA 4.0
    commons.wikimedia.org/wiki/File:Strix_aluco_male.oga
    creativecommons.org/licenses/by-sa/4.0

    Мышка — Yosef Ben Melamed, CC BY-SA 4.0
    commons.wikimedia.org/wiki/File:Mouse_voice.flac
    creativecommons.org/licenses/by-sa/4.0

    Пчела — YleArkisto, CC BY 3.0
    commons.wikimedia.org/wiki/File:263673_ylearkisto_mehilainen-tarhamehilainen-parvi-bees-honeybees-a-swarm-of-bees-buzzing-among-the-flowers-apis-mellifera.wav
    creativecommons.org/licenses/by/3.0

    Индюк — Jonathon Jongsma, CC BY-SA 3.0
    commons.wikimedia.org/wiki/File:Meleagris_gallopavo_-_Wild_Turkey_XC136045.ogg
    creativecommons.org/licenses/by-sa/3.0

    Курица — alys, общественное достояние
    commons.wikimedia.org/wiki/File:Chickens_demanding_food.ogg

    Волк — автор не указан, общественное достояние
    commons.wikimedia.org/wiki/File:Wolf_howls.ogg

    Медведь — Shizhao, CC BY 3.0
    commons.wikimedia.org/wiki/File:Bear_growl.ogg
    creativecommons.org/licenses/by/3.0

    Ворона — Marie-Lan Taÿ Pamart, CC BY-SA 4.0
    commons.wikimedia.org/wiki/File:Corvus_corone_-_Carrion_Crow_XC511975.mp3
    creativecommons.org/licenses/by-sa/4.0

    Кукушка — User:Κ64, CC BY 3.0
    commons.wikimedia.org/wiki/File:Cuculus_canorus_v01.ogg
    creativecommons.org/licenses/by/3.0

    Слон — தகவலுழவன், CC0 1.0
    commons.wikimedia.org/wiki/File:Elephant_voice_-_trumpeting.ogg
    creativecommons.org/publicdomain/zero/1.0

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
            .clickable(
                role = Role.Button,
                onClickLabel = "Открыть экран для родителей",
            ) {
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
    echoAnimalId: String?,
    onLevelChange: (Int) -> Unit,
    onEchoEnabledChange: (Boolean) -> Unit,
    onEchoAnimalChange: (String?) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    onExitApp: () -> Unit,
    onGoHome: (() -> Unit)? = null,
) {
    var confirmReset by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }
    var confirmDisableEcho by remember { mutableStateOf(false) }
    var showEchoAnimalPicker by remember { mutableStateOf(false) }
    var showSoundLicenses by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var micPermissionGranted by remember {
        mutableStateOf(context.hasRecordAudioPermission())
    }
    val canAutoRevoke = supportsRecordAudioPermissionRevocation()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                micPermissionGranted = context.hasRecordAudioPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Cream) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
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
                TextButton(
                    onClick = { confirmExit = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFB3261E),
                    ),
                ) {
                    Text(text = "Выйти", fontSize = 16.sp)
                }
                TextButton(onClick = onDismiss) {
                    Text(text = "Закрыть", fontSize = 16.sp)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    SummaryCard(animals = animals, progress = progress, sessions = sessions)
                }

                if (onGoHome != null) {
                    item {
                        TextButton(
                            onClick = onGoHome,
                            contentPadding = PaddingValues(horizontal = 4.dp),
                        ) {
                            Text(text = "← Домой", fontSize = 15.sp)
                        }
                    }
                }

                // Настройки — выше длинного списка животных: родитель обычно
                // открывает панель именно для «Эхо» или смены сложности.
                item { SectionTitle("Настройки игры") }

                item { LevelCard(level = level, onLevelChange = onLevelChange) }

                item {
                    EchoCard(
                        echoEnabled = echoEnabled,
                        micPermissionGranted = micPermissionGranted,
                        canAutoRevoke = canAutoRevoke,
                        onEchoEnabledChange = { enabled ->
                            if (!enabled && echoEnabled) {
                                confirmDisableEcho = true
                            } else {
                                onEchoEnabledChange(enabled)
                            }
                        },
                        onOpenSettings = { context.openAppSettings() },
                        onRefreshPermission = {
                            micPermissionGranted = context.hasRecordAudioPermission()
                        },
                    )
                }

                item {
                    EchoTargetCard(
                        animals = animals,
                        echoEnabled = echoEnabled,
                        selectedId = echoAnimalId,
                        onPick = { showEchoAnimalPicker = true },
                    )
                }

                item { SectionTitle("Приватность") }

                item { PrivacyCard() }

                item {
                    TextButton(onClick = { context.openPrivacyPolicy() }) {
                        Text(text = "Политика конфиденциальности", fontSize = 15.sp)
                    }
                }

                item {
                    TextButton(onClick = { showSoundLicenses = true }) {
                        Text(text = "Звуки животных: лицензии", fontSize = 15.sp)
                    }
                }

                item { SectionTitle("Прогресс по животным") }

                if (animals.isEmpty()) {
                    item {
                        Text(
                            text = "Каталог ещё загружается…",
                            fontSize = 15.sp,
                            color = Ink.copy(alpha = 0.7f),
                        )
                    }
                }

                items(animals, key = { it.id }) { animal ->
                    AnimalProgressRow(
                        animal = animal,
                        progress = progress[animal.id] ?: AnimalProgress.EMPTY,
                    )
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
                        color = Ink.copy(alpha = 0.7f),
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
                        "Выбранный уровень, режим «Эхо» и его животное останутся.",
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

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Выйти из приложения?") },
            text = {
                Text(
                    "Игра закроется. Прогресс и настройки сохранятся " +
                        "на этом устройстве.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmExit = false
                    onExitApp()
                }) {
                    Text(text = "Выйти", color = Color(0xFFB3261E))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) {
                    Text("Отмена")
                }
            },
        )
    }

    if (confirmDisableEcho) {
        val permissionNote = when {
            !micPermissionGranted -> ""
            canAutoRevoke ->
                " Доступ к микрофону будет отозван после закрытия приложения."
            else ->
                " Разрешение микрофона останется в системе — его можно " +
                    "отозвать в настройках Android."
        }
        AlertDialog(
            onDismissRequest = { confirmDisableEcho = false },
            title = { Text("Выключить «Эхо»?") },
            text = {
                Text(
                    "Временные записи будут удалены. Ребёнок больше не сможет " +
                        "записывать голос.$permissionNote",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDisableEcho = false
                    onEchoEnabledChange(false)
                }) {
                    Text("Выключить", color = Color(0xFFB3261E))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDisableEcho = false }) {
                    Text("Отмена")
                }
            },
        )
    }

    if (showEchoAnimalPicker) {
        EchoAnimalPickerDialog(
            animals = animals,
            selectedId = echoAnimalId,
            onSelect = { animalId ->
                onEchoAnimalChange(animalId)
                showEchoAnimalPicker = false
            },
            onDismiss = { showEchoAnimalPicker = false },
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
            color = Ink.copy(alpha = 0.7f),
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
        color = Ink.copy(alpha = 0.7f),
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
            AnimalArtwork(
                animal = animal,
                modifier = Modifier.size(40.dp),
                contentDescription = null,
            )
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
                    color = Ink.copy(alpha = 0.7f),
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
        Column(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .selectableGroup(),
        ) {
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
                        .selectable(
                            selected = level == value,
                            role = Role.RadioButton,
                            onClick = { onLevelChange(value) },
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = level == value,
                        // Клик обрабатывает вся строка: зона нажатия больше,
                        // а TalkBack читает её как один переключатель.
                        onClick = null,
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
        modifier = Modifier.fillMaxWidth(),
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
                color = Ink.copy(alpha = 0.7f),
            )
        }
    }
}

private fun Context.openPrivacyPolicy() {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
        .onFailure {
            Toast.makeText(this, "Не удалось открыть браузер", Toast.LENGTH_SHORT).show()
        }
}

@Composable
private fun EchoCard(
    echoEnabled: Boolean,
    micPermissionGranted: Boolean,
    canAutoRevoke: Boolean,
    onEchoEnabledChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onRefreshPermission: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                                "При первой записи — объяснение и запрос разрешения. " +
                                "Запись не покидает устройство."
                        } else {
                            "Выключен — микрофон не используется, " +
                                "временные записи удалены."
                        },
                        fontSize = 12.sp,
                        color = Ink.copy(alpha = 0.7f),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(checked = echoEnabled, onCheckedChange = onEchoEnabledChange)
            }

            if (!echoEnabled && micPermissionGranted) {
                Text(
                    text = if (canAutoRevoke) {
                        "Доступ к микрофону будет отозван автоматически " +
                            "после закрытия приложения (Android 13+)."
                    } else {
                        "Микрофон разрешён. На этой версии Android отзовите " +
                            "доступ в настройках приложения."
                    },
                    modifier = Modifier.padding(top = 10.dp),
                    fontSize = 12.sp,
                    color = Ink.copy(alpha = 0.7f),
                )
                if (!canAutoRevoke) {
                    Row {
                        TextButton(onClick = onOpenSettings) {
                            Text(text = "Настройки", fontSize = 14.sp)
                        }
                        TextButton(onClick = onRefreshPermission) {
                            Text(text = "Проверить", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EchoTargetCard(
    animals: List<Animal>,
    echoEnabled: Boolean,
    selectedId: String?,
    onPick: () -> Unit,
) {
    val selected = animals.firstOrNull { it.id == selectedId }
    val subtitle = when {
        selected != null -> "Сейчас: ${selected.name}"
        selectedId != null -> "Выбранное животное недоступно — авто"
        else -> "Авто — последнее нажатое"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .alpha(if (echoEnabled) 1f else 0.55f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected != null) {
                AnimalArtwork(
                    animal = selected,
                    modifier = Modifier.size(40.dp),
                    contentDescription = null,
                )
            } else {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "А",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Животное для «Эхо»",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Ink,
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Ink.copy(alpha = 0.7f),
                )
            }
            TextButton(
                onClick = onPick,
                enabled = echoEnabled && animals.isNotEmpty(),
            ) {
                Text(
                    text = if (selected == null) "Выбрать" else "Изменить",
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun EchoAnimalPickerDialog(
    animals: List<Animal>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Животное для «Эхо»") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item {
                    EchoAnimalOption(
                        selected = selectedId == null,
                        title = "Авто",
                        subtitle = "последнее нажатое",
                        onClick = { onSelect(null) },
                    )
                }
                items(animals, key = { it.id }) { animal ->
                    EchoAnimalOption(
                        selected = selectedId == animal.id,
                        title = animal.name,
                        animal = animal,
                        onClick = { onSelect(animal.id) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Готово")
            }
        },
    )
}

@Composable
private fun EchoAnimalOption(
    selected: Boolean,
    title: String,
    subtitle: String? = null,
    animal: Animal? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (animal != null) {
            AnimalArtwork(
                animal = animal,
                modifier = Modifier.size(36.dp),
                contentDescription = null,
            )
        } else {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "А",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = Ink,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Ink.copy(alpha = 0.7f),
                )
            }
        }
        RadioButton(selected = selected, onClick = null)
    }
}

