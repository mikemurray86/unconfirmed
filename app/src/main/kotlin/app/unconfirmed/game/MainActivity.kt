package app.unconfirmed.game

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
import androidx.compose.foundation.layout.systemBarsPadding
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
import app.unconfirmed.shared.Photo
import app.unconfirmed.shared.Region
import app.unconfirmed.shared.Scene
import app.unconfirmed.shared.caseById

class MainActivity : ComponentActivity() {
    private lateinit var sound: SoundPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sound = SoundPlayer(this)
        // The phone's volume buttons set the game's volume, not the ringer's.
        volumeControlStream = AudioManager.STREAM_MUSIC
        // The game is always dark, so the system icons must stay light whatever the phone's theme.
        val bars = SystemBarStyle.dark(Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
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
            Modifier.fillMaxSize().desk().systemBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
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
            correctDeduction = true,
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
        Modifier.fillMaxSize().desk().systemBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        variants.forEach { (type, trait) ->
            Text("${type.title}: $trait", fontFamily = FieldFont, color = Amber)
            ClueArt(type, trait, seed = (type.name + trait).hashCode(), Modifier.padding(end = 4.dp, bottom = 12.dp))
        }
    }
}
