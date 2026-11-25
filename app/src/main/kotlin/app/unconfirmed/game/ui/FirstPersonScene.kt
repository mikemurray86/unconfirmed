package app.unconfirmed.game.ui

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.unconfirmed.shared.Region
import app.unconfirmed.shared.Scene
import app.unconfirmed.shared.SeededRandom
import app.unconfirmed.shared.SkyState
import app.unconfirmed.shared.TimeOfDay
import app.unconfirmed.shared.Weather
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** How many degrees of the panorama fit across the screen. */
private const val FIELD_OF_VIEW = 70f

/** The horizon, as a fraction of the way down the scene. */
private const val HORIZON = 0.5f

private const val FRAME_MILLIS = 33L
private const val STILL_SECONDS = 11.7f

/** How often the walk is checked when animations are off, since there's no frame clock to follow. */
private const val STILL_STEP_MILLIS = 250L

/** A steady walk, in metres per second. */
private const val WALKING_PACE = 1.4

/** The player's eye height in metres, which sets how far below the horizon the ground lies. */
private const val EYE_HEIGHT = 1.6f

/** Things closer than this, in metres, fade out rather than filling the screen. */
private const val NEAR_CLIP = 1.2f

/**
 * A pine starts fading out this many metres away for each metre of its height, so a 12 m pine is
 * gone by about 3 m and never fills the screen as it goes by.
 */
private const val PINE_CLIP_PER_METRE = 0.25f

/** Distances, in metres, where low mist lies across the ground. */
private val MIST_DISTANCES = listOf(45f, 22f, 11f)

/** Metres between fence posts. A ground patch's size must be a whole number of these. */
private const val FENCE_SPACING = 3f

// How fast each layer slides as the player turns. Only the near layer is true to the compass,
// so the hint and the track line up with it. The others lag behind to give a sense of depth.
private const val SKY_PARALLAX = 0.5f
private const val FAR_PARALLAX = 0.7f
private const val MID_PARALLAX = 0.85f
private const val NEAR_PARALLAX = 1f

/**
 * The first-person view while investigating: a cartoon panorama of the kind of place the case is
 * set in, which pans as the player turns. [headingDegrees] is the way they're facing, or null if
 * the phone can't tell, and [hintBearing] is where the next clue's tracks seem to lead. The hint
 * is only a suggestion: walking any way finds the clue. [walkedMeters] makes the view bob along
 * while the player walks. With [frozenAt], the scene holds still at that many seconds in, as a
 * photo of it does.
 */
@Composable
fun FirstPersonScene(
    scene: Scene,
    region: Region,
    sky: SkyState,
    headingDegrees: Double?,
    walkedMeters: Double,
    hintBearing: Double?,
    modifier: Modifier = Modifier,
    creature: SceneCreature? = null,
    frozenAt: Float? = null,
) {
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
                seconds = STILL_SECONDS + (withFrameNanos { it } - start) / 1e9f
                delay(FRAME_MILLIS)
            }
        }
    }
    // GPS reports walking in jumps of ten metres or more, so each jump is played back at walking
    // pace. That keeps the scene moving steadily while the player walks, instead of lurching.
    var lastWalked by remember { mutableDoubleStateOf(walkedMeters) }
    val stepMillis = ((walkedMeters - lastWalked) / WALKING_PACE * 1000).toInt().coerceIn(1_500, 9_000)
    SideEffect { lastWalked = walkedMeters }
    val stride by animateFloatAsState(
        walkedMeters.toFloat(),
        tween(durationMillis = stepMillis, easing = LinearEasing),
        label = "stride",
    )
    val layout = remember(scene, region) { layoutFor(scene, region) }
    val palette = paletteFor(sky, region)
    val heading = headingDegrees ?: 0.0
    val hint = hintBearing.takeIf { headingDegrees != null }

    // Where the player stands on the ground patch, in metres east and north. Each bit of walking
    // carries them forward the way they're facing, so nearby things come towards them and pass by.
    var walker by remember(layout) { mutableStateOf(Offset.Zero) }
    val currentStride by rememberUpdatedState(stride)
    val currentHeading by rememberUpdatedState(heading)
    if (frozenAt == null) {
        LaunchedEffect(layout, animate) {
            var last = currentStride
            while (isActive) {
                withFrameNanos { }
                val step = currentStride - last
                last = currentStride
                if (step != 0f) {
                    val facing = Math.toRadians(currentHeading)
                    walker =
                        layout.world.wrap(walker + Offset(sin(facing).toFloat() * step, cos(facing).toFloat() * step))
                }
                if (!animate) delay(STILL_STEP_MILLIS)
            }
        }
    }

    Canvas(modifier.semantics { contentDescription = sceneDescription(scene, region) }) {
        val view = View(this, heading, if (animate) sin(stride * 1.1f) * 4.dp.toPx() else 0f)
        drawSky(view, layout, palette, seconds, sky.timeOfDay)
        drawSkyline(view, layout.skyline, palette)
        drawGround(view, layout, palette, seconds)
        layout.layers.forEach { layer ->
            drawLayer(view, layer, layout.moonBearing, palette, seconds)
            // Mist pools at the foot of each band, so the bands read as separate planes.
            drawMist(view, layer, palette)
        }
        drawWorld(view, layout, walker, palette, seconds, creature)
        hint?.let { drawHint(view, it, palette, seconds) }
    }
}

private fun sceneDescription(scene: Scene, region: Region): String {
    val trees = when (region) {
        Region.NORTHWEST, Region.ALASKA -> "pines"
        Region.DESERT -> "junipers"
        Region.PLAINS -> "cottonwoods"
        Region.MIDWEST, Region.EAST -> "hardwoods"
        Region.SOUTH -> "live oaks and pines"
        Region.HAWAII -> "palms"
    }
    val distance = when (region) {
        Region.NORTHWEST -> "under the mountains"
        Region.DESERT -> "under red mesas"
        Region.PLAINS -> "on the open prairie"
        Region.MIDWEST -> "in flat farm country"
        Region.EAST -> "under long, wooded ridges"
        Region.SOUTH -> "in the low, green South"
        Region.ALASKA -> "under snow-capped peaks"
        Region.HAWAII -> "under volcanic mountains"
    }
    return when (scene) {
        Scene.LAKE_SHORE -> "A lake shore $distance, ringed with $trees"
        Scene.TIMBER -> if (region ==
            Region.DESERT
        ) {
            "Dark pine timber up on the high country"
        } else {
            "Dark woods of $trees"
        }
        Scene.PASTURE -> "Pasture with a fence line and a windmill, $distance"
        Scene.RIDGE -> "A rocky ridge $distance"
        Scene.RIVER_TOWN -> "A river town with an old steel bridge, $distance"
        Scene.PINE_BARRENS -> "Scrubby pine barrens along a sand road"
    }
}

// ---- Creatures --------------------------------------------------------------------------------

/** How a cryptid gets about, which sets its gait and how it meets the ground. One that [STANDS] stays put. */
enum class CreatureMotion { WALKS, FLIES, SWIMS, STANDS }

/**
 * A cryptid out in the scene. [bearing] is its compass direction from the player and [distance]
 * how many metres off it is. [visibility] runs from 0, lost in the dark, to 1, clear. [stride]
 * counts its full strides (both feet), wingbeats or rolls of its body, and its fraction is how far
 * through the current one it is. Its art faces left, and is turned round while [movingRight].
 *
 * [frames] is either a cycle of poses spread evenly over one full stride, played in step with
 * [stride], or a single silhouette that's bent into motion instead. [fill] is how much of its
 * square box the art fills from top to bottom, which is less for something long and low, and
 * [footprint] is how far its shadow reaches either side, as a fraction of the box.
 */
class SceneCreature(
    val frames: List<Painter>,
    val motion: CreatureMotion,
    val heightMeters: Float,
    val bearing: Double,
    val distance: Float,
    val visibility: Float,
    val movingRight: Boolean,
    val stride: Float,
    val fill: Float = ART_FILL,
    val footprint: Float = 0.4f,
)

/** Most creature art fills about this much of its square box, from head to feet. */
const val ART_FILL = 0.9f

/** Where the feet are in the creature art, as a fraction of the way down its box. */
private const val ART_FEET = 0.95f

// ---- Palette ----------------------------------------------------------------------------------

private class Palette(
    val skyTop: Color,
    val skyBottom: Color,
    val far: Color,
    val mid: Color,
    val near: Color,
    val groundFar: Color,
    val ground: Color,
    val water: Color,
    val ink: Color,
    val moon: Color,
    val eyes: Color,
    /** How bright the eyes glow: they barely show by day. */
    val eyeGlow: Float,
    val stars: Boolean,
)

/**
 * How a region shifts the land's colours, as a multiplier on red, green and blue. Multiplying keeps
 * night dark and day light, so the same tint works at every time of day.
 */
private class Tint(
    val ground: Triple<Float, Float, Float>,
    val far: Triple<Float, Float, Float>,
    val foliage: Triple<Float, Float, Float>,
    val water: Triple<Float, Float, Float>,
)

private val NO_CHANGE = Triple(1f, 1f, 1f)

private fun tintFor(region: Region): Tint = when (region) {
    Region.NORTHWEST -> Tint(NO_CHANGE, NO_CHANGE, NO_CHANGE, NO_CHANGE)
    // Ochre ground and red-rock mesas under a dusty green.
    Region.DESERT -> Tint(
        Triple(1.5f, 1.15f, 0.72f),
        Triple(1.45f, 1.0f, 0.82f),
        Triple(1.1f, 1.05f, 0.82f),
        Triple(1.1f, 1.0f, 0.9f),
    )
    // Tawny grass to the horizon.
    Region.PLAINS -> Tint(
        Triple(1.3f, 1.17f, 0.78f),
        Triple(1.12f, 1.06f, 0.92f),
        Triple(1.05f, 1.05f, 0.88f),
        NO_CHANGE,
    )
    Region.MIDWEST -> Tint(
        Triple(1.05f, 1.15f, 0.82f),
        Triple(1.0f, 1.05f, 0.95f),
        Triple(1.0f, 1.12f, 0.85f),
        NO_CHANGE,
    )
    // The blue haze of the ridges, over deep green woods.
    Region.EAST -> Tint(Triple(1.0f, 1.08f, 0.9f), Triple(0.88f, 0.98f, 1.18f), Triple(0.98f, 1.1f, 0.88f), NO_CHANGE)
    // Humid green and murky, tea-coloured water.
    Region.SOUTH -> Tint(
        Triple(1.06f, 1.12f, 0.82f),
        Triple(1.0f, 1.06f, 0.92f),
        Triple(0.95f, 1.1f, 0.8f),
        Triple(0.95f, 1.05f, 0.8f),
    )
    // Brown tundra, and cold blue-grey distance.
    Region.ALASKA -> Tint(
        Triple(1.12f, 1.02f, 0.88f),
        Triple(0.95f, 1.02f, 1.15f),
        Triple(0.95f, 1.02f, 1.0f),
        Triple(0.9f, 1.0f, 1.1f),
    )
    // Lush green, and turquoise water.
    Region.HAWAII -> Tint(
        Triple(0.85f, 1.22f, 0.85f),
        Triple(0.88f, 1.08f, 1.02f),
        Triple(0.85f, 1.22f, 0.9f),
        Triple(0.7f, 1.15f, 1.3f),
    )
}

private fun Color.times(m: Triple<Float, Float, Float>) = Color(
    (red * m.first).coerceIn(0f, 1f),
    (green * m.second).coerceIn(0f, 1f),
    (blue * m.third).coerceIn(0f, 1f),
    alpha,
)

private fun paletteFor(sky: SkyState, region: Region): Palette {
    val tint = tintFor(region)
    val base = when (sky.timeOfDay) {
        TimeOfDay.NIGHT -> Palette(
            skyTop = Color(0xFF110C2B), skyBottom = Color(0xFF3A2A5E), far = Color(0xFF2B2452),
            mid = Color(0xFF1B1737), near = Color(0xFF0F0C21), groundFar = Color(0xFF29254A),
            ground = Color(0xFF15132A), water = Color(0xFF1C2A57), ink = Color(0xFF06040E),
            moon = Color(0xFFF4EBB8), eyes = Color(0xFFFFE15A), eyeGlow = 1f, stars = true,
        )
        TimeOfDay.DUSK -> Palette(
            skyTop = Color(0xFF2A1745), skyBottom = Color(0xFFE27B4B), far = Color(0xFF5C2F5F),
            mid = Color(0xFF34203F), near = Color(0xFF1A1124), groundFar = Color(0xFF4A2A44),
            ground = Color(0xFF24172C), water = Color(0xFF6A3A5E), ink = Color(0xFF0A050C),
            moon = Color(0xFFFFD27C), eyes = Color(0xFFFFD34A), eyeGlow = 0.8f, stars = false,
        )
        TimeOfDay.DAWN -> Palette(
            skyTop = Color(0xFF2E3A6B), skyBottom = Color(0xFFE9A1AF), far = Color(0xFF6B5B8B),
            mid = Color(0xFF3E3A5F), near = Color(0xFF221F3A), groundFar = Color(0xFF55506F),
            ground = Color(0xFF2A2740), water = Color(0xFF5E6C9A), ink = Color(0xFF0B0915),
            moon = Color(0xFFF6E7D0), eyes = Color(0xFFFFE07A), eyeGlow = 0.5f, stars = false,
        )
        // Even by day it's a gloomy, storybook kind of day.
        TimeOfDay.DAY -> Palette(
            skyTop = Color(0xFF7A9CA6), skyBottom = Color(0xFFCAD7BF), far = Color(0xFF7D8F87),
            mid = Color(0xFF4B6056), near = Color(0xFF2B3A32), groundFar = Color(0xFF6E7D5A),
            ground = Color(0xFF45553B), water = Color(0xFF5F8A92), ink = Color(0xFF111A15),
            moon = Color(0xFFF7F1D2), eyes = Color(0xFFFFF0A0), eyeGlow = 0.22f, stars = false,
        )
    }
    // Murky weather washes the distance out towards the sky.
    val haze = when (sky.weather) {
        Weather.FOG -> 0.55f
        Weather.CLOUDY, Weather.DRIZZLE -> 0.2f
        Weather.RAIN, Weather.SNOW -> 0.3f
        Weather.STORM -> 0.35f
        Weather.CLEAR -> 0f
    }
    val gloom = if (sky.weather == Weather.STORM || sky.weather == Weather.RAIN) 0.25f else 0f
    fun hazed(c: Color, amount: Float) = lerp(lerp(c, base.skyBottom, haze * amount), Color.Black, gloom * 0.5f)
    return Palette(
        skyTop = lerp(base.skyTop, Color.Black, gloom),
        skyBottom = lerp(base.skyBottom, base.skyTop, gloom),
        far = hazed(base.far.times(tint.far), 1f),
        mid = hazed(base.mid.times(tint.foliage), 0.6f),
        near = hazed(base.near.times(tint.foliage), 0.25f),
        groundFar = hazed(base.groundFar.times(tint.ground), 0.8f),
        ground = hazed(base.ground.times(tint.ground), 0.2f),
        water = hazed(base.water.times(tint.water), 0.6f),
        ink = base.ink,
        moon = base.moon,
        eyes = base.eyes,
        eyeGlow = base.eyeGlow,
        stars = base.stars && sky.weather == Weather.CLEAR,
    )
}

// ---- Layout -----------------------------------------------------------------------------------

private enum class Skyline {
    MOUNTAINS,
    HILLS,
    JAGGED,
    TREELINE,
    LOW_TREELINE,
    TOWN,

    /** Flat-topped mesas with sheer sides. */
    MESAS,

    /** Almost nothing: the prairie runs flat to the sky. */
    FLAT,

    /** The rounded crowns of hardwoods. */
    ROUND_TREELINE,

    /** Long, smooth, wooded ridges, one behind another. */
    ROLLING,

    /** Mountains with snow on the peaks. */
    SNOWCAPS,

    /** Broad volcanic mountains. */
    VOLCANO,
}

private enum class Kind {
    PINE,
    SCRUB_PINE,
    TRUNK,
    FERN,
    REEDS,
    ROCK,
    GRASS,
    WINDMILL,
    BRIDGE,
    WATER_TOWER,
    POOL,
    EYES,
    POST,
    BROADLEAF,
    LIVE_OAK,
    PALM,
    JUNIPER,
    SAGE,
    PALMETTO,
}

/** [size] is a fraction of the scene's height. [depth] runs from 0 at the horizon to 1 at the bottom. */
private class Prop(val kind: Kind, val bearing: Double, val size: Float, val depth: Float, val seed: Int)

/**
 * One band of the scene at a single distance. [nearness] runs from 0 at the horizon to 1 right in
 * front of the player, and sets how fast the band slides, how hazy it is and how strong its lines are.
 */
private class Layer(val nearness: Float, val props: List<Prop>) {
    /** Where the band meets the ground, for its mist. */
    val baseDepth: Float = props.map { it.depth }.sorted().let { it.getOrElse(it.size / 2) { 0f } }
}

/** Something standing on the ground near the player, [east] and [north] metres from the patch's middle. */
private class Thing(val kind: Kind, val east: Float, val north: Float, val height: Float, val seed: Int)

/**
 * The ground around the player: a square patch [size] metres across that repeats in every
 * direction, so there's always more ahead however far they walk. [fog] is the distance, in
 * metres, over which things fade into the haze.
 */
private class World(val size: Float, val fog: Float, val things: List<Thing>) {
    /** Keeps a position inside the patch, so it never grows large enough to lose precision. */
    fun wrap(point: Offset) = Offset(wrapped(point.x), wrapped(point.y))

    /** The shortest way across the patch's repeats, from -size/2 to size/2. */
    fun wrapped(metres: Float) = metres - size * kotlin.math.round(metres / size)
}

private class Layout(
    val skyline: Skyline,
    /** How far below the horizon water reaches, as a depth. Zero means no water. */
    val water: Float,
    /** The far bands on the horizon, far to near. */
    val layers: List<Layer>,
    val world: World,
    val moonBearing: Double,
    val pools: Boolean = false,
    val track: Double? = null,
)

private class Scatter(seed: Long) {
    private val random = SeededRandom(seed)
    private var next = 0
    fun roll(): Float = random.nextDouble().toFloat()
    fun between(low: Float, high: Float) = low + (high - low) * roll()

    /** [count] props spread round the full circle, jittered so they don't look planted in rows. */
    fun around(kind: Kind, count: Int, size: ClosedFloatingPointRange<Float>, depth: ClosedFloatingPointRange<Float>) =
        (0 until count).map { i ->
            val bearing = (i + roll() * 0.8) * 360.0 / count
            Prop(
                kind,
                bearing,
                between(size.start, size.endInclusive),
                between(depth.start, depth.endInclusive),
                next++,
            )
        }

    fun at(kind: Kind, bearing: Double, size: Float, depth: Float) = Prop(kind, bearing, size, depth, next++)

    /** [count] things placed at random across a ground patch [size] metres across. */
    fun scatter(kind: Kind, count: Int, height: ClosedFloatingPointRange<Float>, size: Float) = (0 until count).map {
        Thing(
            kind,
            (roll() - 0.5f) * size,
            (roll() - 0.5f) * size,
            between(height.start, height.endInclusive),
            next++,
        )
    }

    /** A fence running north and south right across the patch, so it joins up with its repeats. */
    fun fence(east: Float, spacing: Float, height: Float, size: Float) =
        (0 until (size / spacing).toInt()).map { i -> Thing(Kind.POST, east, -size / 2 + i * spacing, height, i) }
}

/** The scene's layout, with the skyline, trees and plants swapped for the region's own. */
private fun layoutFor(scene: Scene, region: Region): Layout {
    val layout = baseLayout(scene)
    if (region == Region.NORTHWEST || scene == Scene.PINE_BARRENS) return layout
    return Layout(
        skyline = regionalSkyline(layout.skyline, region),
        water = layout.water,
        layers = layout.layers.map { layer ->
            Layer(
                layer.nearness,
                layer.props.map { p ->
                    val (kind, scale) = regionalKind(p.kind, p.seed, scene, region, far = true)
                    Prop(kind, p.bearing, p.size * scale, p.depth, p.seed)
                },
            )
        },
        world = World(
            layout.world.size,
            layout.world.fog,
            layout.world.things.map { t ->
                val (kind, scale) = regionalKind(t.kind, t.seed, scene, region)
                Thing(kind, t.east, t.north, t.height * scale, t.seed)
            },
        ),
        moonBearing = layout.moonBearing,
        pools = layout.pools,
        track = layout.track,
    )
}

private fun regionalSkyline(skyline: Skyline, region: Region): Skyline = when (region) {
    Region.NORTHWEST -> skyline
    Region.DESERT -> when (skyline) {
        Skyline.MOUNTAINS, Skyline.HILLS, Skyline.JAGGED -> Skyline.MESAS
        else -> skyline
    }
    Region.PLAINS -> when (skyline) {
        Skyline.MOUNTAINS, Skyline.HILLS -> Skyline.FLAT
        Skyline.TREELINE -> Skyline.ROUND_TREELINE
        Skyline.JAGGED -> Skyline.MESAS
        else -> skyline
    }
    Region.MIDWEST, Region.SOUTH -> when (skyline) {
        Skyline.MOUNTAINS, Skyline.TREELINE -> Skyline.ROUND_TREELINE
        Skyline.HILLS -> Skyline.FLAT
        Skyline.JAGGED -> Skyline.HILLS
        else -> skyline
    }
    Region.EAST -> when (skyline) {
        Skyline.MOUNTAINS, Skyline.HILLS, Skyline.JAGGED -> Skyline.ROLLING
        Skyline.TREELINE -> Skyline.ROUND_TREELINE
        else -> skyline
    }
    Region.ALASKA -> when (skyline) {
        Skyline.MOUNTAINS, Skyline.HILLS, Skyline.JAGGED -> Skyline.SNOWCAPS
        else -> skyline
    }
    Region.HAWAII -> when (skyline) {
        Skyline.TOWN -> skyline
        else -> Skyline.VOLCANO
    }
}

/**
 * What grows in the region in place of [kind], and how its height compares. Woods that mix two kinds
 * pick by [seed], so the same tree is always the same kind. [far] is for the bands on the horizon.
 */
private fun regionalKind(kind: Kind, seed: Int, scene: Scene, region: Region, far: Boolean = false): Pair<Kind, Float> =
    when (kind) {
        Kind.PINE -> when (region) {
            Region.NORTHWEST, Region.ALASKA -> Kind.PINE to 1f
            // Ponderosa up on the rims, junipers everywhere lower down.
            Region.DESERT -> if (scene == Scene.TIMBER) Kind.PINE to 1f else Kind.JUNIPER to 0.45f
            Region.PLAINS -> Kind.BROADLEAF to 0.9f
            Region.MIDWEST -> if (scene == Scene.TIMBER && seed % 4 == 0) Kind.PINE to 1f else Kind.BROADLEAF to 0.9f
            Region.EAST -> if (seed % 3 == 0) Kind.PINE to 1f else Kind.BROADLEAF to 0.95f
            // A live oak's broad, low dome flattens into a thin cap at a distance, so far off the woods
            // are pines and rounded hardwoods, and the live oaks only stand up close.
            Region.SOUTH -> when {
                seed % 2 == 0 -> Kind.PINE to 1.05f
                far -> Kind.BROADLEAF to 0.85f
                else -> Kind.LIVE_OAK to 0.7f
            }
            Region.HAWAII -> if (scene == Scene.TIMBER && seed % 3 != 0) Kind.BROADLEAF to 0.9f else Kind.PALM to 0.9f
        }
        Kind.FERN -> when (region) {
            Region.DESERT -> Kind.SAGE to 1f
            Region.SOUTH -> Kind.PALMETTO to 1.1f
            Region.PLAINS -> Kind.GRASS to 0.8f
            else -> Kind.FERN to 1f
        }
        Kind.GRASS -> when (region) {
            Region.DESERT -> if (seed % 2 == 0) Kind.SAGE to 1.2f else Kind.GRASS to 0.8f
            Region.SOUTH -> if (seed % 7 == 0) Kind.PALMETTO to 1.8f else Kind.GRASS to 1f
            else -> Kind.GRASS to 1f
        }
        else -> kind to 1f
    }

private fun baseLayout(scene: Scene): Layout {
    val s = Scatter(0x5CE7EL + scene.ordinal)

    // Far bands stay put on the horizon. Within a band, props are sorted far to near.
    fun band(nearness: Float, vararg groups: List<Prop>) =
        Layer(nearness, groups.toList().flatten().sortedBy { it.depth })

    // Everything closer stands on the ground around the player and moves as they walk.
    fun world(size: Float, fog: Float, vararg groups: List<Thing>) = World(size, fog, groups.toList().flatten())

    return when (scene) {
        Scene.LAKE_SHORE -> Layout(
            skyline = Skyline.MOUNTAINS,
            water = 0.34f,
            moonBearing = 20.0,
            layers = listOf(
                band(0f, s.around(Kind.PINE, 56, 0.03f..0.05f, 0f..0.002f)),
                band(0.25f, s.around(Kind.PINE, 34, 0.06f..0.09f, 0.003f..0.006f)),
                band(0.6f, s.around(Kind.EYES, 3, 0.05f..0.06f, 0.16f..0.28f)),
            ),
            world = world(
                120f,
                60f,
                s.scatter(Kind.REEDS, 240, 1.2f..2f, 120f),
                s.scatter(Kind.ROCK, 110, 0.5f..1.2f, 120f),
            ),
        )
        Scene.TIMBER -> Layout(
            skyline = Skyline.TREELINE,
            water = 0f,
            moonBearing = 300.0,
            layers = listOf(
                band(0f, s.around(Kind.PINE, 72, 0.08f..0.12f, 0f..0.012f)),
                band(
                    0.3f,
                    s.around(Kind.PINE, 48, 0.15f..0.2f, 0.02f..0.035f),
                ),
            ),
            world = world(
                140f,
                40f,
                s.scatter(Kind.PINE, 230, 8f..15f, 140f),
                s.scatter(Kind.TRUNK, 150, 7f..8f, 140f),
                s.scatter(Kind.FERN, 450, 0.6f..0.9f, 140f),
                s.scatter(Kind.EYES, 30, 0.5f..0.5f, 140f),
            ),
        )
        Scene.PASTURE -> Layout(
            skyline = Skyline.HILLS,
            water = 0f,
            moonBearing = 70.0,
            layers = listOf(
                band(0.1f, s.around(Kind.PINE, 12, 0.04f..0.06f, 0.004f..0.01f)),
                band(
                    0.35f,
                    listOf(s.at(Kind.WINDMILL, 140.0, 0.3f, 0.08f)),
                ),
            ),
            world = world(
                120f,
                70f,
                s.scatter(Kind.GRASS, 500, 0.35f..0.7f, 120f),
                s.scatter(Kind.ROCK, 30, 0.3f..0.6f, 120f),
                s.scatter(Kind.EYES, 12, 0.5f..0.5f, 120f),
                s.fence(east = 9f, spacing = FENCE_SPACING, height = 1.3f, size = 120f),
            ),
        )
        Scene.RIDGE -> Layout(
            skyline = Skyline.JAGGED,
            water = 0f,
            moonBearing = 230.0,
            layers = listOf(
                band(
                    0.15f,
                    s.around(Kind.ROCK, 10, 0.03f..0.05f, 0.02f..0.06f),
                ),
            ),
            world = world(
                120f,
                60f,
                s.scatter(Kind.ROCK, 240, 0.4f..2.6f, 120f),
                s.scatter(Kind.GRASS, 200, 0.3f..0.5f, 120f),
                s.scatter(Kind.EYES, 20, 0.5f..0.5f, 120f),
            ),
        )
        Scene.RIVER_TOWN -> Layout(
            skyline = Skyline.TOWN,
            water = 0.18f,
            moonBearing = 340.0,
            layers = listOf(
                band(
                    0.2f,
                    listOf(s.at(Kind.BRIDGE, 30.0, 0.22f, 0f)),
                    listOf(s.at(Kind.EYES, 22.0, 0.05f, 0.08f), s.at(Kind.EYES, 41.0, 0.04f, 0.1f)),
                ),
                band(0.5f, listOf(s.at(Kind.WATER_TOWER, 250.0, 0.28f, 0.22f))),
            ),
            world = world(
                120f,
                60f,
                s.scatter(Kind.GRASS, 300, 0.35f..0.6f, 120f),
                s.scatter(Kind.PINE, 30, 7f..11f, 120f),
                s.scatter(Kind.EYES, 10, 0.5f..0.5f, 120f),
                s.fence(east = -4f, spacing = FENCE_SPACING, height = 1.1f, size = 120f),
            ),
        )
        Scene.PINE_BARRENS -> Layout(
            skyline = Skyline.LOW_TREELINE,
            water = 0f,
            moonBearing = 150.0,
            pools = true,
            track = 0.0,
            layers = listOf(band(0f, s.around(Kind.SCRUB_PINE, 60, 0.04f..0.06f, 0f..0.01f))),
            world = world(
                120f,
                55f,
                s.scatter(Kind.POOL, 25, 2f..3.5f, 120f),
                s.scatter(Kind.SCRUB_PINE, 220, 2.5f..5f, 120f),
                s.scatter(Kind.GRASS, 400, 0.3f..0.6f, 120f),
                s.scatter(Kind.EYES, 25, 0.5f..0.5f, 120f),
            ),
        )
    }
}

// ---- Projection -------------------------------------------------------------------------------

/** Where things land on screen for the current heading, with the head-bob of walking applied. */
private class View(scope: DrawScope, val heading: Double, val bob: Float) {
    val width = scope.size.width
    val height = scope.size.height
    val center = width / 2
    val horizon = height * HORIZON + bob
    val pxPerDegree = width / FIELD_OF_VIEW

    /** Pixels per metre of height at one metre away, for drawing things in perspective. */
    val focal = pxPerDegree * (180f / PI.toFloat())
    val ink = with(scope) { 1.6.dp.toPx() }
    val thinInk = with(scope) { 1.dp.toPx() }

    /** Screen x for [bearing] in a layer that slides at [parallax]. */
    fun x(bearing: Double, parallax: Float): Float {
        val loop = 360f * pxPerDegree * parallax
        var d = (((bearing - heading) / 360.0).toFloat() * loop) % loop
        if (d > loop / 2) d -= loop
        if (d < -loop / 2) d += loop
        return center + d
    }

    /** Where on the strip of a layer the screen column [x] falls, from 0 to 1 round the circle. */
    fun around(x: Float, parallax: Float): Float {
        val loop = 360f * pxPerDegree * parallax
        val u = ((heading / 360.0).toFloat() * loop + (x - center)) / loop
        return u - floor(u)
    }

    fun groundY(depth: Float) = horizon + depth * (height - horizon)

    fun visible(x: Float, halfWidth: Float) = x + halfWidth > 0 && x - halfWidth < width
}

// ---- Drawing helpers --------------------------------------------------------------------------

/** A repeatable number from 0 to 1, so the same prop always gets the same wobble. */
private fun noise(i: Int, k: Int): Float {
    val x = sin(i * 127.1 + k * 311.7) * 43758.5453
    return (x - floor(x)).toFloat()
}

/** A fill with an outline round it. */
private fun DrawScope.inked(path: Path, fill: Color, ink: Color, width: Float) {
    drawPath(path, fill)
    if (width > 0f) drawPath(path, ink, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Which side the moon lights a prop from, and the shadow laid over the other side. */
private class Light(val fromRight: Boolean, val shadow: Color)

/**
 * A fill with its shadowed half darkened, split at [splitX]. The lit half faces the moon, which
 * gives each shape some roundness. The outline goes on last.
 */
private fun DrawScope.shaded(path: Path, fill: Color, light: Light, splitX: Float, ink: Color, width: Float) {
    drawPath(path, fill)
    val left = if (light.fromRight) -size.width else splitX
    val right = if (light.fromRight) splitX else size.width * 2
    clipRect(left = left, top = -size.height, right = right, bottom = size.height * 2) {
        drawPath(path, light.shadow)
    }
    if (width > 0f) drawPath(path, ink, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** A thick stroke with an outline, for trunks, posts and branches. */
private fun DrawScope.inkedLine(a: Offset, b: Offset, fill: Color, ink: Color, thickness: Float, outline: Float) {
    if (outline > 0f) drawLine(ink, a, b, thickness + outline * 2, StrokeCap.Round)
    drawLine(fill, a, b, thickness, StrokeCap.Round)
}

/** A closed shape through [points], drawn with soft corners so edges look hand-drawn. */
private fun blob(points: List<Offset>): Path = Path().apply {
    val mid = { a: Offset, b: Offset -> Offset((a.x + b.x) / 2, (a.y + b.y) / 2) }
    val start = mid(points.last(), points.first())
    moveTo(start.x, start.y)
    for (i in points.indices) {
        val point = points[i]
        val end = mid(point, points[(i + 1) % points.size])
        quadraticTo(point.x, point.y, end.x, end.y)
    }
    close()
}

// ---- Sky and distance -------------------------------------------------------------------------

private fun DrawScope.drawSky(view: View, layout: Layout, palette: Palette, t: Float, time: TimeOfDay) {
    drawRect(Brush.verticalGradient(listOf(palette.skyTop, palette.skyBottom), 0f, view.horizon))
    if (palette.stars) {
        repeat(60) { i ->
            val x = view.x(noise(i, 1) * 360.0, SKY_PARALLAX)
            val y = noise(i, 2) * view.horizon * 0.75f
            val twinkle = 0.5f + 0.5f * sin(t * (1f + noise(i, 3) * 2f) + i)
            drawCircle(
                palette.moon.copy(alpha = 0.25f + 0.55f * twinkle),
                (1f + noise(i, 4) * 1.5f).dp.toPx(),
                Offset(x, y),
            )
        }
    }
    // A big moon, or a pale sun behind the murk by day.
    val moonX = view.x(layout.moonBearing, SKY_PARALLAX)
    val moonY = view.horizon * 0.32f
    val r = view.height * 0.07f
    if (view.visible(moonX, r * 4)) {
        val centre = Offset(moonX, moonY)
        drawCircle(
            Brush.radialGradient(listOf(palette.moon.copy(alpha = 0.35f), Color.Transparent), centre, r * 3.2f),
            r * 3.2f,
            centre,
        )
        drawCircle(palette.moon, r, centre)
        if (time != TimeOfDay.DAY) {
            drawCircle(palette.moon.copy(alpha = 0.6f), r * 0.18f, centre + Offset(-r * 0.35f, -r * 0.2f))
            drawCircle(lerp(palette.moon, palette.skyTop, 0.25f), r * 0.12f, centre + Offset(r * 0.3f, r * 0.3f))
        }
    }
    // Soft clouds that drift by slowly.
    repeat(6) { i ->
        val bearing = (noise(i, 5) * 360f + t * 0.6f).toDouble()
        val x = view.x(bearing, SKY_PARALLAX)
        val width = view.height * (0.16f + noise(i, 6) * 0.12f)
        if (!view.visible(x, width)) return@repeat
        val y = view.horizon * (0.12f + noise(i, 7) * 0.4f)
        val puffs = (0 until 5).map { k ->
            val angle = k * PI.toFloat() * 2 / 5
            Offset(x + cos(angle) * width * 0.5f, y + sin(angle) * width * 0.16f - abs(sin(angle)) * width * 0.08f)
        }
        val cloud = lerp(palette.skyBottom, palette.skyTop, 0.45f).copy(alpha = 0.55f)
        drawPath(blob(puffs), cloud)
    }
}

/** The far horizon, a single ink line all the way round. */
private fun DrawScope.drawSkyline(view: View, skyline: Skyline, palette: Palette) {
    val h = view.height
    fun heightAt(u: Float): Float {
        val a = u * 2 * PI.toFloat()
        return when (skyline) {
            Skyline.MOUNTAINS -> {
                val peaks = abs(sin(a * 5 + 0.3f)) * 0.13f + abs(sin(a * 11 + 1.7f)) * 0.06f
                h * (0.03f + peaks + 0.02f * sin(a * 37))
            }
            Skyline.HILLS -> h * (0.035f + 0.025f * sin(a * 3 + 1f) + 0.015f * sin(a * 7 + 2f))
            Skyline.JAGGED -> {
                val saw = abs((u * 23) - floor(u * 23) - 0.5f) * 2
                h * (0.06f + 0.05f * sin(a * 4) + 0.05f * saw * (0.5f + noise(floor(u * 23).toInt(), 9)))
            }
            Skyline.TREELINE, Skyline.LOW_TREELINE -> {
                val count = 140
                val spike = 1 - abs((u * count) - floor(u * count) - 0.5f) * 2
                val tall = noise(floor(u * count).toInt(), 8)
                val base = if (skyline == Skyline.TREELINE) 0.05f else 0.025f
                h * (base + spike * (0.03f + 0.04f * tall) + 0.01f * sin(a * 5))
            }
            Skyline.TOWN -> {
                val block = floor(u * 90).toInt()
                val gap = noise(block, 4) < 0.18f
                if (gap) h * 0.012f else h * (0.02f + 0.06f * noise(block, 3))
            }
            Skyline.MESAS -> {
                // Some stretches rise into a mesa: a flat top and steep sides. The rest is low desert.
                val cells = 9
                val cell = floor(u * cells).toInt()
                val v = u * cells - cell
                val mesa = noise(cell, 21) > 0.35f
                val edge = (minOf(v, 1 - v) / 0.1f).coerceIn(0f, 1f)
                val top = 0.05f + 0.07f * noise(cell, 22)
                h * (0.012f + 0.004f * sin(a * 29) + if (mesa) top * edge * edge * (3 - 2 * edge) else 0f)
            }
            Skyline.FLAT -> {
                // A windbreak or a farmstead's trees, now and then.
                val cell = floor(u * 160).toInt()
                val v = u * 160 - cell
                val clump = if (noise(cell, 23) > 0.86f) sqrt(maxOf(0f, 1 - (2 * v - 1) * (2 * v - 1))) * 0.012f else 0f
                h * (0.006f + 0.003f * sin(a * 3) + clump)
            }
            Skyline.ROUND_TREELINE -> {
                val count = 110
                val v = u * count - floor(u * count)
                val crown = sqrt(maxOf(0f, 1 - (2 * v - 1) * (2 * v - 1)))
                val tall = noise(floor(u * count).toInt(), 8)
                h * (0.03f + crown * (0.012f + 0.02f * tall) + 0.008f * sin(a * 5))
            }
            Skyline.ROLLING -> {
                val v = u * 150 - floor(u * 150)
                val crowns = sqrt(maxOf(0f, 1 - (2 * v - 1) * (2 * v - 1))) * 0.004f
                h * (0.045f + 0.03f * sin(a * 2 + 1f) + 0.015f * sin(a * 5 + 2f) + crowns)
            }
            Skyline.SNOWCAPS -> {
                val peaks = abs(sin(a * 4 + 0.6f)) * 0.17f + abs(sin(a * 9 + 1.2f)) * 0.07f
                h * (0.03f + peaks + 0.015f * sin(a * 41))
            }
            Skyline.VOLCANO -> {
                // Two broad shield volcanoes, gentle-sided, with the sea's flat line between them.
                fun shield(centre: Float, reach: Float, tall: Float): Float {
                    val d = abs(u - centre).let { minOf(it, 1 - it) }
                    return tall * maxOf(0f, 1 - d / reach).pow(1.4f)
                }
                h * (0.008f + shield(0.15f, 0.2f, 0.17f) + shield(0.62f, 0.16f, 0.12f) + 0.003f * sin(a * 23))
            }
        }
    }
    if (skyline == Skyline.ROLLING) {
        // A paler ridge further off, as the Blue Ridge fades away behind itself.
        val back = Path().apply {
            moveTo(-view.width * 0.01f, view.horizon)
            var x = -3f
            while (x <= view.width + 3f) {
                val a = view.around(x, FAR_PARALLAX * 0.7f) * 2 * PI.toFloat()
                lineTo(x, view.horizon - h * (0.075f + 0.03f * sin(a * 3 + 2.5f) + 0.012f * sin(a * 7)))
                x += 3.dp.toPx()
            }
            lineTo(view.width + 3f, view.horizon)
            close()
        }
        drawPath(back, lerp(palette.far, palette.skyBottom, 0.45f))
    }
    val step = 3.dp.toPx()
    val path = Path().apply {
        moveTo(-step, view.horizon + step)
        var x = -step
        while (x <= view.width + step) {
            lineTo(x, view.horizon - heightAt(view.around(x, FAR_PARALLAX)))
            x += step
        }
        lineTo(view.width + step, view.horizon + step)
        close()
    }
    inked(path, palette.far, palette.ink.copy(alpha = 0.35f), view.thinInk)
    if (skyline == Skyline.SNOWCAPS) {
        // Snow on everything above the snowline, its lower edge ragged. Each peak gets its own patch,
        // so nothing is drawn where the range dips below the line.
        val snowline = h * 0.18f
        fun ragged(u: Float) = snowline + h * 0.012f * noise(floor(u * 300).toInt(), 24)
        val snow = lerp(palette.moon, palette.far, 0.3f).copy(alpha = 0.85f)
        var run = mutableListOf<Offset>()
        fun finish() {
            if (run.size > 1) {
                val patch = Path().apply {
                    moveTo(run.first().x, view.horizon - ragged(view.around(run.first().x, FAR_PARALLAX)))
                    run.forEach { lineTo(it.x, it.y) }
                    for (p in run.reversed()) lineTo(p.x, view.horizon - ragged(view.around(p.x, FAR_PARALLAX)))
                    close()
                }
                drawPath(patch, snow)
            }
            run = mutableListOf()
        }
        var x = -step
        while (x <= view.width + step) {
            val u = view.around(x, FAR_PARALLAX)
            val top = heightAt(u)
            if (top > ragged(u)) run += Offset(x, view.horizon - top) else finish()
            x += step
        }
        finish()
    }
    if (skyline == Skyline.TOWN) {
        // A few lit windows. Somebody's still up.
        repeat(40) { i ->
            val x = view.x(noise(i, 11) * 360.0, FAR_PARALLAX)
            if (!view.visible(x, 4f)) return@repeat
            val u = view.around(x, FAR_PARALLAX)
            val top = view.horizon - heightAt(u)
            val y = top + (view.horizon - top) * (0.25f + noise(i, 12) * 0.5f)
            if (y < view.horizon - 4.dp.toPx()) {
                drawRect(palette.eyes.copy(alpha = 0.55f), Offset(x, y), Size(3.dp.toPx(), 4.dp.toPx()))
            }
        }
    }
}

private fun DrawScope.drawGround(view: View, layout: Layout, palette: Palette, t: Float) {
    val bottom = view.height + 12.dp.toPx()
    drawRect(
        Brush.verticalGradient(listOf(palette.groundFar, palette.ground), view.horizon, bottom),
        Offset(0f, view.horizon),
        Size(view.width, bottom - view.horizon),
    )
    if (layout.water > 0f) {
        val top = view.horizon
        val bottomOfWater = view.groundY(layout.water)
        drawRect(
            Brush.verticalGradient(
                listOf(lerp(palette.water, palette.skyBottom, 0.3f), palette.water),
                top,
                bottomOfWater,
            ),
            Offset(0f, top),
            Size(view.width, bottomOfWater - top),
        )
        // The moon's wobbling reflection, and a few ripples.
        val moonX = view.x(layout.moonBearing, SKY_PARALLAX)
        var y = top + 4.dp.toPx()
        var k = 0
        while (y < bottomOfWater - 2.dp.toPx()) {
            val half = view.height * 0.05f * (1f - (y - top) / (bottomOfWater - top) * 0.6f)
            val sway = sin(t * 1.6f + k) * 6.dp.toPx()
            drawLine(
                palette.moon.copy(alpha = 0.55f),
                Offset(moonX - half + sway, y),
                Offset(moonX + half * 0.7f + sway, y),
                2.dp.toPx(),
                StrokeCap.Round,
            )
            y += 7.dp.toPx()
            k++
        }
        repeat(12) { i ->
            val x = view.x(noise(i, 13) * 360.0, MID_PARALLAX)
            val ry = top + (bottomOfWater - top) * noise(i, 14)
            val len = view.height * 0.03f
            drawLine(
                palette.ink.copy(alpha = 0.35f),
                Offset(x - len, ry),
                Offset(x + len, ry),
                1.5.dp.toPx(),
                StrokeCap.Round,
            )
        }
        drawLine(
            palette.ink.copy(alpha = 0.4f),
            Offset(0f, bottomOfWater),
            Offset(view.width, bottomOfWater),
            view.thinInk,
        )
    }
    layout.track?.let { bearing ->
        // A pale sandy track running off towards the horizon.
        val x = view.x(bearing, NEAR_PARALLAX)
        val spread = view.width * 0.55f
        if (view.visible(x, spread)) {
            val path = Path().apply {
                moveTo(x - view.width * 0.01f, view.horizon + 1)
                lineTo(x + view.width * 0.01f, view.horizon + 1)
                lineTo(x + spread, bottom)
                lineTo(x - spread * 0.8f, bottom)
                close()
            }
            inked(path, lerp(palette.groundFar, palette.moon, 0.18f), palette.ink.copy(alpha = 0.3f), view.thinInk)
        }
    }
    drawLine(palette.ink.copy(alpha = 0.3f), Offset(0f, view.horizon), Offset(view.width, view.horizon), view.thinInk)
}

// ---- Props ------------------------------------------------------------------------------------

private fun DrawScope.drawLayer(view: View, layer: Layer, moonBearing: Double, palette: Palette, t: Float) {
    val parallax = FAR_PARALLAX + (NEAR_PARALLAX - FAR_PARALLAX) * layer.nearness
    layer.props.forEach { prop ->
        val x = view.x(prop.bearing, parallax)
        val s = prop.size * view.height
        val reach = if (prop.kind == Kind.BRIDGE) s * 3 else s
        if (!view.visible(x, reach)) return@forEach
        val fromRight = turnDegrees(prop.bearing, moonBearing) > 0
        drawProp(view, prop.kind, x, view.groundY(prop.depth), s, prop.seed, layer.nearness, 1f, fromRight, palette, t)
    }
}

/**
 * Draws one thing. [nearness] runs from 0 far off in the haze to 1 right beside the player, and
 * sets its colour, line and shadow. [fade] lets things ease in and out at the edges of the view.
 */
private fun DrawScope.drawProp(
    view: View,
    kind: Kind,
    x: Float,
    base: Float,
    s: Float,
    seed: Int,
    nearness: Float,
    fade: Float,
    lightFromRight: Boolean,
    palette: Palette,
    t: Float,
) {
    val n = nearness
    // Distant things fade into the haze. Near ones are dark and crisp.
    val fog = lerp(palette.skyBottom, palette.far, 0.45f)
    val tone = lerp(lerp(palette.mid, palette.near, n), fog, (1 - n) * (1 - n) * 0.75f)
    val fill = tone.faded(fade)
    val ink = palette.ink.copy(alpha = (0.15f + 0.55f * n) * fade)
    val width = if (n < 0.15f) 0f else view.ink * (0.35f + 0.65f * n)
    val light = Light(lightFromRight, Color.Black.copy(alpha = (0.12f + 0.2f * n) * fade))
    // A soft contact shadow pins trees and rocks to the ground.
    if (kind in GROUNDED && n > 0f) {
        val spread = when (kind) {
            Kind.TRUNK -> s * 0.18f
            Kind.LIVE_OAK -> s * 0.75f
            else -> s * 0.3f
        }
        drawOval(
            Color.Black.copy(alpha = (0.3f * n + 0.08f) * fade),
            Offset(x - spread, base - spread * 0.12f),
            Size(spread * 2, spread * 0.24f),
        )
    }
    when (kind) {
        Kind.PINE -> drawPine(x, base, s, seed, fill, light, ink, width)
        Kind.SCRUB_PINE -> drawScrubPine(x, base, s, seed, fill, light, ink, width)
        Kind.TRUNK -> drawTrunk(x, base, s, seed, fill, light, ink, width, palette, t)
        Kind.FERN -> drawFern(x, base, s, seed, lerp(tone, palette.mid, 0.4f).faded(fade), ink, width)
        Kind.REEDS -> drawReeds(x, base, s, seed, fill, ink, width)
        Kind.ROCK -> drawRock(x, base, s, seed, lerp(tone, palette.far, 0.3f).faded(fade), light, ink, width)
        Kind.GRASS -> drawGrass(x, base, s, seed, lerp(tone, palette.mid, 0.3f).faded(fade), ink, width)
        Kind.WINDMILL -> drawWindmill(x, base, s, fill, ink, width, t)
        Kind.BRIDGE -> drawBridge(x, base, s, fill, ink, width)
        Kind.WATER_TOWER -> drawWaterTower(x, base, s, fill, ink, width)
        Kind.POOL -> drawPool(x, base, s, palette, fade)
        Kind.EYES -> drawEyes(x, base - s * 0.4f, s, seed, palette, t, fade)
        Kind.BROADLEAF -> drawBroadleaf(x, base, s, seed, fill, light, ink, width)
        Kind.LIVE_OAK -> drawLiveOak(x, base, s, seed, fill, light, ink, width, palette)
        Kind.PALM -> drawPalm(x, base, s, seed, fill, light, ink, width)
        Kind.JUNIPER -> drawJuniper(x, base, s, seed, fill, light, ink, width)
        Kind.SAGE -> drawSage(x, base, s, seed, lerp(tone, Color(0xFF8E9A84), 0.25f).faded(fade), ink, width)
        Kind.PALMETTO -> drawPalmetto(x, base, s, seed, lerp(tone, palette.mid, 0.35f).faded(fade), ink, width)
        Kind.POST -> inkedLine(
            Offset(x, base),
            Offset(x, base - s),
            postWood(tone).faded(fade),
            ink,
            s * 0.08f,
            width / 2,
        )
    }
}

private fun postWood(tone: Color) = lerp(tone, Color(0xFF6A5540), 0.3f)

private fun Color.faded(fade: Float) = if (fade >= 1f) this else copy(alpha = alpha * fade)

/** Where a spot on the ground lands on screen from where the player stands, or null if it's behind them. */
private class Seen(val thing: Thing, val x: Float, val base: Float, val s: Float, val r: Float, val bearing: Double)

private fun look(
    view: View,
    world: World,
    walker: Offset,
    east: Float,
    north: Float,
    height: Float,
): Triple<Float, Float, Float>? {
    val dx = world.wrapped(east - walker.x)
    val dy = world.wrapped(north - walker.y)
    val r = hypot(dx, dy)
    if (r < NEAR_CLIP * 0.5f) return null
    val bearing = Math.toDegrees(atan2(dx, dy).toDouble())
    if (abs(turnDegrees(view.heading, bearing)) > 89.0) return null
    val x = view.x(bearing, NEAR_PARALLAX)
    return Triple(x, view.horizon + view.focal * EYE_HEIGHT / r, view.focal * height / r)
}

/**
 * The ground around the player, drawn in true perspective from where they stand: far things small
 * and high, near things big and low, and everything swinging out to the sides as they walk past.
 */
private fun DrawScope.drawWorld(
    view: View,
    layout: Layout,
    walker: Offset,
    palette: Palette,
    t: Float,
    creature: SceneCreature?,
) {
    val world = layout.world
    val half = world.size / 2
    val waterline = if (layout.water > 0f) view.groundY(layout.water) else Float.NEGATIVE_INFINITY
    val seen = ArrayList<Seen>(world.things.size / 4)
    for (thing in world.things) {
        val dx = world.wrapped(thing.east - walker.x)
        val dy = world.wrapped(thing.north - walker.y)
        val r = hypot(dx, dy)
        if (r < NEAR_CLIP) continue
        val bearing = Math.toDegrees(atan2(dx, dy).toDouble())
        if (abs(turnDegrees(view.heading, bearing)) > FIELD_OF_VIEW / 2 + 30) continue
        val x = view.x(bearing, NEAR_PARALLAX)
        val s = view.focal * thing.height / r
        if (!view.visible(x, s)) continue
        val base = view.horizon + view.focal * EYE_HEIGHT / r
        // Only things at home in the water stand out in the lake or the river.
        if (base < waterline && thing.kind !in IN_WATER) continue
        seen += Seen(thing, x, base, s, r, bearing)
    }
    seen.sortByDescending { it.r }

    // Mist banks and the creature are slotted in among the things on the ground by distance, so
    // whatever stands in front of the creature hides it, and mist further off lies behind it.
    var mist = 0
    var pending = creature
    fun drawEverythingBeyond(r: Float) {
        while (true) {
            val nextMist = MIST_DISTANCES.getOrNull(mist)?.takeIf { it > r }
            val nextCreature = pending?.distance?.takeIf { it > r }
            when {
                nextMist != null && (nextCreature == null || nextMist >= nextCreature) -> {
                    drawMistAt(
                        view,
                        view.horizon + view.focal * EYE_HEIGHT / nextMist,
                        exp(-nextMist / world.fog),
                        palette,
                    )
                    mist++
                }
                pending != null && nextCreature != null -> {
                    drawCreature(view, pending!!, palette, layout.moonBearing, world.fog, t)
                    pending = null
                }
                else -> return
            }
        }
    }
    for (it in seen) {
        drawEverythingBeyond(it.r)
        val thing = it.thing
        val dx = abs(world.wrapped(thing.east - walker.x))
        val dy = abs(world.wrapped(thing.north - walker.y))
        // Eases things in at the far edge of the patch and out as they brush past the player.
        val edge = ((1f - maxOf(dx, dy) / half) / 0.15f).coerceIn(0f, 1f)
        // Tall trees fade sooner, so the player seems to pass between them rather than into them.
        val clip = if (thing.kind in TALL_TREES) thing.height * PINE_CLIP_PER_METRE else NEAR_CLIP
        val close = ((it.r - clip) / clip).coerceIn(0f, 1f)
        val fade = edge * close * seeThrough(view, creature, it)
        if (fade <= 0.01f) continue
        val nearness = exp(-it.r / world.fog)
        val fromRight = turnDegrees(it.bearing, layout.moonBearing) > 0
        when (thing.kind) {
            // Eyes peer out from about waist height, whatever their distance.
            Kind.EYES -> drawEyes(
                it.x,
                it.base - view.focal * 1.1f / it.r,
                view.focal * 0.5f / it.r,
                thing.seed,
                palette,
                t,
                fade,
            )
            Kind.POST -> {
                drawRails(view, world, walker, thing, it, nearness, fade, palette)
                drawProp(view, thing.kind, it.x, it.base, it.s, thing.seed, nearness, fade, fromRight, palette, t)
            }
            else -> drawProp(view, thing.kind, it.x, it.base, it.s, thing.seed, nearness, fade, fromRight, palette, t)
        }
    }
    drawEverythingBeyond(0f)
}

/**
 * A cryptid in perspective: smaller and hazier further off, with a gait for how it gets about, a
 * moonlit edge, and a shadow or ripples where it meets the ground. When it's hard to see, it smears
 * like a shaky photo. Off screen, a faint arrow shows which way it went.
 */
private fun DrawScope.drawCreature(
    view: View,
    c: SceneCreature,
    palette: Palette,
    moonBearing: Double,
    fogDistance: Float,
    t: Float,
) {
    val vis = c.visibility
    if (vis <= 0.01f) return
    val d = c.distance
    val x = view.x(c.bearing, NEAR_PARALLAX)
    val ground = view.horizon + view.focal * EYE_HEIGHT / d
    val height = view.focal * c.heightMeters / d
    val side = height / c.fill
    if (!view.visible(x, side * 0.4f)) {
        drawCreatureArrow(view, turnDegrees(view.heading, c.bearing) > 0, vis, t)
        return
    }
    val haze = 1f - exp(-d / fogDistance)
    // How far through its stride, wingbeat or roll of its body it is, in radians.
    val gait = c.stride * 2 * PI.toFloat()
    // With a cycle of frames the art itself steps or beats its wings, bobbing as it goes, so it's
    // drawn as it is.
    val animated = c.frames.size > 1
    val art = if (animated) c.frames[frameAt(gait, c.frames.size)] else c.frames.first()
    // Walkers rise on each step, fliers lift on each downstroke, swimmers ride low in the water.
    val lift = when (c.motion) {
        CreatureMotion.WALKS -> if (animated) 0f else abs(sin(gait)) * height * 0.035f
        CreatureMotion.FLIES -> view.focal * (1.6f + if (animated) 0f else 0.25f * sin(gait)) / d
        CreatureMotion.SWIMS -> -height * 0.4f + sin(gait * 0.5f) * height * 0.03f
        CreatureMotion.STANDS -> 0f
    }
    val feet = ground - lift

    if (c.motion == CreatureMotion.SWIMS) {
        // Rings spreading out across the water round it.
        repeat(3) { k ->
            val grow = (t * 0.45f + k / 3f) % 1f
            val w = side * (0.35f + 0.45f * grow)
            drawOval(
                palette.moon.copy(alpha = 0.35f * vis * (1 - grow)),
                Offset(x - w, ground - w * 0.08f),
                Size(w * 2, w * 0.16f),
                style = Stroke(1.5.dp.toPx()),
            )
        }
    } else {
        val w = side * c.footprint
        val strength = if (c.motion == CreatureMotion.FLIES) 0.2f else 0.4f
        drawOval(
            Color.Black.copy(alpha = strength * vis * (1 - haze * 0.5f)),
            Offset(x - w, ground - w * 0.1f),
            Size(w * 2, w * 0.2f),
        )
    }

    val sway = when (c.motion) {
        CreatureMotion.WALKS -> if (animated) 0f else sin(gait) * 2.5f
        CreatureMotion.FLIES -> sin(gait * 0.3f) * 6f
        CreatureMotion.SWIMS -> sin(gait * 0.4f) * 2f
        CreatureMotion.STANDS -> 0f
    }
    val mirror = if (c.movingRight) -1f else 1f
    val middle = Offset(side / 2, side / 2)
    val fog = ColorFilter.tint(lerp(palette.skyBottom, palette.far, 0.45f), BlendMode.SrcIn)
    val moonlight = ColorFilter.tint(palette.moon, BlendMode.SrcIn)
    val rim = 1.4.dp.toPx() * if (turnDegrees(c.bearing, moonBearing) > 0) 1f else -1f
    // Swimmers are cut off at the waterline.
    val bottom = if (c.motion == CreatureMotion.SWIMS) ground else size.height * 2
    clipRect(left = -size.width, top = -size.height, right = size.width * 2, bottom = bottom) {
        translate(x - side / 2, feet - side * ART_FEET) {
            rotate(sway, pivot = Offset(side / 2, side * ART_FEET)) {
                // A smear trailing behind it when it's hard to make out, like a shaky photo.
                val smear = 1f - vis
                if (smear > 0.05f) {
                    val behind = if (c.movingRight) -1f else 1f
                    for (k in 1..2) {
                        translate(behind * k * side * 0.07f * smear, 0f) {
                            scale(mirror, 1f, pivot = middle) {
                                drawBending(art, side, 0.22f * vis, fog, c.motion.takeUnless { animated }, gait)
                            }
                        }
                    }
                }
                // Moonlight along the edge facing the moon.
                translate(rim, -abs(rim)) {
                    scale(mirror, 1f, pivot = middle) {
                        drawBending(
                            art,
                            side,
                            0.55f * vis * (1 - haze),
                            moonlight,
                            c.motion.takeUnless {
                                animated
                            },
                            gait,
                        )
                    }
                }
                scale(mirror, 1f, pivot = middle) {
                    drawBending(art, side, vis, null, c.motion.takeUnless { animated }, gait)
                    // Further off, the haze settles over it like everything else.
                    drawBending(art, side, haze * 0.75f * vis, fog, c.motion.takeUnless { animated }, gait)
                }
            }
        }
    }
}

/** Which of [count] frames, spread evenly over one stride, to show at [gait] radians into it. */
private fun frameAt(gait: Float, count: Int): Int {
    val cycle = (gait / (2 * PI.toFloat())).let { it - floor(it) }
    return (cycle * count).toInt().coerceIn(0, count - 1)
}

/** Thin enough that the steps between neighbouring strips don't show. */
private const val BEND_STRIPS = 24

/**
 * Draws one creature's art bent into motion, or just as it is when [motion] is null because the
 * art is already a frame of its own animation. A single silhouette has no separate limbs, so it's
 * cut into thin strips, each shifted a little:
 *  - walkers in rows, so the legs swing with each step, the body counter-sways and the head nods
 *  - fliers in columns, so the outer wings rise and fall about a steady body
 *  - swimmers in columns, so a wave rolls along the body and its humps rise and fall
 * The shifts change smoothly from strip to strip, so the silhouette bends rather than breaks.
 */
private fun DrawScope.drawBending(
    art: Painter,
    side: Float,
    alpha: Float,
    filter: ColorFilter?,
    motion: CreatureMotion?,
    gait: Float,
) {
    if (alpha <= 0.01f) return
    val box = Size(side, side)
    if (motion == null) {
        with(art) { draw(box, alpha, filter) }
        return
    }
    val step = side / BEND_STRIPS
    for (i in 0 until BEND_STRIPS) {
        // Where this strip falls across the art, from 0 to 1.
        val f = (i + 0.5f) / BEND_STRIPS
        var dx = 0f
        var dy = 0f
        when (motion) {
            CreatureMotion.WALKS -> {
                val legs = ((f - 0.55f) / 0.45f).coerceAtLeast(0f).let { it * sqrt(it) }
                val head = ((0.3f - f) / 0.3f).coerceAtLeast(0f)
                dx = side * (sin(gait) * (0.08f * legs - 0.012f) + sin(gait + 0.6f) * 0.012f * head)
            }
            CreatureMotion.FLIES -> {
                val out = (f - 0.5f) * 2
                dy = -sin(gait) * side * 0.14f * out * out
            }
            CreatureMotion.SWIMS -> dy = sin(gait - f * 7.5f) * side * 0.035f
            CreatureMotion.STANDS -> Unit
        }
        val from = i * step
        val to = from + step
        if (motion == CreatureMotion.WALKS) {
            clipRect(left = -side, top = from, right = side * 2, bottom = to) {
                translate(dx, dy) { with(art) { draw(box, alpha, filter) } }
            }
        } else {
            clipRect(left = from, top = -side, right = to, bottom = side * 2) {
                translate(dx, dy) { with(art) { draw(box, alpha, filter) } }
            }
        }
    }
}

/** A faint chevron at the edge of the screen pointing the way the creature went. */
private fun DrawScope.drawCreatureArrow(view: View, right: Boolean, visibility: Float, t: Float) {
    val dir = if (right) 1f else -1f
    val x = if (right) view.width - 24.dp.toPx() else 24.dp.toPx()
    val y = view.horizon - 30.dp.toPx()
    val nudge = sin(t * 4f) * 3.dp.toPx() * dir
    val arrow = Path().apply {
        moveTo(x + nudge - dir * 8.dp.toPx(), y - 13.dp.toPx())
        lineTo(x + nudge + dir * 6.dp.toPx(), y)
        lineTo(x + nudge - dir * 8.dp.toPx(), y + 13.dp.toPx())
    }
    drawPath(
        arrow,
        Color.White.copy(alpha = 0.55f * visibility),
        style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

/**
 * Trees and rocks standing between the player and the creature thin out, so it shows through them
 * like a shape glimpsed between branches instead of vanishing for half the encounter.
 */
private fun seeThrough(view: View, creature: SceneCreature?, seen: Seen): Float {
    if (creature == null || seen.r >= creature.distance || seen.thing.kind !in SEE_THROUGH) return 1f
    val creatureX = view.x(creature.bearing, NEAR_PARALLAX)
    val creatureHalf = view.focal * creature.heightMeters / creature.distance / creature.fill / 2
    val reach = seen.s * if (seen.thing.kind == Kind.TRUNK) 0.12f else 0.3f
    // Eases in as the creature moves behind it rather than snapping, and only while it can be seen.
    val overlap = ((creatureHalf + reach - abs(seen.x - creatureX)) / (creatureHalf + reach * 0.5f)).coerceIn(0f, 1f)
    return 1f - 0.65f * overlap * creature.visibility
}

private val SEE_THROUGH = setOf(
    Kind.PINE,
    Kind.SCRUB_PINE,
    Kind.TRUNK,
    Kind.ROCK,
    Kind.REEDS,
    Kind.BROADLEAF,
    Kind.LIVE_OAK,
    Kind.PALM,
    Kind.JUNIPER,
)

/** Trees tall enough that the player seems to pass between them rather than into them. */
private val TALL_TREES = setOf(Kind.PINE, Kind.BROADLEAF, Kind.LIVE_OAK, Kind.PALM)

/** Rails from a fence post back to the posts either side of it that are further away. */
private fun DrawScope.drawRails(
    view: View,
    world: World,
    walker: Offset,
    post: Thing,
    here: Seen,
    nearness: Float,
    fade: Float,
    palette: Palette,
) {
    val tone = lerp(
        lerp(palette.mid, palette.near, nearness),
        lerp(palette.skyBottom, palette.far, 0.45f),
        (1 - nearness) * (1 - nearness) * 0.75f,
    )
    val wood = postWood(tone).faded(fade)
    val ink = palette.ink.copy(alpha = (0.15f + 0.55f * nearness) * fade)
    val width = if (nearness < 0.15f) 0f else view.ink * (0.35f + 0.65f * nearness)
    for (step in listOf(-FENCE_SPACING, FENCE_SPACING)) {
        val north = post.north + step
        val (nx, nBase, ns) = look(view, world, walker, post.east, north, post.height) ?: continue
        val r = hypot(world.wrapped(post.east - walker.x), world.wrapped(north - walker.y))
        if (r <= here.r) continue
        for (f in listOf(0.35f, 0.75f)) {
            inkedLine(
                Offset(here.x, here.base - here.s * f),
                Offset(nx, nBase - ns * f),
                wood,
                ink,
                here.s * 0.05f,
                width / 2,
            )
        }
    }
}

private val IN_WATER = setOf(Kind.REEDS, Kind.ROCK, Kind.EYES)

private val GROUNDED = setOf(
    Kind.PINE,
    Kind.SCRUB_PINE,
    Kind.TRUNK,
    Kind.ROCK,
    Kind.BROADLEAF,
    Kind.LIVE_OAK,
    Kind.PALM,
    Kind.JUNIPER,
)

/** A soft bank of mist lying along the foot of a band, thicker the further off it is. */
private fun DrawScope.drawMist(view: View, layer: Layer, palette: Palette) =
    drawMistAt(view, view.groundY(layer.baseDepth), layer.nearness, palette)

private fun DrawScope.drawMistAt(view: View, y: Float, nearness: Float, palette: Palette) {
    val rise = view.height * (0.04f + 0.08f * nearness)
    val fog = lerp(palette.skyBottom, palette.far, 0.45f).copy(alpha = 0.22f + 0.18f * (1 - nearness))
    drawRect(
        Brush.verticalGradient(
            listOf(Color.Transparent, fog, fog.copy(alpha = fog.alpha * 0.5f)),
            y - rise,
            y + rise * 0.3f,
        ),
        Offset(0f, y - rise),
        Size(view.width, rise * 1.3f),
    )
}

private fun DrawScope.drawPine(
    x: Float,
    base: Float,
    s: Float,
    seed: Int,
    fill: Color,
    light: Light,
    ink: Color,
    width: Float,
) {
    val lean = (noise(seed, 1) - 0.5f) * 0.1f * s
    // A square-cut trunk, so the tree stands on the ground instead of hanging off a peg.
    drawLine(lerp(fill, Color.Black, 0.3f), Offset(x, base), Offset(x, base - s * 0.12f), s * 0.05f, StrokeCap.Butt)
    // Six ragged tiers whose tips droop, narrowing to a thin leader at the top.
    val tiers = 6
    val left = mutableListOf<Offset>()
    val right = mutableListOf<Offset>()
    for (i in 0 until tiers) {
        val y = base - s * (0.08f + 0.14f * i)
        val half = s * (0.27f - 0.038f * i) * (0.85f + noise(seed, i + 2) * 0.3f)
        val middle = x + lean * i / tiers
        val droop = s * 0.025f
        left += Offset(middle - half, y + droop)
        left += Offset(middle - half * 0.35f, y - s * 0.07f)
        right += Offset(middle + half * (0.9f + noise(seed, i + 9) * 0.2f), y + droop)
        right += Offset(middle + half * 0.35f, y - s * 0.07f)
    }
    val path = Path().apply {
        moveTo(left[0].x, left[0].y)
        left.drop(1).forEach { lineTo(it.x, it.y) }
        lineTo(x + lean, base - s)
        right.reversed().forEach { lineTo(it.x, it.y) }
        close()
    }
    shaded(path, fill, light, x + lean * 0.5f, ink, width)
}

private fun DrawScope.drawScrubPine(
    x: Float,
    base: Float,
    s: Float,
    seed: Int,
    fill: Color,
    light: Light,
    ink: Color,
    width: Float,
) {
    val lean = (noise(seed, 1) - 0.5f) * 0.3f * s
    inkedLine(
        Offset(x, base),
        Offset(x + lean * 0.5f, base - s * 0.45f),
        lerp(fill, Color.Black, 0.25f),
        ink,
        s * 0.07f,
        width / 2,
    )
    // Pitch pines grow in ragged clumps rather than neat cones.
    repeat(3) { i ->
        val cx = x + lean * (0.4f + i * 0.3f) + (noise(seed, i + 5) - 0.5f) * s * 0.3f
        val cy = base - s * (0.45f + i * 0.2f)
        val r = s * (0.28f - i * 0.05f)
        val points = (0 until 9).map { k ->
            val angle = k * 2 * PI.toFloat() / 9
            val wobble = 0.75f + noise(seed * 7 + i, k) * 0.45f
            Offset(cx + cos(angle) * r * wobble, cy + sin(angle) * r * 0.6f * wobble)
        }
        shaded(blob(points), fill, light, cx, ink, width)
    }
}

private fun DrawScope.drawTrunk(
    x: Float,
    base: Float,
    s: Float,
    seed: Int,
    fill: Color,
    light: Light,
    ink: Color,
    width: Float,
    palette: Palette,
    t: Float,
) {
    val w = s * 0.12f
    val lean = (noise(seed, 1) - 0.5f) * s * 0.12f
    // The trunk runs on up out of view into the canopy.
    val top = minOf(base - s, -size.height * 0.05f)
    val path = Path().apply {
        moveTo(x - w * 0.9f, base + w * 0.2f)
        quadraticTo(x - w * 0.55f, base - w * 0.4f, x - w * 0.5f, base - w)
        quadraticTo(x - w * 0.4f + lean * 0.5f, (base + top) / 2, x - w * 0.35f + lean, top)
        lineTo(x + w * 0.35f + lean, top)
        quadraticTo(x + w * 0.45f + lean * 0.5f, (base + top) / 2, x + w * 0.5f, base - w)
        quadraticTo(x + w * 0.55f, base - w * 0.4f, x + w * 0.9f, base + w * 0.2f)
        close()
    }
    // Rounded like a real trunk: lit on the moon's side, falling away into shadow on the other.
    val lit = lerp(fill, palette.moon, 0.12f)
    val dark = lerp(fill, Color.Black, 0.45f)
    val tones = if (light.fromRight) listOf(dark, fill, lit, fill) else listOf(fill, lit, fill, dark)
    drawPath(path, Brush.horizontalGradient(tones, x - w, x + w + lean))
    drawPath(path, ink, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    // Bark grooves.
    repeat(3) { i ->
        val gx = x + (i - 1) * w * 0.25f + lean * 0.4f
        val y0 = base - s * (0.15f + noise(seed, i + 3) * 0.2f)
        drawLine(
            ink.copy(alpha = 0.35f),
            Offset(gx, y0),
            Offset(gx + lean * 0.1f, y0 - s * 0.25f),
            width * 0.5f,
            StrokeCap.Round,
        )
    }
    // Some trunks have a knot hole, and some knot holes have something in them.
    if (noise(seed, 9) > 0.45f) {
        val hole = Offset(x + lean * 0.3f, base - s * 0.42f)
        drawOval(palette.ink, hole - Offset(w * 0.18f, w * 0.26f), Size(w * 0.36f, w * 0.52f))
        if (noise(seed, 10) > 0.4f) drawEyes(hole.x, hole.y, w * 0.5f, seed, palette, t)
    }
}

/** A lumpy outline round [centre], [rx] by [ry], for crowns and bushes. */
private fun lumpy(centre: Offset, rx: Float, ry: Float, seed: Int, lumps: Int = 11, flatBottom: Boolean = false): Path =
    blob(
        (0 until lumps).map { k ->
            val angle = k * 2 * PI.toFloat() / lumps
            val wobble = 0.8f + noise(seed, k + 30) * 0.35f
            val y = sin(angle) * ry * wobble
            Offset(centre.x + cos(angle) * rx * wobble, centre.y + if (flatBottom && y > 0) y * 0.45f else y)
        },
    )

/** A hardwood: a straight trunk under a big, rounded, lumpy crown. */
private fun DrawScope.drawBroadleaf(
    x: Float,
    base: Float,
    s: Float,
    seed: Int,
    fill: Color,
    light: Light,
    ink: Color,
    width: Float,
) {
    val lean = (noise(seed, 1) - 0.5f) * 0.08f * s
    inkedLine(
        Offset(x, base),
        Offset(x + lean, base - s * 0.5f),
        lerp(fill, Color.Black, 0.3f),
        ink,
        s * 0.06f,
        width / 2,
    )
    val crown = Offset(x + lean, base - s * 0.66f)
    val rx = s * (0.3f + 0.08f * noise(seed, 2))
    shaded(lumpy(crown, rx, s * 0.32f, seed), fill, light, crown.x, ink, width)
    // A second, smaller mass of leaves on one side breaks up the outline.
    val side = if (noise(seed, 3) > 0.5f) 1f else -1f
    shaded(
        lumpy(crown + Offset(side * rx * 0.55f, s * 0.08f), rx * 0.5f, s * 0.18f, seed + 1, 9),
        fill,
        light,
        crown.x + side * rx * 0.55f,
        ink,
        width,
    )
}

/**
 * A live oak: a short, massive trunk that splits low into heavy limbs sprawling out sideways, a
 * broad dome of separate, rough leaf masses with sky between them, and Spanish moss hanging in
 * ragged curtains from underneath.
 */
private fun DrawScope.drawLiveOak(
    x: Float,
    base: Float,
    s: Float,
    seed: Int,
    fill: Color,
    light: Light,
    ink: Color,
    width: Float,
    palette: Palette,
) {
    val bark = lerp(fill, Color(0xFF3A3028), 0.45f)
    val fork = Offset(x, base - s * 0.26f)
    // The trunk flares at the foot and is nearly as thick as it is tall.
    val trunk = Path().apply {
        moveTo(x - s * 0.13f, base)
        quadraticTo(x - s * 0.07f, base - s * 0.06f, x - s * 0.065f, fork.y)
        lineTo(x + s * 0.065f, fork.y)
        quadraticTo(x + s * 0.07f, base - s * 0.06f, x + s * 0.14f, base)
        close()
    }
    inked(trunk, bark, ink, width)
    // Leaf masses spread wide and low at the ends, higher over the middle.
    val masses = 7
    val centres = (0 until masses).map { k ->
        val t = k / (masses - 1f) * 2 - 1
        Offset(
            x + t * s * 0.68f + (noise(seed, k + 60) - 0.5f) * s * 0.08f,
            base - s * (0.5f + 0.24f * (1 - t * t)) - noise(seed, k + 70) * s * 0.06f,
        )
    }
    // Heavy limbs from the fork out to the masses, bending up and then out.
    for ((k, c) in centres.withIndex()) {
        if (k % 2 == 1 && k != masses / 2) continue
        val limb = Path().apply {
            moveTo(fork.x, fork.y + s * 0.02f)
            quadraticTo((fork.x + c.x) / 2, fork.y - s * 0.02f - abs(c.x - x) * 0.15f, c.x, c.y + s * 0.06f)
        }
        drawPath(limb, ink, style = Stroke(s * 0.045f + width, cap = StrokeCap.Round))
        drawPath(limb, bark, style = Stroke(s * 0.045f, cap = StrokeCap.Round))
    }
    for ((k, c) in centres.withIndex()) {
        val rx = s * (0.19f + 0.06f * noise(seed, k + 80))
        val ry = s * (0.13f + 0.04f * noise(seed, k + 90))
        val lumps = 13
        val outline = blob(
            (0 until lumps).map { i ->
                val angle = i * 2 * PI.toFloat() / lumps
                val wobble = 0.7f + noise(seed * 3 + k, i) * 0.55f
                Offset(c.x + cos(angle) * rx * wobble, c.y + sin(angle) * ry * wobble)
            },
        )
        shaded(outline, fill, light, c.x, ink, width)
    }
    // Spanish moss: loose clumps of thin, wavy strands in the shade under each mass. Each clump hangs
    // from one spot, its strands of different lengths fading out at the ends.
    val moss = lerp(fill, Color(0xFF9AA08E), 0.4f)
    for ((k, c) in centres.withIndex()) {
        repeat(3) { clump ->
            val hang = Offset(
                c.x + (clump - 1f) * s * 0.11f + (noise(seed + clump, k * 3) - 0.5f) * s * 0.05f,
                c.y + s * (0.06f + 0.05f * noise(seed, k * 17 + clump)),
            )
            val clumpLength = s * (0.1f + 0.18f * noise(seed + clump * 13, k * 11))
            repeat(6) { strand ->
                val length = clumpLength * (0.55f + 0.45f * noise(seed + strand, k * 7 + clump))
                val spread = (strand - 2.5f) * s * 0.009f
                val steps = 6
                for (step in 0 until steps) {
                    val f0 = step / steps.toFloat()
                    val f1 = (step + 1) / steps.toFloat()
                    // Strands fan out a little as they hang, and wave.
                    val at = { f: Float ->
                        Offset(
                            hang.x + spread * (0.4f + f) + sin(f * 6f + strand * 1.9f + k) * s * 0.008f * f,
                            hang.y + length * f,
                        )
                    }
                    drawLine(
                        moss.copy(alpha = fill.alpha * 0.7f * (1f - f0 * 0.8f)),
                        at(f0),
                        at(f1),
                        s * 0.0065f,
                        StrokeCap.Round,
                    )
                }
            }
        }
    }
}

/** A coconut palm: a thin, curving trunk and a burst of drooping fronds at the top. */
private fun DrawScope.drawPalm(
    x: Float,
    base: Float,
    s: Float,
    seed: Int,
    fill: Color,
    light: Light,
    ink: Color,
    width: Float,
) {
    val lean = (noise(seed, 1) - 0.5f) * 0.5f * s
    val top = Offset(x + lean, base - s)
    val trunk = Path().apply {
        moveTo(x, base)
        quadraticTo(x + lean * 0.1f, base - s * 0.6f, top.x, top.y)
    }
    drawPath(trunk, ink, style = Stroke(s * 0.045f + width, cap = StrokeCap.Round))
    drawPath(trunk, lerp(fill, Color(0xFF6A5A44), 0.35f), style = Stroke(s * 0.045f, cap = StrokeCap.Round))
    repeat(7) { i ->
        val angle = (-150f + i * 50f + (noise(seed, i + 2) - 0.5f) * 20f) * PI.toFloat() / 180f
        val reach = s * (0.32f + 0.08f * noise(seed, i + 9))
        val tip = Offset(top.x + cos(angle) * reach, top.y + abs(sin(angle)) * reach * 0.2f + reach * 0.35f)
        val bend = Offset(top.x + cos(angle) * reach * 0.55f, top.y - reach * 0.18f)
        val frond = Path().apply {
            moveTo(top.x, top.y)
            quadraticTo(bend.x, bend.y - s * 0.03f, tip.x, tip.y)
            quadraticTo(bend.x, bend.y + s * 0.03f, top.x, top.y + s * 0.02f)
            close()
        }
        shaded(frond, fill, light, top.x, ink, width)
    }
}

/** A juniper: short and gnarled, wider than it is tall, its foliage in dense, uneven masses. */
private fun DrawScope.drawJuniper(
    x: Float,
    base: Float,
    s: Float,
    seed: Int,
    fill: Color,
    light: Light,
    ink: Color,
    width: Float,
) {
    val lean = (noise(seed, 1) - 0.5f) * 0.4f * s
    inkedLine(
        Offset(x, base),
        Offset(x + lean * 0.4f, base - s * 0.35f),
        lerp(fill, Color(0xFF5A4636), 0.4f),
        ink,
        s * 0.09f,
        width / 2,
    )
    repeat(2) { i ->
        val c = Offset(x + lean * (0.3f + 0.4f * i) + (i - 0.5f) * s * 0.25f, base - s * (0.5f + 0.18f * i))
        shaded(lumpy(c, s * (0.34f - 0.06f * i), s * (0.26f - 0.04f * i), seed + i, 10), fill, light, c.x, ink, width)
    }
}

/** A low, round, grey-green sagebrush. */
private fun DrawScope.drawSage(x: Float, base: Float, s: Float, seed: Int, fill: Color, ink: Color, width: Float) {
    inked(lumpy(Offset(x, base - s * 0.4f), s * 0.6f, s * 0.42f, seed, 10, flatBottom = true), fill, ink, width)
}

/** A palmetto: a fan of stiff, pointed leaves from the ground. */
private fun DrawScope.drawPalmetto(x: Float, base: Float, s: Float, seed: Int, fill: Color, ink: Color, width: Float) {
    repeat(7) { i ->
        val angle = (-75f + i * 25f + (noise(seed, i) - 0.5f) * 10f) * PI.toFloat() / 180f
        val stem = Offset(x + sin(angle) * s * 0.3f, base - cos(angle) * s * 0.4f)
        inkedLine(Offset(x, base), stem, fill, ink, s * 0.025f, width / 2)
        // Each leaf is a fan of narrow blades.
        val blades = Path().apply {
            moveTo(stem.x, stem.y)
            for (k in -2..2) {
                val b = angle + k * 0.16f
                lineTo(stem.x + sin(b) * s * 0.42f, stem.y - cos(b) * s * 0.36f)
                lineTo(stem.x + sin(b + 0.08f) * s * 0.15f, stem.y - cos(b + 0.08f) * s * 0.13f)
            }
            close()
        }
        inked(blades, fill, ink, width)
    }
}

private fun DrawScope.drawFern(x: Float, base: Float, s: Float, seed: Int, fill: Color, ink: Color, width: Float) {
    repeat(5) { i ->
        val angle = (-70f + i * 35f + (noise(seed, i) - 0.5f) * 20f) * PI.toFloat() / 180f
        val tip = Offset(x + sin(angle) * s * 0.7f, base - cos(angle) * s * 0.55f)
        val bend = Offset(x + sin(angle) * s * 0.25f, base - s * 0.6f)
        val path = Path().apply {
            moveTo(x, base)
            quadraticTo(bend.x, bend.y, tip.x, tip.y)
        }
        drawPath(path, ink, style = Stroke(s * 0.07f + width, cap = StrokeCap.Round))
        drawPath(path, fill, style = Stroke(s * 0.07f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawReeds(x: Float, base: Float, s: Float, seed: Int, fill: Color, ink: Color, width: Float) {
    repeat(6) { i ->
        val lean = (noise(seed, i) - 0.5f) * s * 0.35f
        val foot = Offset(x + (i - 2.5f) * s * 0.05f, base)
        val tip = Offset(foot.x + lean, base - s * (0.6f + noise(seed, i + 7) * 0.4f))
        inkedLine(foot, tip, fill, ink, s * 0.025f, width / 2)
        if (i % 2 == 0) {
            val head = Offset(foot.x + lean * 0.85f, tip.y + s * 0.12f)
            val cattail = Path().apply {
                addOval(
                    Rect(
                        head.x - s * 0.035f,
                        head.y - s * 0.09f,
                        head.x + s * 0.035f,
                        head.y + s * 0.09f,
                    ),
                )
            }
            inked(cattail, Color(0xFF4A3426), ink, width / 2)
        }
    }
}

private fun DrawScope.drawRock(
    x: Float,
    base: Float,
    s: Float,
    seed: Int,
    fill: Color,
    light: Light,
    ink: Color,
    width: Float,
) {
    val points = (0 until 8).map { k ->
        val angle = PI.toFloat() + k * PI.toFloat() / 7
        val wobble = 0.75f + noise(seed, k) * 0.4f
        Offset(x + cos(angle) * s * 0.55f * wobble, base + sin(angle) * s * 0.6f * wobble)
    }
    val outline = points + Offset(x + s * 0.5f, base) + Offset(x - s * 0.5f, base)
    shaded(blob(outline), fill, light, x + s * 0.1f, ink, width)
    drawLine(
        Color.White.copy(alpha = 0.18f),
        Offset(x - s * 0.25f, base - s * 0.35f),
        Offset(
            x + s * 0.05f,
            base - s * 0.45f,
        ),
        width,
        StrokeCap.Round,
    )
}

private fun DrawScope.drawGrass(x: Float, base: Float, s: Float, seed: Int, fill: Color, ink: Color, width: Float) {
    val path = Path().apply {
        moveTo(x - s * 0.3f, base)
        repeat(5) { i ->
            val bx = x - s * 0.3f + i * s * 0.15f
            val lean = (noise(seed, i) - 0.5f) * s * 0.4f
            lineTo(bx + lean, base - s * (0.6f + noise(seed, i + 5) * 0.4f))
            lineTo(bx + s * 0.15f, base)
        }
        close()
    }
    inked(path, fill, ink, width)
}

private fun DrawScope.drawWindmill(x: Float, base: Float, s: Float, fill: Color, ink: Color, width: Float, t: Float) {
    val hub = Offset(x, base - s * 0.85f)
    for (side in listOf(-1f, 1f)) {
        inkedLine(Offset(x + side * s * 0.14f, base), hub, fill, ink, s * 0.025f, width / 2)
    }
    repeat(3) { i ->
        val y = base - s * (0.2f + i * 0.2f)
        val half = s * 0.14f * (1 - (base - y) / (s * 0.85f))
        drawLine(ink, Offset(x - half, y), Offset(x + half, y), width)
    }
    // The wheel turns on a breeze nobody else can feel.
    val spin = t * 0.5f
    repeat(10) { i ->
        val angle = spin + i * 2 * PI.toFloat() / 10
        val tip = hub + Offset(cos(angle) * s * 0.2f, sin(angle) * s * 0.2f)
        inkedLine(hub, tip, fill, ink, s * 0.03f, width / 2)
    }
    drawCircle(ink, s * 0.035f, hub)
    val vane = Path().apply {
        moveTo(hub.x, hub.y)
        lineTo(hub.x + s * 0.32f, hub.y - s * 0.04f)
        lineTo(hub.x + s * 0.32f, hub.y + s * 0.08f)
        close()
    }
    inked(vane, fill, ink, width / 2)
}

private fun DrawScope.drawBridge(x: Float, base: Float, s: Float, fill: Color, ink: Color, width: Float) {
    val span = s * 3
    val deck = base - s * 0.3f
    val left = x - span / 2
    // Stone piers down into the river.
    for (f in listOf(0.05f, 0.5f, 0.95f)) {
        inkedLine(
            Offset(left + span * f, deck),
            Offset(left + span * f, base + s * 0.12f),
            fill,
            ink,
            s * 0.1f,
            width / 2,
        )
    }
    inkedLine(Offset(left, deck), Offset(left + span, deck), fill, ink, s * 0.06f, width / 2)
    // Two humped steel trusses.
    for (half in 0..1) {
        val from = left + span * half / 2
        val panels = 6
        val tops = (0..panels).map { k ->
            val f = k / panels.toFloat()
            Offset(from + span / 2 * f, deck - s * 0.5f * sin(f * PI.toFloat()))
        }
        tops.zipWithNext().forEachIndexed { k, (a, b) ->
            drawLine(ink, a, b, width)
            drawLine(ink, Offset(b.x, deck), b, width * 0.6f)
            drawLine(
                ink,
                if (k % 2 ==
                    0
                ) {
                    Offset(a.x, deck)
                } else {
                    a
                },
                if (k % 2 == 0) b else Offset(b.x, deck),
                width * 0.5f,
            )
        }
    }
}

private fun DrawScope.drawWaterTower(x: Float, base: Float, s: Float, fill: Color, ink: Color, width: Float) {
    val tankBottom = base - s * 0.6f
    for (f in listOf(-0.22f, -0.07f, 0.07f, 0.22f)) {
        inkedLine(Offset(x + f * s * 1.3f, base), Offset(x + f * s, tankBottom), fill, ink, s * 0.02f, width / 2)
    }
    val tank = Path().apply {
        moveTo(x - s * 0.26f, tankBottom)
        lineTo(x - s * 0.26f, tankBottom - s * 0.22f)
        lineTo(x, tankBottom - s * 0.38f)
        lineTo(x + s * 0.26f, tankBottom - s * 0.22f)
        lineTo(x + s * 0.26f, tankBottom)
        close()
    }
    inked(tank, lerp(fill, Color.White, 0.08f), ink, width)
    drawLine(
        ink,
        Offset(x - s * 0.26f, tankBottom - s * 0.11f),
        Offset(x + s * 0.26f, tankBottom - s * 0.11f),
        width * 0.6f,
    )
}

private fun DrawScope.drawPool(x: Float, base: Float, s: Float, palette: Palette, fade: Float) {
    val cedar = lerp(palette.water, Color(0xFF5A3A1E), 0.55f).faded(fade)
    val path = Path().apply {
        addOval(Rect(x - s, base - s * 0.12f, x + s, base + s * 0.12f))
    }
    inked(path, cedar, palette.ink.faded(0.4f * fade), 1.dp.toPx())
    drawLine(
        palette.moon.copy(alpha = 0.35f * fade),
        Offset(x - s * 0.4f, base - s * 0.03f),
        Offset(
            x + s * 0.1f,
            base - s * 0.03f,
        ),
        2.dp.toPx(),
        StrokeCap.Round,
    )
}

/** A pair of glowing eyes that blink now and then, each pair on its own rhythm. */
private fun DrawScope.drawEyes(x: Float, y: Float, s: Float, seed: Int, palette: Palette, t: Float, fade: Float = 1f) {
    val period = 4f + noise(seed, 20) * 5f
    val phase = ((t + noise(seed, 21) * period) % period) / period
    // Shut briefly at the end of each cycle, and some pairs vanish for a while altogether.
    val open = when {
        phase > 0.96f -> 0.1f
        noise(seed + floor(t / period).toInt(), 22) < 0.3f -> 0f
        else -> 1f
    }
    if (open == 0f) return
    val gap = s * 0.32f
    val eye = Size(s * 0.22f, s * 0.14f * open)
    for (side in listOf(-1f, 1f)) {
        val c = Offset(x + side * gap / 2, y)
        drawCircle(
            Brush.radialGradient(
                listOf(palette.eyes.copy(alpha = 0.45f * palette.eyeGlow * fade), Color.Transparent),
                c,
                s * 0.4f,
            ),
            s * 0.4f,
            c,
        )
        drawOval(
            palette.eyes.copy(alpha = palette.eyeGlow.coerceAtLeast(0.35f) * fade),
            c - Offset(eye.width / 2, eye.height / 2),
            eye,
        )
        drawOval(
            palette.ink.faded(fade),
            c - Offset(eye.width * 0.08f, eye.height * 0.4f),
            Size(
                eye.width * 0.16f,
                eye.height * 0.8f,
            ),
        )
    }
}

// ---- The hint ---------------------------------------------------------------------------------

/**
 * Glowing tracks on the ground leading off the way the hint points, or an arrow at the edge of the
 * screen when that way is out of view. It's only ever a suggestion.
 */
private fun DrawScope.drawHint(view: View, bearing: Double, palette: Palette, t: Float) {
    val glow = Color(0xFFFFC85A)
    val pulse = 0.65f + 0.35f * sin(t * 2.2f)
    val turn = turnDegrees(view.heading, bearing).toFloat()
    if (abs(turn) < FIELD_OF_VIEW / 2 - 4) {
        val x = view.x(bearing, NEAR_PARALLAX)
        val depths = listOf(0.92f, 0.66f, 0.46f, 0.32f, 0.22f)
        depths.forEachIndexed { i, depth ->
            val y = view.groundY(depth)
            val size = view.height * 0.06f * depth
            val side = if (i % 2 == 0) -1f else 1f
            val px = x + side * size * 0.7f
            val foot = Path().apply {
                addOval(
                    Rect(
                        px - size * 0.35f,
                        y - size * 0.5f,
                        px + size * 0.35f,
                        y + size * 0.5f,
                    ),
                )
            }
            drawCircle(
                Brush.radialGradient(
                    listOf(glow.copy(alpha = 0.3f * pulse), Color.Transparent),
                    Offset(px, y),
                    size * 1.4f,
                ),
                size * 1.4f,
                Offset(px, y),
            )
            inked(foot, glow.copy(alpha = 0.75f * pulse), palette.ink, view.thinInk * depth)
            repeat(3) { k ->
                drawCircle(
                    glow.copy(alpha = 0.8f * pulse),
                    size * 0.12f,
                    Offset(
                        px + (k - 1) * size * 0.22f,
                        y - size * 0.68f,
                    ),
                )
            }
        }
    } else {
        val right = turn > 0
        val x = if (right) view.width - 28.dp.toPx() else 28.dp.toPx()
        val y = view.height * 0.7f
        val dir = if (right) 1f else -1f
        val nudge = sin(t * 3f) * 4.dp.toPx() * dir
        val arrow = Path().apply {
            moveTo(x + nudge - dir * 10.dp.toPx(), y - 16.dp.toPx())
            lineTo(x + nudge + dir * 8.dp.toPx(), y)
            lineTo(x + nudge - dir * 10.dp.toPx(), y + 16.dp.toPx())
            lineTo(x + nudge - dir * 4.dp.toPx(), y)
            close()
        }
        inked(arrow, glow.copy(alpha = pulse), palette.ink, view.thinInk)
    }
}
