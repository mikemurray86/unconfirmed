package app.unconfirmed.game.ui

import androidx.compose.runtime.Composable
import app.unconfirmed.shared.ActiveCase
import app.unconfirmed.shared.CaseDefinition
import app.unconfirmed.shared.journalRank

@Composable
fun ResultScreen(case: ActiveCase, definition: CaseDefinition, onAnotherCase: () -> Unit, onOpenJournal: () -> Unit) {
    val answer = definition.answer
    val correct = case.deductionId == answer.id
    val score = case.photoScore ?: 0
    val rank = journalRank(correct, score)
    val named = definition.suspects.firstOrNull { it.id == case.deductionId }?.name ?: "nothing"

    val minutes = case.startedAtMillis?.let { start ->
        case.finishedAtMillis?.let { end -> ((end - start) / 60_000).coerceAtLeast(1) }
    }
    val walk = "You walked ${formatDistance(case.walkedMeters)}" +
        (minutes?.let { " in $it min" } ?: "") + "."

    MessageScreen(
        title = "Journal: ${answer.name} — ${rank.label}",
        body = buildString {
            append(
                if (correct) {
                    "You named it correctly. ${definition.solvedNote}"
                } else {
                    "You named it $named, and that's not what the evidence showed. It was the " +
                        "${answer.name}. ${definition.solvedNote}"
                },
            )
            append("\n\nBest photo: $score of 100.\n")
            append(walk)
        },
        action = "Choose another case" to onAnotherCase,
        secondary = "Open journal" to onOpenJournal,
    )
}
