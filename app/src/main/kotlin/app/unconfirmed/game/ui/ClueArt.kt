package app.unconfirmed.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unconfirmed.shared.ClueType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

// The picture that comes with each clue: the sample in its evidence bag, the prints in the mud,
// the recording's trace, the trail-cam still and the witness's sketch. Each shows only the trait
// the clue gives, the same for every creature that shares it. All hair is the same dark tuft and
// every upright figure on a trail cam is the same blur, so the art never settles more than the
// clue's words do.

// Traits, as the case files spell them.
private const val DEEP = "deep"
private const val SHALLOW = "shallow"
private const val CLAWED = "clawed"
private const val HOOVES = "hooves"
private const val PADDED = "padded"
private const val SHRILL = "shrill"
private const val HOWL = "howl"
private const val SPLASH = "splash"
private const val VOICE = "voice"
private const val KNOCK = "knock"
private const val LOW = "low"
private const val UPRIGHT = "upright"
private const val SHADOW = "shadow"
private const val GLOW = "glow"
private const val HUMPS = "humps"
private const val SMUDGE = "smudge"
private const val HUNCHED = "hunched"
private const val MAN = "man"
private const val WINGED = "winged"
private const val LIZARD = "lizard"
private const val LEGS = "legs"
private const val HORNED = "horned"
private const val DOG = "dog"
private const val CAT = "cat"
private const val HOOP = "hoop"
private const val TENTACLES = "tentacles"
private const val HAIR = "hair"
private const val FEATHER = "feather"
private const val SLIME = "slime"
private const val SCALE = "scale"

private val HairDark = Color(0xFF2E2119)
private val HairMid = Color(0xFF4B3526)
private val HairLight = Color(0xFF6E5442)
private val FeatherDark = Color(0xFF25201D)
private val FeatherBarb = Color(0xFF4A4038)
private val Quill = Color(0xFFDCCDAA)
private val Mud = Color(0xFF6B5240)
private val MudDark = Color(0xFF4F3B2C)
private val MudPit = Color(0xFF2F2219)
private val MudLip = Color(0xFF8D6F55)
private val RulerYellow = Color(0xFFE6C658)
private val Screen = Color(0xFF16201B)
private val NightGreen = Color(0xFF0E1A12)
private val NightGround = Color(0xFF35513B)
private val NightPale = Color(0xFFCFEBD2)
private val Graphite = Color(0xFF3B3936)

/**
 * The picture for a clue of [type] that shows [trait], on a card that fits the kind of evidence.
 * [seed] keeps the card's hand-drawn wobble steady from one look to the next.
 */
@Composable
fun ClueArt(type: ClueType, trait: String?, seed: Int, modifier: Modifier = Modifier) {
    val text = rememberTextMeasurer()
    val fill = when (type) {
        ClueType.WITNESS_SKETCH -> Paper
        ClueType.AUDIO, ClueType.TRAIL_CAM -> Ink
        else -> Card
    }
    PaperSheet(fill = fill, seed = seed, shadow = 3.dp, modifier = modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .padding(8.dp)
                .semantics { contentDescription = describeClueArt(type, trait) },
        ) {
            when (type) {
                ClueType.SAMPLE -> drawSample(trait, text)
                ClueType.FOOTPRINTS -> drawPrints(trait)
                ClueType.AUDIO -> drawRecording(trait, text)
                ClueType.TRAIL_CAM -> drawTrailCam(trait, text)
                ClueType.WITNESS_SKETCH -> drawSketch(trait, text)
            }
        }
    }
}

private fun describeClueArt(type: ClueType, trait: String?): String = when (type) {
    ClueType.SAMPLE -> when (trait) {
        HAIR -> "A tuft of coarse dark hair in an evidence bag"
        FEATHER -> "A long dark feather in an evidence bag"
        SLIME -> "A swab of clear slime in an evidence bag"
        SCALE -> "A single large scale in an evidence bag"
        else -> "An empty evidence bag"
    }
    ClueType.FOOTPRINTS -> when (trait) {
        DEEP -> "Deep, wide prints pressed into the mud beside a ruler"
        SHALLOW -> "Faint, shallow prints far apart in the mud"
        CLAWED -> "Clawed prints on either side of a tail drag in the mud"
        HOOVES -> "A line of cloven hoof prints in the mud"
        PADDED -> "Round prints of four toes and a broad pad, with no claw marks, in the mud"
        else -> "Smooth mud beside a ruler, with no prints in it"
    }
    ClueType.AUDIO -> when (trait) {
        SHRILL -> "A recording's trace with sharp, shrill bursts"
        HOWL -> "A recording's trace swelling into one long howl"
        SPLASH -> "A recording's trace with one sudden splash"
        VOICE -> "A recording's trace broken into phrases, like someone singing"
        KNOCK -> "A recording's trace of sharp, evenly spaced taps"
        else -> "A recording's trace, flat and silent"
    }
    ClueType.TRAIL_CAM -> when (trait) {
        LOW -> "A trail-cam still of something long and low in the grass"
        UPRIGHT -> "A trail-cam still of a blurred upright figure"
        SHADOW -> "A trail-cam still of a huge shadow across the ground"
        GLOW -> "A trail-cam still of a pale, glowing shape"
        else -> "A trail-cam still of an empty clearing"
    }
    ClueType.WITNESS_SKETCH -> when (trait) {
        HUMPS -> "A pencil sketch of humps in the water and a long neck"
        SMUDGE -> "A pencil sketch that is only a scribbled smudge"
        HUNCHED -> "A pencil sketch of something hunched with its head low"
        MAN -> "A pencil sketch of a tall, man-shaped figure"
        WINGED -> "A pencil sketch of a figure with great wings"
        LIZARD -> "A pencil sketch of something like an alligator"
        LEGS -> "A pencil sketch of a pale body on two long legs"
        HORNED -> "A pencil sketch of a horned, upright figure"
        DOG -> "A pencil sketch of an upright figure with a dog's head and pointed ears"
        CAT -> "A pencil sketch of a long, low cat with a long tail"
        HOOP -> "A pencil sketch of a snake bent into a ring, its tail in its mouth"
        TENTACLES -> "A pencil sketch of long arms curling up out of the water"
        else -> "A pencil sketch"
    }
}

// ---- Shapes -----------------------------------------------------------------------------------

/**
 * An outline in a unit box, and how wide the box is for its height. Points go round the shape
 * and are joined with soft curves, so a handful of them makes a rounded silhouette.
 */
private class Outline(val aspect: Float, val points: List<Offset>)

private fun outline(aspect: Float, vararg xy: Float) =
    Outline(aspect, xy.toList().chunked(2).map { (x, y) -> Offset(x, y) })

private val BIPED = outline(
    0.55f,
    0.50f, 0.02f, 0.58f, 0.04f, 0.60f, 0.12f, 0.58f, 0.18f, 0.74f, 0.24f, 0.80f, 0.40f, 0.82f, 0.58f,
    0.77f, 0.62f, 0.72f, 0.45f, 0.68f, 0.56f, 0.66f, 0.64f, 0.66f, 0.82f, 0.65f, 0.98f, 0.54f, 0.99f,
    0.54f, 0.71f, 0.50f, 0.66f, 0.46f, 0.71f, 0.46f, 0.99f, 0.35f, 0.98f, 0.34f, 0.82f, 0.34f, 0.64f,
    0.32f, 0.56f, 0.28f, 0.45f, 0.23f, 0.62f, 0.18f, 0.58f, 0.20f, 0.40f, 0.26f, 0.24f, 0.42f, 0.18f,
    0.40f, 0.12f, 0.42f, 0.04f,
)

private val LOW_LONG = outline(
    3.2f,
    0.03f, 0.78f, 0.12f, 0.58f, 0.30f, 0.40f, 0.55f, 0.32f, 0.78f, 0.40f, 0.92f, 0.56f, 0.99f, 0.76f,
    0.92f, 0.92f, 0.60f, 0.98f, 0.30f, 0.98f, 0.10f, 0.94f,
)

private val HUNCHED_BEAST = outline(
    1.45f,
    0.08f, 0.64f, 0.12f, 0.52f, 0.24f, 0.46f, 0.32f, 0.26f, 0.46f, 0.20f, 0.58f, 0.30f, 0.78f, 0.44f,
    0.92f, 0.54f, 0.97f, 0.66f, 0.88f, 0.70f, 0.85f, 0.97f, 0.76f, 0.97f, 0.72f, 0.74f, 0.46f, 0.76f,
    0.43f, 0.97f, 0.34f, 0.97f, 0.31f, 0.73f, 0.20f, 0.71f, 0.11f, 0.71f,
)

private val WINGED_FIGURE = outline(
    1.6f,
    0.50f, 0.08f, 0.56f, 0.12f, 0.56f, 0.22f, 0.70f, 0.14f, 0.86f, 0.10f, 0.99f, 0.18f, 0.92f, 0.38f,
    0.84f, 0.32f, 0.78f, 0.48f, 0.69f, 0.40f, 0.60f, 0.54f, 0.58f, 0.70f, 0.60f, 0.93f, 0.54f, 0.94f,
    0.50f, 0.76f, 0.46f, 0.94f, 0.40f, 0.93f, 0.42f, 0.70f, 0.40f, 0.54f, 0.31f, 0.40f, 0.22f, 0.48f,
    0.16f, 0.32f, 0.08f, 0.38f, 0.01f, 0.18f, 0.14f, 0.10f, 0.30f, 0.14f, 0.44f, 0.22f, 0.44f, 0.12f,
)

private val LIZARD_BODY = outline(
    2.4f,
    0.02f, 0.58f, 0.10f, 0.50f, 0.22f, 0.48f, 0.28f, 0.42f, 0.42f, 0.38f, 0.60f, 0.40f, 0.72f, 0.48f,
    0.86f, 0.56f, 0.99f, 0.62f, 0.86f, 0.66f, 0.72f, 0.66f, 0.70f, 0.80f, 0.62f, 0.80f, 0.60f, 0.68f,
    0.38f, 0.68f, 0.36f, 0.82f, 0.28f, 0.82f, 0.28f, 0.66f, 0.18f, 0.64f, 0.08f, 0.64f,
)

/** Long and low, the head level, a heavy tail curling up behind. */
private val BIG_CAT = outline(
    2.6f,
    0.02f, 0.32f, 0.05f, 0.22f, 0.09f, 0.17f, 0.10f, 0.09f, 0.13f, 0.16f, 0.19f, 0.20f, 0.32f, 0.23f,
    0.50f, 0.23f, 0.62f, 0.25f, 0.69f, 0.30f, 0.76f, 0.40f, 0.84f, 0.56f, 0.92f, 0.58f, 0.97f, 0.46f,
    0.99f, 0.52f, 0.93f, 0.68f, 0.82f, 0.66f, 0.72f, 0.50f, 0.70f, 0.97f, 0.62f, 0.97f, 0.60f, 0.58f,
    0.40f, 0.55f, 0.30f, 0.57f, 0.28f, 0.97f, 0.20f, 0.97f, 0.19f, 0.56f, 0.12f, 0.48f, 0.06f, 0.40f,
)

private val TWO_LEGS = outline(
    0.45f,
    0.28f, 0.10f, 0.50f, 0.02f, 0.72f, 0.10f, 0.80f, 0.28f, 0.68f, 0.42f, 0.64f, 0.70f, 0.70f, 0.97f,
    0.56f, 0.97f, 0.54f, 0.48f, 0.46f, 0.48f, 0.44f, 0.97f, 0.30f, 0.97f, 0.36f, 0.70f, 0.32f, 0.42f,
    0.20f, 0.28f,
)

/** Where an outline lands in [area], as large as fits while keeping its shape, standing on its foot. */
private fun fit(shape: Outline, area: Rect): Rect {
    val width = minOf(area.width, area.height * shape.aspect)
    val height = width / shape.aspect
    val left = area.left + (area.width - width) / 2
    return Rect(left, area.bottom - height, left + width, area.bottom)
}

/** [shape] drawn into [box], as a closed path of soft curves. */
private fun shapePath(shape: Outline, box: Rect): Path =
    smoothClosed(shape.points.map { Offset(box.left + it.x * box.width, box.top + it.y * box.height) })

private fun smoothClosed(points: List<Offset>): Path {
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

/** A lumpy round blob of [radius] round [center], for slime and pebbles. */
private fun blobPath(center: Offset, radius: Size, seed: Int, lumps: Int = 9, roughness: Float = 0.22f): Path =
    smoothClosed(
        (0 until lumps).map { k ->
            val a = k * 2 * PI.toFloat() / lumps
            val r = 1f + paperNoise(seed, k) * roughness
            Offset(center.x + cos(a) * radius.width * r, center.y + sin(a) * radius.height * r)
        },
    )

private fun DrawScope.label(text: TextMeasurer, words: String, at: Offset, style: TextStyle) {
    drawText(text, words, topLeft = at, style = style)
}

// ---- Samples ----------------------------------------------------------------------------------

/** A clear evidence bag with a red seal and a label, holding whatever was found. */
private fun DrawScope.drawSample(trait: String?, text: TextMeasurer) {
    val w = size.width
    val h = size.height
    val bag = Rect(w * 0.12f, h * 0.04f, w * 0.88f, h * 0.96f)
    val bagPath = wobblyRect(bag.size, 41, 1.2.dp.toPx())

    // Under the plastic.
    translate(bag.left, bag.top) {
        drawPath(bagPath, Color.White.copy(alpha = 0.35f))
    }
    when (trait) {
        HAIR -> drawHairTuft()
        FEATHER -> drawFeather()
        SLIME -> drawSlime()
        SCALE -> drawScale()
        else -> Unit
    }
    // The plastic itself: a sheen down one side and the seal across the top.
    translate(bag.left, bag.top) {
        drawPath(bagPath, Ink.copy(alpha = 0.55f), style = Stroke(1.4.dp.toPx(), join = StrokeJoin.Round))
    }
    drawLine(
        Color.White.copy(alpha = 0.7f),
        Offset(bag.left + w * 0.05f, bag.top + h * 0.18f),
        Offset(bag.left + w * 0.09f, bag.bottom - h * 0.32f),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawRect(RedPencil.copy(alpha = 0.85f), Offset(bag.left, bag.top + h * 0.05f), Size(bag.width, h * 0.045f))
    drawLine(
        Ink.copy(alpha = 0.4f),
        Offset(bag.left, bag.top + h * 0.115f),
        Offset(bag.right, bag.top + h * 0.115f),
        1.dp.toPx(),
    )

    // The label, filled in by hand.
    // Down in the right-hand corner, clear of the sample.
    val tag = Rect(bag.right - w * 0.37f, bag.bottom - h * 0.27f, bag.right - w * 0.04f, bag.bottom - h * 0.05f)
    translate(tag.left, tag.top) {
        val path = wobblyRect(tag.size, 43, 0.8.dp.toPx(), step = 30f)
        drawPath(path, Paper)
        drawPath(path, Ink.copy(alpha = 0.7f), style = Stroke(1.dp.toPx()))
    }
    label(
        text,
        "EVIDENCE",
        Offset(tag.left + w * 0.02f, tag.top + h * 0.015f),
        TextStyle(fontFamily = FieldFont, fontSize = 9.sp, color = RedPencil, letterSpacing = 1.sp),
    )
    label(
        text,
        when (trait) {
            HAIR -> "hair"
            FEATHER -> "feather"
            SLIME -> "slime"
            SCALE -> "scale"
            else -> "none found"
        },
        Offset(tag.left + w * 0.02f, tag.top + h * 0.085f),
        TextStyle(fontFamily = HandFont, fontSize = 15.sp, color = BluePencil),
    )

    if (trait != HAIR && trait != FEATHER && trait != SLIME && trait != SCALE) {
        rotate(-12f, Offset(w * 0.58f, h * 0.42f)) {
            val stamp = text.measure(
                "NOTHING FOUND",
                TextStyle(fontFamily = FieldFont, fontSize = 15.sp, letterSpacing = 2.sp),
            )
            val at = Offset(w * 0.58f - stamp.size.width / 2f, h * 0.42f - stamp.size.height / 2f)
            drawText(stamp, color = RedPencil.copy(alpha = 0.85f), topLeft = at)
            val pad = 5.dp.toPx()
            drawRect(
                RedPencil.copy(alpha = 0.85f),
                at - Offset(pad, pad / 2),
                Size(stamp.size.width + pad * 2, stamp.size.height + pad),
                style = Stroke(2.dp.toPx()),
            )
        }
    }
}

/** A tuft of long, coarse, dark hairs, pulled out together at the root, with a few loose ones. */
private fun DrawScope.drawHairTuft() {
    val w = size.width
    val h = size.height
    val root = Offset(w * 0.30f, h * 0.72f)
    val tones = listOf(HairDark, HairMid, HairDark, HairLight, HairMid)
    val count = 46
    for (i in 0 until count) {
        val spread = i / (count - 1f)
        val end = Offset(
            w * (0.54f + 0.22f * spread + 0.04f * paperNoise(5, i)),
            h * (0.22f + 0.28f * (1 - spread) * (1 - spread) + 0.05f * paperNoise(6, i)),
        )
        val wave = h * 0.06f * paperNoise(7, i)
        val start = root + Offset(w * 0.012f * paperNoise(8, i), h * 0.012f * paperNoise(9, i))
        val path = Path().apply {
            moveTo(start.x, start.y)
            cubicTo(
                start.x + (end.x - start.x) * 0.3f,
                start.y + (end.y - start.y) * 0.3f + wave,
                start.x + (end.x - start.x) * 0.7f,
                start.y + (end.y - start.y) * 0.7f - wave,
                end.x,
                end.y,
            )
        }
        drawPath(
            path,
            tones[i % tones.size],
            style = Stroke((1.4f + 1.2f * abs(paperNoise(10, i))).dp.toPx(), cap = StrokeCap.Round),
        )
        // The pale bulb of a root pulled out whole.
        if (i % 4 == 0) drawCircle(HairLight.copy(alpha = 0.8f), 1.6.dp.toPx(), start)
    }
    // A few loose ones that came away on their own.
    for (i in 0 until 3) {
        val from = Offset(w * (0.22f + 0.18f * i), h * (0.30f + 0.12f * (i % 2)))
        val path = Path().apply {
            moveTo(from.x, from.y)
            cubicTo(
                from.x + w * 0.06f,
                from.y - h * 0.08f,
                from.x + w * 0.10f,
                from.y + h * 0.06f,
                from.x + w * 0.16f,
                from.y - h * 0.02f,
            )
        }
        drawPath(path, HairMid, style = Stroke(1.1.dp.toPx(), cap = StrokeCap.Round))
    }
}

/**
 * A long flight feather, laid across the bag on a slant: a bare pale quill, a little down at the
 * base, and a dark vane, wider on one side, with a split where the barbs have come apart.
 */
private fun DrawScope.drawFeather() {
    val w = size.width
    val h = size.height
    val base = Offset(w * 0.22f, h * 0.84f)
    val bend = Offset(w * 0.44f, h * 0.58f)
    val tip = Offset(w * 0.68f, h * 0.20f)
    fun along(t: Float): Offset {
        val u = 1 - t
        return Offset(
            u * u * base.x + 2 * u * t * bend.x + t * t * tip.x,
            u * u * base.y + 2 * u * t * bend.y + t * t * tip.y,
        )
    }
    fun normal(t: Float): Offset {
        val a = along((t - 0.01f).coerceAtLeast(0f))
        val b = along((t + 0.01f).coerceAtMost(1f))
        val len = hypot(b.x - a.x, b.y - a.y)
        return Offset(-(b.y - a.y) / len, (b.x - a.x) / len)
    }
    val vaneStart = 0.2f
    val widest = w * 0.11f
    fun halfWidth(t: Float, side: Float): Float {
        val u = ((t - vaneStart) / (1 - vaneStart)).coerceIn(0f, 1f)
        val profile = sin(PI.toFloat() * u.pow(0.7f)).coerceAtLeast(0f).pow(0.6f)
        return widest * profile * if (side > 0) 1f else 0.6f
    }
    val steps = 40
    val ts = (0..steps).map { vaneStart + (1 - vaneStart) * it / steps }
    val vane = Path().apply {
        ts.forEachIndexed { k, t ->
            val p = along(t) + normal(t) * halfWidth(t, 1f)
            if (k == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        ts.reversed().forEach { t ->
            val p = along(t) - normal(t) * halfWidth(t, -1f)
            lineTo(p.x, p.y)
        }
        close()
    }
    drawPath(vane, FeatherDark)
    // Barbs, raking out from the shaft towards the tip.
    clipPath(vane) {
        for (k in 0..70) {
            val t = vaneStart + (1 - vaneStart) * k / 70f
            val from = along(t)
            for (side in listOf(1f, -1f)) {
                val reach = widest * 1.3f
                val to = from + normal(t) * (reach * side) + (along((t + 0.06f).coerceAtMost(1f)) - from) * 0.9f
                drawLine(FeatherBarb, from, to, strokeWidth = 0.8.dp.toPx())
            }
        }
    }
    // A split in the wide side, where the barbs have pulled apart.
    val split = 0.56f
    drawLine(
        Card,
        along(split) + normal(split) * (widest * 0.25f),
        along(split + 0.05f) + normal(split + 0.05f) * (widest * 1.2f),
        strokeWidth = 1.6.dp.toPx(),
    )
    drawPath(vane, Ink, style = Stroke(1.dp.toPx(), join = StrokeJoin.Round))
    // Fluffy down at the foot of the vane.
    for (k in 0 until 9) {
        val t = 0.13f + 0.08f * k / 9f
        val side = if (k % 2 == 0) 1f else -1f
        val from = along(t)
        val to = from + normal(t) * (side * widest * (0.5f + 0.3f * abs(paperNoise(12, k)))) + Offset(0f, h * 0.02f)
        val path = Path().apply {
            moveTo(from.x, from.y)
            quadraticTo((from.x + to.x) / 2 + w * 0.02f * paperNoise(13, k), (from.y + to.y) / 2, to.x, to.y)
        }
        drawPath(path, Color(0xFF8A817A), style = Stroke(1.dp.toPx(), cap = StrokeCap.Round))
    }
    // The shaft, pale and bare at the quill.
    val shaft = Path().apply {
        moveTo(base.x, base.y)
        quadraticTo(bend.x, bend.y, tip.x, tip.y)
    }
    drawPath(shaft, Ink, style = Stroke(3.4.dp.toPx(), cap = StrokeCap.Round))
    drawPath(shaft, Quill, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round))
}

/** A cotton swab, its tip sunk in a glossy smear of clear slime. */
private fun DrawScope.drawSlime() {
    val w = size.width
    val h = size.height
    val center = Offset(w * 0.58f, h * 0.44f)
    drawLine(Color(0xFFE9DFC6), Offset(w * 0.26f, h * 0.76f), center, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(Color.White, w * 0.035f, center)
    val smear = blobPath(center, Size(w * 0.17f, h * 0.17f), 51)
    drawPath(smear, Color(0xFFB9CDA1).copy(alpha = 0.6f))
    drawPath(smear, Color(0xFF6E8A58).copy(alpha = 0.7f), style = Stroke(1.2.dp.toPx()))
    // Drips running down the inside of the bag.
    listOf(0.36f to 0.62f, 0.46f to 0.67f).forEachIndexed { k, (x, y) ->
        val drip = blobPath(Offset(w * x, h * y), Size(w * 0.025f, h * 0.04f), 60 + k, lumps = 6, roughness = 0.1f)
        drawPath(drip, Color(0xFFB9CDA1).copy(alpha = 0.6f))
        drawPath(drip, Color(0xFF6E8A58).copy(alpha = 0.6f), style = Stroke(1.dp.toPx()))
    }
    // Wet shine.
    drawOval(Color.White.copy(alpha = 0.75f), Offset(w * 0.50f, h * 0.33f), Size(w * 0.07f, h * 0.035f))
    drawOval(Color.White.copy(alpha = 0.55f), Offset(w * 0.64f, h * 0.48f), Size(w * 0.03f, h * 0.02f))
}

/** One big scale, ridged with growth lines and shining like old bronze. */
private fun DrawScope.drawScale() {
    val w = size.width
    val h = size.height
    val c = Offset(w * 0.56f, h * 0.38f)
    val rw = w * 0.16f
    val rh = h * 0.25f
    fun shield(k: Float) = Path().apply {
        moveTo(c.x - rw * k, c.y - rh * 0.55f * k)
        quadraticTo(c.x, c.y - rh * 0.85f * k, c.x + rw * k, c.y - rh * 0.55f * k)
        quadraticTo(c.x + rw * 1.05f * k, c.y + rh * 0.35f * k, c.x, c.y + rh * k)
        quadraticTo(c.x - rw * 1.05f * k, c.y + rh * 0.35f * k, c.x - rw * k, c.y - rh * 0.55f * k)
        close()
    }
    val outer = shield(1f)
    drawPath(
        outer,
        Brush.linearGradient(
            listOf(Color(0xFF8A8C46), Color(0xFF55602F), Color(0xFF353E22)),
            start = Offset(c.x - rw, c.y - rh),
            end = Offset(c.x + rw, c.y + rh),
        ),
    )
    for (k in listOf(0.8f, 0.6f, 0.4f)) {
        drawPath(shield(k), Ink.copy(alpha = 0.35f), style = Stroke(1.dp.toPx()))
    }
    drawLine(Ink.copy(alpha = 0.3f), Offset(c.x, c.y - rh * 0.7f), Offset(c.x, c.y + rh * 0.9f), 1.dp.toPx())
    drawOval(Color.White.copy(alpha = 0.4f), Offset(c.x - rw * 0.7f, c.y - rh * 0.45f), Size(rw * 0.5f, rh * 0.18f))
    drawPath(outer, Ink, style = Stroke(1.4.dp.toPx(), join = StrokeJoin.Round))
}

// ---- Footprints -------------------------------------------------------------------------------

/** Mud, photographed from above next to a ruler, with whatever prints are in it. */
private fun DrawScope.drawPrints(trait: String?) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(Mud, MudDark)))
    for (k in 0 until 90) {
        val at = Offset(w * (0.5f + 0.5f * paperNoise(71, k)), h * (0.5f + 0.5f * paperNoise(72, k)))
        val tone = if (k % 3 == 0) MudLip else MudPit
        drawCircle(tone.copy(alpha = 0.35f), (0.8f + abs(paperNoise(73, k)) * 1.6f).dp.toPx(), at)
    }
    when (trait) {
        DEEP -> {
            deepPrint(Offset(w * 0.30f, h * 0.56f), -10f)
            deepPrint(Offset(w * 0.64f, h * 0.30f), 6f)
        }
        SHALLOW -> {
            shallowPrint(Offset(w * 0.14f, h * 0.70f), -20f)
            shallowPrint(Offset(w * 0.86f, h * 0.18f), -20f)
            // A pencilled tape line between them, measuring the stride.
            drawLine(
                Paper.copy(alpha = 0.7f),
                Offset(w * 0.20f, h * 0.64f),
                Offset(w * 0.80f, h * 0.24f),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx())),
            )
        }
        CLAWED -> {
            val drag = Path().apply {
                moveTo(w * 0.02f, h * 0.62f)
                cubicTo(w * 0.30f, h * 0.48f, w * 0.60f, h * 0.60f, w * 0.98f, h * 0.38f)
            }
            drawPath(drag, MudPit.copy(alpha = 0.6f), style = Stroke(h * 0.035f, cap = StrokeCap.Round))
            drawPath(drag, MudLip.copy(alpha = 0.5f), style = Stroke(1.dp.toPx()))
            clawedPrint(Offset(w * 0.22f, h * 0.30f), -70f)
            clawedPrint(Offset(w * 0.48f, h * 0.76f), -110f)
            clawedPrint(Offset(w * 0.76f, h * 0.20f), -70f)
        }
        HOOVES -> {
            hoofPrint(Offset(w * 0.20f, h * 0.68f), 30f)
            hoofPrint(Offset(w * 0.42f, h * 0.40f), 30f)
            hoofPrint(Offset(w * 0.60f, h * 0.62f), 30f)
            hoofPrint(Offset(w * 0.80f, h * 0.30f), 30f)
        }
        PADDED -> {
            paddedPrint(Offset(w * 0.26f, h * 0.60f), -15f)
            paddedPrint(Offset(w * 0.52f, h * 0.30f), -15f)
            paddedPrint(Offset(w * 0.80f, h * 0.58f), -15f)
        }
        else -> {
            // An evidence marker on smooth, empty mud.
            val m = Offset(w * 0.56f, h * 0.48f)
            val tent = Path().apply {
                moveTo(m.x - w * 0.06f, m.y + h * 0.08f)
                lineTo(m.x, m.y - h * 0.08f)
                lineTo(m.x + w * 0.06f, m.y + h * 0.08f)
                close()
            }
            drawPath(tent, RulerYellow)
            drawPath(tent, Ink, style = Stroke(1.4.dp.toPx(), join = StrokeJoin.Round))
            drawCircle(Ink, w * 0.012f, Offset(m.x, m.y + h * 0.03f))
        }
    }
    drawRuler()
}

private fun DrawScope.drawRuler() {
    val w = size.width
    val h = size.height
    val top = h * 0.86f
    drawRect(RulerYellow, Offset(w * 0.04f, top), Size(w * 0.70f, h * 0.10f))
    drawRect(Ink, Offset(w * 0.04f, top), Size(w * 0.70f, h * 0.10f), style = Stroke(1.2.dp.toPx()))
    for (k in 0..28) {
        val x = w * 0.04f + w * 0.70f * k / 28f
        val long = k % 4 == 0
        drawLine(Ink, Offset(x, top), Offset(x, top + h * if (long) 0.05f else 0.025f), 1.dp.toPx())
    }
}

/** A big, deep print: a dark pit with a raised lip, wide at the front with vague toe dents. */
private fun DrawScope.deepPrint(at: Offset, angle: Float) {
    val w = size.width
    val h = size.height
    rotate(angle, at) {
        val length = h * 0.36f
        val width = w * 0.11f
        val sole = Path().apply {
            moveTo(at.x, at.y - length / 2)
            cubicTo(
                at.x + width * 0.75f,
                at.y - length / 2,
                at.x + width * 0.55f,
                at.y + length * 0.2f,
                at.x + width * 0.3f,
                at.y + length / 2,
            )
            quadraticTo(at.x, at.y + length * 0.58f, at.x - width * 0.3f, at.y + length / 2)
            cubicTo(
                at.x - width * 0.55f,
                at.y + length * 0.2f,
                at.x - width * 0.75f,
                at.y - length / 2,
                at.x,
                at.y - length / 2,
            )
            close()
        }
        translate(2.dp.toPx(), 2.dp.toPx()) { drawPath(sole, MudLip, style = Stroke(3.dp.toPx())) }
        drawPath(sole, Brush.verticalGradient(listOf(MudPit, Color(0xFF3E2D21)), at.y - length / 2, at.y + length / 2))
        drawPath(sole, Ink.copy(alpha = 0.6f), style = Stroke(1.2.dp.toPx()))
        for (k in 0 until 4) {
            val x = at.x + width * (-0.45f + 0.3f * k)
            // The outer toes sit a little further back than the middle ones.
            val y = at.y - length * 0.56f + abs(k - 1.5f) * length * 0.02f
            drawCircle(MudPit, width * 0.12f, Offset(x, y))
            drawCircle(Ink.copy(alpha = 0.5f), width * 0.12f, Offset(x, y), style = Stroke(1.dp.toPx()))
        }
    }
}

/** A faint, shallow print, barely pressed in. */
private fun DrawScope.shallowPrint(at: Offset, angle: Float) {
    rotate(angle, at) {
        val s = Size(size.width * 0.08f, size.height * 0.20f)
        drawOval(MudPit.copy(alpha = 0.3f), at - Offset(s.width / 2, s.height / 2), s)
        drawOval(MudLip.copy(alpha = 0.4f), at - Offset(s.width / 2, s.height / 2), s, style = Stroke(1.dp.toPx()))
    }
}

/** Splayed toes ending in scratches, either side of a round pad. */
private fun DrawScope.clawedPrint(at: Offset, angle: Float) {
    val r = size.height * 0.06f
    rotate(angle, at) {
        drawOval(MudPit.copy(alpha = 0.8f), at - Offset(r, r * 0.8f), Size(r * 2, r * 1.6f))
        for (k in 0 until 5) {
            val a = (-60f + 30f * k) * PI.toFloat() / 180f
            val toe = at + Offset(sin(a), -cos(a)) * (r * 2.1f)
            drawLine(
                MudPit.copy(alpha = 0.8f),
                at + Offset(sin(a), -cos(a)) * r,
                toe,
                strokeWidth = r * 0.45f,
                cap = StrokeCap.Round,
            )
            drawLine(
                Ink.copy(alpha = 0.7f),
                toe,
                toe + Offset(sin(a), -cos(a)) * (r * 0.8f),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

/** A big cat's print: a broad, three-lobed heel pad and four round toes in an arc, and no claws. */
private fun DrawScope.paddedPrint(at: Offset, angle: Float) {
    val r = size.height * 0.05f
    rotate(angle, at) {
        val pad = Path().apply {
            moveTo(at.x - r * 1.5f, at.y + r * 0.2f)
            cubicTo(
                at.x - r * 1.5f,
                at.y - r * 1.1f,
                at.x + r * 1.5f,
                at.y - r * 1.1f,
                at.x + r * 1.5f,
                at.y + r * 0.2f,
            )
            quadraticTo(at.x + r * 1.3f, at.y + r * 1.3f, at.x + r * 0.5f, at.y + r * 1.0f)
            quadraticTo(at.x, at.y + r * 1.5f, at.x - r * 0.5f, at.y + r * 1.0f)
            quadraticTo(at.x - r * 1.3f, at.y + r * 1.3f, at.x - r * 1.5f, at.y + r * 0.2f)
            close()
        }
        drawPath(pad, MudPit.copy(alpha = 0.8f))
        drawPath(pad, MudLip.copy(alpha = 0.6f), style = Stroke(1.dp.toPx()))
        for (k in 0 until 4) {
            val a = (-48f + 32f * k) * PI.toFloat() / 180f
            val toe = at + Offset(sin(a) * r * 2.0f, -cos(a) * r * 2.1f - r * 0.4f)
            drawOval(MudPit.copy(alpha = 0.8f), toe - Offset(r * 0.5f, r * 0.62f), Size(r, r * 1.24f))
            drawOval(
                MudLip.copy(alpha = 0.6f),
                toe - Offset(r * 0.5f, r * 0.62f),
                Size(r, r * 1.24f),
                style = Stroke(1.dp.toPx()),
            )
        }
    }
}

/** A cloven hoof: two halves with a gap between. */
private fun DrawScope.hoofPrint(at: Offset, angle: Float) {
    val w = size.width * 0.035f
    val l = size.height * 0.13f
    rotate(angle, at) {
        for (side in listOf(-1f, 1f)) {
            val half = Path().apply {
                val inner = at.x + side * w * 0.2f
                val outer = at.x + side * w * 1.2f
                moveTo(inner, at.y - l / 2)
                quadraticTo(outer, at.y - l * 0.4f, outer, at.y + l * 0.1f)
                quadraticTo(outer, at.y + l / 2, inner, at.y + l / 2)
                close()
            }
            drawPath(half, MudPit.copy(alpha = 0.85f))
            drawPath(half, MudLip.copy(alpha = 0.6f), style = Stroke(1.dp.toPx()))
        }
    }
}

// ---- Recordings -------------------------------------------------------------------------------

/** The recorder's screen: a grid and the sound's trace across it. */
/** The words of the sung line in a [VOICE] recording: where each starts and ends, and its pitch. */
private val SUNG_WORDS = listOf(
    Triple(0.08f, 0.15f, 300f),
    Triple(0.17f, 0.24f, 340f),
    Triple(0.26f, 0.38f, 380f),
    Triple(0.50f, 0.57f, 340f),
    Triple(0.59f, 0.66f, 300f),
    Triple(0.68f, 0.86f, 260f),
)

private fun DrawScope.drawRecording(trait: String?, text: TextMeasurer) {
    val w = size.width
    val h = size.height
    drawRect(Screen)
    for (k in 1 until 8) drawLine(Signal.copy(alpha = 0.08f), Offset(w * k / 8f, 0f), Offset(w * k / 8f, h), 1f)
    for (k in 1 until 6) drawLine(Signal.copy(alpha = 0.08f), Offset(0f, h * k / 6f), Offset(w, h * k / 6f), 1f)
    val middle = h * 0.56f
    drawLine(Signal.copy(alpha = 0.25f), Offset(0f, middle), Offset(w, middle), 1f)

    fun level(x: Float): Float = when (trait) {
        SHRILL -> {
            val bursts = listOf(0.22f, 0.30f, 0.58f, 0.80f).sumOf { c -> exp(-((x - c) / 0.025f).pow(2)).toDouble() }
            bursts.toFloat() * sin(x * 520f) * 0.95f
        }
        HOWL -> {
            val swell = sin(PI.toFloat() * ((x - 0.08f) / 0.84f).coerceIn(0f, 1f)).pow(1.4f)
            swell * sin(x * 110f + 6f * sin(x * 5f)) * 0.85f
        }
        SPLASH -> {
            val hit = if (x < 0.38f) 0f else exp(-(x - 0.38f) * 9f)
            hit * paperNoise(81, (x * 600).toInt()) * 0.95f
        }
        // A line of song: separate words, each a steady note with a soft start and end, the pitch
        // stepping up and down from word to word, and a breath between phrases.
        VOICE -> SUNG_WORDS.sumOf { (start, end, pitch) ->
            val t = ((x - start) / (end - start)).coerceIn(0f, 1f)
            val swell = if (x < start || x > end) 0f else sin(PI.toFloat() * t).coerceAtLeast(0f).pow(0.5f)
            (swell * sin(x * pitch)).toDouble()
        }.toFloat() * 0.7f
        // Sharp taps at a steady beat, each dying away fast, like a hammer on rock.
        KNOCK -> {
            val beat = 0.085f
            val since = ((x - 0.06f) % beat + beat) % beat
            if (x < 0.06f || x > 0.92f) 0f else exp(-since * 140f) * sin(x * 900f) * 0.9f
        }
        else -> paperNoise(82, (x * 600).toInt()) * 0.02f
    }
    val trace = Path().apply {
        val steps = 600
        for (k in 0..steps) {
            val x = k / steps.toFloat()
            val y = middle - level(x) * h * 0.34f
            if (k == 0) moveTo(0f, y) else lineTo(x * w, y)
        }
    }
    drawPath(trace, Signal.copy(alpha = 0.25f), style = Stroke(4.dp.toPx(), join = StrokeJoin.Round))
    drawPath(trace, Signal, style = Stroke(1.2.dp.toPx(), join = StrokeJoin.Round))

    val style = TextStyle(fontFamily = FieldFont, fontSize = 10.sp, color = Signal.copy(alpha = 0.85f))
    drawCircle(Warning, 3.dp.toPx(), Offset(w * 0.04f, h * 0.08f))
    label(text, "REC  02:14:37", Offset(w * 0.07f, h * 0.035f), style)
    label(
        text,
        if (trait == SHRILL ||
            trait == HOWL ||
            trait == SPLASH ||
            trait == VOICE ||
            trait == KNOCK
        ) {
            "-12 dB"
        } else {
            "-60 dB"
        },
        Offset(w * 0.80f, h * 0.035f),
        style,
    )
}

// ---- Trail cam --------------------------------------------------------------------------------

/** A grainy night-vision still, stamped with the camera's clock. */
private fun DrawScope.drawTrailCam(trait: String?, text: TextMeasurer) {
    val w = size.width
    val h = size.height
    val ground = h * 0.66f
    drawRect(Brush.verticalGradient(listOf(NightGreen, Color(0xFF213826)), endY = ground))
    drawRect(Brush.verticalGradient(listOf(NightGround, Color(0xFF243A29)), ground, h), Offset(0f, ground))
    // Trunks either side, black against the infrared.
    drawRect(Color(0xFF07100A), Offset(w * 0.04f, 0f), Size(w * 0.07f, ground + h * 0.06f))
    drawRect(Color(0xFF07100A), Offset(w * 0.86f, 0f), Size(w * 0.05f, ground + h * 0.03f))

    val area = Rect(w * 0.22f, h * 0.18f, w * 0.80f, ground + h * 0.06f)
    when (trait) {
        LOW -> blurredFigure(
            shapePath(
                LOW_LONG,
                fit(
                    LOW_LONG,
                    Rect(
                        area.left,
                        ground - h * 0.18f,
                        area.right,
                        ground + h * 0.06f,
                    ),
                ),
            ),
        )
        UPRIGHT -> blurredFigure(shapePath(BIPED, fit(BIPED, area)))
        SHADOW -> {
            // Only the shadow of something passing over, sweeping the ground.
            val shadow = blobPath(Offset(w * 0.52f, ground + h * 0.12f), Size(w * 0.36f, h * 0.09f), 91, lumps = 11)
            for (k in 0 until 4) {
                translate(w * 0.025f * k, 0f) { drawPath(shadow, Color.Black.copy(alpha = 0.22f)) }
            }
        }
        GLOW -> {
            val box = fit(TWO_LEGS, area)
            drawCircle(
                Brush.radialGradient(
                    listOf(NightPale.copy(alpha = 0.55f), Color.Transparent),
                    center = box.center,
                    radius = box.height * 0.7f,
                ),
                radius = box.height * 0.7f,
                center = box.center,
            )
            drawPath(shapePath(TWO_LEGS, box), NightPale.copy(alpha = 0.95f))
        }
        else -> Unit
    }
    // Grass in front, hiding where things meet the ground.
    for (k in 0 until 50) {
        val x = w * k / 50f + w * 0.01f * paperNoise(92, k)
        val tall = h * (0.06f + 0.06f * abs(paperNoise(93, k)))
        drawLine(
            Color(0xFF0B160E),
            Offset(x, h * 0.9f),
            Offset(
                x + w * 0.01f * paperNoise(94, k),
                ground + h * 0.12f - tall,
            ),
            2.dp.toPx(),
            StrokeCap.Round,
        )
    }
    // Grain.
    for (k in 0 until 260) {
        val at = Offset(w * (0.5f + 0.5f * paperNoise(95, k)), h * (0.5f + 0.5f * paperNoise(96, k)))
        drawCircle(NightPale.copy(alpha = 0.12f), 0.8.dp.toPx(), at)
    }
    drawRect(Color.Black.copy(alpha = 0.75f), Offset(0f, h * 0.88f), Size(w, h * 0.12f))
    val style = TextStyle(fontFamily = FieldFont, fontSize = 10.sp, color = NightPale)
    label(text, "CAM 02   03:47:12 AM   41°F", Offset(w * 0.04f, h * 0.895f), style)
    label(text, "IR", Offset(w * 0.04f, h * 0.03f), style)
}

/** A dark shape on the trail cam, smeared by its own movement. */
private fun DrawScope.blurredFigure(shape: Path) {
    for (k in 3 downTo 1) {
        translate(-size.width * 0.018f * k, 0f) { drawPath(shape, Color(0xFF050A06).copy(alpha = 0.2f)) }
    }
    drawPath(shape, Color(0xFF050A06).copy(alpha = 0.9f))
}

// ---- Witness sketches -------------------------------------------------------------------------

/** A page of lined paper with the witness's pencil drawing on it. */
private fun DrawScope.drawSketch(trait: String?, text: TextMeasurer) {
    val w = size.width
    val h = size.height
    var y = h * 0.1f
    while (y < h) {
        drawLine(BluePencil.copy(alpha = 0.18f), Offset(0f, y), Offset(w, y), 1.dp.toPx())
        y += h * 0.1f
    }
    drawLine(RedPencil.copy(alpha = 0.3f), Offset(w * 0.08f, 0f), Offset(w * 0.08f, h), 1.dp.toPx())

    val area = Rect(w * 0.16f, h * 0.08f, w * 0.92f, h * 0.86f)
    when (trait) {
        HUMPS -> sketchHumps(area)
        SMUDGE -> sketchSmudge(area)
        HUNCHED -> pencil(shapePath(HUNCHED_BEAST, fit(HUNCHED_BEAST, area)))
        WINGED -> pencil(shapePath(WINGED_FIGURE, fit(WINGED_FIGURE, area)))
        LIZARD -> pencil(shapePath(LIZARD_BODY, fit(LIZARD_BODY, area)))
        LEGS -> pencil(shapePath(TWO_LEGS, fit(TWO_LEGS, area)))
        HORNED -> {
            val box = fit(BIPED, Rect(area.left, area.top + h * 0.1f, area.right, area.bottom))
            pencil(shapePath(BIPED, box))
            for (side in listOf(-1f, 1f)) {
                // Each horn sweeps out from the top of the head and curls up to a point.
                val x = { f: Float -> box.left + box.width * (0.5f + side * f) }
                val y = { f: Float -> box.top + box.height * f }
                val horn = Path().apply {
                    moveTo(x(0.04f), y(0.04f))
                    quadraticTo(x(0.22f), y(0.04f), x(0.26f), y(-0.12f))
                    quadraticTo(x(0.16f), y(-0.01f), x(0.09f), y(0.08f))
                    close()
                }
                drawPath(horn, Graphite.copy(alpha = 0.55f))
                drawPath(horn, Graphite, style = Stroke(1.5.dp.toPx(), join = StrokeJoin.Round))
            }
        }
        MAN -> pencil(shapePath(BIPED, fit(BIPED, area)))
        DOG -> {
            // Man-shaped, but with pointed ears and a long muzzle out to one side.
            val box = fit(BIPED, Rect(area.left, area.top + h * 0.1f, area.right, area.bottom))
            val x = { f: Float -> box.left + box.width * f }
            val y = { f: Float -> box.top + box.height * f }
            val head = Path().apply {
                moveTo(x(0.43f), y(0.05f))
                lineTo(x(0.45f), y(-0.08f))
                lineTo(x(0.50f), y(0.02f))
                lineTo(x(0.56f), y(-0.08f))
                lineTo(x(0.58f), y(0.05f))
                lineTo(x(0.58f), y(0.15f))
                lineTo(x(0.44f), y(0.15f))
                lineTo(x(0.22f), y(0.12f))
                lineTo(x(0.22f), y(0.09f))
                close()
            }
            pencil(shapePath(BIPED, box))
            pencil(head)
        }
        CAT -> pencil(shapePath(BIG_CAT, fit(BIG_CAT, area)))
        HOOP -> sketchHoop(area)
        TENTACLES -> sketchTentacles(area)
        else -> Unit
    }
    label(
        text,
        if (trait == SMUDGE) "it was too dark" else "what I saw",
        Offset(w * 0.12f, h * 0.88f),
        TextStyle(fontFamily = HandFont, fontSize = 14.sp, color = Graphite.copy(alpha = 0.8f)),
    )
}

/** A snake bent into a hoop, the head biting down on the tail, with lines to show it rolling. */
private fun DrawScope.sketchHoop(area: Rect) {
    val r = minOf(area.width, area.height) * 0.36f
    val c = Offset(area.center.x + r * 0.15f, area.center.y)
    val thick = r * 0.24f
    val ring = Path().apply {
        addOval(Rect(c - Offset(r + thick / 2, r + thick / 2), Size((r + thick / 2) * 2, (r + thick / 2) * 2)))
        addOval(Rect(c - Offset(r - thick / 2, r - thick / 2), Size((r - thick / 2) * 2, (r - thick / 2) * 2)))
        fillType = PathFillType.EvenOdd
    }
    pencil(ring)
    // The head at the top, a little wider than the body, biting the tail.
    val head = Path().apply {
        addOval(Rect(Offset(c.x - r * 0.42f, c.y - r - thick * 0.9f), Size(r * 0.5f, thick * 1.8f)))
    }
    pencil(head)
    drawCircle(Graphite, thick * 0.12f, Offset(c.x - r * 0.3f, c.y - r - thick * 0.2f))
    // Speed lines behind it.
    for (k in 0 until 3) {
        val yy = c.y - r * 0.5f + k * r * 0.5f
        drawLine(
            Graphite.copy(alpha = 0.6f),
            Offset(c.x + r * 1.35f, yy),
            Offset(c.x + r * 1.75f, yy),
            1.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

/** Arms curling up out of a wavy waterline, thick at the root and thinning to a curl. */
private fun DrawScope.sketchTentacles(area: Rect) {
    val water = area.top + area.height * 0.82f
    for ((k, lean) in listOf(-0.32f, -0.1f, 0.12f, 0.34f).withIndex()) {
        val root = Offset(area.center.x + area.width * lean * 0.7f, water)
        val height = area.height * (0.55f + 0.18f * (k % 2))
        val bend = area.width * 0.16f * if (k % 2 == 0) -1f else 1f
        val tip = Offset(root.x + bend, water - height)
        val arm = Path().apply {
            moveTo(root.x - area.width * 0.035f, root.y)
            cubicTo(
                root.x - area.width * 0.03f,
                root.y - height * 0.5f,
                tip.x - bend * 0.6f,
                tip.y + height * 0.2f,
                tip.x,
                tip.y,
            )
            quadraticTo(tip.x + bend * 0.35f, tip.y + height * 0.08f, tip.x + bend * 0.1f, tip.y + height * 0.16f)
            cubicTo(
                tip.x - bend * 0.3f,
                tip.y + height * 0.3f,
                root.x + area.width * 0.03f,
                root.y - height * 0.4f,
                root.x + area.width * 0.035f,
                root.y,
            )
            close()
        }
        pencil(arm)
    }
    val waves = Path().apply {
        moveTo(area.left, water)
        var x = area.left
        while (x < area.right) {
            quadraticTo(x + area.width * 0.04f, water - area.height * 0.03f, x + area.width * 0.08f, water)
            x += area.width * 0.08f
        }
    }
    drawPath(waves, Graphite, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
}

/** A shape in pencil: a sketchy doubled outline and some shading hatched across it. */
private fun DrawScope.pencil(shape: Path) {
    clipPath(shape) {
        var x = -size.height
        while (x < size.width) {
            drawLine(Graphite.copy(alpha = 0.3f), Offset(x, size.height), Offset(x + size.height, 0f), 1.dp.toPx())
            x += 5.dp.toPx()
        }
    }
    drawPath(shape, Graphite.copy(alpha = 0.9f), style = Stroke(1.5.dp.toPx(), join = StrokeJoin.Round))
    translate(1.2.dp.toPx(), -0.8.dp.toPx()) {
        drawPath(shape, Graphite.copy(alpha = 0.35f), style = Stroke(1.dp.toPx(), join = StrokeJoin.Round))
    }
}

/** Humps breaking the surface, and a long neck with a small head at one end. */
private fun DrawScope.sketchHumps(area: Rect) {
    val water = area.top + area.height * 0.7f
    val stroke = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val waves = Path().apply {
        moveTo(area.left, water)
        var x = area.left
        while (x < area.right) {
            x += area.width * 0.06f
            quadraticTo(x - area.width * 0.03f, water + area.height * 0.03f, x, water)
        }
    }
    listOf(0.42f to 0.12f, 0.62f to 0.1f, 0.80f to 0.075f).forEach { (cx, r) ->
        val hump = Path().apply {
            moveTo(area.left + area.width * (cx - r), water)
            cubicTo(
                area.left + area.width * (cx - r),
                water - area.height * r * 2.6f,
                area.left + area.width * (cx + r),
                water - area.height * r * 2.6f,
                area.left + area.width * (cx + r),
                water,
            )
        }
        clipPath(
            Path().apply {
                addPath(hump)
                close()
            },
        ) {
            var x = area.left
            while (x < area.right) {
                drawLine(
                    Graphite.copy(alpha = 0.3f),
                    Offset(x, water),
                    Offset(
                        x + area.height * 0.3f,
                        water - area.height * 0.3f,
                    ),
                    1.dp.toPx(),
                )
                x += 5.dp.toPx()
            }
        }
        drawPath(hump, Graphite, style = stroke)
    }
    val neck = Path().apply {
        moveTo(area.left + area.width * 0.14f, water)
        cubicTo(
            area.left + area.width * 0.24f,
            water - area.height * 0.3f,
            area.left + area.width * 0.06f,
            area.top + area.height * 0.25f,
            area.left + area.width * 0.16f,
            area.top + area.height * 0.08f,
        )
    }
    drawPath(neck, Graphite, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
    drawOval(
        Graphite,
        Offset(area.left + area.width * 0.13f, area.top + area.height * 0.02f),
        Size(area.width * 0.10f, area.height * 0.08f),
    )
    drawPath(waves, Graphite.copy(alpha = 0.8f), style = stroke)
}

/** Nothing the witness could make out: a scribbled-over smudge and a question mark. */
private fun DrawScope.sketchSmudge(area: Rect) {
    val c = area.center
    val path = Path().apply {
        moveTo(c.x, c.y)
        for (k in 0..160) {
            val a = k * 0.35f
            val r = area.height * (0.12f + 0.22f * abs(sin(k * 0.05f))) * (1f + 0.15f * paperNoise(101, k))
            lineTo(c.x + cos(a) * r * 1.5f, c.y + sin(a) * r * 0.8f)
        }
    }
    drawPath(path, Graphite.copy(alpha = 0.55f), style = Stroke(1.4.dp.toPx(), join = StrokeJoin.Round))
    val mark = Path().apply {
        val x = area.right - area.width * 0.08f
        val y = area.top + area.height * 0.1f
        val r = area.height * 0.08f
        arcTo(Rect(x - r, y, x + r, y + r * 2), 180f, 270f, true)
        lineTo(x, y + r * 3f)
    }
    drawPath(mark, Graphite, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    drawCircle(
        Graphite,
        1.6.dp.toPx(),
        Offset(
            area.right - area.width * 0.08f,
            area.top + area.height * 0.1f + area.height * 0.32f,
        ),
    )
}
