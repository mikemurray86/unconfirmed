package app.unconfirmed.shared

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrailTest {
    private fun turn(a: Double, b: Double): Double = abs(a - b).let { minOf(it, 360.0 - it) }

    @Test
    fun theSameSeedGivesTheSameTrail() {
        assertEquals(suggestedBearings(99L, 3), suggestedBearings(99L, 3))
        assertTrue(suggestedBearings(1L, 3) != suggestedBearings(2L, 3))
    }

    @Test
    fun bearingsAreCompassHeadingsThatKeepTurning() {
        for (seed in 0L until 500L) {
            val bearings = suggestedBearings(seed, 5)
            assertEquals(5, bearings.size)
            assertTrue(bearings.all { it >= 0.0 && it < 360.0 }, "$seed: $bearings")
            bearings.zipWithNext().forEach { (a, b) ->
                assertTrue(turn(a, b) >= MIN_TURN_DEGREES - 1e-9, "$seed: $a then $b")
            }
        }
    }

    @Test
    fun bearingsCoverEveryDirection() {
        val points = (0L until 200L).flatMap { suggestedBearings(it, 3) }.map(::compassPoint).toSet()
        assertEquals(setOf("N", "NE", "E", "SE", "S", "SW", "W", "NW"), points)
    }

    @Test
    fun theHintMovesOnWithEachClueAndStopsAtTheEnd() {
        val case = ActiveCase("flat-water", "MT", 0, listOf(10.0, 130.0, 250.0))
        assertEquals(10.0, nextHint(case))
        assertEquals(130.0, nextHint(case.copy(cluesFound = 1)))
        assertNull(nextHint(case.copy(cluesFound = 3)))
    }

    @Test
    fun seededDoublesStayInRange() {
        val random = SeededRandom(7L)
        val values = (0 until 10_000).map { random.nextDouble() }
        assertTrue(values.all { it >= 0.0 && it < 1.0 })
        assertTrue(values.average() in 0.48..0.52)
    }
}
