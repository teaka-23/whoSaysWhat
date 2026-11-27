package ru.tac.whosayswhat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.tac.whosayswhat.data.Animal
import ru.tac.whosayswhat.ui.theme.Ink
import ru.tac.whosayswhat.ui.theme.Teal

/**
 * Одна фраза для TalkBack: «Корова. Му-у-у!». Пустые части опускаются,
 * чтобы не оставалось висящей точки.
 */
internal fun closeUpAnnouncement(name: String, onomatopoeia: String?): String =
    listOfNotNull(
        name.trim().takeIf { it.isNotEmpty() },
        onomatopoeia?.trim()?.takeIf { it.isNotEmpty() },
    ).joinToString(". ")

/**
 * Крупный план животного: увеличенная иллюстрация, название и «Му-у-у!».
 *
 * Появляется по тапу в песочнице и закрывается сам, когда звук и озвучка
 * названия доиграли. Ребёнку окно закрывать не нужно: тап по картинке
 * повторяет звук, а вернуться к животным можно тапом по затемнённому фону
 * или кнопкой Back.
 *
 * Тексты — подпись для взрослого: ребёнок 1,5–4 лет ещё не читает, он
 * рассматривает картинку и слушает. Название при этом остаётся каноническим,
 * как на карточках, — ласковое имя звучит только из синтезатора речи.
 */
@Composable
fun AnimalCloseUp(
    animal: Animal,
    onRepeat: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Карточка «вырастает» из центра: без полёта от карточки в сетке, потому
    // что сетка прокручивается и на слабом устройстве полёт дёргался бы.
    val visible = remember {
        MutableTransitionState(false).apply { targetState = true }
    }
    // На невысоких экранах (телефон в альбомной ориентации) тексты занимают
    // заметную долю высоты — уменьшаем и их, и картинку.
    val compact = LocalConfiguration.current.screenHeightDp < 520
    val onomatopoeia = animal.closeUpOnomatopoeia
    val backdropInteraction = remember { MutableInteractionSource() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            // Затемнённый фон — отдельный слой, а не родитель карточки:
            // clearAndSetSemantics стирает поддерево, и карточка потеряла бы
            // описание для TalkBack. Фон не озвучивается: вернуться можно
            // кнопкой Back.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Ink.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = backdropInteraction,
                        indication = null,
                        onClick = onDismiss,
                    )
                    .clearAndSetSemantics { },
            )
            AnimatedVisibility(
                visibleState = visible,
                enter = fadeIn(tween(durationMillis = 160)) +
                    scaleIn(
                        initialScale = 0.85f,
                        animationSpec = tween(durationMillis = 200),
                    ),
            ) {
                // Размер считаем от экрана: на планшете карточка не
                // растягивается на пол-экрана, а на телефоне остаются поля.
                BoxWithConstraints(Modifier.fillMaxSize().padding(16.dp)) {
                    val cardWidth = minOf(maxWidth * 0.92f, 520.dp)
                    val cardHeight = minOf(maxHeight * 0.88f, 640.dp)
                    Card(
                        onClick = onRepeat,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(width = cardWidth, height = cardHeight)
                            // Один узел вместо картинки, названия и
                            // звукоподражания по отдельности: TalkBack читает
                            // «Корова. Му-у-у!» и повторяет звук тем же
                            // действием клика.
                            .clearAndSetSemantics {
                                contentDescription = closeUpAnnouncement(
                                    name = animal.name,
                                    onomatopoeia = onomatopoeia,
                                )
                                role = Role.Button
                                onClick(label = "Повторить звук") {
                                    onRepeat()
                                    true
                                }
                            },
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AnimalArtwork(
                                animal = animal,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                // Описание задаёт карточка: у картинки оно
                                // было бы вторым, и TalkBack читал бы название
                                // дважды.
                                contentDescription = null,
                                fullSize = true,
                            )
                            Spacer(modifier = Modifier.height(if (compact) 4.dp else 12.dp))
                            Text(
                                text = animal.name,
                                fontSize = if (compact) 26.sp else 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ink,
                                textAlign = TextAlign.Center,
                            )
                            if (onomatopoeia != null) {
                                Text(
                                    text = onomatopoeia,
                                    fontSize = if (compact) 22.sp else 30.sp,
                                    fontWeight = FontWeight.Bold,
                                    // Бирюзовый на белом читается, а солнечный
                                    // жёлтый — нет: подпись должна быть видна и
                                    // при ярком свете.
                                    color = Teal,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
