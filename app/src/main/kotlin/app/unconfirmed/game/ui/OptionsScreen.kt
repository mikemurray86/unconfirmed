package app.unconfirmed.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unconfirmed.game.LocalSound
import app.unconfirmed.game.TRAVEL_STATES
import app.unconfirmed.shared.stateName

/**
 * The game's settings, on a page at the back of the field journal: sound, traveler mode and where
 * the traveler is reading the paper from, and the way to About and to requesting a cryptid.
 */
@Composable
fun OptionsScreen(
    travelerMode: Boolean,
    visitingState: String?,
    homeState: String?,
    onTravelerMode: (Boolean) -> Unit,
    onTravelTo: (String?) -> Unit,
    onOpenAbout: () -> Unit,
    /** Null when there's no contact to send requests to. */
    onOpenRequest: (() -> Unit)?,
    /** Debug builds only: whether the +25m button is on, and how to flip it. Null hides the switch. */
    debugWalkButton: Pair<Boolean, (Boolean) -> Unit>?,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .desk()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .readable()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        InkButton("← Back to the journal", onBack, fill = Paper)
        PaperSheet(seed = 44, modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .ruledLines(margin = 32.dp)
                    .padding(start = 40.dp, end = 16.dp, top = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Options", fontFamily = HandFont, fontSize = 32.sp, lineHeight = 36.sp)

                LocalSound.current?.let { sound ->
                    Setting("Sound", "The sounds of the night, and a chime when a clue turns up.", sound.enabled) {
                        sound.turnOn(it)
                    }
                }

                Setting(
                    "Traveler mode",
                    "Read another state's paper and take its cases as if you were there. Clues still " +
                        "turn up by walking wherever you really are.",
                    travelerMode,
                    onTravelerMode,
                )
                if (travelerMode) TravelPicker(visitingState, homeState, onTravelTo)

                debugWalkButton?.let { (shown, setShown) ->
                    Setting("Debug: +25m button", "Counts as walking 25 m, to play sitting down.", shown, setShown)
                }

                onOpenRequest?.let { PencilLink("Request a cryptid", it) }
                PencilLink("About Unconfirmed", onOpenAbout)
            }
        }
    }
}

/** A setting that's on or off: its name, a line on what it does, and a switch. */
@Composable
private fun Setting(name: String, note: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = on, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, fontFamily = HandFont, fontSize = 22.sp)
            Text(note, fontFamily = HandFont, fontSize = 16.sp, lineHeight = 20.sp, color = BluePencil)
        }
        Switch(
            checked = on,
            // The whole row toggles it, so the switch itself is only a picture of the setting.
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Paper,
                checkedTrackColor = BluePencil,
                uncheckedThumbColor = Ink.copy(alpha = 0.6f),
                uncheckedTrackColor = Paper,
                uncheckedBorderColor = Ink.copy(alpha = 0.6f),
            ),
        )
    }
}

/** Where the traveler is reading the paper from: home, or any state with cases. */
@Composable
private fun TravelPicker(visiting: String?, home: String?, onTravelTo: (String?) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    val homeLabel = home?.let { "Home (${stateName(it)})" } ?: "Home"
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Reading the paper from", fontFamily = HandFont, fontSize = 18.sp)
        Box {
            InkButton(
                "${visiting?.let(::stateName) ?: homeLabel}  ▾",
                onClick = { open = true },
                fill = Card,
            )
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(
                    text = { Text(homeLabel) },
                    onClick = {
                        open = false
                        onTravelTo(null)
                    },
                )
                TRAVEL_STATES.forEach { state ->
                    DropdownMenuItem(
                        text = { Text(stateName(state)) },
                        onClick = {
                            open = false
                            onTravelTo(state)
                        },
                    )
                }
            }
        }
    }
}

/** A page of the journal to turn to, written as an underlined note. */
@Composable
private fun PencilLink(text: String, onClick: () -> Unit) {
    Text(
        "$text →",
        fontFamily = HandFont,
        fontSize = 20.sp,
        color = RedPencil,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp),
    )
}
