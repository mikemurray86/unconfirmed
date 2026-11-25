package app.unconfirmed.game.ui

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.unconfirmed.shared.Biome
import app.unconfirmed.shared.SkyState
import app.unconfirmed.shared.TimeOfDay
import app.unconfirmed.shared.Weather
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val FRAME_MILLIS = 33L
private const val CROSSFADE_MILLIS = 2000

/** When animations are off, the scene is frozen at this moment, which happens to look calm. */
private const val STILL_SECONDS = 17.3f

/**
 * A repeatable random number in 0..1 for particle [i] and channel [k]. Particles have no state:
 * each frame works out where every one should be from the clock alone, so nothing is allocated
 * per frame and a frozen clock gives a still picture for free.
 */
private fun hash(i: Int, k: Int): Float {
    val x = sin(i * 127.1 + k * 311.7) * 43758.5453
    return (x - kotlin.math.floor(x)).toFloat()
}

private fun Float.wrap(): Float = this - kotlin.math.floor(this)

private val firefly = Color(0xFFD9E86A)
private val mist = Color(0xFFBFD8DC)
private val sodium = Color(0xFFE0A43A)
private val parchment = Color(0xFFE6E2D6)
private val dust = Color(0xFFC9B98F)
private val rain = Color(0xFF9FC7D9)

/**
 * Atmosphere drawn over the scene: particles for the kind of country the player is in, the
 * weather, a time-of-day tint, a vignette and film grain. It never takes touches, so
 * anything underneath still gets them. With [frozenAt], it holds still at that many seconds in.
 */
@Composable
fun AtmosphereOverlay(biome: Biome, sky: SkyState, modifier: Modifier = Modifier, frozenAt: Float? = null) {
    val context = LocalContext.current
    val animate = remember(frozenAt) {
        frozenAt == null &&
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
    var seconds by remember(frozenAt) { mutableFloatStateOf(STILL_SECONDS + (frozenAt ?: 0f)) }
    if (animate) {
        LaunchedEffect(Unit) {
            val start = withFrameNanos { it }
            while (isActive) {
                val now = withFrameNanos { it }
                seconds = STILL_SECONDS + (now - start) / 1e9f
                delay(FRAME_MILLIS)
            }
        }
    }

    // Each biome and weather fades in and out on its own, so a change crossfades instead of snapping.
    val biomeWeights = Biome.entries.associateWith { b ->
        animateFloatAsState(if (b == biome) 1f else 0f, tween(CROSSFADE_MILLIS), label = "biome").value
    }
    val weatherWeights = Weather.entries.associateWith { w ->
        animateFloatAsState(if (w == sky.weather) 1f else 0f, tween(CROSSFADE_MILLIS), label = "weather").value
    }
    val (tintTop, tintBottom) = tintFor(sky.timeOfDay)
    val top by animateColorAsState(tintTop, tween(CROSSFADE_MILLIS), label = "tintTop")
    val bottom by animateColorAsState(tintBottom, tween(CROSSFADE_MILLIS), label = "tintBottom")
    // Bats come out at dusk and are busiest after dark.
    val bats by animateFloatAsState(
        when (sky.timeOfDay) {
            TimeOfDay.NIGHT -> 1f
            TimeOfDay.DUSK -> 0.7f
            TimeOfDay.DAWN -> 0.25f
            TimeOfDay.DAY -> 0f
        },
        tween(CROSSFADE_MILLIS),
        label = "bats",
    )
    val vignette by animateFloatAsState(
        if (sky.timeOfDay == TimeOfDay.DAY) 0.32f else 0.5f,
        tween(CROSSFADE_MILLIS),
        label = "vignette",
    )
    val grain = remember { grainTexture() }

    Canvas(modifier) {
        val t = seconds
        drawRect(Brush.verticalGradient(listOf(top, bottom)))

        fun weight(b: Biome) = biomeWeights.getValue(b)
        fun weight(w: Weather) = weatherWeights.getValue(w)

        weight(Biome.FOREST).takeIf { it > 0.01f }?.let {
            drawFireflies(t, it)
            drawLeaves(t, it)
        }
        max(weight(Biome.WETLAND), weight(Biome.WATERSIDE) * 0.7f).takeIf { it > 0.01f }?.let {
            drawMist(t, it, mist, count = 5, alpha = 0.09f)
        }
        weight(Biome.URBAN).takeIf { it > 0.01f }?.let { drawSodium(t, it) }
        weight(Biome.GRASSLAND).takeIf { it > 0.01f }?.let { drawSeeds(t, it) }
        max(weight(Biome.ROCKY), weight(Biome.OPEN) * 0.5f).takeIf { it > 0.01f }?.let { drawDust(t, it) }

        weight(Weather.CLOUDY).takeIf { it > 0.01f }?.let {
            drawMist(t * 0.6f, it, Color.Black, count = 4, alpha = 0.16f)
        }
        weight(Weather.FOG).takeIf { it > 0.01f }?.let {
            drawRect(parchment.copy(alpha = 0.10f * it))
            drawMist(t * 0.5f, it, mist, count = 6, alpha = 0.14f)
        }
        weight(Weather.DRIZZLE).takeIf {
            it > 0.01f
        }?.let { drawRain(t, it, count = 40, lengthDp = 10f, alpha = 0.18f) }
        weight(Weather.RAIN).takeIf {
            it > 0.01f
        }?.let { drawRain(t, it, count = 90, lengthDp = 18f, alpha = 0.24f) }
        weight(Weather.STORM).takeIf { it > 0.01f }?.let {
            drawRain(t, it, count = 120, lengthDp = 22f, alpha = 0.28f)
            drawLightning(t, it)
        }
        weight(Weather.SNOW).takeIf { it > 0.01f }?.let { drawSnow(t, it) }

        if (bats > 0.01f) drawBats(t, bats)
        drawVignette(vignette)
        drawGrain(grain, t, animate)
    }
}

/** The top and bottom of the wash over the whole scene, for each time of day. */
private fun tintFor(timeOfDay: TimeOfDay): Pair<Color, Color> = when (timeOfDay) {
    TimeOfDay.NIGHT -> Color(0xFF0A1530).copy(alpha = 0.28f) to Color(0xFF0A1530).copy(alpha = 0.08f)
    TimeOfDay.DUSK -> Color(0xFFE0A43A).copy(alpha = 0.12f) to Color(0xFF6A2E4A).copy(alpha = 0.10f)
    TimeOfDay.DAWN -> Color(0xFFE8A0B0).copy(alpha = 0.10f) to Color(0xFF6FC7D9).copy(alpha = 0.05f)
    TimeOfDay.DAY -> Color.Transparent to Color.Transparent
}

private fun DrawScope.drawFireflies(t: Float, weight: Float) {
    val glow = 9.dp.toPx()
    val core = 1.8.dp.toPx()
    repeat(26) { i ->
        val x = (hash(i, 0) + 0.03f * sin(t * (0.2f + 0.3f * hash(i, 2)) + i)).wrap() * size.width
        val y = (hash(i, 1) + 0.02f * cos(t * (0.15f + 0.25f * hash(i, 3)) + i * 1.7f)).wrap() * size.height
        // Mostly dark, with a brief soft flash: the way fireflies actually signal.
        val blink = max(0f, sin(t * (0.5f + hash(i, 4)) + hash(i, 5) * 2 * PI.toFloat())).pow(4) * weight
        if (blink < 0.02f) return@repeat
        drawCircle(
            Brush.radialGradient(listOf(firefly.copy(alpha = 0.45f * blink), Color.Transparent), Offset(x, y), glow),
            glow,
            Offset(x, y),
        )
        drawCircle(firefly.copy(alpha = 0.9f * blink), core, Offset(x, y))
    }
}

private fun DrawScope.drawLeaves(t: Float, weight: Float) {
    val leaf = Size(7.dp.toPx(), 3.5.dp.toPx())
    repeat(6) { i ->
        val speed = 0.03f + 0.02f * hash(i, 1)
        val fall = (hash(i, 0) + t * speed).wrap()
        val x = (hash(i, 2) + 0.08f * sin(t * 0.7f + i) + fall * 0.2f).wrap() * size.width
        val y = fall * size.height
        rotate(degrees = t * 40f * (hash(i, 3) - 0.5f) + i * 50f, pivot = Offset(x, y)) {
            drawOval(
                Color(0xFF7A6230).copy(alpha = 0.5f * weight),
                topLeft = Offset(x - leaf.width / 2, y - leaf.height / 2),
                size = leaf,
            )
        }
    }
}

private fun DrawScope.drawMist(t: Float, weight: Float, color: Color, count: Int, alpha: Float) {
    repeat(count) { i ->
        val radius = size.width * (0.35f + 0.3f * hash(i, 0))
        val x = (hash(i, 1) + t * (0.004f + 0.006f * hash(i, 2))).wrap() * (size.width + 2 * radius) - radius
        val y = size.height * (0.1f + 0.85f * hash(i, 3)) + 12.dp.toPx() * sin(t * 0.1f + i)
        drawCircle(
            Brush.radialGradient(listOf(color.copy(alpha = alpha * weight), Color.Transparent), Offset(x, y), radius),
            radius,
            Offset(x, y),
        )
    }
}

private fun DrawScope.drawSodium(t: Float, weight: Float) {
    // A streetlight that can't quite stay on.
    val flicker = 0.75f + 0.25f * sin(t * 13f) * sin(t * 3.1f) - if (sin(t * 0.9f) > 0.97f) 0.5f else 0f
    drawRect(sodium.copy(alpha = 0.045f * flicker * weight))
    // Faint bands of static, as on a cheap camera, that jump every few frames.
    val step = (t * 6).toInt()
    repeat(3) { i ->
        val y = hash(step, i) * size.height
        drawRect(
            parchment.copy(alpha = 0.05f * weight * hash(step, i + 7)),
            topLeft = Offset(0f, y),
            size = Size(size.width, (1 + 2 * hash(step, i + 3)).dp.toPx()),
        )
    }
}

private fun DrawScope.drawSeeds(t: Float, weight: Float) {
    val length = 9.dp.toPx()
    repeat(18) { i ->
        val x = (hash(i, 0) + t * (0.05f + 0.05f * hash(i, 1))).wrap() * size.width
        val y = (hash(i, 2) * size.height + 10.dp.toPx() * sin(t * 1.3f + i))
        drawLine(
            parchment.copy(alpha = 0.22f * weight),
            Offset(x, y),
            Offset(x - length, y + length * 0.15f),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawDust(t: Float, weight: Float) {
    val r = 1.2.dp.toPx()
    repeat(30) { i ->
        val x = (hash(i, 0) + t * 0.006f * (hash(i, 1) - 0.3f)).wrap() * size.width
        val y = (hash(i, 2) + 0.01f * sin(t * 0.4f + i)).wrap() * size.height
        val shimmer = 0.5f + 0.5f * sin(t * (0.6f + hash(i, 3)) + i)
        drawCircle(dust.copy(alpha = 0.28f * shimmer * weight), r, Offset(x, y))
    }
}

private fun DrawScope.drawRain(t: Float, weight: Float, count: Int, lengthDp: Float, alpha: Float) {
    val length = lengthDp.dp.toPx()
    val slant = 0.25f
    repeat(count) { i ->
        val fall = (hash(i, 0) + t * (1.1f + 0.6f * hash(i, 1))).wrap()
        val y = fall * (size.height + length) - length
        val x = (hash(i, 2) * (size.width + size.height * slant)) - fall * size.height * slant
        drawLine(
            rain.copy(alpha = alpha * weight * (0.6f + 0.4f * hash(i, 3))),
            Offset(x, y),
            Offset(x - length * slant, y + length),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawLightning(t: Float, weight: Float) {
    // About one flash every thirteen seconds: a bright hit, then a weaker echo.
    val phase = t % 13f
    val flash = when {
        phase < 0.08f -> 1f
        phase in 0.18f..0.26f -> 0.5f
        else -> 0f
    }
    if (flash > 0) drawRect(Color.White.copy(alpha = 0.22f * flash * weight))
}

private fun DrawScope.drawSnow(t: Float, weight: Float) {
    repeat(60) { i ->
        val fall = (hash(i, 0) + t * (0.04f + 0.04f * hash(i, 1))).wrap()
        val x = (hash(i, 2) + 0.03f * sin(t * (0.5f + hash(i, 3)) + i)).wrap() * size.width
        drawCircle(
            Color.White.copy(alpha = 0.6f * weight * (0.5f + 0.5f * hash(i, 4))),
            (1f + 1.6f * hash(i, 5)).dp.toPx(),
            Offset(x, fall * size.height),
        )
    }
}

private fun DrawScope.drawVignette(strength: Float) {
    drawRect(
        Brush.radialGradient(
            0.55f to Color.Transparent,
            1f to Color.Black.copy(alpha = strength),
            center = center,
            radius = max(size.width, size.height) * 0.75f,
        ),
    )
}

private const val GRAIN_PX = 128

/** One tile of grey noise, made once and repeated across the screen. */
private fun grainTexture(): ImageBitmap {
    val pixels = IntArray(GRAIN_PX * GRAIN_PX) { i ->
        val v = (hash(i, 9) * 255).toInt()
        android.graphics.Color.argb(255, v, v, v)
    }
    return android.graphics.Bitmap.createBitmap(pixels, GRAIN_PX, GRAIN_PX, android.graphics.Bitmap.Config.ARGB_8888)
        .asImageBitmap()
}

private fun DrawScope.drawGrain(grain: ImageBitmap, t: Float, animate: Boolean) {
    // Jumping the tile around a few times a second makes it read as film grain, not a texture.
    val step = if (animate) (t * 12).toInt() else 0
    val dx = hash(step, 1) * GRAIN_PX
    val dy = hash(step, 2) * GRAIN_PX
    translate(-dx, -dy) {
        drawRect(
            ShaderBrush(ImageShader(grain, TileMode.Repeated, TileMode.Repeated)),
            size = Size(size.width + GRAIN_PX, size.height + GRAIN_PX),
            alpha = 0.045f,
        )
    }
}

private const val BAT_COUNT = 5

/** The share of each bat's cycle spent crossing the screen. The rest is a pause before the next pass. */
private const val BAT_FLIGHT_SHARE = 0.4f

private val batBody = Color(0xFF050807)
private val moonlight = Color(0xFFD8DCE6)

/**
 * Bats crossing the scene at night: dark silhouettes, each lit faintly along its edge and casting a
 * soft shadow on the ground below. Each flies its own fluttering path, then disappears for a
 * while, so there are only ever one or two on screen.
 */
private fun DrawScope.drawBats(t: Float, weight: Float) {
    repeat(BAT_COUNT) { i ->
        val period = 9f + 8f * hash(i, 20)
        val progress = (t / period + hash(i, 21)).wrap()
        if (progress > BAT_FLIGHT_SHARE) return@repeat
        val f = progress / BAT_FLIGHT_SHARE

        val leftToRight = hash(i, 22) > 0.5f
        val across = if (leftToRight) f else 1 - f
        val x = (-0.1f + 1.2f * across) * size.width
        // Bats never fly straight: a slow swoop, plus quick darting jinks on top.
        val baseY = (0.15f + 0.6f * hash(i, 23)) * size.height
        val y = baseY +
            size.height * 0.08f * sin(f * 5f + i) +
            size.height * 0.015f * sin(t * 9f + i * 2.3f)
        // Smaller ones read as farther away, so the sky has some depth.
        val span = (16f + 14f * hash(i, 24)).dp.toPx()
        val flap = sin(t * (13f + 5f * hash(i, 25)) + i)
        val tilt = 12f * sin(t * 3f + i) + if (leftToRight) 6f else -6f
        // Fades in and out at the edges of its pass instead of popping.
        val presence = weight * kotlin.math.min(1f, kotlin.math.min(f, 1 - f) * 8f)

        val shadowOffset = Offset(span * 0.5f, span * 0.9f)
        translate(x + shadowOffset.x, y + shadowOffset.y) {
            scale(0.9f, pivot = Offset.Zero) {
                drawPath(batPath(span, flap), Color.Black.copy(alpha = 0.22f * presence))
            }
        }
        translate(x, y) {
            rotate(tilt, pivot = Offset.Zero) {
                val bat = batPath(span, flap)
                drawPath(bat, batBody.copy(alpha = 0.9f * presence))
                drawPath(bat, moonlight.copy(alpha = 0.22f * presence), style = Stroke(1.dp.toPx()))
            }
        }
    }
}

/** A bat centred on the origin, [span] wide, with its wings at [flap] from -1 (down) to 1 (up). */
private fun batPath(span: Float, flap: Float): Path = Path().apply {
    val s = span
    val tipY = -0.32f * s * flap
    fun wing(side: Float) {
        moveTo(0f, -0.04f * s)
        quadraticTo(side * 0.22f * s, -0.12f * s + tipY * 0.6f, side * 0.5f * s, tipY)
        // The trailing edge scallops between the finger bones.
        quadraticTo(side * 0.44f * s, tipY * 0.6f + 0.08f * s, side * 0.36f * s, tipY * 0.45f + 0.1f * s)
        quadraticTo(side * 0.29f * s, tipY * 0.35f + 0.04f * s, side * 0.21f * s, tipY * 0.2f + 0.11f * s)
        quadraticTo(side * 0.13f * s, 0.05f * s, side * 0.05f * s, 0.12f * s)
        close()
    }
    wing(-1f)
    wing(1f)
    addOval(androidx.compose.ui.geometry.Rect(-0.06f * s, -0.1f * s, 0.06f * s, 0.14f * s))
    // Ears.
    moveTo(-0.05f * s, -0.06f * s)
    lineTo(-0.035f * s, -0.15f * s)
    lineTo(-0.01f * s, -0.07f * s)
    close()
    moveTo(0.05f * s, -0.06f * s)
    lineTo(0.035f * s, -0.15f * s)
    lineTo(0.01f * s, -0.07f * s)
    close()
}
