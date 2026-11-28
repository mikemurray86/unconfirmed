package app.unconfirmed.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.unconfirmed.game.BuildConfig
import app.unconfirmed.game.GameState
import app.unconfirmed.game.LocalSound
import app.unconfirmed.shared.ActiveCase
import app.unconfirmed.shared.CaseDefinition
import app.unconfirmed.shared.CasePhase
import app.unconfirmed.shared.Clue
import app.unconfirmed.shared.ClueType
import app.unconfirmed.shared.Lure
import app.unconfirmed.shared.MAX_PAUSED_CASES
import app.unconfirmed.shared.SIGHTING_SECONDS
import app.unconfirmed.shared.TimeOfDay
import app.unconfirmed.shared.Weather
import app.unconfirmed.shared.canPause
import app.unconfirmed.shared.compassPoint
import app.unconfirmed.shared.foundClues
import app.unconfirmed.shared.guessesLeft
import app.unconfirmed.shared.nextHint
import app.unconfirmed.shared.traitOf
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val FEET_PER_METER = 3.28084
private const val METERS_PER_MILE = 1609.344

/** A distance in feet, to the nearest ten, up to a tenth of a mile, then in miles. */
fun formatDistance(meters: Double): String = if (meters < METERS_PER_MILE / 10) {
    "${(meters * FEET_PER_METER / 10).toInt() * 10} ft"
} else {
    "%.1f mi".format(Locale.US, meters / METERS_PER_MILE)
}

@Composable
fun InvestigationScreen(
    state: GameState,
    case: ActiveCase,
    definition: CaseDefinition,
    onStart: () -> Unit,
    onReturn: () -> Unit,
    onPause: () -> Unit,
    onFindShown: () -> Unit,
    onDeduce: () -> Unit,
    onDebugWalk: () -> Unit,
    onDebugSighting: () -> Unit,
    onEngage: () -> Unit,
    onMissSighting: () -> Unit,
    onPickUpBait: () -> Unit,
    onUseBait: (Lure) -> Unit,
    onDebugBait: () -> Unit,
    onScoutingChange: (Boolean) -> Unit,
    onScout: (Double) -> Unit,
) {
    // The number (from 1) of the clue on show, if any.
    var openClue by rememberSaveable { mutableStateOf<Int?>(null) }
    var justFound by rememberSaveable { mutableStateOf(false) }
    var confirmingDrop by rememberSaveable { mutableStateOf(false) }
    var choosingBait by rememberSaveable { mutableStateOf(false) }
    // A short line over the scene after picking up or setting out bait. It says what the player
    // did, never whether it worked.
    var notice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(notice) {
        if (notice != null) {
            delay(NOTICE_MILLIS)
            notice = null
        }
    }
    // Debug builds can step through every time of day and weather from the readout.
    var debugTime by rememberSaveable { mutableStateOf<TimeOfDay?>(null) }
    var debugWeather by rememberSaveable { mutableStateOf<Weather?>(null) }
    val sky = rememberSky(state.fix?.point, debugTime, debugWeather)
    val compass = rememberHeading(fallbackDegrees = state.fix?.bearingDegrees)
    val scouting = state.scouting
    // While scouting the stick turns the player, starting from wherever the compass last pointed.
    var scoutHeading by rememberSaveable { mutableStateOf<Double?>(null) }
    var stick by remember { mutableStateOf(Offset.Zero) }
    val heading = if (scouting) scoutHeading ?: compass ?: 0.0 else compass
    // How far the player has gone while scouting, for the scene to follow smoothly. The case is
    // only told every couple of metres, so it isn't saved on every frame.
    var scoutedMeters by remember(scouting) { mutableDoubleStateOf(case.walkedMeters) }
    val hint = if (case.phase == CasePhase.INVESTIGATING) nextHint(case) else null
    val haptics = LocalHapticFeedback.current
    val sound = LocalSound.current

    val investigating = case.phase == CasePhase.INVESTIGATING
    val latestHeading by rememberUpdatedState(heading ?: 0.0)
    val latestStick by rememberUpdatedState(stick)
    val latestScout by rememberUpdatedState(onScout)
    LaunchedEffect(scouting, investigating) {
        if (!scouting || !investigating) return@LaunchedEffect
        var unsent = 0.0
        var last = withFrameNanos { it }
        try {
            while (isActive) {
                val now = withFrameNanos { it }
                val seconds = ((now - last) / 1e9).coerceAtMost(0.1)
                last = now
                val (turn, walk) = stickMotion(latestStick)
                if (turn != 0.0) scoutHeading = ((scoutHeading ?: latestHeading) + turn * seconds + 360.0) % 360.0
                if (walk > 0.0) {
                    val meters = walk * seconds
                    scoutedMeters += meters
                    unsent += meters
                }
                if (unsent >= SCOUT_REPORT_METERS || (unsent > 0.0 && latestStick == Offset.Zero)) {
                    latestScout(unsent)
                    unsent = 0.0
                }
            }
        } finally {
            if (unsent > 0.0) latestScout(unsent)
        }
    }

    // The creature showing itself: where it stands, and how far through its few seconds it is.
    // It waits while a clue is open, so it's never hidden behind one.
    val glimpse = rememberSighting(
        showing = state.sighting && investigating && openClue == null,
        heading = heading ?: 0.0,
        call = definition.answer.traits[ClueType.AUDIO] ?: "silent",
        onMissed = onMissSighting,
    )
    val hit = remember { CreatureHit() }
    // Bait turns up a little to one side of the way the player is facing, a few steps ahead.
    val groundBait = state.groundBait.takeIf { investigating }
    val baitBearing = remember(groundBait) { (heading ?: 0.0) + kotlin.random.Random.nextDouble(-15.0, 15.0) }
    val tapReach = with(LocalDensity.current) { SIGHTING_TAP_SLOP.toPx() }
    val latestEngage by rememberUpdatedState(onEngage)

    // Clues turn up on their own while walking, so each one interrupts like an alert.
    LaunchedEffect(state.foundClueNumber) {
        val found = state.foundClueNumber ?: return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        sound?.chime()
        openClue = found
        justFound = true
        onFindShown()
    }

    // On a wide screen the case file is a panel down the side, so the scene keeps the whole
    // height and the extra width shows more of the view around the player.
    val side = sideBySide()
    val panel: @Composable (Modifier) -> Unit = { modifier ->
        CasePanel(
            state = state,
            case = case,
            definition = definition,
            onReread = {
                openClue = it
                justFound = false
            },
            onDeduce = onDeduce,
            onOpenBait = { choosingBait = true },
            onPause = onPause,
            onDrop = { confirmingDrop = true },
            side = side,
            modifier = modifier,
        )
    }
    val layout: @Composable (@Composable (Modifier) -> Unit) -> Unit = { scene ->
        if (side) {
            Row(Modifier.fillMaxSize()) {
                scene(Modifier.weight(1f).fillMaxHeight())
                panel(Modifier.width(CASE_PANEL_WIDTH).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                scene(Modifier.weight(1f).fillMaxWidth())
                panel(Modifier.fillMaxWidth())
            }
        }
    }
    layout { sceneModifier ->
        Box(sceneModifier) {
            FirstPersonScene(
                scene = definition.scene,
                region = case.region ?: definition.region,
                sky = sky,
                headingDegrees = heading,
                walkedMeters = if (scouting) scoutedMeters else case.walkedMeters,
                hintBearing = hint,
                steadyStride = scouting,
                creature = glimpse?.let {
                    sceneCreature(
                        definition.answerId,
                        bearing = it.bearing,
                        distance = it.distance,
                        visibility = it.visibility,
                        movingRight = it.movingRight,
                        stride = it.stride,
                        glow = 1f,
                    )
                },
                hit = hit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(glimpse != null) {
                        if (glimpse == null) return@pointerInput
                        detectTapGestures { tap ->
                            if (hit.bounds?.inflate(tapReach)?.contains(tap) == true) latestEngage()
                        }
                    },
            )
            AtmosphereOverlay(biome = definition.scene.biome, sky = sky, modifier = Modifier.fillMaxSize())
            groundBait?.let { lure ->
                BaitOnGround(
                    lure = lure,
                    heading = heading ?: 0.0,
                    bearing = baitBearing,
                    onPickUp = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPickUpBait()
                        notice = "You pick up the ${lure.title.lowercase()}."
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            notice?.let {
                FieldNotice(it, Modifier.align(Alignment.Center))
            }
            Column(
                Modifier.align(Alignment.TopStart).safeTopPadding().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FieldReadout(
                    point = state.fix?.point,
                    biome = definition.scene.biome,
                    sky = sky,
                    onDebugTap = if (BuildConfig.DEBUG) {
                        { debugWeather = Weather.entries[((debugWeather?.ordinal ?: -1) + 1) % Weather.entries.size] }
                    } else {
                        null
                    },
                    onDebugLongPress = if (BuildConfig.DEBUG) {
                        { debugTime = TimeOfDay.entries[((debugTime?.ordinal ?: -1) + 1) % TimeOfDay.entries.size] }
                    } else {
                        null
                    },
                )
            }
            Column(
                Modifier.align(Alignment.TopEnd).safeTopPadding().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (BuildConfig.DEBUG && state.debugWalkButton && case.phase == CasePhase.INVESTIGATING) {
                    SmallFloatingActionButton(
                        onClick = onDebugWalk,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) { Text("+25m", fontFamily = FieldFont) }
                    SmallFloatingActionButton(
                        onClick = onDebugSighting,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) { Text("Spot", fontFamily = FieldFont) }
                    SmallFloatingActionButton(
                        onClick = onDebugBait,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) { Text("Bait", fontFamily = FieldFont) }
                }
            }
            val bottomInset = if (side) Modifier.safeBottomPadding() else Modifier
            if (heading != null || scouting) {
                CompassStick(
                    headingDegrees = heading,
                    scouting = scouting && investigating,
                    stick = stick,
                    onStick = { stick = it },
                    modifier = Modifier.align(Alignment.BottomStart).then(bottomInset).padding(12.dp),
                )
            }
            if (investigating) {
                MoveModeSwitch(
                    scouting = scouting,
                    onChange = { on ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        stick = Offset.Zero
                        if (on) scoutHeading = compass
                        onScoutingChange(on)
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).then(bottomInset).padding(12.dp),
                )
            }
        }
    }

    if (confirmingDrop) {
        AlertDialog(
            onDismissRequest = { confirmingDrop = false },
            title = { Text("Drop this case?", fontFamily = FontFamily.Serif) },
            text = { Text("You'll lose the clues you've found and go back to the list of cases.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDrop = false
                    onReturn()
                }) { Text("Drop it") }
            },
            dismissButton = { TextButton(onClick = { confirmingDrop = false }) { Text("Keep going") } },
        )
    }

    if (choosingBait && investigating && case.bait.isNotEmpty()) {
        BaitDialog(
            carried = case.bait,
            onUse = { lure ->
                choosingBait = false
                onUseBait(lure)
                notice = lure.used
            },
            onDismiss = { choosingBait = false },
        )
    }

    if (case.phase == CasePhase.LEAD) LeadDialog(definition, case.title(definition), onStart, onReturn)

    openClue?.let { number ->
        foundClues(case, definition).getOrNull(number - 1)?.let { clue ->
            ClueDialog(clue, definition.traitOf(clue), seed = (definition.id + clue.id).hashCode(), justFound) {
                openClue = null
                justFound = false
            }
        }
    }
}

@Composable
private fun LeadDialog(definition: CaseDefinition, headline: String, onStart: () -> Unit, onReturn: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("New lead: $headline", fontFamily = FontFamily.Serif) },
        text = {
            Text(
                definition.lead +
                    "\n\nWalk and keep your eyes open. Clues turn up as you go, in whatever " +
                    "direction you choose. If you spot tracks, they hint at a way to try, but any " +
                    "way works. Once you have a clue, keep your eyes and ears open: if the " +
                    "creature shows itself, tap it to raise your camera.\n\n" +
                    "Stay on paths and sidewalks you're allowed to be on.\n\n" +
                    "Weather keeping you in? Switch to $SCOUTING_LABEL and use the stick in the " +
                    "corner to search from where you are.",
            )
        },
        confirmButton = { Button(onClick = onStart) { Text("Take the case") } },
        dismissButton = { TextButton(onClick = onReturn) { Text("Pick another") } },
    )
}

@Composable
private fun ClueDialog(clue: Clue, trait: String?, seed: Int, justFound: Boolean, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(clue.type.title, fontFamily = FontFamily.Serif) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    if (justFound) "Something turned up while you walked" else "From your case file",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(12.dp))
                ClueArt(clue.type, trait, seed, Modifier.padding(end = 4.dp, bottom = 4.dp))
                Spacer(Modifier.height(14.dp))
                Text(clue.text)
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Add to case file") } },
    )
}

@Composable
private fun CasePanel(
    state: GameState,
    case: ActiveCase,
    definition: CaseDefinition,
    onReread: (Int) -> Unit,
    onDeduce: () -> Unit,
    onOpenBait: () -> Unit,
    onPause: () -> Unit,
    onDrop: () -> Unit,
    /** True when the panel runs down the side of the screen rather than across the bottom. */
    side: Boolean,
    modifier: Modifier = Modifier,
) {
    val clues = case.clues(definition)
    val found = foundClues(case, definition)

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.caseFileEdge(side)) {
        Column(
            Modifier
                .caseFileLines()
                .then(
                    if (side) {
                        Modifier.windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End),
                        )
                    } else {
                        Modifier.safeBottomPadding().heightIn(max = 340.dp)
                    },
                )
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "CASE FILE",
                fontFamily = FieldFont,
                style = MaterialTheme.typography.labelMedium,
                color = Amber.copy(alpha = 0.8f),
            )
            Text(
                case.title(definition),
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FieldFont,
                modifier = Modifier.offset(y = (-6).dp),
            )
            StatusLine(state, case)

            clues.indices.forEach { index ->
                ClueRow(
                    number = index + 1,
                    clue = found.getOrNull(index),
                    searching = index == found.size,
                    onReread = { onReread(index + 1) },
                )
            }
            TornEdge()

            if (found.isNotEmpty() && case.phase == CasePhase.INVESTIGATING) {
                val left = guessesLeft(case)
                OutlinedButton(onClick = onDeduce, enabled = left > 0, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        when {
                            left > 0 -> "Make a guess ($left left)"
                            case.allCluesFound(definition) -> "No guesses left"
                            else -> "Find another clue to guess again"
                        },
                    )
                }
            }
            if (case.phase == CasePhase.INVESTIGATING && case.bait.isNotEmpty()) {
                OutlinedButton(onClick = onOpenBait, modifier = Modifier.fillMaxWidth()) {
                    Text("Use bait (${case.bait.size})")
                }
            }
            if (case.phase == CasePhase.INVESTIGATING) {
                if (canPause(case, state.pausedCases)) {
                    OutlinedButton(onClick = onPause, modifier = Modifier.fillMaxWidth()) {
                        Text("Pause and return to the papers")
                    }
                } else {
                    Text(
                        "You have $MAX_PAUSED_CASES cases paused already. Finish or drop one to pause this one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(onClick = onDrop, modifier = Modifier.fillMaxWidth()) {
                Text("Drop this case")
            }
        }
    }
}

@Composable
private fun StatusLine(state: GameState, case: ActiveCase) {
    val fix = state.fix
    val hint = nextHint(case)
    val (message, color) = when {
        state.scouting && case.phase == CasePhase.INVESTIGATING && hint != null ->
            "$SCOUTING_LABEL · Tracks seem to lead ${compassPoint(hint)}, but any direction will do · " +
                "${formatDistance(case.walkedMeters)} covered" to Amber
        state.scouting -> "$SCOUTING_LABEL · ${formatDistance(case.walkedMeters)} covered" to
            MaterialTheme.colorScheme.onSurfaceVariant
        state.tooFast ->
            "Moving too fast. If you're in a vehicle, this can wait until you're on foot." to
                MaterialTheme.colorScheme.error
        fix == null -> "Finding your position..." to MaterialTheme.colorScheme.onSurfaceVariant
        fix.accuracyMeters > 40 ->
            "Weak GPS signal (within ${(fix.accuracyMeters * FEET_PER_METER).toInt()} ft)." to
                MaterialTheme.colorScheme.error
        case.phase == CasePhase.INVESTIGATING && hint != null ->
            "Tracks seem to lead ${compassPoint(hint)}, but any direction will do · " +
                "${formatDistance(case.walkedMeters)} walked" to Amber
        else -> "${formatDistance(case.walkedMeters)} walked" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(message, style = MaterialTheme.typography.bodyMedium, color = color)
}

/** One line per clue: read it again once found, or a note that the next one is still out there. */
@Composable
private fun ClueRow(number: Int, clue: Clue?, searching: Boolean, onReread: () -> Unit) {
    val rowModifier = if (clue != null) Modifier.clickable(onClick = onReread) else Modifier
    Row(
        modifier = rowModifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (clue != null) "✓" else "$number",
            fontWeight = FontWeight.Bold,
            color = if (clue != null) Moss else Amber,
            modifier = Modifier.width(24.dp),
        )
        Text(
            when {
                clue != null -> clue.type.title
                searching -> "Keep walking..."
                else -> "Still out there"
            },
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        if (clue != null) {
            Text(
                "Read again",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** How long a notice stays over the scene. */
private const val NOTICE_MILLIS = 3_500L

/** How far outside the creature a tap still counts, since it's small and only there a moment. */
private val SIGHTING_TAP_SLOP = 32.dp

/** How long the creature takes to fade in, and to fade away again. */
private const val SIGHTING_FADE_SECONDS = 0.6

/** How clear it gets during a sighting: it's a glimpse, not a portrait. */
private const val SIGHTING_CLARITY = 0.85

/** How quickly it drifts across the view, in degrees a second, and how its gait keeps time. */
private const val SIGHTING_DRIFT = 2.0
private const val SIGHTING_STRIDES_PER_SECOND = 0.6

/** Where the creature is during a sighting, and how clearly it can be seen. */
private class Glimpse(
    val bearing: Double,
    val distance: Double,
    val movingRight: Boolean,
    val visibility: Double,
    val stride: Double,
)

/**
 * Runs a sighting while [showing]: the creature's [call] sounds, the phone buzzes, and it fades in
 * a little to one side of [heading], drifts across, and fades away after [SIGHTING_SECONDS]. Then
 * [onMissed]. Null while there's nothing to see.
 */
@Composable
private fun rememberSighting(showing: Boolean, heading: Double, call: String, onMissed: () -> Unit): Glimpse? {
    val haptics = LocalHapticFeedback.current
    val sound = LocalSound.current
    val missed by rememberUpdatedState(onMissed)
    val facing by rememberUpdatedState(heading)
    var glimpse by remember { mutableStateOf<Glimpse?>(null) }
    LaunchedEffect(showing) {
        if (!showing) {
            glimpse = null
            return@LaunchedEffect
        }
        val random = kotlin.random.Random
        val right = random.nextBoolean()
        // It turns up off to one side, never dead ahead, and wanders towards the middle.
        val start = facing + (if (right) -1 else 1) * (8.0 + random.nextDouble() * 14.0)
        val distance = 14.0 + random.nextDouble() * 8.0
        sound?.call(call)
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        val begin = withFrameNanos { it }
        while (isActive) {
            val t = (withFrameNanos { it } - begin) / 1e9
            if (t >= SIGHTING_SECONDS) break
            val fade = minOf(t, SIGHTING_SECONDS - t) / SIGHTING_FADE_SECONDS
            glimpse = Glimpse(
                bearing = start + (if (right) 1 else -1) * SIGHTING_DRIFT * t,
                distance = distance,
                movingRight = right,
                visibility = SIGHTING_CLARITY * fade.coerceIn(0.0, 1.0),
                stride = t * SIGHTING_STRIDES_PER_SECOND,
            )
        }
        glimpse = null
        missed()
    }
    return glimpse
}

/** The fastest the stick walks, a little under walking pace so going out on foot pays more. */
private const val SCOUT_MAX_SPEED = 1.2 // Metres per second.

/** How fast the stick turns the player when pushed all the way to one side. */
private const val SCOUT_TURN_SPEED = 90.0 // Degrees per second.

/** Below this much push, the stick is taken as centred, so a thumb resting on it doesn't drift. */
private const val STICK_DEAD_ZONE = 0.15f

/** How much scouting builds up before it's counted in the case. */
private const val SCOUT_REPORT_METERS = 2.0

/**
 * What the stick at [stick] does: a turn in degrees a second, and walking forward in metres a
 * second. Up walks, sideways turns, and a diagonal does both. Pulling back only stops.
 */
private fun stickMotion(stick: Offset): Pair<Double, Double> {
    fun live(value: Float): Double = if (abs(value) < STICK_DEAD_ZONE) {
        0.0
    } else {
        (abs(value) - STICK_DEAD_ZONE) / (1 - STICK_DEAD_ZONE) * sign(value).toDouble()
    }
    return live(stick.x) * SCOUT_TURN_SPEED to (-live(stick.y)).coerceAtLeast(0.0) * SCOUT_MAX_SPEED
}

/** How wide the case file is when it runs down the side of a wide screen. */
private val CASE_PANEL_WIDTH = 360.dp

/**
 * A folder tab and a thin amber edge along the top, so the panel reads as a case file. When it
 * runs down the [side] of the screen, the edge and the tab are along its left side instead.
 */
private fun Modifier.caseFileEdge(side: Boolean = false): Modifier = drawBehind {
    val amber = Amber.copy(alpha = 0.7f)
    val start = 16.dp.toPx()
    val length = 96.dp.toPx()
    val depth = 6.dp.toPx()
    val tab = Path()
    if (side) {
        drawRect(amber, size = Size(2.dp.toPx(), size.height))
        // Below the status bar, where it can be seen.
        val top = start + 48.dp.toPx()
        tab.moveTo(0f, top)
        tab.lineTo(depth, top + depth)
        tab.lineTo(depth, top + length - depth)
        tab.lineTo(0f, top + length)
    } else {
        drawRect(amber, size = Size(size.width, 2.dp.toPx()))
        tab.moveTo(start, 0f)
        tab.lineTo(start + depth, depth)
        tab.lineTo(start + length - depth, depth)
        tab.lineTo(start + length, 0f)
    }
    tab.close()
    drawPath(tab, amber)
}

/** Faint ruled lines behind the notes, like a page in a field notebook. */
private fun Modifier.caseFileLines(): Modifier = drawBehind {
    val gap = 26.dp.toPx()
    var y = gap * 1.6f
    while (y < size.height) {
        drawLine(Signal.copy(alpha = 0.05f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        y += gap
    }
}

/** A divider with a ragged edge, like a page torn out along the fold. */
@Composable
private fun TornEdge() {
    val color = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.fillMaxWidth().height(6.dp)) {
        val step = 6.dp.toPx()
        val path = Path().apply {
            moveTo(0f, size.height / 2)
            var x = 0f
            var up = true
            while (x < size.width) {
                x += step
                lineTo(x, if (up) 0f else size.height)
                up = !up
            }
        }
        drawPath(path, color, style = Stroke(1.dp.toPx()))
    }
}
