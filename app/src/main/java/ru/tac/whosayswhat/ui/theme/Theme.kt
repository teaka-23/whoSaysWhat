package ru.tac.whosayswhat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Cream = Color(0xFFFFF8EE)
val Ink = Color(0xFF4C3D35)
val Mint = Color(0xFF80CBC4)
val Teal = Color(0xFF00695C)
val Sunny = Color(0xFFFFD180)
val Sky = Color(0xFFB3E5FC)
val Peach = Color(0xFFFFCCBC)

private val KidColorScheme = lightColorScheme(
    // Тёмный бирюзовый для текстовых кнопок, переключателей и радио:
    // светлый Mint на белом даёт слишком низкий контраст.
    primary = Teal,
    onPrimary = Color.White,
    secondary = Sunny,
    onSecondary = Ink,
    background = Cream,
    onBackground = Ink,
    surface = Color(0xFFFFFDF8),
    onSurface = Ink,
    surfaceVariant = Color(0xFFF2E7D8),
    onSurfaceVariant = Color(0xFF6D5A4E),
)

@Composable
fun WhoSaysWhatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KidColorScheme,
        typography = Typography(),
        content = content,
    )
}
