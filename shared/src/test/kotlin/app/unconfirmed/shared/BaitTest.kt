package app.unconfirmed.shared

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaitTest {
    private val definition = ALL_CASES.first()
    private val rightLure = CREATURE_LURES.getValue(definition.answerId)
    private val wrongLure = Lure.entries.first { it != rightLure }

    private fun case(lures: List<Lure> = emptyList(), carried: List<Lure> = emptyList()) = ActiveCase(
        caseId = definition.id,
        state = definition.state,
        slot = 0,
        bearings = emptyList(),
        cluesFound = 2,
        phase = CasePhase.INVESTIGATING,
        luresUsed = lures,
        bait = carried,
    )

    @Test
    fun everyCreatureThatCanBeTheAnswerHasALure() {
        val missing = ALL_CASES.map { it.answerId }.toSet() - CREATURE_LURES.keys
        assertTrue(missing.isEmpty(), "no lure for $missing")
    }

    @Test
    fun theLureTableOnlyNamesRealCreatures() {
        val unknown = CREATURE_LURES.keys - ALL_CASES.map { it.answerId }.toSet()
        assertTrue(unknown.isEmpty(), "unknown creatures $unknown")
    }

    @Test
    fun baitIsWorthHalfAGuess() {
        assertEquals(HEAT_PER_RIGHT_GUESS / 2, HEAT_PER_RIGHT_LURE)
        assertEquals(HEAT_PER_WRONG_GUESS / 2, HEAT_PER_WRONG_LURE)
        val base = heatOf(case(), definition)
        assertEquals(base + HEAT_PER_RIGHT_LURE, heatOf(case(lures = listOf(rightLure)), definition))
        assertEquals(base - HEAT_PER_WRONG_LURE, heatOf(case(lures = listOf(wrongLure)), definition))
    }

    @Test
    fun carryingIsLimited() {
        assertTrue(canCarryMoreBait(case(carried = List(MAX_BAIT_CARRIED - 1) { Lure.FISH })))
        assertFalse(canCarryMoreBait(case(carried = List(MAX_BAIT_CARRIED) { Lure.FISH })))
    }

    @Test
    fun standingStillTurnsUpNothing() {
        assertFalse(baitTurnsUp(0.0, 0.0))
    }

    @Test
    fun baitTurnsUpAboutAsOftenAsExpected() {
        val random = Random(11)
        val waits = (1..2_000).map {
            var walked = 1.0
            while (!baitTurnsUp(1.0, random.nextDouble())) walked += 1.0
            walked
        }
        val mean = waits.average()
        assertTrue(mean in MEAN_BAIT_METERS * 0.85..MEAN_BAIT_METERS * 1.15, "waited $mean")
    }

    @Test
    fun everyLureCanTurnUp() {
        assertEquals(Lure.entries.toSet(), (0 until 100).map { lureAt(it / 100.0) }.toSet())
        assertEquals(Lure.entries.last(), lureAt(1.0))
    }
}
