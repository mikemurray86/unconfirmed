package app.unconfirmed.shared

import kotlin.math.exp

/** On average a clue turns up after this much walking. */
private const val MEAN_SEARCH_METERS = 150.0

/** Nobody walks further than this without finding the next clue. */
const val MAX_SEARCH_METERS = 400.0

/**
 * Whether walking [movedMeters] more turns up the next clue. [searchedMeters] is how far the
 * player has already walked without finding anything, and [roll] is a random number from 0 to 1.
 * Standing still never finds anything, so clues reward walking, in whatever direction.
 */
fun searchFindsClue(searchedMeters: Double, movedMeters: Double, roll: Double): Boolean = when {
    movedMeters <= 0.0 -> false
    searchedMeters + movedMeters >= MAX_SEARCH_METERS -> true
    else -> roll < 1 - exp(-movedMeters / MEAN_SEARCH_METERS)
}
