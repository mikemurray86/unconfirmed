package app.unconfirmed.game.ui

import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import app.unconfirmed.game.LocalSound
import app.unconfirmed.game.R
import app.unconfirmed.shared.CreatureSpot
import app.unconfirmed.shared.EncounterPath
import app.unconfirmed.shared.EncounterSetup
import app.unconfirmed.shared.GeoPoint
import app.unconfirmed.shared.ON_SCREEN_WANDER_DEGREES
import app.unconfirmed.shared.Photo
import app.unconfirmed.shared.Region
import app.unconfirmed.shared.Scene
import app.unconfirmed.shared.TRACKING_WANDER_DEGREES
import app.unconfirmed.shared.creatureSpot
import app.unconfirmed.shared.shotScore
import kotlin.math.PI
import kotlin.math.hypot
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val POST_SHOT_PAUSE_MILLIS = 1_200L

/**
 * The quickest a walker's legs go, in full strides a second. When it crosses ground faster than
 * that, its feet slide a little rather than the walk turning into a blur.
 */
private const val MAX_STRIDES_PER_SECOND = 1.6

@Composable
fun EncounterScreen(
    creatureId: String,
    guessedRight: Boolean,
    cluesFound: Int,
    scene: Scene,
    region: Region,
    point: GeoPoint?,
    onFinished: (List<Photo>) -> Unit,
) {
    val setup = remember(guessedRight, cluesFound) { EncounterSetup.forSighting(guessedRight, cluesFound) }
    var started by remember { mutableStateOf(false) }

    if (!started) {
        val shots = if (setup.shots == 1) "one shot" else "${setup.shots} shots"
        MessageScreen(
            title = "It's close",
            // Says nothing of whether the player's guesses were right: they're never told.
            body = "You saw it, and it hasn't gone far. You have $shots. Hold your phone up and turn " +
                "to follow it. Wait until it's clear and in the middle of the frame.",
            action = "Raise the camera" to { started = true },
        )
        return
    }

    EncounterScene(creatureId, setup, scene, region, point, onFinished)
}

@Composable
private fun EncounterScene(
    creatureId: String,
    setup: EncounterSetup,
    scene: Scene,
    region: Region,
    point: GeoPoint?,
    onFinished: (List<Photo>) -> Unit,
) {
    var seconds by remember { mutableDoubleStateOf(0.0) }
    var shotsLeft by remember { mutableIntStateOf(setup.shots) }
    val photos = remember { mutableStateListOf<Photo>() }
    var lastScore by remember { mutableStateOf<Int?>(null) }
    // The size the scene is drawn at, in dp, so each photo can be drawn again the same way.
    var frame by remember { mutableStateOf(DpSize.Zero) }
    val density = LocalDensity.current
    var finished by remember { mutableStateOf(false) }
    val profile = creatureProfile(creatureId)

    // The player turns on the spot with the phone held up, which could tip the screen round
    // mid-shot. It stays whichever way up it was until the encounter's over.
    val activity = LocalActivity.current
    DisposableEffect(activity) {
        val before = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        onDispose { if (before != null) activity.requestedOrientation = before }
    }

    val sky = rememberSky(point)
    val heading = rememberHeading(fallbackDegrees = null)
    // With a compass, the creature roams all around and the player turns to follow it. Without
    // one, the view stays put and the creature keeps to what's on screen.
    val tracking = heading != null
    val wander = if (tracking) TRACKING_WANDER_DEGREES else ON_SCREEN_WANDER_DEGREES
    val currentWander by rememberUpdatedState(wander)

    // How many strides or wingbeats it has made, for its animation. A walker's steps follow the
    // ground it covers, so its feet don't skate. Wings beat at their own steady pace.
    var gait by remember { mutableDoubleStateOf(0.0) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        var last = profile.spot(setup, 0.0, currentWander)
        while (isActive && !finished) {
            val now = (withFrameNanos { it } - start) / 1_000_000_000.0
            val dt = now - seconds
            seconds = now
            val spot = profile.spot(setup, now, currentWander)
            gait += when (profile.motion) {
                CreatureMotion.WALKS -> {
                    val across = Math.toRadians(spot.offsetDegrees - last.offsetDegrees) * spot.distanceMeters
                    val covered = hypot(across, spot.distanceMeters - last.distanceMeters)
                    minOf(covered / profile.strideMeters, MAX_STRIDES_PER_SECOND * dt)
                }
                else -> (spot.stride - last.stride) * profile.pace * profile.motion.beatsPerStride
            }
            last = spot
            if (seconds >= setup.durationSeconds) finished = true
        }
    }
    LaunchedEffect(finished) {
        if (finished) {
            delay(POST_SHOT_PAUSE_MILLIS)
            onFinished(photos.toList())
        }
    }

    val spot = profile.spot(setup, seconds, wander)
    // Its path is turned so it starts straight ahead of wherever the player was first facing.
    var startBearing by remember { mutableStateOf<Double?>(null) }
    if (startBearing == null && (heading != null || seconds > 0.5)) {
        startBearing = (heading ?: 0.0) - profile.spot(setup, 0.0, wander).offsetDegrees
    }
    val aim = heading ?: 0.0
    val creatureBearing = (startBearing ?: aim) + spot.offsetDegrees

    // The music tightens for the whole encounter, and most of all while the creature is clear.
    val sound = LocalSound.current
    SideEffect { sound?.tension = 0.6f + 0.4f * spot.visibility.toFloat() }
    DisposableEffect(sound) { onDispose { sound?.tension = 0f } }

    Box(Modifier.fillMaxSize().onSizeChanged { frame = with(density) { it.toSize().toDpSize() } }) {
        FirstPersonScene(
            scene = scene,
            region = region,
            sky = sky,
            headingDegrees = aim,
            walkedMeters = 0.0,
            hintBearing = null,
            modifier = Modifier.fillMaxSize(),
            creature = sceneCreature(
                creatureId,
                bearing = creatureBearing,
                distance = spot.distanceMeters,
                visibility = spot.visibility,
                movingRight = spot.movingRight,
                stride = gait,
            ),
        )
        AtmosphereOverlay(biome = scene.biome, sky = sky, modifier = Modifier.fillMaxSize())
        Viewfinder(Modifier.fillMaxSize())

        val remaining = (setup.durationSeconds - seconds).toInt().coerceAtLeast(0)
        Column(
            Modifier.align(Alignment.TopCenter).safeTopPadding().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Film left: $shotsLeft   ·   ${remaining}s", color = Color.White)
            lastScore?.let {
                Spacer(Modifier.height(4.dp))
                Text(describeShot(it), color = Amber)
            }
        }

        Button(
            onClick = {
                val score = shotScore(
                    offAimDegrees = turnDegrees(aim, creatureBearing),
                    distanceMeters = spot.distanceMeters,
                    visibility = spot.visibility,
                )
                lastScore = score
                photos += Photo(
                    score = score,
                    headingDegrees = aim,
                    creatureBearing = creatureBearing,
                    distanceMeters = spot.distanceMeters,
                    visibility = spot.visibility,
                    movingRight = spot.movingRight,
                    stride = gait,
                    sceneSeconds = seconds,
                    timeOfDay = sky.timeOfDay,
                    weather = sky.weather,
                    frameWidthDp = frame.width.value,
                    frameHeightDp = frame.height.value,
                    takenAtMillis = System.currentTimeMillis(),
                )
                shotsLeft -= 1
                if (shotsLeft == 0) finished = true
            },
            enabled = shotsLeft > 0 && !finished,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeBottomPadding()
                .padding(bottom = 28.dp)
                .size(76.dp),
        ) {}
    }
}

private fun describeShot(score: Int) = when {
    score >= 75 -> "That's the one. ($score)"
    score >= 35 -> "Usable. Blurry, but usable. ($score)"
    score > 0 -> "Mostly trees. ($score)"
    else -> "Nothing on film."
}

/**
 * How many degrees of the panorama the viewfinder spans: 60% of a portrait screen's width. It sits
 * dead in the middle of the screen. It's measured in degrees rather than in screen widths, so a
 * shot is just as hard to frame whichever way up the phone is.
 */
const val VIEWFINDER_DEGREES = 42f

/** The frame in the middle of the screen. A shot scores for how close the creature is to its centre. */
@Composable
private fun Viewfinder(modifier: Modifier) {
    Canvas(modifier) {
        val side = VIEWFINDER_DEGREES * scenePxPerDegree(size.width, size.height)
        val topLeft = Offset(center.x - side / 2, size.height * 0.5f - side / 2)
        drawRect(
            color = Color.White.copy(alpha = 0.7f),
            topLeft = topLeft,
            size = Size(side, side),
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

/**
 * The art for each creature, so the one in the viewfinder is the one the case was about. It's
 * always the real creature, even after a wrong guess: that's what was out there. Walkers, fliers
 * and the Dark Watchers have a cycle of frames from tools/cryptid_frames.py. The swimmers have a
 * single silhouette that the scene bends into motion. Creatures only share art when they're
 * regional versions of the same kind of thing, like one lake serpent and another.
 */
fun creatureFrames(creatureId: String): List<Int> = when (creatureId) {
    "bigfoot" -> CreatureFrames.bigfoot
    "mothman" -> CreatureFrames.mothman
    "jersey-devil" -> CreatureFrames.jerseyDevil
    "flathead-lake-monster" -> listOf(R.drawable.cryptid_flathead_lake_monster)
    "ringdocus" -> CreatureFrames.ringdocus
    "prairie-giant" -> CreatureFrames.prairieGiant
    "blacktail-monster" -> listOf(R.drawable.cryptid_blacktail_monster)
    "thunderbird" -> CreatureFrames.thunderbird
    // Washington's sea serpent and California's lake monster are the same kind of creature as
    // Montana's: Caddy has the Flathead monster's humps and long neck, and Tessie is the
    // Blacktail's dark back under the water.
    "cadborosaurus" -> listOf(R.drawable.cryptid_flathead_lake_monster)
    "tahoe-tessie" -> listOf(R.drawable.cryptid_blacktail_monster)
    "batsquatch" -> CreatureFrames.batsquatch
    "lake-chelan-monster" -> CreatureFrames.lakeChelan
    "gumberoo" -> CreatureFrames.gumberoo
    "dark-watchers" -> CreatureFrames.darkWatchers
    "fresno-nightcrawler" -> CreatureFrames.nightcrawler
    "billiwhack-monster" -> CreatureFrames.billiwhack
    "jackalope" -> CreatureFrames.jackalope
    "wimpus" -> CreatureFrames.wimpus
    // Smetty is a lake serpent like the Flathead monster and Caddy.
    "smetty" -> listOf(R.drawable.cryptid_flathead_lake_monster)
    "chupacabra" -> CreatureFrames.chupacabra
    "red-ghost" -> CreatureFrames.redGhost
    "winged-monster" -> CreatureFrames.wingedMonster
    // The same kinds of creature as Bigfoot and the Billiwhack: the Southwest's own Bigfoot, and
    // Fort Worth's goatman.
    "mogollon-monster" -> CreatureFrames.bigfoot
    "lake-worth-monster" -> CreatureFrames.billiwhack
    // Creatures that range over a whole region. Goatman is the same kind of creature as the
    // Billiwhack, Champ, Bessie, the Bear Lake Monster and the Atlantic sea serpent are lake and sea
    // serpents like the Flathead monster, and the rest are already in other states' guides.
    "goatman" -> CreatureFrames.billiwhack
    "champ", "south-bay-bessie", "bear-lake-monster", "sea-serpent" -> listOf(R.drawable.cryptid_flathead_lake_monster)
    "black-panther" -> CreatureFrames.blackPanther
    "dogman" -> CreatureFrames.dogman
    "hoop-snake" -> CreatureFrames.hoopSnake
    "hidebehind" -> CreatureFrames.hidebehind
    "tommyknocker" -> CreatureFrames.tommyknocker
    // Alaska's and Hawaii's. Illie is a dark back under the water like Tessie and the Blacktail
    // monster, Alaska's Caddy and the Pacific serpent are Flathead's kind, and Hawaii's big cat is
    // the same kind of creature as the black panther.
    "iliamna-lake-monster" -> listOf(R.drawable.cryptid_blacktail_monster)
    "pacific-sea-serpent" -> listOf(R.drawable.cryptid_flathead_lake_monster)
    "island-big-cat" -> CreatureFrames.blackPanther
    "caiman" -> CreatureFrames.caiman
    "kraken" -> CreatureFrames.kraken
    "menehune" -> CreatureFrames.menehune
    else -> CreatureFrames.bigfoot
}

/** [creatureId] out in the scene, in its own art and with its own way of moving. */
@Composable
fun sceneCreature(
    creatureId: String,
    bearing: Double,
    distance: Double,
    visibility: Double,
    movingRight: Boolean,
    stride: Double,
    glow: Float = 0f,
): SceneCreature {
    val profile = creatureProfile(creatureId)
    return SceneCreature(
        frames = creatureFrames(creatureId).map { painterResource(it) },
        motion = profile.motion,
        heightMeters = profile.heightMeters,
        bearing = bearing,
        distance = distance.toFloat(),
        visibility = visibility.toFloat(),
        movingRight = movingRight,
        stride = stride.toFloat(),
        fill = profile.fill,
        footprint = profile.footprint,
        glow = glow,
    )
}

/**
 * A single picture of each creature for the journal: walkers in mid-stride, and fliers with their
 * wings level on the way up, spread their widest.
 */
@DrawableRes
fun creatureArt(creatureId: String): Int {
    val frames = creatureFrames(creatureId)
    val walks = creatureProfile(creatureId).motion == CreatureMotion.WALKS
    return if (walks) frames[frames.size / 4 % frames.size] else frames.first()
}

/**
 * How a creature moves, and how tall it stands, in metres, from the ground or waterline. A walker
 * covers [strideMeters] with each full stride of its walk cycle, both feet. A flier's or swimmer's
 * [pace] sets how quickly its wings beat or its body rolls: big wings beat slowly. [fill] and
 * [footprint] fit the scene to its art: see [SceneCreature]. A small creature comes [nearness] times
 * as close as the others, so it isn't a speck in the viewfinder.
 */
private class CreatureProfile(
    val motion: CreatureMotion,
    val heightMeters: Float,
    val strideMeters: Double = 1.0,
    val pace: Double = 1.0,
    val fill: Float = ART_FILL,
    val footprint: Float = 0.4f,
    val nearness: Double = 1.0,
) {
    /** Something that stands still keeps watch from posts, rather than roaming. */
    val path: EncounterPath get() = if (motion == CreatureMotion.STANDS) EncounterPath.WATCHES else EncounterPath.ROAMS

    /** Where it is [seconds] in, on its own path and at its own range: see [nearness]. */
    fun spot(setup: EncounterSetup, seconds: Double, wanderDegrees: Double): CreatureSpot =
        creatureSpot(setup, seconds, wanderDegrees, path).let { it.copy(distanceMeters = it.distanceMeters * nearness) }
}

/** Wingbeats or rolls of the body for each unit of [app.unconfirmed.shared.CreatureSpot.stride]. */
private val CreatureMotion.beatsPerStride: Double get() = when (this) {
    CreatureMotion.WALKS -> 0.0
    CreatureMotion.FLIES -> 3.0 / (2 * PI)
    CreatureMotion.SWIMS -> 1.2 / (2 * PI)
    // A creature that stands still is timed by the clock: its loop runs once every four seconds.
    CreatureMotion.STANDS -> 0.25
}

private fun creatureProfile(creatureId: String): CreatureProfile = when (creatureId) {
    // Stride lengths match the walk cycles' art, so the feet stay put on the ground.
    // The Ringdocus is long and low, so its art fills only half its box from top to bottom.
    "bigfoot", "mogollon-monster" -> CreatureProfile(CreatureMotion.WALKS, 2.4f, strideMeters = 1.9, footprint = 0.2f)
    "ringdocus" -> CreatureProfile(CreatureMotion.WALKS, 1.2f, strideMeters = 1.2, fill = 0.5f, footprint = 0.38f)
    "prairie-giant" -> CreatureProfile(CreatureMotion.WALKS, 3.2f, strideMeters = 1.9, footprint = 0.12f)
    "mothman" -> CreatureProfile(CreatureMotion.FLIES, 2.0f, pace = 0.8, footprint = 0.3f)
    "jersey-devil" -> CreatureProfile(CreatureMotion.FLIES, 1.5f, pace = 1.2, footprint = 0.28f)
    "thunderbird" -> CreatureProfile(CreatureMotion.FLIES, 2.2f, pace = 0.65, footprint = 0.35f)
    "flathead-lake-monster" -> CreatureProfile(CreatureMotion.SWIMS, 2.2f)
    "blacktail-monster" -> CreatureProfile(CreatureMotion.SWIMS, 1.2f)
    "cadborosaurus" -> CreatureProfile(CreatureMotion.SWIMS, 2.6f)
    "tahoe-tessie" -> CreatureProfile(CreatureMotion.SWIMS, 1.6f)
    // Measured from the art: the Gumberoo's box is mostly empty above its back, the Nightcrawlers
    // are a pair and only the nearer one sets the height, and the Lake Chelan dragon is long and
    // very low.
    "gumberoo" -> CreatureProfile(CreatureMotion.WALKS, 1.7f, strideMeters = 0.84, fill = 0.73f, footprint = 0.38f)
    "billiwhack-monster", "lake-worth-monster" -> CreatureProfile(
        CreatureMotion.WALKS,
        2.3f,
        strideMeters = 1.24,
        fill = 0.93f,
        footprint = 0.2f,
    )
    "fresno-nightcrawler" -> CreatureProfile(
        CreatureMotion.WALKS,
        1.1f,
        strideMeters = 1.07,
        fill = 0.56f,
        footprint = 0.25f,
    )
    "lake-chelan-monster" -> CreatureProfile(
        CreatureMotion.WALKS,
        1.8f,
        strideMeters = 1.13,
        fill = 0.39f,
        footprint = 0.4f,
    )
    "batsquatch" -> CreatureProfile(CreatureMotion.FLIES, 2.7f, pace = 0.7, footprint = 0.3f)
    "dark-watchers" -> CreatureProfile(CreatureMotion.STANDS, 2.3f, fill = 0.89f, footprint = 0.18f)
    "jackalope" -> CreatureProfile(
        CreatureMotion.WALKS,
        0.6f,
        strideMeters = 0.7,
        fill = 0.51f,
        footprint = 0.25f,
        nearness = 0.35,
    )
    "smetty" -> CreatureProfile(CreatureMotion.SWIMS, 2.4f)
    "chupacabra" -> CreatureProfile(
        CreatureMotion.WALKS,
        0.9f,
        strideMeters = 0.74,
        fill = 0.55f,
        footprint = 0.4f,
        nearness = 0.6,
    )
    "red-ghost" -> CreatureProfile(CreatureMotion.WALKS, 2.6f, strideMeters = 1.6, fill = 0.78f, footprint = 0.32f)
    "winged-monster" -> CreatureProfile(CreatureMotion.FLIES, 2.0f, pace = 0.6, footprint = 0.4f)
    "goatman" -> CreatureProfile(CreatureMotion.WALKS, 2.3f, strideMeters = 1.24, fill = 0.93f, footprint = 0.2f)
    "champ", "south-bay-bessie", "sea-serpent" -> CreatureProfile(CreatureMotion.SWIMS, 2.4f)
    "bear-lake-monster" -> CreatureProfile(CreatureMotion.SWIMS, 2.0f)
    // Measured from the art, like the others. The hoop snake's stride is one turn of the hoop.
    "black-panther" -> CreatureProfile(
        CreatureMotion.WALKS,
        0.8f,
        strideMeters = 0.58,
        fill = 0.45f,
        footprint = 0.4f,
        nearness = 0.6,
    )
    "dogman" -> CreatureProfile(CreatureMotion.WALKS, 2.2f, strideMeters = 1.73, fill = 0.92f, footprint = 0.2f)
    "hoop-snake" -> CreatureProfile(
        CreatureMotion.WALKS,
        1.0f,
        strideMeters = 2.99,
        fill = 0.62f,
        footprint = 0.3f,
        nearness = 0.6,
    )
    // The Hidebehind's box is mostly the dead snag it hides behind.
    "hidebehind" -> CreatureProfile(CreatureMotion.STANDS, 4.0f, fill = 0.94f, footprint = 0.16f)
    "iliamna-lake-monster" -> CreatureProfile(CreatureMotion.SWIMS, 2.0f)
    "pacific-sea-serpent" -> CreatureProfile(CreatureMotion.SWIMS, 2.6f)
    "island-big-cat" -> CreatureProfile(
        CreatureMotion.WALKS,
        0.8f,
        strideMeters = 0.58,
        fill = 0.45f,
        footprint = 0.4f,
        nearness = 0.6,
    )
    // The caiman is long and very low, so it comes in closer than most.
    "caiman" -> CreatureProfile(
        CreatureMotion.WALKS,
        0.36f,
        strideMeters = 0.47,
        fill = 0.17f,
        footprint = 0.48f,
        nearness = 0.45,
    )
    // The kraken rises from the water at its posts, like the Dark Watchers on their ridges.
    "kraken" -> CreatureProfile(CreatureMotion.STANDS, 4.0f, fill = 0.81f, footprint = 0.4f)
    // A pair of small figures: the nearer one sets the height.
    "menehune" -> CreatureProfile(
        CreatureMotion.WALKS,
        0.9f,
        strideMeters = 0.52,
        fill = 0.48f,
        footprint = 0.3f,
        nearness = 0.5,
    )
    "tommyknocker" -> CreatureProfile(
        CreatureMotion.WALKS,
        0.9f,
        strideMeters = 0.51,
        fill = 0.52f,
        footprint = 0.15f,
        nearness = 0.5,
    )
    // The Wimpus stays in the water: its crawl plays as a paddle, riding low with the gills up.
    "wimpus" -> CreatureProfile(CreatureMotion.SWIMS, 0.3f, pace = 2.0, fill = 0.29f, nearness = 0.25)
    else -> CreatureProfile(CreatureMotion.WALKS, 2.0f, strideMeters = 1.6)
}
