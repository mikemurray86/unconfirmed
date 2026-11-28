package app.unconfirmed.shared

import kotlin.math.exp
import kotlin.math.pow

/*
 * Heat is how close the creature is to showing itself. The player never sees it. It's worked out
 * from what the case already keeps, rather than saved as a number of its own, so it's always in
 * step with the clues, guesses and walking that make it up.
 */

/** Heat runs from nothing to this. */
const val MAX_HEAT = 100.0

/** What each clue found adds. Clues are the main way to draw the creature out. */
const val HEAT_PER_CLUE = 25.0

/** What each guess that names the right creature adds. */
const val HEAT_PER_RIGHT_GUESS = 20.0

/** What each guess that names the wrong creature takes away. */
const val HEAT_PER_WRONG_GUESS = 15.0

/** What each 100 m covered adds, on foot or scouting. */
const val HEAT_PER_100_METERS = 5.0

/** What each sighting the player didn't tap in time adds: it's still close by. */
const val HEAT_PER_MISSED_SIGHTING = 10.0

/** On average, how far it takes for a sighting with hardly any heat, and with all of it. */
const val COLD_SIGHTING_METERS = 400.0
const val HOT_SIGHTING_METERS = 60.0

/** Nobody goes further than this many times the average without a sighting. */
const val SIGHTING_GUARANTEE = 2.5

/** How long a sighting stays on screen to be tapped. */
const val SIGHTING_SECONDS = 5.0

/** One guess can be made for each clue found. */
fun guessesLeft(case: ActiveCase): Int = (case.cluesFound - case.guesses.size).coerceAtLeast(0)

/** How hot [case] is, from 0 to [MAX_HEAT]. */
fun heatOf(case: ActiveCase, definition: CaseDefinition): Double {
    val right = case.guesses.count { it == definition.answerId }
    val wrong = case.guesses.size - right
    val lure = CREATURE_LURES[definition.answerId]
    val rightLures = case.luresUsed.count { it == lure }
    val wrongLures = case.luresUsed.size - rightLures
    val heat = case.cluesFound * HEAT_PER_CLUE +
        right * HEAT_PER_RIGHT_GUESS -
        wrong * HEAT_PER_WRONG_GUESS +
        rightLures * HEAT_PER_RIGHT_LURE -
        wrongLures * HEAT_PER_WRONG_LURE +
        case.walkedMeters / 100.0 * HEAT_PER_100_METERS +
        case.missedSightings * HEAT_PER_MISSED_SIGHTING
    return heat.coerceIn(0.0, MAX_HEAT)
}

/** On average, how far the player goes before a sighting at [heat]. Hotter is sooner. */
fun meanSightingMeters(heat: Double): Double =
    COLD_SIGHTING_METERS * (HOT_SIGHTING_METERS / COLD_SIGHTING_METERS).pow(heat.coerceIn(0.0, MAX_HEAT) / MAX_HEAT)

/**
 * Whether covering [movedMeters] more brings a sighting, at [heat]. [searchedMeters] is how far the
 * player has gone since the last chance of one, and [roll] is a random number from 0 to 1. Like
 * clues, standing still never brings one, and past [SIGHTING_GUARANTEE] times the average one is
 * certain.
 */
fun sightingAppears(heat: Double, searchedMeters: Double, movedMeters: Double, roll: Double): Boolean {
    if (movedMeters <= 0.0) return false
    val mean = meanSightingMeters(heat)
    if (searchedMeters + movedMeters >= mean * SIGHTING_GUARANTEE) return true
    return roll < 1 - exp(-movedMeters / mean)
}

/** Sightings only start once there's at least one clue, so there's always something to go on. */
fun canBeSighted(case: ActiveCase): Boolean = case.phase == CasePhase.INVESTIGATING && case.cluesFound > 0
