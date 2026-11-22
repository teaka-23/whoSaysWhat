package ru.tac.whosayswhat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import ru.tac.whosayswhat.audio.SoundPlayer
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
) {
    val catalog by produceState<Catalog?>(initialValue = null) {
        value = runCatching { repository.load() }.getOrNull()
    }
    val progress by progressStore.animalProgress
        .collectAsState(initial = emptyMap<String, AnimalProgress>())
    val sessions by progressStore.completedSessions.collectAsState(initial = 0)
    val level by progressStore.level.collectAsState(initial = 1)
    val echoEnabled by progressStore.echoEnabled.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

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

    BackHandler(enabled = !parentPanel) { parentPanel = true }
    BackHandler(enabled = parentPanel) { parentPanel = false }

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
                        onAnimalSound = { soundPlayer.play(it.soundFile) },
                        onStartGame = { screen = Screen.Guess },
                        onEcho = { echoAnimal = it },
                        onOpenParentPanel = { parentPanel = true },
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
                onLevelChange = { newLevel -> scope.launch { progressStore.setLevel(newLevel) } },
                onEchoEnabledChange = { enabled ->
                    scope.launch { progressStore.setEchoEnabled(enabled) }
                },
                onReset = { scope.launch { progressStore.resetProgress() } },
                onDismiss = { parentPanel = false },
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
