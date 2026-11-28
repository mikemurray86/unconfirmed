package app.unconfirmed.game.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.unconfirmed.shared.Lure
import kotlin.math.roundToInt

private val Wood = Color(0xFF8A6A45)
private val WoodDark = Color(0xFF5E4630)
private val Flesh = Color(0xFFB4544A)
private val Bone = Color(0xFFE8E0CC)
private val Scale = Color(0xFF9DB3BA)
private val Brass = Color(0xFFC79A3E)
private val LureInk = Color(0xFF0C0F0D)

/** A small drawing of [lure], in the same inked style as the clue art. */
@Composable
fun LureIcon(lure: Lure, modifier: Modifier = Modifier) {
    Canvas(modifier.size(40.dp)) { drawLure(lure) }
}

private fun DrawScope.drawLure(lure: Lure) {
    val w = size.width
    val h = size.height
    val ink = 1.5.dp.toPx()
    when (lure) {
        Lure.KNOCKING_STICK -> rotate(-35f) {
            drawLine(LureInk, Offset(w * 0.1f, h / 2), Offset(w * 0.9f, h / 2), ink * 5, StrokeCap.Round)
            drawLine(Wood, Offset(w * 0.1f, h / 2), Offset(w * 0.9f, h / 2), ink * 3.4f, StrokeCap.Round)
            // A knot, and a stub where a twig was snapped off.
            drawCircle(WoodDark, ink * 1.1f, Offset(w * 0.42f, h / 2))
            drawLine(Wood, Offset(w * 0.65f, h / 2), Offset(w * 0.74f, h * 0.36f), ink * 1.6f, StrokeCap.Round)
        }
        Lure.RAW_MEAT -> {
            drawLine(LureInk, Offset(w * 0.55f, h * 0.45f), Offset(w * 0.88f, h * 0.18f), ink * 3.4f, StrokeCap.Round)
            drawLine(Bone, Offset(w * 0.55f, h * 0.45f), Offset(w * 0.88f, h * 0.18f), ink * 2f, StrokeCap.Round)
            drawCircle(Bone, ink * 1.6f, Offset(w * 0.9f, h * 0.14f))
            drawCircle(Bone, ink * 1.6f, Offset(w * 0.84f, h * 0.12f))
            val meat = Path().apply {
                moveTo(w * 0.12f, h * 0.6f)
                cubicTo(w * 0.1f, h * 0.3f, w * 0.5f, h * 0.22f, w * 0.62f, h * 0.42f)
                cubicTo(w * 0.75f, h * 0.62f, w * 0.55f, h * 0.88f, w * 0.32f, h * 0.84f)
                cubicTo(w * 0.18f, h * 0.82f, w * 0.13f, h * 0.72f, w * 0.12f, h * 0.6f)
                close()
            }
            drawPath(meat, Flesh)
            drawPath(meat, LureInk, style = Stroke(ink))
            drawCircle(Bone.copy(alpha = 0.8f), ink * 1.8f, Offset(w * 0.36f, h * 0.58f))
        }
        Lure.FISH -> {
            val body = Path().apply {
                moveTo(w * 0.1f, h / 2)
                quadraticTo(w * 0.4f, h * 0.2f, w * 0.7f, h / 2)
                quadraticTo(w * 0.4f, h * 0.8f, w * 0.1f, h / 2)
                close()
            }
            val tail = Path().apply {
                moveTo(w * 0.66f, h / 2)
                lineTo(w * 0.92f, h * 0.3f)
                lineTo(w * 0.86f, h / 2)
                lineTo(w * 0.92f, h * 0.7f)
                close()
            }
            drawPath(tail, Scale)
            drawPath(tail, LureInk, style = Stroke(ink))
            drawPath(body, Scale)
            drawPath(body, LureInk, style = Stroke(ink))
            drawCircle(LureInk, ink * 0.9f, Offset(w * 0.22f, h * 0.46f))
            drawLine(LureInk, Offset(w * 0.32f, h * 0.38f), Offset(w * 0.32f, h * 0.62f), ink * 0.7f)
        }
        Lure.LANTERN -> {
            drawCircle(
                Brush.radialGradient(
                    listOf(Amber.copy(alpha = 0.55f), Color.Transparent),
                    Offset(w / 2, h * 0.58f),
                    w * 0.45f,
                ),
                w * 0.45f,
                Offset(w / 2, h * 0.58f),
            )
            drawArc(
                LureInk,
                200f,
                140f,
                false,
                Offset(w * 0.34f, h * 0.06f),
                Size(w * 0.32f, h * 0.3f),
                style = Stroke(ink),
            )
            drawRect(Brass, Offset(w * 0.3f, h * 0.24f), Size(w * 0.4f, h * 0.08f))
            drawRect(Amber.copy(alpha = 0.9f), Offset(w * 0.34f, h * 0.32f), Size(w * 0.32f, h * 0.44f))
            drawRect(LureInk, Offset(w * 0.34f, h * 0.32f), Size(w * 0.32f, h * 0.44f), style = Stroke(ink))
            drawRect(Brass, Offset(w * 0.28f, h * 0.76f), Size(w * 0.44f, h * 0.1f))
            drawLine(LureInk, Offset(w / 2, h * 0.32f), Offset(w / 2, h * 0.76f), ink * 0.7f)
        }
        Lure.WHISTLE -> {
            drawArc(
                Bone.copy(alpha = 0.7f),
                120f,
                300f,
                false,
                Offset(w * 0.06f, h * 0.1f),
                Size(w * 0.36f, h * 0.36f),
                style = Stroke(ink),
            )
            drawRect(Brass, Offset(w * 0.3f, h * 0.42f), Size(w * 0.42f, h * 0.16f))
            drawRect(LureInk, Offset(w * 0.3f, h * 0.42f), Size(w * 0.42f, h * 0.16f), style = Stroke(ink))
            drawCircle(Brass, w * 0.16f, Offset(w * 0.72f, h * 0.56f))
            drawCircle(LureInk, w * 0.16f, Offset(w * 0.72f, h * 0.56f), style = Stroke(ink))
            drawRect(LureInk, Offset(w * 0.4f, h * 0.44f), Size(w * 0.08f, h * 0.05f))
        }
    }
}

/**
 * [lure] lying on the ground a few steps ahead, at [bearing] while the player faces [heading],
 * with a faint glint so it can be noticed. It waits there, so there's no hurry. Tap to pick it up.
 */
@Composable
fun BaitOnGround(lure: Lure, heading: Double, bearing: Double, onPickUp: () -> Unit, modifier: Modifier = Modifier) {
    val glint by rememberInfiniteTransition(label = "bait glint").animateFloat(
        0.25f,
        0.75f,
        infiniteRepeatable(tween(1_400), RepeatMode.Reverse),
        label = "bait glint",
    )
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val (w, h) = with(density) { maxWidth.toPx() to maxHeight.toPx() }
        val spot = sceneGroundPoint(w, h, heading, bearing, BAIT_DISTANCE)
        val half = with(density) { (BAIT_TARGET / 2).toPx() }
        if (spot.x < -half || spot.x > w + half) return@BoxWithConstraints
        Box(
            Modifier
                .offset { IntOffset((spot.x - half).roundToInt(), (spot.y - half * 1.4f).roundToInt()) }
                .size(BAIT_TARGET)
                .semantics { contentDescription = "${lure.title} lying on the ground. Tap to pick it up." }
                .clickable(role = Role.Button, onClick = onPickUp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(BAIT_TARGET)) {
                drawCircle(
                    Brush.radialGradient(listOf(Color.White.copy(alpha = glint * 0.5f), Color.Transparent)),
                    size.minDimension / 2,
                )
            }
            LureIcon(lure)
        }
    }
}

/** How far ahead bait lies when it turns up, in metres. */
private const val BAIT_DISTANCE = 5f

/** How big a target bait on the ground makes, so it's easy to tap. */
private val BAIT_TARGET = 64.dp

/** The bait the player is carrying, to pick one to set out. Each kind is listed once, with how many. */
@Composable
fun BaitDialog(carried: List<Lure>, onUse: (Lure) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Use bait", fontFamily = FontFamily.Serif) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                carried.groupingBy { it }.eachCount().forEach { (lure, count) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { onUse(lure) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LureIcon(lure)
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(
                                if (count > 1) "${lure.title} ×$count" else lure.title,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                lure.note,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Keep it for now") } },
    )
}

/** A short line over the scene saying what just happened, like picking up or setting out bait. */
@Composable
fun FieldNotice(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontFamily = FieldFont,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
            .widthIn(max = 280.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(LureInk.copy(alpha = 0.62f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
