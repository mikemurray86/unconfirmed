package app.unconfirmed.shared

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HeatTest {
    private val definition = ALL_CASES.first()
    private val wrongId = definition.suspects.first { it.id != definition.answerId }.id

    private fun case(clues: Int = 0, guesses: List<String> = emptyList(), walked: Double = 0.0, missed: Int = 0) =
        ActiveCase(
            caseId = definition.id,
            state = definition.state,
            slot = 0,
            bearings = emptyList(),
            cluesFound = clues,
            phase = CasePhase.INVESTIGATING,
            walkedMeters = walked,
            guesses = guesses,
            missedSightings = missed,
        )

    @Test
    fun cluesGuessesAndWalkingAllMoveTheHeat() {
        assertEquals(0.0, heatOf(case(), definition))
        assertEquals(25.0, heatOf(case(clues = 1), definition))
        assertEquals(45.0, heatOf(case(clues = 1, guesses = listOf(definition.answerId)), definition))
        assertEquals(10.0, heatOf(case(clues = 1, guesses = listOf(wrongId)), definition))
        assertEquals(35.0, heatOf(case(clues = 1, walked = 200.0), definition))
        assertEquals(35.0, heatOf(case(clues = 1, missed = 1), definition))
    }

    @Test
    fun heatStaysInRange() {
        assertEquals(0.0, heatOf(case(clues = 0, guesses = listOf(wrongId, wrongId)), definition))
        val everything = case(clues = 3, guesses = List(3) { definition.answerId }, walked = 5_000.0)
        assertEquals(MAX_HEAT, heatOf(everything, definition))
    }

    @Test
    fun oneGuessForEachClue() {
        assertEquals(0, guessesLeft(case()))
        assertEquals(2, guessesLeft(case(clues = 2)))
        assertEquals(1, guessesLeft(case(clues = 2, guesses = listOf(wrongId))))
        assertEquals(0, guessesLeft(case(clues = 1, guesses = listOf(wrongId))))
    }

    @Test
    fun noSightingsBeforeTheFirstClue() {
        assertFalse(canBeSighted(case()))
        assertTrue(canBeSighted(case(clues = 1)))
    }

    @Test
    fun hotterMeansSooner() {
        assertTrue(meanSightingMeters(0.0) > meanSightingMeters(50.0))
        assertTrue(meanSightingMeters(50.0) > meanSightingMeters(100.0))
        assertEquals(COLD_SIGHTING_METERS, meanSightingMeters(0.0), 0.001)
        assertEquals(HOT_SIGHTING_METERS, meanSightingMeters(MAX_HEAT), 0.001)
    }

    @Test
    fun standingStillNeverBringsASighting() {
        assertFalse(sightingAppears(MAX_HEAT, searchedMeters = 10_000.0, movedMeters = 0.0, roll = 0.0))
    }

    @Test
    fun aSightingIsCertainPastTheGuarantee() {
        val far = meanSightingMeters(25.0) * SIGHTING_GUARANTEE
        assertTrue(sightingAppears(25.0, searchedMeters = far, movedMeters = 1.0, roll = 0.999))
    }

    @Test
    fun sightingsComeAboutWhenExpected() {
        // Walking a metre at a time, the average wait should land near the mean.
        val random = Random(7)
        for (heat in listOf(25.0, 75.0)) {
            val waits = (1..2_000).map {
                var searched = 0.0
                while (!sightingAppears(heat, searched, 1.0, random.nextDouble())) searched += 1.0
                searched + 1.0
            }
            val mean = waits.average()
            val expected = meanSightingMeters(heat)
            assertTrue(mean in expected * 0.85..expected * 1.1, "heat $heat waited $mean against $expected")
        }
    }
}
