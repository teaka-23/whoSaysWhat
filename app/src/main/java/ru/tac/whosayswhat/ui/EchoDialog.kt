package ru.tac.whosayswhat.ui

import android.Manifest
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import ru.tac.whosayswhat.audio.ECHO_MAX_DURATION_MS
import ru.tac.whosayswhat.audio.EchoRecorder
import ru.tac.whosayswhat.audio.SoundPlayer
import ru.tac.whosayswhat.audio.hasRecordAudioPermission
import ru.tac.whosayswhat.data.Animal
import ru.tac.whosayswhat.ui.theme.Ink
import ru.tac.whosayswhat.ui.theme.Peach
import ru.tac.whosayswhat.ui.theme.Sky
import ru.tac.whosayswhat.ui.theme.Sunny
import java.io.File

private sealed interface EchoUiState {
    data object Idle : EchoUiState
    data object Recording : EchoUiState
    data class Recorded(val file: File) : EchoUiState
}

/**
 * Режим «Эхо»: ребёнок слушает животное, повторяет звук и сразу слышит себя.
 *
 * Микрофон запрашивается только здесь, после явного объяснения. Запись
 * остаётся в cacheDir и удаляется при закрытии диалога, а после аварийного
 * завершения процесса — при следующем запуске приложения.
 */
@Composable
fun EchoDialog(
    animal: Animal,
    soundPlayer: SoundPlayer,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val recorder = remember { EchoRecorder(context) }

    var state by remember { mutableStateOf<EchoUiState>(EchoUiState.Idle) }
    var playing by remember { mutableStateOf(false) }
    var hasPlayed by remember { mutableStateOf(false) }
    var remainingMs by remember { mutableLongStateOf(ECHO_MAX_DURATION_MS) }
    var message by remember { mutableStateOf<String?>(null) }

    var permissionGranted by remember { mutableStateOf(context.hasRecordAudioPermission()) }
    var showPermissionIntro by remember { mutableStateOf(!permissionGranted) }
    // На невысоких телефонах уменьшаем иллюстрацию, чтобы кнопки
    // «Разрешить микрофон» и «Только слушать» помещались без прокрутки.
    val compact = LocalConfiguration.current.screenHeightDp < 780

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionGranted = granted
        if (!granted) {
            message = "Без микрофона можно слушать звук, но не записывать голос."
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder.release()
            // release() удаляет текущую запись, а cleanup() подчищает всё,
            // что могло остаться от предыдущих аварийных завершений.
            EchoRecorder.cleanup(context)
        }
    }

    fun finishRecording() {
        if (state !is EchoUiState.Recording) return
        val file = recorder.stop()
        remainingMs = ECHO_MAX_DURATION_MS
        if (file == null) {
            state = EchoUiState.Idle
            message = "Запись слишком короткая — попробуйте ещё раз."
            return
        }
        state = EchoUiState.Recorded(file)
        message = null
        // Не запускаем воспроизведение автоматически: ребёнок сам решает,
        // когда нажать «Послушать себя». Это и безопаснее для слуха, и
        // понятнее — после «Стоп» сразу видно, что делать дальше.
        playing = false
        hasPlayed = false
    }

    fun beginRecording() {
        if (!permissionGranted) {
            showPermissionIntro = true
            return
        }
        soundPlayer.stop()
        recorder.stopPlayback()
        message = null
        if (recorder.start()) {
            state = EchoUiState.Recording
            remainingMs = ECHO_MAX_DURATION_MS
            playing = false
            hasPlayed = false
        } else {
            message = "Не удалось включить микрофон. Проверьте, что он не занят другим приложением."
        }
    }

    fun replay(file: File) {
        hasPlayed = true
        playing = recorder.play(file) { playing = false }
        if (!playing) message = "Не удалось воспроизвести запись."
    }

    LaunchedEffect(state) {
        if (state is EchoUiState.Recording) {
            val startedAt = SystemClock.elapsedRealtime()
            while (true) {
                val elapsed = SystemClock.elapsedRealtime() - startedAt
                remainingMs = (ECHO_MAX_DURATION_MS - elapsed).coerceAtLeast(0L)
                if (remainingMs == 0L) {
                    finishRecording()
                    break
                }
                delay(100)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .padding(20.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .heightIn(max = 560.dp),
            shape = RoundedCornerShape(32.dp),
            color = Color.White,
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Эхо",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.semantics {
                            contentDescription = "Закрыть"
                        },
                    ) {
                        Text(text = "✕", fontSize = 22.sp, color = Ink)
                    }
                }

                if (showPermissionIntro) {
                    // На экране разрешения не занимаем полэкрана картинкой:
                    // важнее объяснение и обе кнопки — «Разрешить» и
                    // «Только слушать».
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AnimalArtwork(
                            animal = animal,
                            modifier = Modifier.size(64.dp),
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = animal.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ink,
                            )
                            if (animal.onomatopoeiaText.isNotBlank()) {
                                Text(
                                    text = "«${animal.onomatopoeiaText}»",
                                    fontSize = 16.sp,
                                    color = Ink.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                } else {
                    AnimalArtwork(
                        animal = animal,
                        modifier = Modifier.size(if (compact) 104.dp else 148.dp),
                        contentDescription = null,
                    )
                    Text(
                        text = animal.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                    )
                    if (animal.onomatopoeiaText.isNotBlank()) {
                        Text(
                            text = "«${animal.onomatopoeiaText}»",
                            fontSize = 19.sp,
                            color = Ink.copy(alpha = 0.7f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (compact) 8.dp else 16.dp))

                when {
                    showPermissionIntro -> PermissionIntro(
                        onAllow = {
                            showPermissionIntro = false
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onSkip = { showPermissionIntro = false },
                    )

                    state is EchoUiState.Recording -> RecordingContent(
                        remainingMs = remainingMs,
                        onStop = ::finishRecording,
                    )

                    state is EchoUiState.Recorded -> RecordedContent(
                        playing = playing,
                        hasPlayed = hasPlayed,
                        onReplay = { replay((state as EchoUiState.Recorded).file) },
                        onRecordAgain = ::beginRecording,
                        onDone = onDismiss,
                    )

                    else -> IdleContent(
                        permissionGranted = permissionGranted,
                        onListen = { soundPlayer.play(animal.gameSoundFile) },
                        onRecord = ::beginRecording,
                    )
                }

                if (!permissionGranted && !showPermissionIntro) {
                    Text(
                        text = "Микрофон не разрешён — запись недоступна.",
                        modifier = Modifier.padding(top = 8.dp),
                        fontSize = 13.sp,
                        color = Ink.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            permissionGranted = context.hasRecordAudioPermission()
                        }) {
                            Text(text = "Проверить", fontSize = 14.sp)
                        }
                        TextButton(onClick = { context.openAppSettings() }) {
                            Text(text = "Настройки", fontSize = 14.sp)
                        }
                    }
                }

                message?.let { text ->
                    Text(
                        text = text,
                        modifier = Modifier.padding(top = 8.dp),
                        fontSize = 14.sp,
                        color = Color(0xFFB3261E),
                        textAlign = TextAlign.Center,
                    )
                }

                Text(
                    text = "Запись остаётся на устройстве и удаляется при закрытии. " +
                        "При аварийном завершении временные файлы очищаются " +
                        "при следующем запуске.",
                    modifier = Modifier.padding(top = 12.dp),
                    fontSize = 12.sp,
                    color = Ink.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun PermissionIntro(onAllow: () -> Unit, onSkip: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Зачем микрофон?",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )
        Text(
            text = "В режиме «Эхо» приложение записывает голос ребёнка на 5 секунд, " +
                "чтобы он услышал, как повторил звук животного. Запись не покидает " +
                "устройство и удаляется после закрытия окна. Доступа в интернет у " +
                "приложения нет.",
            fontSize = 14.sp,
            color = Ink.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        EchoButton(
            text = "Разрешить микрофон",
            containerColor = Sunny,
            onClick = onAllow,
        )
        TextButton(onClick = onSkip) {
            Text(text = "Только слушать", fontSize = 15.sp)
        }
    }
}

@Composable
private fun IdleContent(
    permissionGranted: Boolean,
    onListen: () -> Unit,
    onRecord: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Повтори звук и послушай себя",
            fontSize = 15.sp,
            color = Ink.copy(alpha = 0.7f),
        )
        EchoButton(text = "🔊 Послушать звук", containerColor = Sky, onClick = onListen)
        EchoButton(
            text = if (permissionGranted) "🎤 Записать голос" else "🎤 Разрешить и записать",
            containerColor = Peach,
            onClick = onRecord,
        )
    }
}

@Composable
private fun RecordingContent(remainingMs: Long, onStop: () -> Unit) {
    val fraction = 1f - remainingMs.toFloat() / ECHO_MAX_DURATION_MS
    val secondsLeft = ((remainingMs + 999) / 1000).toInt()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Говори!",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Ink.copy(alpha = 0.1f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(Peach),
            )
        }
        Text(
            text = "Осталось $secondsLeft с",
            fontSize = 16.sp,
            color = Ink.copy(alpha = 0.7f),
        )
        EchoButton(text = "Стоп", containerColor = Sunny, onClick = onStop)
    }
}

@Composable
private fun RecordedContent(
    playing: Boolean,
    hasPlayed: Boolean,
    onReplay: () -> Unit,
    onRecordAgain: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Послушай себя",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )
        EchoButton(
            text = when {
                playing -> "▶ Играет…"
                hasPlayed -> "▶ Послушать ещё раз"
                else -> "▶ Послушать себя"
            },
            containerColor = Sky,
            onClick = onReplay,
        )
        EchoButton(text = "🎤 Записать снова", containerColor = Peach, onClick = onRecordAgain)
        TextButton(onClick = onDone) {
            Text(text = "Готово", fontSize = 16.sp)
        }
    }
}

@Composable
private fun EchoButton(text: String, containerColor: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Ink,
        ),
    ) {
        Text(text = text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}
