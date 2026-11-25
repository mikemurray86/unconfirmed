package app.unconfirmed.game.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import app.unconfirmed.game.R

val Amber = Color(0xFFE0A43A)
val Moss = Color(0xFF7FA66A)
val Signal = Color(0xFF6FC7D9)
val Warning = Color(0xFFE2725B)

/** A worn typewriter face for field notes: the scene readout and case-file headings. */
val FieldFont = FontFamily(Font(R.font.special_elite))

private val Colors = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF1B1408),
    secondary = Moss,
    tertiary = Signal,
    background = Color(0xFF0C0F0D),
    onBackground = Color(0xFFE6E2D6),
    surface = Color(0xFF151A16),
    onSurface = Color(0xFFE6E2D6),
    surfaceVariant = Color(0xFF232A24),
    onSurfaceVariant = Color(0xFFB1B3A4),
    error = Warning,
)

@Composable
fun UnconfirmedTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
