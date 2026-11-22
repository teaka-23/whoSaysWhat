package ru.tac.whosayswhat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Cream = Color(0xFFF5F6FA)
val Ink = Color(0xFF37474F)
val Mint = Color(0xFF80CBC4)
val Sunny = Color(0xFFFFD180)
val Sky = Color(0xFFB3E5FC)
val Peach = Color(0xFFFFCCBC)

private val KidColorScheme = lightColorScheme(
    primary = Mint,
    onPrimary = Color(0xFF00332F),
    secondary = Sunny,
    onSecondary = Ink,
    background = Cream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE8EDF2),
    onSurfaceVariant = Color(0xFF546E7A),
)

@Composable
fun WhoSaysWhatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KidColorScheme,
        typography = Typography(),
        content = content,
    )
}
