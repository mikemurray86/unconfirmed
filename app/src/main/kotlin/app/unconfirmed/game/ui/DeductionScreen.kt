package app.unconfirmed.game.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.unconfirmed.shared.ActiveCase
import app.unconfirmed.shared.CaseDefinition
import app.unconfirmed.shared.foundClues
import app.unconfirmed.shared.guessesLeft

/** What the field guide is open for: a guess while searching, or naming what was photographed. */
enum class DeductionMode { GUESS, NAME }

/**
 * The evidence and the field guide, to pick a creature from. While searching it's a [DeductionMode.GUESS],
 * one for each clue, and the player is never told whether it was right. After the encounter it's
 * naming what they photographed, which goes in the journal, and there's no going back. [onBack]
 * is only offered for a guess.
 */
@Composable
fun DeductionScreen(
    case: ActiveCase,
    definition: CaseDefinition,
    mode: DeductionMode,
    onBack: (() -> Unit)?,
    onDeduce: (String) -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val clues = foundClues(case, definition)
    val guessing = mode == DeductionMode.GUESS
    val left = guessesLeft(case)

    val chosen = definition.suspects.firstOrNull { it.id == selected }
    val bestPhoto = case.photos.maxByOrNull { it.score }.takeIf { !guessing }

    val intro: @Composable ColumnScope.() -> Unit = {
        onBack?.let { TextButton(onClick = it) { Text("Back to the search") } }
        Text(
            if (guessing) "Who do you think it is?" else "What did you see?",
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Serif,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            if (guessing) {
                "You get one guess for each clue you find, and ${if (left == 1) "one is" else "$left are"} " +
                    "left. You won't be told whether you're right."
            } else {
                "Name the creature you photographed. It goes in your journal, and you can't change it."
            },
        )
        bestPhoto?.let {
            PhotoPrint(
                it,
                definition.answerId,
                definition.scene,
                case.region ?: definition.region,
                width = 280.dp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        if (guessing && case.guesses.isNotEmpty()) {
            val names = case.guesses.mapNotNull { id -> definition.suspects.firstOrNull { it.id == id }?.name }
            Text(
                "Your guesses so far: ${names.joinToString(", ")}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    val evidence: @Composable () -> Unit = {
        Text("Evidence", style = MaterialTheme.typography.titleMedium)
        clues.forEach { clue ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(clue.type.title, style = MaterialTheme.typography.labelLarge)
                    Text(clue.text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    val fieldGuide: @Composable () -> Unit = {
        Text("Field guide", style = MaterialTheme.typography.titleMedium)
        Text("Tap the creature you think it was.", style = MaterialTheme.typography.bodyMedium)
        definition.suspects.forEach { suspect ->
            val isSelected = selected == suspect.id
            OutlinedCard(
                onClick = { selected = suspect.id },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) Amber else MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            suspect.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Serif,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Text("✓ Your pick", style = MaterialTheme.typography.labelLarge, color = Amber)
                        }
                    }
                    Text(suspect.entry, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    // The answer button stays pinned below the scrolling evidence and field guide, so it's always
    // in sight however long the case is.
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        if (sideBySide()) {
            // Room for the evidence and the field guide next to each other, each scrolling on its
            // own, so the clues stay in view while reading down the suspects.
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    intro()
                    evidence()
                }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) { fieldGuide() }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .readable()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                intro()
                evidence()
                fieldGuide()
            }
        }

        Surface(tonalElevation = 3.dp, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { selected?.let(onDeduce) },
                enabled = chosen != null,
                modifier = Modifier.readable().fillMaxWidth().padding(16.dp),
            ) {
                Text(
                    when {
                        chosen == null -> "Pick a creature from the field guide"
                        guessing -> "Guess ${chosen.name}"
                        else -> "It was ${chosen.name}"
                    },
                )
            }
        }
    }
}
