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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.tac.whosayswhat.audio.SoundPlayer
import ru.tac.whosayswhat.game.Answer
import ru.tac.whosayswhat.game.SessionEngine
import ru.tac.whosayswhat.ui.theme.Cream
import ru.tac.whosayswhat.ui.theme.Ink
import ru.tac.whosayswhat.ui.theme.Sky
import ru.tac.whosayswhat.ui.theme.Teal

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
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var round by remember { mutableStateOf(engine.start()) }
    var solved by remember { mutableIntStateOf(0) }
    var locked by remember { mutableStateOf(false) }
    var wrongAnimalId by remember { mutableStateOf<String?>(null) }
    // Верное животное подсвечивается на время похвалы, до следующего раунда.
    var correctAnimalId by remember { mutableStateOf<String?>(null) }
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

    Box(modifier = modifier.fillMaxSize().background(Cream).safeDrawingPadding()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                // Верхняя строка с «Домой», прогрессом и замком не должна
                // перекрывать круг со звуком даже на 320dp.
                .padding(start = 16.dp, end = 16.dp, top = 64.dp, bottom = 16.dp),
        ) {
            val layout = guessLayout(
                maxWidth = maxWidth,
                maxHeight = maxHeight,
                options = round.options.size,
            )
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (layout.scrollable) {
                            Modifier.verticalScroll(scrollState)
                        } else {
                            Modifier
                        },
                    ),
                verticalArrangement = if (layout.scrollable) {
                    // Прокрутка начинается сверху: центрировать нечего.
                    Arrangement.spacedBy(GUESS_PROMPT_GAP)
                } else {
                    Arrangement.spacedBy(GUESS_PROMPT_GAP, Alignment.CenterVertically)
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(layout.promptSize)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                        .shadow(elevation = 8.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(Sky)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Повторить звук",
                        ) {
                            soundPlayer.play(round.target.gameSoundFile)
                        }
                        // Подпись задана явно: эмодзи внутри — украшение,
                        // и без этого TalkBack читал бы «динамик, громкость
                        // высокая» вместо понятного действия.
                        .semantics { contentDescription = "Повторить звук" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "🔊",
                        fontSize = 64.sp,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }

                // Карточки раскладываются по columns из [guessLayout]:
                // в портрете это 2×N, в ландшафте — один ряд из 3–4.
                Column(verticalArrangement = Arrangement.spacedBy(GUESS_CARD_GAP)) {
                    round.options.chunked(layout.columns).forEach { rowAnimals ->
                        Row(
                            modifier = Modifier.fillMaxWidth().height(layout.cardHeight),
                            horizontalArrangement = Arrangement.spacedBy(GUESS_CARD_GAP),
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
                                                correctAnimalId = animal.id
                                                solved++
                                                val step = correctAnswerStep(solved)
                                                if (step.finishSession) {
                                                    // Награда открывается сразу и сама
                                                    // играет похвалу: пауза здесь сыграла
                                                    // бы тот же файл второй раз.
                                                    onSessionFinished()
                                                } else {
                                                    scope.launch {
                                                        soundPlayer.play("sounds/praise.wav")
                                                        delay(step.pauseMs)
                                                        round = engine.start()
                                                        locked = false
                                                        // Новая цель — чистая доска: иначе
                                                        // подсветка останется на прежней карточке.
                                                        correctAnimalId = null
                                                        wrongAnimalId = null
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    shakeKey = if (wrongAnimalId == animal.id) shakeKey else 0,
                                    feedback = guessFeedback(
                                        animalId = animal.id,
                                        correctId = correctAnimalId,
                                        wrongId = wrongAnimalId,
                                    ),
                                    onClickLabel = "Выбрать",
                                )
                            }
                            repeat(layout.columns - rowAnimals.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(start = 16.dp, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.8f))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Выйти в песочницу",
                        onClick = onExit,
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "← Домой",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Ink,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            RoundProgress(solved = solved)

            Spacer(modifier = Modifier.width(4.dp))

            ParentGateButton(onOpen = onOpenParentPanel)
        }
    }
}

/** Пять точек вверху: ребёнок видит, сколько раундов уже позади. */
@Composable
private fun RoundProgress(solved: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.75f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .semantics {
                contentDescription = "Пройдено $solved из $ROUNDS_PER_SESSION"
            },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(ROUNDS_PER_SESSION) { index ->
            Box(
                modifier = Modifier
                    .size(if (index < solved) 11.dp else 9.dp)
                    .clip(CircleShape)
                    .background(if (index < solved) Teal else Ink.copy(alpha = 0.18f)),
            )
        }
    }
}
