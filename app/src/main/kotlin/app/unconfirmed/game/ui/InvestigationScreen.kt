package app.unconfirmed.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import app.unconfirmed.shared.MAX_PAUSED_CASES
import app.unconfirmed.shared.TimeOfDay
import app.unconfirmed.shared.Weather
import app.unconfirmed.shared.canPause
import app.unconfirmed.shared.compassPoint
import app.unconfirmed.shared.foundClues
import app.unconfirmed.shared.nextHint
import app.unconfirmed.shared.traitOf
import java.util.Locale

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
) {
    // The number (from 1) of the clue on show, if any.
    var openClue by rememberSaveable { mutableStateOf<Int?>(null) }
    var justFound by rememberSaveable { mutableStateOf(false) }
    var confirmingDrop by rememberSaveable { mutableStateOf(false) }
    // Debug builds can step through every time of day and weather from the readout.
    var debugTime by rememberSaveable { mutableStateOf<TimeOfDay?>(null) }
    var debugWeather by rememberSaveable { mutableStateOf<Weather?>(null) }
    val sky = rememberSky(state.fix?.point, debugTime, debugWeather)
    val heading = rememberHeading(fallbackDegrees = state.fix?.bearingDegrees)
    val hint = if (case.phase == CasePhase.INVESTIGATING) nextHint(case) else null
    val haptics = LocalHapticFeedback.current
    val sound = LocalSound.current

    // Clues turn up on their own while walking, so each one interrupts like an alert.
    LaunchedEffect(state.foundClueNumber) {
        val found = state.foundClueNumber ?: return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        sound?.chime()
        openClue = found
        justFound = true
        onFindShown()
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            FirstPersonScene(
                scene = definition.scene,
                region = case.region ?: definition.region,
                sky = sky,
                headingDegrees = heading,
                walkedMeters = case.walkedMeters,
                hintBearing = hint,
                modifier = Modifier.fillMaxSize(),
            )
            AtmosphereOverlay(biome = definition.scene.biome, sky = sky, modifier = Modifier.fillMaxSize())
            Column(
                Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp),
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
                Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                heading?.let { HeadingDial(headingDegrees = it) }
                if (BuildConfig.DEBUG && state.debugWalkButton && case.phase == CasePhase.INVESTIGATING) {
                    SmallFloatingActionButton(
                        onClick = onDebugWalk,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) { Text("+25m", fontFamily = FieldFont) }
                }
            }
        }
        CasePanel(
            state = state,
            case = case,
            definition = definition,
            onReread = {
                openClue = it
                justFound = false
            },
            onDeduce = onDeduce,
            onPause = onPause,
            onDrop = { confirmingDrop = true },
        )
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
                    "way works.\n\nStay on paths and sidewalks you're allowed to be on.",
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
    onPause: () -> Unit,
    onDrop: () -> Unit,
) {
    val clues = case.clues(definition)
    val found = foundClues(case, definition)

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().caseFileEdge()) {
        Column(
            Modifier
                .caseFileLines()
                .navigationBarsPadding()
                .heightIn(max = 340.dp)
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

            if (found.isNotEmpty()) {
                if (case.allCluesFound(definition)) {
                    Button(onClick = onDeduce, modifier = Modifier.fillMaxWidth()) {
                        Text("Name the creature")
                    }
                } else {
                    OutlinedButton(onClick = onDeduce, modifier = Modifier.fillMaxWidth()) {
                        Text("Name the creature early")
                    }
                }
            }
            if (case.phase == CasePhase.INVESTIGATING) {
                if (canPause(case, state.pausedCases)) {
                    OutlinedButton(onClick = onPause, modifier = Modifier.fillMaxWidth()) {
                        Text("Pause and pick another case")
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

/** A folder tab and a thin amber edge along the top, so the panel reads as a case file. */
private fun Modifier.caseFileEdge(): Modifier = drawBehind {
    drawRect(Amber.copy(alpha = 0.7f), size = Size(size.width, 2.dp.toPx()))
    val tab = Path().apply {
        val left = 16.dp.toPx()
        val width = 96.dp.toPx()
        val height = 6.dp.toPx()
        moveTo(left, 0f)
        lineTo(left + height, height)
        lineTo(left + width - height, height)
        lineTo(left + width, 0f)
        close()
    }
    drawPath(tab, Amber.copy(alpha = 0.7f))
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
