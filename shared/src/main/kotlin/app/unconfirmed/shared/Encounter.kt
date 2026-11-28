package app.unconfirmed.shared

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Each clue found before the sighting is another shot and a little more time, up to the full roll
 * once every clue is in. A player who guessed the right creature along the way knows what they're
 * waiting for, so it moves slower and shows itself more. Without a right guess it's quicker and
 * harder to make out.
 */
data class EncounterSetup(val shots: Int, val speed: Double, val visibilityBias: Double, val durationSeconds: Double) {
    companion object {
        /** The film and the time for an encounter once all [CLUES_PER_CASE] clues are in. */
        const val FULL_SHOTS = 3
        const val FULL_SECONDS = 45.0

        /** The time lost for each clue still out there when the creature is named. */
        const val SECONDS_PER_CLUE = 5.0

        fun forSighting(guessedRight: Boolean, cluesFound: Int = CLUES_PER_CASE): EncounterSetup {
            val missing = (CLUES_PER_CASE - cluesFound).coerceIn(0, CLUES_PER_CASE - 1)
            return EncounterSetup(
                shots = (FULL_SHOTS - missing).coerceAtLeast(1),
                speed = if (guessedRight) 0.55 else 0.75,
                visibilityBias = if (guessedRight) 0.35 else 0.15,
                durationSeconds = FULL_SECONDS - SECONDS_PER_CLUE * missing,
            )
        }
    }
}

/** How far either side of where the player first faced the creature can roam, when they can turn to follow it. */
const val TRACKING_WANDER_DEGREES = 55.0

/** How far it roams when the phone has no compass, so it always stays somewhere on screen. */
const val ON_SCREEN_WANDER_DEGREES = 22.0

/**
 * Where along its sweep the creature starts. Chosen so it crosses dead ahead while it's clear and
 * close, which is what keeps a good photo possible on a phone with no compass.
 */
private const val PATH_PHASE = 1.2

/** Within this many degrees of dead ahead, a shot still counts as aimed at the creature. */
const val AIM_TOLERANCE_DEGREES = 12.0

/**
 * Where the creature is at a moment in time, in the scene around the player. [offsetDegrees] is
 * clockwise from the way the player first faced it, [distanceMeters] how far off it is, and
 * [visibility] from 0 (lost in the dark) to 1 (clear). [stride] counts its steps or wingbeats, for
 * animating its gait, and [movingRight] says which way it's heading so its art can face that way.
 */
data class CreatureSpot(
    val offsetDegrees: Double,
    val distanceMeters: Double,
    val visibility: Double,
    val movingRight: Boolean,
    val stride: Double,
)

/**
 * How a creature keeps the player company in an encounter. Most [ROAMS] about. One that [WATCHES]
 * never moves while it's seen: it stands at a post, fades in, holds still, fades away, then turns
 * up somewhere else.
 */
enum class EncounterPath { ROAMS, WATCHES }

/** How long a watcher stays at each post at full speed. A slower encounter keeps it there longer. */
const val WATCH_POST_SECONDS = 4.0

/** Where the creature is [seconds] into the encounter, moving the way its [path] says. */
fun creatureSpot(
    setup: EncounterSetup,
    seconds: Double,
    wanderDegrees: Double,
    path: EncounterPath = EncounterPath.ROAMS,
): CreatureSpot = when (path) {
    EncounterPath.ROAMS -> roamingSpot(setup, seconds, wanderDegrees)
    EncounterPath.WATCHES -> watchingSpot(setup, seconds, wanderDegrees)
}

/**
 * A watcher at its posts. The first is dead ahead and near, so a phone with no compass can always
 * get a good photo. The rest are scattered across the wander and further off, as if on the next
 * ridge. At each post it fades in, holds, fades out, and is gone for a moment before the next.
 * [CreatureSpot.stride] is just the clock, for its art's loop.
 */
private fun watchingSpot(setup: EncounterSetup, seconds: Double, wanderDegrees: Double): CreatureSpot {
    val post = WATCH_POST_SECONDS / setup.speed
    val k = floor(seconds / post).toInt()
    val u = seconds / post - k
    fun offset(n: Int) = if (n == 0) 0.0 else wanderDegrees * sin(n * GOLDEN_ANGLE + PATH_PHASE)
    val distance = if (k == 0) 14.0 else 18.0 + 6.0 * sin(k * 1.7 + 0.4)
    val peak = (setup.visibilityBias + 0.65).coerceAtMost(1.0)
    val presence = when {
        u < 0.2 -> u / 0.2
        u < 0.7 -> 1.0
        u < 0.88 -> (0.88 - u) / 0.18
        else -> 0.0
    }
    return CreatureSpot(
        offsetDegrees = offset(k),
        distanceMeters = distance,
        visibility = peak * presence,
        movingRight = offset(k + 1) > offset(k),
        stride = seconds,
    )
}

/** Turns between posts that never line up, so a watcher's posts spread evenly across its range. */
private const val GOLDEN_ANGLE = 2.399963

/** The creature roams [wanderDegrees] either side of where it was first seen, coming near and drifting off. */
private fun roamingSpot(setup: EncounterSetup, seconds: Double, wanderDegrees: Double): CreatureSpot {
    val t = seconds * setup.speed
    return CreatureSpot(
        offsetDegrees = wanderDegrees * sin(t * 0.55 + PATH_PHASE),
        distanceMeters = 14.0 + 8.0 * sin(t * 0.43 + 2.2),
        visibility = (setup.visibilityBias + 0.75 * sin(0.8 * t - 0.6)).coerceIn(0.0, 1.0),
        movingRight = cos(t * 0.55 + PATH_PHASE) > 0,
        stride = t * 3.0,
    )
}

/**
 * Scores a photo from 0 to 100. Aimed dead at it, clear and close is perfect. [offAimDegrees] is
 * how far the creature is from the middle of the shot.
 */
fun shotScore(offAimDegrees: Double, distanceMeters: Double, visibility: Double): Int {
    val framing = (1.0 - abs(offAimDegrees) / AIM_TOLERANCE_DEGREES).coerceIn(0.0, 1.0)
    // Further off it's smaller in the frame, but even a distant shot is worth something.
    val size = (1.0 - (distanceMeters - 8.0) / 28.0).coerceIn(0.55, 1.0)
    return (100 * (0.6 * framing + 0.4 * visibility) * minOf(1.0, visibility * 3) * size).roundToInt()
}
