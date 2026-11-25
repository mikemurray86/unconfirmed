package app.unconfirmed.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unconfirmed.shared.Biome
import app.unconfirmed.shared.GeoPoint
import app.unconfirmed.shared.SkyState
import app.unconfirmed.shared.TimeOfDay
import app.unconfirmed.shared.Weather
import app.unconfirmed.shared.compassPoint
import java.util.Locale
import kotlin.math.abs

private val readoutBackground = Color(0xFF0C0F0D).copy(alpha = 0.62f)

fun Biome.label(): String = when (this) {
    Biome.FOREST -> "Deep woods"
    Biome.WETLAND -> "Marshland"
    Biome.WATERSIDE -> "Waterside"
    Biome.URBAN -> "In town"
    Biome.GRASSLAND -> "Open fields"
    Biome.ROCKY -> "Bare ground"
    Biome.OPEN -> "Open country"
}

fun TimeOfDay.label(): String = when (this) {
    TimeOfDay.NIGHT -> "Night"
    TimeOfDay.DAWN -> "Dawn"
    TimeOfDay.DAY -> "Daylight"
    TimeOfDay.DUSK -> "Dusk"
}

fun Weather.label(): String = when (this) {
    Weather.CLEAR -> "Clear"
    Weather.CLOUDY -> "Overcast"
    Weather.FOG -> "Fog"
    Weather.DRIZZLE -> "Drizzle"
    Weather.RAIN -> "Rain"
    Weather.SNOW -> "Snow"
    Weather.STORM -> "Storm"
}

fun coordinates(point: GeoPoint): String = String.format(
    Locale.US,
    "%.2f°%s %.2f°%s",
    abs(point.lat),
    if (point.lat >= 0) "N" else "S",
    abs(point.lon),
    if (point.lon >= 0) "E" else "W",
)

/**
 * A field recorder's strip of notes over the scene: where you are, what kind of country it is,
 * and the light and weather. In debug builds, tap to cycle the weather and long-press to cycle
 * the time of day, to preview every look.
 */
@Composable
fun FieldReadout(
    point: GeoPoint?,
    biome: Biome,
    sky: SkyState,
    modifier: Modifier = Modifier,
    onDebugTap: (() -> Unit)? = null,
    onDebugLongPress: (() -> Unit)? = null,
) {
    val conditions = listOf(biome.label(), sky.timeOfDay.label(), sky.weather.label()).joinToString(" · ")
    val text = point?.let { coordinates(it) + "\n" + conditions } ?: conditions
    val gestures = if (onDebugTap != null || onDebugLongPress != null) {
        Modifier.pointerInput(onDebugTap, onDebugLongPress) {
            detectTapGestures(onTap = { onDebugTap?.invoke() }, onLongPress = { onDebugLongPress?.invoke() })
        }
    } else {
        Modifier
    }
    Text(
        text,
        fontFamily = FieldFont,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
        maxLines = 2,
        lineHeight = 16.sp,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(readoutBackground)
            .then(gestures)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/** A small compass that turns as the player does, with north picked out in amber. */
@Composable
fun HeadingDial(headingDegrees: Double, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
            .semantics { contentDescription = "Facing ${compassPoint(headingDegrees)}" },
    ) {
        val r = size.minDimension / 2
        drawCircle(Color(0xFFB1B3A4).copy(alpha = 0.5f), r * 0.82f, style = Stroke(1.dp.toPx()))
        rotate(-headingDegrees.toFloat()) {
            val north = Path().apply {
                moveTo(center.x, center.y - r * 0.7f)
                lineTo(center.x + r * 0.18f, center.y)
                lineTo(center.x - r * 0.18f, center.y)
                close()
            }
            val south = Path().apply {
                moveTo(center.x, center.y + r * 0.7f)
                lineTo(center.x + r * 0.18f, center.y)
                lineTo(center.x - r * 0.18f, center.y)
                close()
            }
            drawPath(north, Amber)
            drawPath(south, Color(0xFFB1B3A4).copy(alpha = 0.6f))
            drawCircle(Color(0xFF0C0F0D), r * 0.07f, Offset(center.x, center.y))
        }
    }
}
