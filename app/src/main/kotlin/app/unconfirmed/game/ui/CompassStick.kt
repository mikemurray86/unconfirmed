package app.unconfirmed.game.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import app.unconfirmed.shared.compassPoint
import kotlin.math.cos
import kotlin.math.sin

/** What the stick mode is called on screen. Walking with GPS is "On foot". */
const val SCOUTING_LABEL = "Scouting"
const val ON_FOOT_LABEL = "On foot"

/** How big the control is once it has opened out into the stick. */
val COMPASS_STICK_SIZE: Dp = 132.dp

private val DIAL_RADIUS = 22.dp
private val PAD_RADIUS = 60.dp
private val DialPale = Color(0xFFB1B3A4)
private val DialInk = Color(0xFF0C0F0D)

/** How long the compass takes to open out into the stick, or fold back. */
private const val SPLIT_MILLIS = 650

/**
 * A small compass in the corner while the player is on foot. While [scouting], its points split
 * out to the rim and its hub becomes a thumb stick, and back again on foot, so the control itself
 * shows which mode the game is in. On the stick the amber point is forward, and a small N on the
 * rim still shows where north is.
 *
 * [onStick] gets where the thumb is, from -1 to 1 each way with forward as negative y, and zero
 * when it lets go. [stick] is that same value, for drawing the thumb.
 */
@Composable
fun CompassStick(
    headingDegrees: Double?,
    scouting: Boolean,
    stick: Offset,
    onStick: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val split by animateFloatAsState(
        if (scouting) 1f else 0f,
        tween(SPLIT_MILLIS, easing = FastOutSlowInEasing),
        label = "compass split",
    )
    val text = rememberTextMeasurer()
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val report by rememberUpdatedState(onStick)
    val heading = headingDegrees ?: 0.0
    val description = when {
        scouting -> "Scouting stick. Push up to walk forward, left or right to turn. Facing ${compassPoint(heading)}"
        headingDegrees != null -> "Facing ${compassPoint(headingDegrees)}"
        else -> "Compass"
    }

    Canvas(
        modifier
            .size(COMPASS_STICK_SIZE)
            .semantics { contentDescription = description }
            .pointerInput(scouting) {
                if (!scouting) return@pointerInput
                val center = Offset(size.width / 2f, size.height / 2f)
                val travel = PAD_RADIUS.toPx() * 0.6f
                fun at(position: Offset): Offset {
                    val pull = (position - center) / travel
                    val length = pull.getDistance()
                    return if (length > 1f) pull / length else pull
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if ((down.position - center).getDistance() > PAD_RADIUS.toPx() * 1.15f) return@awaitEachGesture
                    down.consume()
                    try {
                        report(at(down.position))
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            report(at(change.position))
                        }
                    } finally {
                        report(Offset.Zero)
                    }
                }
            },
    ) {
        val small = DIAL_RADIUS.toPx()
        val r = lerp(small, PAD_RADIUS.toPx(), split)
        // The dial sits in the bottom corner, and opens out to fill the space.
        val center = Offset(
            lerp(small, size.width / 2, split),
            lerp(size.height - small, size.height / 2, split),
        )
        drawCircle(surface.copy(alpha = lerp(0.85f, 0.28f, split)), r, center)
        drawCircle(DialPale.copy(alpha = 0.5f), r * 0.82f, center, style = Stroke(1.dp.toPx()))

        // On foot the needle turns with the player. Opening out, it swings round to forward.
        val needle = turnDegrees(0.0, -heading).toFloat() * (1 - split)
        rotate(needle, center) {
            point(center, r, 0f, split, Amber, long = true)
            point(center, r, 180f, split, DialPale.copy(alpha = 0.6f), long = true)
            point(center, r, 90f, split, DialPale.copy(alpha = 0.45f), long = false)
            point(center, r, 270f, split, DialPale.copy(alpha = 0.45f), long = false)
        }

        if (split > 0f) {
            // North stays on the rim of the stick, so it's still a compass.
            val north = Math.toRadians(-heading)
            val at = center + Offset(sin(north).toFloat(), -cos(north).toFloat()) * r * 0.62f
            val label = text.measure(
                "N",
                TextStyle(fontFamily = FieldFont, fontSize = 11.sp, color = Amber.copy(alpha = split)),
            )
            drawText(label, topLeft = at - Offset(label.size.width / 2f, label.size.height / 2f))
        }

        // The hub grows into the thumb, which follows the player's thumb once scouting.
        val thumb = center + stick * (r * 0.6f) * split
        val hub = lerp(r * 0.07f, r * 0.3f, split)
        drawCircle(lerp(DialInk, DialPale.copy(alpha = 0.9f), split), hub, thumb)
        if (split > 0f) drawCircle(Amber.copy(alpha = 0.8f * split), hub, thumb, style = Stroke(1.5.dp.toPx()))
    }
}

/**
 * One compass point at [degrees] round from the top. Folded ([split] 0) it's a needle from the
 * hub, [long] for north and south. Opened out (1) it's a small arrowhead on the rim.
 */
private fun DrawScope.point(center: Offset, r: Float, degrees: Float, split: Float, color: Color, long: Boolean) {
    val tip = lerp(if (long) 0.7f else 0.42f, 0.95f, split) * r
    val base = lerp(0f, 0.8f, split) * r
    val half = lerp(if (long) 0.18f else 0.11f, 0.13f, split) * r
    rotate(degrees, center) {
        val path = Path().apply {
            moveTo(center.x, center.y - tip)
            lineTo(center.x + half, center.y - base)
            lineTo(center.x - half, center.y - base)
            close()
        }
        drawPath(path, color)
    }
}

/** A two-way switch between walking on foot and scouting with the stick. */
@Composable
fun MoveModeSwitch(scouting: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(DialInk.copy(alpha = 0.62f))
            .padding(3.dp),
    ) {
        listOf(false to ON_FOOT_LABEL, true to SCOUTING_LABEL).forEach { (mode, label) ->
            val chosen = mode == scouting
            Text(
                label,
                fontFamily = FieldFont,
                fontSize = 12.sp,
                color = if (chosen) DialInk else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (chosen) Amber else Color.Transparent)
                    .semantics { selected = chosen }
                    .clickable(role = Role.Tab) { if (!chosen) onChange(mode) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}
