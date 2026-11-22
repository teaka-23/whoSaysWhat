package ru.tac.whosayswhat.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.tac.whosayswhat.audio.SoundPlayer
import ru.tac.whosayswhat.game.Answer
import ru.tac.whosayswhat.game.SessionEngine
import ru.tac.whosayswhat.ui.theme.Cream
import ru.tac.whosayswhat.ui.theme.Sky

/** Сколько правильных ответов до экрана награды. */
const val ROUNDS_PER_SESSION = 5

/**
 * Экран «Угадай»: сверху большая кнопка повтора звука, снизу карточки.
 * Ошибка не наказывает — карточка мягко качается, раунд продолжается.
 */
@Composable
fun GuessScreen(
    engine: SessionEngine,
    soundPlayer: SoundPlayer,
    onAnswer: (animalId: String, correct: Boolean) -> Unit,
    onSessionFinished: () -> Unit,
    onOpenParentPanel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var round by remember { mutableStateOf(engine.start()) }
    var solved by remember { mutableIntStateOf(0) }
    var locked by remember { mutableStateOf(false) }
    var wrongAnimalId by remember { mutableStateOf<String?>(null) }
    var shakeKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    LaunchedEffect(round) {
        delay(350)
        soundPlayer.play(round.target.gameSoundFile)
    }

    Box(modifier = modifier.fillMaxSize().background(Cream)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val gap = 16.dp
            val promptSize = 148.dp
            val promptGap = 24.dp
            val rows = (round.options.size + 1) / 2
            val cellWidth = (maxWidth - gap) / 2
            val availableForCards = maxHeight - promptSize - promptGap
            // Высота карточек ограничена и шириной (чтобы не были вытянутыми),
            // и доступной высотой (чтобы 3–4 карточки всегда помещались целиком).
            val cellHeight = if (rows > 0) {
                minOf(
                    (availableForCards - gap * (rows - 1)) / rows,
                    cellWidth / 0.82f,
                ).coerceAtLeast(96.dp)
            } else {
                120.dp
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(
                    promptGap,
                    Alignment.CenterVertically,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(promptSize)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                        .shadow(elevation = 8.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(Sky)
                        .clickable { soundPlayer.play(round.target.gameSoundFile) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "🔊", fontSize = 64.sp)
                }

                // 2×N без прокрутки: уровни на 2, 3 и 4 карточки помещаются
                // целиком, а зоны нажатия остаются крупными для малыша.
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    round.options.chunked(2).forEach { rowAnimals ->
                        Row(
                            modifier = Modifier.fillMaxWidth().height(cellHeight),
                            horizontalArrangement = Arrangement.spacedBy(gap),
                        ) {
                            rowAnimals.forEach { animal ->
                                AnimalCard(
                                    animal = animal,
                                    onClick = {
                                        if (locked) return@AnimalCard
                                        when (engine.answer(animal.id)) {
                                            Answer.Wrong -> {
                                                onAnswer(round.target.id, false)
                                                wrongAnimalId = animal.id
                                                shakeKey++
                                                soundPlayer.play("sounds/nudge.wav")
                                            }

                                            is Answer.Correct -> {
                                                onAnswer(round.target.id, true)
                                                locked = true
                                                solved++
                                                val isLast = solved >= ROUNDS_PER_SESSION
                                                scope.launch {
                                                    soundPlayer.play("sounds/praise.wav")
                                                    delay(900)
                                                    if (isLast) {
                                                        onSessionFinished()
                                                    } else {
                                                        round = engine.start()
                                                        locked = false
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    shakeKey = if (wrongAnimalId == animal.id) shakeKey else 0,
                                )
                            }
                            if (rowAnimals.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        ParentGateButton(
            onOpen = onOpenParentPanel,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
        )
    }
}
