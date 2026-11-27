package ru.tac.whosayswhat.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import ru.tac.whosayswhat.audio.EchoRecorder
import ru.tac.whosayswhat.audio.SoundPlayer
import ru.tac.whosayswhat.audio.SpeechAnnouncer
import ru.tac.whosayswhat.audio.scheduleRecordAudioPermissionRevocation
import ru.tac.whosayswhat.audio.spotlightFloorMs
import ru.tac.whosayswhat.audio.spotlightMaxWaitMs
import ru.tac.whosayswhat.data.Animal
import ru.tac.whosayswhat.data.AnimalProgress
import ru.tac.whosayswhat.data.Catalog
import ru.tac.whosayswhat.data.CatalogRepository
import ru.tac.whosayswhat.data.ProgressStore
import ru.tac.whosayswhat.data.isPlayable
import ru.tac.whosayswhat.game.SessionEngine
import ru.tac.whosayswhat.ui.theme.Cream
import ru.tac.whosayswhat.ui.theme.Ink
import ru.tac.whosayswhat.ui.theme.Sunny

private const val TAG = "App"

private sealed interface Screen {
    data object Sandbox : Screen
    data object Guess : Screen
    data object Reward : Screen
}

/**
 * Состояние каталога. Ошибка — такое же нормальное состояние, как загрузка:
 * ребёнок (или родитель) должен увидеть «Повторить», а не бесконечный
 * индикатор, из которого нет выхода.
 */
private sealed interface CatalogState {
    data object Loading : CatalogState
    data object Failed : CatalogState
    data class Ready(val catalog: Catalog) : CatalogState
}

/**
 * Навигация без библиотеки: три экрана и родительская панель.
 * Панель открывается только родительским гейтом, приложение закрывается
 * кнопкой «Выйти» с подтверждением; Back приложение не закрывает.
 */
@Composable
fun App(
    soundPlayer: SoundPlayer,
    repository: CatalogRepository,
    progressStore: ProgressStore,
    onExitApp: () -> Unit,
) {
    // Номер попытки чтения каталога: «Повторить» перезапускает загрузку.
    var loadAttempt by remember { mutableIntStateOf(0) }
    var catalogState by remember(repository) {
        mutableStateOf<CatalogState>(CatalogState.Loading)
    }
    LaunchedEffect(repository, loadAttempt) {
        catalogState = CatalogState.Loading
        catalogState = loadCatalog(repository)
    }
    val progress by progressStore.animalProgress
        .collectAsState(initial = emptyMap<String, AnimalProgress>())
    val sessions by progressStore.completedSessions.collectAsState(initial = 0)
    val level by progressStore.level.collectAsState(initial = 1)
    // null — настройки ещё не прочитаны. Это важно: не планируем отзыв
    // микрофона, пока не знаем, что «Эхо» действительно выключено.
    val echoEnabledSetting: Boolean? by progressStore.echoEnabled
        .collectAsState(initial = null)
    // Переключатель должен реагировать сразу, не ожидая асинхронной записи
    // DataStore. Заодно это защищает от гонки: выключил → сразу вышел из
    // приложения, пока настройка ещё не успела сохраниться.
    var echoEnabledOverride by remember { mutableStateOf<Boolean?>(null) }
    val storedEchoEnabled = echoEnabledSetting == true
    val echoEnabled = echoEnabledOverride ?: storedEchoEnabled
    val echoAnimalId by progressStore.echoAnimalId.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Названия животных озвучивает системный синтезатор речи: в песочнице
    // после звука ребёнок слышит ласковое имя, а карточка показывает
    // каноническое название. Сеть приложению для этого не нужна.
    val announcer = remember(context) { SpeechAnnouncer(context) }
    DisposableEffect(announcer) {
        onDispose { announcer.release() }
    }
    // Ушли в фон — не досказываем название поверх чужого приложения.
    DisposableEffect(lifecycleOwner, announcer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) announcer.stop()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(storedEchoEnabled, echoEnabledOverride) {
        if (
            echoEnabledOverride != null &&
            echoEnabledSetting != null &&
            echoEnabledOverride == storedEchoEnabled
        ) {
            echoEnabledOverride = null
        }
    }

    val latestEchoEnabled by rememberUpdatedState(echoEnabled)
    val latestEchoEnabledSetting by rememberUpdatedState(echoEnabledSetting)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (
                event == Lifecycle.Event.ON_STOP &&
                latestEchoEnabledSetting != null &&
                !latestEchoEnabled
            ) {
                // На Android 13+ разрешение отзывается асинхронно после
                // выхода из foreground. На старых версиях функция вернёт
                // false — там доступ отзывается через настройки.
                context.scheduleRecordAudioPermissionRevocation()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var screen by remember { mutableStateOf<Screen>(Screen.Sandbox) }
    var parentPanel by remember { mutableStateOf(false) }
    var echoAnimal by remember { mutableStateOf<Animal?>(null) }
    // Крупный план животного: null — песочница на весь экран. Номер показа
    // отсекает запоздавший таймер прошлой карточки: ребёнок уже тапнул другое
    // животное, и старое окно не должно закрыть новое.
    var closeUpAnimal by remember { mutableStateOf<Animal?>(null) }
    var closeUpToken by remember { mutableIntStateOf(0) }

    // Название относится только к песочнице: в игре, панели родителей
    // и диалоге «Эхо» оно не должно досказываться поверх интерфейса.
    LaunchedEffect(screen, parentPanel, echoAnimal) {
        if (screen != Screen.Sandbox || parentPanel || echoAnimal != null) {
            announcer.stop()
            // Крупный план — тоже часть песочницы: под панелью или поверх
            // игры он не нужен.
            closeUpAnimal = null
        }
    }

    val catalog = (catalogState as? CatalogState.Ready)?.catalog
    val animals = catalog?.animals.orEmpty()

    LaunchedEffect(catalog) {
        soundPlayer.preload(
            animals.flatMap { listOf(it.soundFile, it.gameSoundFile) } +
                listOf("sounds/praise.wav", "sounds/nudge.wav"),
        )
    }

    // Back ведёт себя предсказуемо: закрывает диалог/панель, возвращает
    // из игры в песочницу. На главном экране Back сознательно ничего не
    // делает: родительская панель открывается только гейтом (три нажатия
    // по замку), а приложение закрывается кнопкой «Выйти» с подтверждением.
    // Ветку нельзя просто убрать — тогда Back закроет активность сам.
    // Исключение — ошибка каталога: играть не во что, поэтому Back выходит.
    BackHandler {
        when {
            echoAnimal != null -> echoAnimal = null
            // Крупный план перехватывает Back сам (это Dialog), но ветка
            // страхует: ребёнок должен уметь вернуться к животным всегда.
            closeUpAnimal != null -> closeUpAnimal = null
            parentPanel -> parentPanel = false
            catalogState is CatalogState.Failed -> onExitApp()
            screen != Screen.Sandbox -> screen = Screen.Sandbox
            else -> Unit
        }
    }

    /**
     * Показывает крупный план и запускает звук с озвучкой названия.
     *
     * Окно не обрывает звук: оно ждёт, пока договорит синтезатор речи, но не
     * меньше нижней границы — иначе короткое «Мяу!» мелькало бы на экране.
     * Повторный тап по увеличенной картинке вызывает эту же функцию: звук
     * играет заново, и отсчёт начинается с начала.
     */
    fun showCloseUp(animal: Animal) {
        val token = closeUpToken + 1
        closeUpToken = token
        closeUpAnimal = animal
        val soundMs = soundPlayer.durationMs(animal.soundFile)
        val floorMs = spotlightFloorMs(soundMs)
        soundPlayer.play(animal.soundFile)
        // Сигнал «фраза договорена» приходит и тогда, когда озвучки не будет
        // вовсе (нет русского голоса) — иначе ждали бы таймаут зря.
        val phraseDone = CompletableDeferred<Unit>()
        announcer.announceAfter(
            soundDurationMs = soundMs,
            narrationName = animal.narrationName,
            onFinished = { phraseDone.complete(Unit) },
        )
        scope.launch {
            delay(floorMs)
            // Синтезатор может не прислать колбэк: без предела картинка
            // осталась бы поверх песочницы навсегда.
            withTimeoutOrNull(spotlightMaxWaitMs(floorMs) - floorMs) {
                phraseDone.await()
            }
            if (closeUpToken == token) closeUpAnimal = null
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Cream)) {
        when (val state = catalogState) {
            CatalogState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )

            CatalogState.Failed -> CatalogLoadError(
                onRetry = { loadAttempt++ },
                onExit = onExitApp,
            )

            is CatalogState.Ready -> {
                val engine = remember(state.catalog, level) {
                    SessionEngine(state.catalog.animals, requestedOptions = level + 1)
                }

                when (screen) {
                    Screen.Sandbox -> SandboxScreen(
                        animals = state.catalog.animals,
                        echoEnabled = echoEnabled,
                        preferredEchoAnimalId = echoAnimalId,
                        onAnimalSound = { animal -> showCloseUp(animal) },
                        onStartGame = { screen = Screen.Guess },
                        onEcho = { echoAnimal = it },
                        onOpenParentPanel = { parentPanel = true },
                        onExitApp = onExitApp,
                    )

                    Screen.Guess -> key(engine) {
                        GuessScreen(
                            engine = engine,
                            soundPlayer = soundPlayer,
                            onAnswer = { animalId, correct ->
                                scope.launch { progressStore.recordAnswer(animalId, correct) }
                            },
                            onSessionFinished = {
                                scope.launch { progressStore.completeSession() }
                                screen = Screen.Reward
                            },
                            onOpenParentPanel = { parentPanel = true },
                            onExit = { screen = Screen.Sandbox },
                        )
                    }

                    Screen.Reward -> RewardScreen(
                        soundPlayer = soundPlayer,
                        onDone = { screen = Screen.Sandbox },
                    )
                }
            }
        }

        if (parentPanel) {
            ParentPanel(
                animals = animals,
                progress = progress,
                sessions = sessions,
                level = level,
                echoEnabled = echoEnabled,
                echoAnimalId = echoAnimalId,
                onLevelChange = { newLevel -> scope.launch { progressStore.setLevel(newLevel) } },
                onEchoEnabledChange = { enabled ->
                    echoEnabledOverride = enabled
                    if (!enabled) {
                        // Закрываем возможный диалог и сразу удаляем записи,
                        // не дожидаясь onDispose.
                        echoAnimal = null
                        EchoRecorder.cleanup(context)
                    }
                    scope.launch { progressStore.setEchoEnabled(enabled) }
                },
                onEchoAnimalChange = { animalId ->
                    scope.launch { progressStore.setEchoAnimalId(animalId) }
                },
                onReset = { scope.launch { progressStore.resetProgress() } },
                onDismiss = { parentPanel = false },
                onExitApp = {
                    parentPanel = false
                    onExitApp()
                },
                onGoHome = if (screen != Screen.Sandbox) {
                    {
                        screen = Screen.Sandbox
                        parentPanel = false
                    }
                } else {
                    null
                },
            )
        }

        // Крупный план — отдельное окно поверх песочницы: оно перехватывает
        // фокус (TalkBack читает название и звукоподражание) и закрывается
        // кнопкой Back или тапом по затемнённому фону.
        val closeUp = closeUpAnimal
        if (closeUp != null) {
            AnimalCloseUp(
                animal = closeUp,
                onRepeat = { showCloseUp(closeUp) },
                onDismiss = { closeUpAnimal = null },
            )
        }

        val echoTarget = echoAnimal
        if (echoTarget != null) {
            EchoDialog(
                animal = echoTarget,
                soundPlayer = soundPlayer,
                onDismiss = { echoAnimal = null },
            )
        }
    }
}

/**
 * Читает каталог и превращает любую проблему в [CatalogState.Failed]:
 * исключение из assets или каталог, из которого не собрать раунд.
 */
private suspend fun loadCatalog(repository: CatalogRepository): CatalogState =
    runCatching { repository.load() }.fold(
        onSuccess = { catalog ->
            if (catalog.isPlayable()) {
                CatalogState.Ready(catalog)
            } else {
                Log.w(TAG, "В каталоге меньше двух разных животных: ${catalog.animals.size}")
                CatalogState.Failed
            }
        },
        onFailure = { error ->
            Log.w(TAG, "Каталог не загрузился", error)
            CatalogState.Failed
        },
    )

/**
 * Каталог не загрузился или в нём слишком мало животных.
 *
 * Вместо вечного индикатора — понятное объяснение, «Повторить» и «Выйти»:
 * приложение не должно запирать ребёнка с родителем на пустом экране.
 */
@Composable
private fun CatalogLoadError(
    onRetry: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Не удалось загрузить животных",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.heightIn(min = 56.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Sunny, contentColor = Ink),
        ) {
            Text(text = "Повторить", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onExit) {
            Text(text = "Выйти", fontSize = 16.sp, color = Ink)
        }
    }
}
