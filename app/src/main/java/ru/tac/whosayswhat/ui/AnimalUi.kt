package ru.tac.whosayswhat.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.tac.whosayswhat.data.Animal
import ru.tac.whosayswhat.ui.theme.Ink
import ru.tac.whosayswhat.ui.theme.Mint
import ru.tac.whosayswhat.ui.theme.Teal
import kotlin.math.roundToInt

/**
 * Иллюстрация животного из assets. Пока PNG нет — показываем эмодзи-заглушку,
 * чтобы можно было играть и проверять логику. Финальные картинки просто
 * кладутся в assets/images по путям из catalog.json.
 */
@Composable
fun AnimalArtwork(
    animal: Animal,
    modifier: Modifier = Modifier,
    contentDescription: String? = animal.name,
) {
    val context = LocalContext.current
    val assetPath = remember(animal.image) { animalAssetPath(animal.image) }
    // Кэш читается прямо в композиции: иначе при каждом возврате на экран
    // карточка успевала мигнуть эмодзи-заглушкой.
    var image by remember(assetPath) { mutableStateOf(assetPath?.let(AnimalImages::get)) }
    LaunchedEffect(assetPath) {
        if (assetPath == null || image != null) return@LaunchedEffect
        image = withContext(Dispatchers.IO) {
            loadAssetImage(context, assetPath)?.also { decoded ->
                AnimalImages.put(assetPath, decoded)
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        } else {
            Text(
                text = animalEmoji(animal.id),
                fontSize = 64.sp,
                textAlign = TextAlign.Center,
                // Эмодзи — картинка-заглушка, а не подпись: TalkBack читал бы
                // «корова» дважды — из эмодзи и из contentDescription.
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}

/** Карточка животного: крупная зона нажатия, без текста и мелких деталей. */
@Composable
fun AnimalCard(
    animal: Animal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shakeKey: Int = 0,
    feedback: GuessFeedback = GuessFeedback.None,
    onClickLabel: String = "Послушать",
) {
    val shake = remember { Animatable(0f) }
    val interaction = remember { MutableInteractionSource() }
    val spoken = remember(animal.name, feedback) {
        guessCardSemantics(name = animal.name, feedback = feedback)
    }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "animalCardPressScale",
    )
    // Верный ответ подтверждается цветом рамки и лёгким «прыжком»: карточка
    // не должна выглядеть так же, как до нажатия, пока идёт похвала.
    val confirmed = feedback == GuessFeedback.Correct
    val borderColor by animateColorAsState(
        targetValue = if (confirmed) Teal else Ink.copy(alpha = 0.08f),
        animationSpec = tween(durationMillis = 180),
        label = "animalCardBorder",
    )
    val containerColor by animateColorAsState(
        targetValue = if (confirmed) Mint.copy(alpha = 0.35f) else Color.White,
        animationSpec = tween(durationMillis = 180),
        label = "animalCardContainer",
    )
    val confirmScale by animateFloatAsState(
        targetValue = if (confirmed) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "animalCardConfirmScale",
    )

    LaunchedEffect(shakeKey) {
        if (shakeKey > 0) {
            listOf(-14f, 14f, -10f, 10f, -5f, 0f).forEach { target ->
                shake.animateTo(target, tween(durationMillis = 55))
            }
        }
    }

    Card(
        modifier = modifier
            .graphicsLayer {
                val scale = pressScale * confirmScale
                scaleX = scale
                scaleY = scale
            }
            .offset { IntOffset(shake.value.roundToInt(), 0) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(if (confirmed) 4.dp else 3.dp, borderColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClickLabel = onClickLabel,
                    onClick = onClick,
                )
                // Слабовидящему ребёнку цвет ничего не говорит: исход нажатия
                // объявляет TalkBack. Тексты и условие объявления считает
                // [guessCardSemantics] — их проверяет unit-тест.
                .semantics {
                    // Имя — отдельной подписью, а действие — коротким
                    // «Выбрать»/«Послушать»: склейка вида «Выбрать Корова»
                    // читалась бы с ошибкой.
                    contentDescription = spoken.name
                    spoken.outcome?.let { stateDescription = it }
                    if (spoken.announceOutcome) liveRegion = LiveRegionMode.Polite
                }
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimalArtwork(
                animal = animal,
                modifier = Modifier.fillMaxSize(),
                // Карточка уже озвучивается через contentDescription; образ
                // не должен становиться второй остановкой TalkBack.
                contentDescription = null,
            )
        }
    }
}

fun animalEmoji(id: String): String = when (id) {
    "cow" -> "🐮"
    "cat" -> "🐱"
    "dog" -> "🐶"
    "duck" -> "🦆"
    "horse" -> "🐴"
    "rooster" -> "🐓"
    "sheep" -> "🐑"
    "pig" -> "🐷"
    "goat" -> "🐐"
    "frog" -> "🐸"
    "goose" -> "🦢"
    "donkey" -> "🐴"
    "owl" -> "🦉"
    "mouse" -> "🐭"
    "bee" -> "🐝"
    "turkey" -> "🦃"
    "chicken" -> "🐔"
    "wolf" -> "🐺"
    "bear" -> "🐻"
    "crow" -> "🐦"
    "cuckoo" -> "🐦"
    "elephant" -> "🐘"
    else -> "🐾"
}

private fun loadAssetImage(context: Context, assetPath: String): ImageBitmap? =
    runCatching {
        context.assets.open(assetPath).use { stream ->
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    }.getOrNull()
