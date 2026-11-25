package app.unconfirmed.game.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unconfirmed.game.R
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

// The paper side of the game: the newspaper the cases come from and the journal they're written
// up in. Everything is drawn like the investigation scene, as flat cartoon fills with a dark ink
// outline, laid out on a dark desk under a lamp.

/** A fat newspaper display face, for headlines. */
val HeadlineFont = FontFamily(Font(R.font.abril_fatface))

/** Old blackletter, for the newspaper's masthead. */
val MastheadFont = FontFamily(Font(R.font.unifraktur_maguntia))

/** Neat handwriting, for the player's own notes in the journal. */
val HandFont = FontFamily(Font(R.font.patrick_hand))

val Ink = Color(0xFF1C1814)
val Paper = Color(0xFFEFE5CC)
val Newsprint = Color(0xFFE2DAC5)
val Card = Color(0xFFF7F2E6)
val RedPencil = Color(0xFFB23A2A)
val BluePencil = Color(0xFF2F5F86)
val Tape = Color(0xFFE9DDB0)
private val Desk = Color(0xFF15110D)

/** A small, steady hash in -1..1, so the same card always wobbles and tilts the same way. */
fun paperNoise(seed: Int, i: Int, j: Int = 0): Float {
    val v = sin(seed * 37.719f + i * 12.9898f + j * 78.233f) * 43758.547f
    return (v - kotlin.math.floor(v)) * 2f - 1f
}

/** A slight tilt in degrees for a card, from its [key], up to [max] either way. */
fun tiltFor(key: String, max: Float = 1.6f): Float = paperNoise(key.hashCode(), 1) * max

/**
 * A rectangle traced by hand: each side is a few short strokes that wander off the line by up to
 * [wobble] pixels, and the corners come out softly rounded.
 */
fun wobblyRect(size: Size, seed: Int, wobble: Float, step: Float = 48f): Path {
    val corners =
        listOf(Offset(0f, 0f), Offset(size.width, 0f), Offset(size.width, size.height), Offset(0f, size.height))
    val points = mutableListOf<Offset>()
    corners.forEachIndexed { side, from ->
        val to = corners[(side + 1) % 4]
        val count = max(1, (hypot(to.x - from.x, to.y - from.y) / step).toInt())
        for (k in 0 until count) {
            val t = k / count.toFloat()
            val n = points.size
            points += Offset(
                from.x + (to.x - from.x) * t + paperNoise(seed, n, 0) * wobble,
                from.y + (to.y - from.y) * t + paperNoise(seed, n, 1) * wobble,
            )
        }
    }
    fun mid(a: Offset, b: Offset) = Offset((a.x + b.x) / 2, (a.y + b.y) / 2)
    return Path().apply {
        val start = mid(points.last(), points.first())
        moveTo(start.x, start.y)
        points.forEachIndexed { k, point ->
            val next = mid(point, points[(k + 1) % points.size])
            quadraticTo(point.x, point.y, next.x, next.y)
        }
        close()
    }
}

/**
 * Draws a cartoon card behind the content: a hard ink shadow [shadow] down and to the right, the
 * [fill], and a hand-drawn ink outline. The shadow falls outside the bounds, so leave room for it.
 */
fun Modifier.inkPanel(fill: Color, seed: Int = 0, shadow: Dp = 4.dp, outline: Dp = 2.dp): Modifier = drawWithCache {
    val path = wobblyRect(size, seed, 1.4.dp.toPx())
    val offset = shadow.toPx()
    val stroke = Stroke(outline.toPx(), join = StrokeJoin.Round)
    onDrawBehind {
        if (offset > 0f) translate(offset, offset) { drawPath(path, Ink.copy(alpha = 0.85f)) }
        drawPath(path, fill)
        drawPath(path, Ink, style = stroke)
    }
}

/** The dark desk everything is laid out on, lit from above by a warm lamp. */
fun Modifier.desk(): Modifier = drawBehind {
    drawRect(Desk)
    // Wood grain: a few long, faint waves.
    val grain = 1.dp.toPx()
    var y = 18.dp.toPx()
    var row = 0
    while (y < size.height) {
        val path = Path().apply {
            moveTo(0f, y)
            var x = 0f
            while (x < size.width) {
                x += 40.dp.toPx()
                quadraticTo(x - 20.dp.toPx(), y + paperNoise(7, row, x.toInt()) * 4.dp.toPx(), x, y)
            }
        }
        drawPath(path, Color.White.copy(alpha = 0.025f), style = Stroke(grain))
        y += 22.dp.toPx() + paperNoise(3, row) * 8.dp.toPx()
        row++
    }
    drawRect(
        Brush.radialGradient(
            listOf(Amber.copy(alpha = 0.16f), Color.Transparent),
            center = Offset(size.width * 0.5f, 0f),
            radius = max(size.width, size.height) * 0.75f,
        ),
    )
}

/** Faint ruled lines and a red margin, like a page in a school exercise book. */
fun Modifier.ruledLines(margin: Dp = 0.dp, gap: Dp = 28.dp, top: Dp = 56.dp): Modifier = drawBehind {
    val step = gap.toPx()
    var y = top.toPx()
    while (y < size.height - step / 2) {
        drawLine(BluePencil.copy(alpha = 0.16f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        y += step
    }
    if (margin > 0.dp) {
        val x = margin.toPx()
        drawLine(RedPencil.copy(alpha = 0.35f), Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
    }
}

/**
 * A sheet of paper with a cartoon ink outline. Text on it is dark ink by default, since the rest
 * of the app is light text on a dark background.
 */
@Composable
fun PaperSheet(
    modifier: Modifier = Modifier,
    fill: Color = Paper,
    seed: Int = 0,
    tilt: Float = 0f,
    shadow: Dp = 4.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.graphicsLayer { rotationZ = tilt }.inkPanel(fill, seed, shadow)) {
        CompositionLocalProvider(LocalContentColor provides Ink) { content() }
    }
}

/** A strip of masking tape, laid across whatever it's placed over. */
@Composable
fun TapeStrip(modifier: Modifier = Modifier, angle: Float = -4f, width: Dp = 64.dp) {
    Box(
        modifier
            .size(width, 20.dp)
            .graphicsLayer { rotationZ = angle }
            .drawBehind {
                // Torn ends: a zigzag down each short side.
                val tooth = 3.dp.toPx()
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    var y = 0f
                    var out = true
                    while (y < size.height) {
                        y = minOf(size.height, y + tooth)
                        lineTo(size.width - if (out) tooth else 0f, y)
                        out = !out
                    }
                    lineTo(0f, size.height)
                    y = size.height
                    out = true
                    while (y > 0f) {
                        y = maxOf(0f, y - tooth)
                        lineTo(if (out) tooth else 0f, y)
                        out = !out
                    }
                    close()
                }
                drawPath(path, Tape.copy(alpha = 0.82f))
                drawPath(path, Ink.copy(alpha = 0.18f), style = Stroke(1.dp.toPx()))
            },
    )
}

/** A rubber stamp, inked a little unevenly and never quite straight. */
@Composable
fun RubberStamp(text: String, color: Color, modifier: Modifier = Modifier, angle: Float = -8f, size: Int = 13) {
    val shape = RoundedCornerShape(3.dp)
    Text(
        text.uppercase(),
        fontFamily = FieldFont,
        fontSize = size.sp,
        letterSpacing = 1.5.sp,
        color = color,
        maxLines = 1,
        modifier = modifier
            .graphicsLayer {
                rotationZ = angle
                alpha = 0.88f
            }
            .border(2.dp, color, shape)
            .padding(2.dp)
            .border(1.dp, color.copy(alpha = 0.6f), shape)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** A chunky cartoon button that sinks into its shadow when pressed. */
@Composable
fun InkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fill: Color = Amber,
    seed: Int = text.hashCode(),
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sink = if (pressed) 2.dp else 0.dp
    Box(
        modifier
            .offset(sink, sink)
            .inkPanel(fill, seed, shadow = 3.dp - sink)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = 44.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontFamily = HandFont, fontSize = 19.sp, color = Ink)
    }
}
