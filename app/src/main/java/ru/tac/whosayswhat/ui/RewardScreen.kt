package ru.tac.whosayswhat.ui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import ru.tac.whosayswhat.audio.SoundPlayer
import ru.tac.whosayswhat.ui.theme.Cream
import ru.tac.whosayswhat.ui.theme.Ink
import ru.tac.whosayswhat.ui.theme.Mint
import ru.tac.whosayswhat.ui.theme.Peach
import ru.tac.whosayswhat.ui.theme.Sky
import ru.tac.whosayswhat.ui.theme.Sunny
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val x: Float,
    val delay: Float,
    val speed: Float,
    val sway: Float,
    val phase: Float,
    val size: Float,
    val color: Color,
)

/** Сколько секунд радость висит сама, если ребёнок не нажал «Домой». */
private const val REWARD_VISIBLE_MS = 5_000L

/** Короткая награда без «петард» на весь экран: конфетти и крупное «Молодец!». */
@Composable
fun RewardScreen(
    soundPlayer: SoundPlayer,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnDone by rememberUpdatedState(onDone)

    // Похвала и отсчёт идут только пока экран на виду. Раньше корутина
    // досчитывала в фоне и уводила ребёнка в песочницу, пока он отвлёкся:
    // вернувшись, он уже не заставал «Молодец!».
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            soundPlayer.play("sounds/praise.wav")
            delay(REWARD_VISIBLE_MS)
            latestOnDone()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Cream).safeDrawingPadding()) {
        Confetti(modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier.align(Alignment.Center).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "🎉", fontSize = 96.sp)
            Text(
                text = "Молодец!",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Не ждём пяти секунд, если ребёнок хочет вернуться сразу.
            TextButton(onClick = onDone) {
                Text(text = "← Домой", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun Confetti(modifier: Modifier = Modifier) {
    val palette = listOf(Sunny, Mint, Sky, Peach, Color(0xFFFFAB91))
    val particles = remember {
        List(90) {
            ConfettiParticle(
                x = Random.nextFloat(),
                delay = Random.nextFloat() * 1.2f,
                speed = 0.18f + Random.nextFloat() * 0.22f,
                sway = 0.02f + Random.nextFloat() * 0.04f,
                phase = Random.nextFloat() * 6.28f,
                size = 8f + Random.nextFloat() * 10f,
                color = palette.random(),
            )
        }
    }
    var seconds by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now -> seconds = (now - start) / 1_000_000_000f }
        }
    }

    Canvas(modifier = modifier) {
        particles.forEach { particle ->
            val t = (seconds - particle.delay).coerceAtLeast(0f)
            if (t == 0f) return@forEach
            val y = ((t * particle.speed) % 1.2f) * size.height - 40f
            val x = size.width * (particle.x + particle.sway * 6f * sin(t * 2.2f + particle.phase))
            rotate(degrees = t * 220f + particle.phase * 30f, pivot = Offset(x, y)) {
                drawRect(
                    color = particle.color,
                    topLeft = Offset(x - particle.size / 2f, y - particle.size / 2f),
                    size = Size(particle.size, particle.size * 0.6f),
                )
            }
        }
    }
}
