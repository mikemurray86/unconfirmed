package app.unconfirmed.shared

import kotlinx.serialization.Serializable

/**
 * One photo the player took in an encounter. It isn't kept as an image: it's everything the scene
 * needs to draw that moment again, so the journal shows exactly what was in the viewfinder and an
 * entry stays small enough to move to a server later.
 *
 * [headingDegrees] is where the camera pointed and [creatureBearing] where the creature was, both
 * compass bearings. [stride] is how far through its gait it was, and [sceneSeconds] the scene's
 * own clock, which sets things like ripples and swaying grass. [frameWidthDp] and [frameHeightDp]
 * are the size the scene was drawn at, since the viewfinder is cut from the middle of it.
 */
@Serializable
data class Photo(
    val score: Int,
    val headingDegrees: Double,
    val creatureBearing: Double,
    val distanceMeters: Double,
    val visibility: Double,
    val movingRight: Boolean,
    val stride: Double,
    val sceneSeconds: Double,
    val timeOfDay: TimeOfDay = TimeOfDay.DAY,
    val weather: Weather = Weather.CLEAR,
    val frameWidthDp: Float,
    val frameHeightDp: Float,
    val takenAtMillis: Long,
)

/** The best photo of a creature in its journal entries, and the entry it was filed with. */
fun bestPhoto(entries: List<JournalEntry>): Pair<JournalEntry, Photo>? = entries
    .flatMap { entry -> entry.photos.map { entry to it } }
    .filter { (_, photo) -> photo.score > 0 }
    .maxByOrNull { (_, photo) -> photo.score }

/** What the creature behind [definition] shows for [clue]'s type, so the clue's art can show it too. */
fun CaseDefinition.traitOf(clue: Clue): String? = answer.traits[clue.type]
