package app.unconfirmed.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NeighborsTest {
    /** A journal with an entry for every creature reported in [states]. */
    private fun metEverythingIn(vararg states: String): List<JournalEntry> = states.flatMap { state ->
        casesIn(state).distinctBy { it.answerId }.map { case ->
            JournalEntry(
                id = case.id,
                caseId = case.id,
                caseTitle = case.headline(0),
                creatureId = case.answerId,
                creatureName = case.answer.name,
                namedId = case.answerId,
                namedName = case.answer.name,
                rank = JournalRank.SIGHTED,
                photoScore = 0,
                state = state,
                cluesFound = 0,
                cluesTotal = CLUES_PER_CASE,
                walkedMeters = 0.0,
                startedAtMillis = null,
                finishedAtMillis = 0L,
            )
        }
    }

    @Test
    fun everyStateHasNeighborsAndBordersGoBothWays() {
        assertEquals(STATE_NAMES.keys, STATE_NEIGHBORS.keys)
        STATE_NEIGHBORS.forEach { (state, neighbors) ->
            assertTrue(neighbors.isNotEmpty(), "$state has a neighbor")
            neighbors.forEach { assertTrue(state in STATE_NEIGHBORS.getValue(it), "$it borders $state back") }
        }
    }

    @Test
    fun nothingComesInUntilTheHomeStateIsFilledIn() {
        assertTrue(reachedStates("MT", emptySet()).isEmpty())
        val almost = metEverythingIn("MT").drop(1)
        assertTrue(reachedStates("MT", metCreatures(almost)).isEmpty())
        assertTrue(wireCases("MT", almost, edition = 1L).isEmpty())
    }

    @Test
    fun fillingInTheHomeStateOpensItsNeighbors() {
        val met = metCreatures(metEverythingIn("MT"))
        assertTrue(stateComplete("MT", met))
        assertEquals(STATE_NEIGHBORS.getValue("MT").toSet(), reachedStates("MT", met).toSet())
    }

    @Test
    fun fillingInANeighborOpensTheStatesPastIt() {
        val met = metCreatures(metEverythingIn("MT", "ID"))
        val reached = reachedStates("MT", met)
        assertTrue("WA" in reached && "OR" in reached, "Idaho's neighbors come in")
        assertFalse("MT" in reached, "home isn't its own neighbor")
        val reachedByIdaho = STATE_NEIGHBORS.getValue("ID") - STATE_NEIGHBORS.getValue("MT").toSet() - "MT"
        assertTrue(
            reached.indexOf("ID") < reached.indexOf(reachedByIdaho.first()),
            "nearer states come first",
        )
        reached.forEach { state ->
            val via = STATE_NEIGHBORS.getValue(state).filter { it == "MT" || (it in reached && stateComplete(it, met)) }
            assertTrue(via.isNotEmpty(), "$state is only reached through home or a finished state")
        }
    }

    @Test
    fun theWireRunsOnlyCreaturesNotYetMetFromReachedStates() {
        val journal = metEverythingIn("MT")
        val met = metCreatures(journal)
        val reached = reachedStates("MT", met).toSet()
        val wire = wireCases("MT", journal, edition = 7L)
        assertTrue(wire.isNotEmpty())
        assertTrue(wire.size <= MAX_WIRE_STORIES)
        assertTrue(wire.all { it.state in reached })
        assertTrue(wire.none { it.answerId in met })
        assertEquals(wire.size, wire.map { it.answerId }.distinct().size, "one story per creature")
    }

    @Test
    fun theWireChangesFromDayToDay() {
        val journal = metEverythingIn("MT")
        val runs = (0L until 20L).map { wireCases("MT", journal, it).map { it.id }.toSet() }.toSet()
        assertTrue(runs.size > 1, "different editions run different stories")
        assertEquals(wireCases("MT", journal, 3L), wireCases("MT", journal, 3L), "the same edition is steady")
    }

    @Test
    fun islandsReachTheMainland() {
        assertEquals(listOf("CA"), reachedStates("HI", metCreatures(metEverythingIn("HI"))))
        assertEquals(listOf("WA"), reachedStates("AK", metCreatures(metEverythingIn("AK"))))
    }

    @Test
    fun theJournalListsTheReachedStatesCreaturesAsRumors() {
        val journal = metEverythingIn("MT")
        val pages = journalPages(journal, "MT")
        val wire = wireCases("MT", journal, edition = 1L)
        assertTrue(wire.all { case -> pages.any { it.creatureId == case.answerId && it.best == null } })
    }
}
