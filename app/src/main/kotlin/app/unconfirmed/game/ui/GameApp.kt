package app.unconfirmed.game.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.unconfirmed.game.BuildConfig
import app.unconfirmed.game.GameState
import app.unconfirmed.game.GameViewModel
import app.unconfirmed.game.WorldStatus
import app.unconfirmed.game.locationUpdates
import app.unconfirmed.shared.ActiveCase
import app.unconfirmed.shared.CaseDefinition
import app.unconfirmed.shared.CasePhase
import app.unconfirmed.shared.caseById

@Composable
fun GameApp(viewModel: GameViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var access by remember { mutableStateOf(locationAccess(context)) }
    // Set once Android has stopped showing the permission prompt, so the only way on is Settings.
    var promptBlocked by rememberSaveable { mutableStateOf(false) }
    val requestLocation = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        access = locationAccess(context)
        promptBlocked = access != LocationAccess.PRECISE &&
            context.findActivity()?.shouldShowRequestPermissionRationale(FINE_LOCATION) == false
    }
    // The player may have changed the permission in Settings while the game was in the background.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        access = locationAccess(context)
        if (access == LocationAccess.PRECISE) promptBlocked = false
    }
    val hasLocation = access == LocationAccess.PRECISE

    // RESUMED ties GPS to the game being on screen: there's no background location at all.
    LaunchedEffect(hasLocation, lifecycle) {
        if (!hasLocation) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            locationUpdates(context).collect(viewModel::onLocation)
        }
    }

    var showingJournal by rememberSaveable { mutableStateOf(false) }
    val openJournal = { showingJournal = true }
    var showingAbout by rememberSaveable { mutableStateOf(false) }
    var showingRequest by rememberSaveable { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val case = state.activeCase
        val definition = case?.let { caseById(it.caseId) }
        when {
            !state.safetyAccepted -> SafetyScreen(viewModel::acceptSafety)
            showingAbout -> AboutScreen { showingAbout = false }
            showingRequest -> RequestCryptidScreen { showingRequest = false }
            showingJournal -> JournalScreen(state.journal, state.areaState) { showingJournal = false }
            !hasLocation -> LocationNeededScreen(
                access = access,
                promptBlocked = promptBlocked,
                onRequest = { requestLocation.launch(arrayOf(FINE_LOCATION, COARSE_LOCATION)) },
                onOpenSettings = { context.openAppSettings() },
            )
            case != null && definition != null -> when (case.phase) {
                CasePhase.LEAD, CasePhase.INVESTIGATING -> InvestigationRoute(state, case, definition, viewModel)
                CasePhase.ENCOUNTER -> EncounterScreen(
                    creatureId = definition.answerId,
                    correctDeduction = case.deductionId == definition.answerId,
                    scene = definition.scene,
                    region = case.region ?: definition.region,
                    point = state.fix?.point,
                    onFinished = viewModel::finishEncounter,
                )
                CasePhase.DONE -> ResultScreen(case, definition, viewModel::takeAnotherCase, openJournal)
            }
            else -> WorldStatusScreen(
                state = state,
                viewModel = viewModel,
                onOpenJournal = openJournal,
                onOpenAbout = { showingAbout = true },
                onOpenRequest = { showingRequest = true },
            )
        }
    }
}

@Composable
private fun InvestigationRoute(
    state: app.unconfirmed.game.GameState,
    case: ActiveCase,
    definition: CaseDefinition,
    viewModel: GameViewModel,
) {
    var deducing by rememberSaveable { mutableStateOf(false) }
    if (deducing) {
        BackHandler { deducing = false }
        DeductionScreen(
            case = case,
            definition = definition,
            onBack = { deducing = false },
            onDeduce = { suspectId ->
                deducing = false
                viewModel.deduce(suspectId)
            },
        )
    } else {
        InvestigationScreen(
            state = state,
            case = case,
            definition = definition,
            onStart = viewModel::startInvestigating,
            onReturn = viewModel::returnCase,
            onPause = viewModel::pauseCase,
            onFindShown = viewModel::findShown,
            onDeduce = { deducing = true },
            onDebugWalk = { viewModel.debugWalk() },
        )
    }
}

@Composable
private fun WorldStatusScreen(
    state: GameState,
    viewModel: GameViewModel,
    onOpenJournal: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenRequest: () -> Unit,
) {
    when (val status = state.status) {
        WorldStatus.WaitingForLocation -> LoadingScreen("Waiting for a GPS signal. Stepping outside can help.")
        WorldStatus.LocatingState -> LoadingScreen("Checking which reports cover this area...")
        WorldStatus.Ready -> LoadingScreen("Opening the case file...")
        is WorldStatus.ChoosingCase -> CaseSelectionScreen(
            status = status,
            paused = state.pausedCases,
            onChoose = viewModel::chooseCase,
            onResume = viewModel::resumeCase,
            onOpenJournal = onOpenJournal,
            onOpenAbout = onOpenAbout,
            // Requests go to the game's contact, so there's nowhere to send one without it.
            onOpenRequest = if (BuildConfig.GAME_CONTACT.isNotBlank()) onOpenRequest else null,
            onDebugNextState = if (BuildConfig.DEBUG) viewModel::debugNextState else null,
            debugWalkButton = if (BuildConfig.DEBUG) state.debugWalkButton to viewModel::setDebugWalkButton else null,
        )
        WorldStatus.NeedsConnection -> MessageScreen(
            title = "Where are we?",
            body = "Your phone couldn't say which state you're in, so the game can't tell which " +
                "reports are local. This usually needs a data connection. Once it's found your " +
                "state, the game remembers it for next time.",
            action = "Try again" to viewModel::retryState,
            secondary = if (BuildConfig.DEBUG) "Debug: pick a state" to viewModel::debugNextState else null,
        )
    }
}

private const val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
private const val COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION

private enum class LocationAccess { PRECISE, APPROXIMATE, NONE }

private fun locationAccess(context: Context): LocationAccess = when {
    ContextCompat.checkSelfPermission(context, FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ->
        LocationAccess.PRECISE
    ContextCompat.checkSelfPermission(context, COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ->
        LocationAccess.APPROXIMATE
    else -> LocationAccess.NONE
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

/**
 * Approximate location only updates every few minutes and is blurred by up to a few kilometres,
 * which can't measure a walk, so the game asks for precise location and explains why.
 */
@Composable
private fun LocationNeededScreen(
    access: LocationAccess,
    promptBlocked: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val why = "Unconfirmed uses your location while it's open to measure how far you've " +
        "walked, which is what turns up clues, and to find which state you're in, so " +
        "you get local cases. It never tracks you in the background or keeps a record " +
        "of where you've been. Your phone's location service may look up the state " +
        "online, and the weather service sees roughly where you are."
    val approximate = "\n\nYou've allowed approximate location. That's too blurry to measure a walk, so " +
        "the game needs precise location to work."
    val settings = "\n\nYour phone won't ask again, so turn it on in Settings: Permissions, then " +
        "Location, then \"Allow only while using the app\" with \"Use precise location\" on."
    MessageScreen(
        title = if (access == LocationAccess.APPROXIMATE) "Precise location needed" else "Location needed",
        body = why + (if (access == LocationAccess.APPROXIMATE) approximate else "") +
            (if (promptBlocked) settings else ""),
        action = if (promptBlocked) "Open Settings" to onOpenSettings else "Allow location" to onRequest,
    )
}

@Composable
private fun SafetyScreen(onAccept: () -> Unit) {
    MessageScreen(
        title = "Before you go looking",
        body = "This game sends you outside, on foot.\n\n" +
            "• Watch where you're going, not your screen.\n" +
            "• The game never sends you anywhere. Clues turn up wherever you walk.\n" +
            "• Tracks only hint at a direction. Ignore them if that way isn't safe or open.\n" +
            "• Stay on public paths and sidewalks, and respect private property.\n" +
            "• Cases never need you out at night. Go in daylight.\n\n" +
            "Your own judgment comes first.",
        action = "Understood" to onAccept,
    )
}

@Composable
private fun LoadingScreen(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(24.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun MessageScreen(
    title: String,
    body: String,
    action: Pair<String, () -> Unit>? = null,
    secondary: Pair<String, () -> Unit>? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Serif,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge)
        action?.let { (label, onClick) ->
            Spacer(Modifier.height(28.dp))
            Button(onClick = onClick) { Text(label) }
        }
        secondary?.let { (label, onClick) ->
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onClick) { Text(label) }
        }
    }
}
