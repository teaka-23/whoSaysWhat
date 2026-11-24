package ru.tac.whosayswhat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import ru.tac.whosayswhat.audio.EchoRecorder
import ru.tac.whosayswhat.audio.SoundPlayer
import ru.tac.whosayswhat.audio.scheduleRecordAudioPermissionRevocation
import ru.tac.whosayswhat.data.Animal
import ru.tac.whosayswhat.data.AnimalProgress
import ru.tac.whosayswhat.data.Catalog
import ru.tac.whosayswhat.data.CatalogRepository
import ru.tac.whosayswhat.data.ProgressStore
import ru.tac.whosayswhat.game.SessionEngine
import ru.tac.whosayswhat.ui.theme.Cream

private sealed interface Screen {
    data object Sandbox : Screen
    data object Guess : Screen
    data object Reward : Screen
}

/**
 * Навигация без библиотеки: три экрана и родительская панель.
 * Выход из приложения закрыт родительским гейтом (BackHandler).
 */
@Composable
fun App(
    soundPlayer: SoundPlayer,
    repository: CatalogRepository,
    progressStore: ProgressStore,
    onExitApp: () -> Unit,
) {
    val catalog by produceState<Catalog?>(initialValue = null) {
        value = runCatching { repository.load() }.getOrNull()
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

    val animals = catalog?.animals.orEmpty()

    LaunchedEffect(catalog) {
        soundPlayer.preload(
            animals.flatMap { listOf(it.soundFile, it.gameSoundFile) } +
                listOf("sounds/praise.wav", "sounds/nudge.wav"),
        )
    }

    // Back ведёт себя предсказуемо: закрывает диалог/панель, возвращает
    // из игры в песочницу. Только на главном экране открывает панель
    // родителей — приложение не закрывается случайным нажатием ребёнка.
    BackHandler {
        when {
            echoAnimal != null -> echoAnimal = null
            parentPanel -> parentPanel = false
            screen != Screen.Sandbox -> screen = Screen.Sandbox
            else -> parentPanel = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Cream)) {
        val loaded = catalog
        when {
            loaded == null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

            loaded.animals.size < 2 -> Text(
                text = "Не удалось загрузить каталог",
                modifier = Modifier.align(Alignment.Center),
            )

            else -> {
                val engine = remember(loaded, level) {
                    SessionEngine(loaded.animals, requestedOptions = level + 1)
                }

                when (screen) {
                    Screen.Sandbox -> SandboxScreen(
                        animals = loaded.animals,
                        echoEnabled = echoEnabled,
                        preferredEchoAnimalId = echoAnimalId,
                        onAnimalSound = { soundPlayer.play(it.soundFile) },
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
