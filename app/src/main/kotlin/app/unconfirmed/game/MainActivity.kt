package app.unconfirmed.game

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.unconfirmed.game.ui.Amber
import app.unconfirmed.game.ui.ClueArt
import app.unconfirmed.game.ui.EncounterScreen
import app.unconfirmed.game.ui.FieldFont
import app.unconfirmed.game.ui.GameApp
import app.unconfirmed.game.ui.InkButton
import app.unconfirmed.game.ui.PhotoPrint
import app.unconfirmed.game.ui.UnconfirmedTheme
import app.unconfirmed.game.ui.desk
import app.unconfirmed.shared.ALL_CASES
import app.unconfirmed.shared.CLUES_PER_CASE
import app.unconfirmed.shared.Photo
import app.unconfirmed.shared.Region
import app.unconfirmed.shared.Scene
import app.unconfirmed.shared.caseById

/**
 * The game's only activity. A phone keeps it upright: its screen is too small for the newspaper
 * and the case file to work on their side. A tablet or an unfolded foldable turns it either way
 * and resizes it for split screen. None of these recreate it: the manifest's configChanges hands
 * them to Compose, which redraws for the new size and keeps every screen's state, mid-encounter
 * included.
 */
class MainActivity : ComponentActivity() {
    private lateinit var sound: SoundPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockPhonesUpright(resources.configuration)
        sound = SoundPlayer(this)
        // The phone's volume buttons set the game's volume, not the ringer's.
        volumeControlStream = AudioManager.STREAM_MUSIC
        // The game is always dark, so the system icons must stay light whatever the phone's theme.
        val bars = SystemBarStyle.dark(Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        Ads.gatherConsent(this)
        val debugEncounter = intent.getStringExtra(EXTRA_DEBUG_ENCOUNTER).takeIf { BuildConfig.DEBUG }
        val debugClues = BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_DEBUG_CLUES, false)
        setContent {
            CompositionLocalProvider(LocalSound provides sound) {
                UnconfirmedTheme {
                    when {
                        debugEncounter != null -> DebugEncounter(debugEncounter)
                        debugClues -> DebugClueGallery()
                        else -> GameApp()
                    }
                }
            }
        }
    }

    // A foldable becomes a phone or a tablet as it's closed or opened.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        lockPhonesUpright(newConfig)
    }

    /**
     * Keeps a phone in portrait and lets a large screen turn. Android ignores an app's orientation
     * on large screens anyway, so the lock is only asked for where it's honored. It's set here
     * rather than in the manifest, which would restrict large screens too.
     */
    private fun lockPhonesUpright(config: Configuration) {
        val wanted = if (config.smallestScreenWidthDp < LARGE_SCREEN_DP) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        // An encounter holds the screen still while it runs; it puts this back when it's over.
        if (requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_LOCKED && requestedOrientation != wanted) {
            requestedOrientation = wanted
        }
    }

    // Sound only while the game is on screen.
    override fun onStart() {
        super.onStart()
        sound.start()
    }

    override fun onStop() {
        sound.stop()
        super.onStop()
    }

    companion object {
        /** The narrowest a screen can be, in dp, and still count as large: a tablet or an open foldable. */
        private const val LARGE_SCREEN_DP = 600

        /**
         * Debug builds only: a creature id to go straight to its encounter, over and over, for
         * checking its art and animation. A case id works too, to see the encounter in that case's
         * own scenery, such as a regional story's copy in one state. See the README.
         */
        const val EXTRA_DEBUG_ENCOUNTER = "debugEncounter"

        /** Debug builds only: true to show every clue's art on one page. See the README. */
        const val EXTRA_DEBUG_CLUES = "debugClues"
    }
}

/**
 * A creature's encounter after a correct deduction, in the scene of its case. When it ends, the
 * photos from it are shown as the journal will print them, then it starts again.
 */
@Composable
private fun DebugEncounter(id: String) {
    val case = caseById(id) ?: ALL_CASES.firstOrNull { it.answerId == id }
    val creatureId = case?.answerId ?: id
    val scene = case?.scene ?: Scene.TIMBER
    val region = case?.region ?: Region.NORTHWEST
    var round by remember { mutableIntStateOf(0) }
    var photos by remember { mutableStateOf<List<Photo>?>(null) }
    val shown = photos
    if (shown != null) {
        Column(
            Modifier.fillMaxSize().desk().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            InkButton("Again", {
                photos = null
                round++
            })
            if (shown.isEmpty()) Text("No photos taken.")
            shown.forEach { PhotoPrint(it, creatureId, scene, region, width = 280.dp) }
        }
        return
    }
    key(round) {
        EncounterScreen(
            creatureId = creatureId,
            guessedRight = true,
            cluesFound = CLUES_PER_CASE,
            scene = scene,
            region = region,
            point = null,
            onFinished = { photos = it },
        )
    }
}

/** Every clue picture there is, each type with each trait the cases use, for checking the art. */
@Composable
private fun DebugClueGallery() {
    val variants = ALL_CASES
        .flatMap { case -> case.suspects.flatMap { suspect -> suspect.traits.entries.map { it.key to it.value } } }
        .distinct()
        .sortedWith(compareBy({ it.first.ordinal }, { it.second }))
    Column(
        Modifier.fillMaxSize().desk().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        variants.forEach { (type, trait) ->
            Text("${type.title}: $trait", fontFamily = FieldFont, color = Amber)
            ClueArt(type, trait, seed = (type.name + trait).hashCode(), Modifier.padding(end = 4.dp, bottom = 12.dp))
        }
    }
}
