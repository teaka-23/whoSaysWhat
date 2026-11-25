package ru.tac.whosayswhat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.tac.whosayswhat.data.Animal
import ru.tac.whosayswhat.data.EchoTarget
import ru.tac.whosayswhat.ui.theme.Cream
import ru.tac.whosayswhat.ui.theme.Ink
import ru.tac.whosayswhat.ui.theme.Peach
import ru.tac.whosayswhat.ui.theme.Sunny

/**
 * Песочница: ребёнок сам нажимает на животных и слышит звуки.
 * Никакого текста для ребёнка; кнопки «Играть» и «Выйти» — для взрослого.
 */
@Composable
fun SandboxScreen(
    animals: List<Animal>,
    echoEnabled: Boolean,
    onAnimalSound: (Animal) -> Unit,
    onStartGame: () -> Unit,
    onEcho: (Animal) -> Unit,
    onOpenParentPanel: () -> Unit,
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier,
    preferredEchoAnimalId: String? = null,
) {
    var lastAnimal by remember { mutableStateOf<Animal?>(null) }
    var confirmExit by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(Cream).safeDrawingPadding()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Сверху оставляем место для кнопки «Выйти» и замка
                // родительской панели, чтобы они не перекрывали карточку
                // животного.
                .padding(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 12.dp),
        ) {
            // На телефоне — две колонки, на планшете — больше: карточки
            // остаются крупными, а 22 животных просто прокручиваются.
            // 150dp — порог, при котором две колонки помещаются даже на
            // бюджетном телефоне шириной 360dp.
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                items(animals, key = { it.id }) { animal ->
                    AnimalCard(
                        animal = animal,
                        onClick = {
                            lastAnimal = animal
                            onAnimalSound(animal)
                        },
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onStartGame,
                    modifier = Modifier.weight(1f).height(72.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Sunny,
                        contentColor = Ink,
                    ),
                ) {
                    Text(text = "Играть", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                if (echoEnabled) {
                    // Явный выбор родителя имеет приоритет. Если его нет,
                    // работает привычное «последнее нажатое животное».
                    val echoAnimal = EchoTarget.resolve(
                        animals = animals,
                        preferredId = preferredEchoAnimalId,
                        lastTapped = lastAnimal,
                    )
                    Button(
                        onClick = { echoAnimal?.let(onEcho) },
                        modifier = Modifier
                            .size(72.dp)
                            .semantics {
                                contentDescription = if (echoAnimal != null) {
                                    "Эхо: повторить звук ${echoAnimal.name}"
                                } else {
                                    "Эхо"
                                }
                            },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Peach,
                            contentColor = Ink,
                        ),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🎤", fontSize = 24.sp)
                            Text(
                                text = "Эхо",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        // Выход живёт рядом с замком родителей, но слева: родителю один тап,
        // а ребёнка останавливает подтверждение. Кнопка приглушённая, чтобы не
        // перетягивать внимание с животных.
        Button(
            onClick = { confirmExit = true },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 10.dp)
                .height(40.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Ink.copy(alpha = 0.08f),
                contentColor = Ink,
            ),
            contentPadding = PaddingValues(horizontal = 18.dp),
        ) {
            Text(text = "Выйти", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }

        ParentGateButton(
            onOpen = onOpenParentPanel,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
        )
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text(text = "Выйти из игры?") },
            text = {
                Text(
                    text = "Звёзды и уровень сохранятся — при следующем " +
                        "запуске всё будет на месте.",
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
                    Text(text = "Остаться")
                }
            },
        )
    }
}
