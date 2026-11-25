package app.unconfirmed.game.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
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

@Composable
fun DeductionScreen(case: ActiveCase, definition: CaseDefinition, onBack: () -> Unit, onDeduce: (String) -> Unit) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val clues = foundClues(case, definition)
    val total = case.clues(definition).size
    val missing = total - clues.size

    val chosen = definition.suspects.firstOrNull { it.id == selected }

    // The answer button stays pinned below the scrolling evidence and field guide, so it's always
    // in sight however long the case is.
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onBack) { Text("Back to the search") }
            Text(
                "What was it?",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Serif,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                if (missing > 0) {
                    "You have ${clues.size} of $total clues. A wrong name makes " +
                        "the encounter much harder, and you can't change your answer."
                } else {
                    "You've found every clue. A correct name gives you the time and the shots " +
                        "for a good photo. You can't change your answer."
                },
            )

            Text("Evidence", style = MaterialTheme.typography.titleMedium)
            clues.forEach { clue ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(clue.type.title, style = MaterialTheme.typography.labelLarge)
                        Text(clue.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

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

        Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
            Button(
                onClick = { selected?.let(onDeduce) },
                enabled = chosen != null,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) { Text(chosen?.let { "Commit to ${it.name}" } ?: "Pick a creature from the field guide") }
        }
    }
}
