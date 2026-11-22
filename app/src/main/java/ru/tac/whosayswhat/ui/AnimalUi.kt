package ru.tac.whosayswhat.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.tac.whosayswhat.data.Animal
import ru.tac.whosayswhat.ui.theme.Ink
import kotlin.math.roundToInt

/**
 * Иллюстрация животного из assets. Пока PNG нет — показываем эмодзи-заглушку,
 * чтобы можно было играть и проверять логику. Финальные картинки просто
 * кладутся в assets/images по путям из catalog.json.
 */
@Composable
fun AnimalArtwork(animal: Animal, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var image by remember(animal.image) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(animal.image) {
        image = withContext(Dispatchers.IO) { loadAssetImage(context, animal.image) }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = animal.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        } else {
            Text(
                text = animalEmoji(animal.id),
                fontSize = 64.sp,
                textAlign = TextAlign.Center,
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
) {
    val shake = remember { Animatable(0f) }
    val interaction = remember { MutableInteractionSource() }

    LaunchedEffect(shakeKey) {
        if (shakeKey > 0) {
            listOf(-14f, 14f, -10f, 10f, -5f, 0f).forEach { target ->
                shake.animateTo(target, tween(durationMillis = 55))
            }
        }
    }

    Card(
        modifier = modifier.offset { IntOffset(shake.value.roundToInt(), 0) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(3.dp, Ink.copy(alpha = 0.08f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimalArtwork(animal = animal, modifier = Modifier.fillMaxSize())
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
    else -> "🐾"
}

private fun loadAssetImage(context: Context, path: String?): ImageBitmap? {
    val assetPath = path?.removePrefix("assets/")?.takeIf { it.isNotBlank() } ?: return null
    return runCatching {
        context.assets.open(assetPath).use { stream ->
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    }.getOrNull()
}
