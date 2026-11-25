package app.unconfirmed.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RegionalTest {
    private fun suspect(id: String, vararg traits: String) =
        Suspect(id, id, "", ClueType.entries.zip(traits.toList()).toMap())

    // Four local creatures in "AA", and a critter that ranges over "AA", "BB" and "CC".
    private val local = listOf(
        suspect("a", "deep", "howl", "upright", "man", "hair"),
        suspect("b", "none", "splash", "low", "humps", "nothing"),
        suspect("c", "shallow", "shrill", "shadow", "winged", "feather"),
        suspect("d", "clawed", "silent", "low", "lizard", "scale"),
    )
    private val critter =
        RegionalCreature(suspect("critter", "hooves", "voice", "glow", "horned", "slime"), setOf("AA", "BB", "CC"))
    private val elsewhere =
        RegionalCreature(suspect("elsewhere", "none", "silent", "glow", "smudge", "nothing"), setOf("DD"))

    private val story = RegionalStory(
        id = "critter-story",
        creature = critter,
        scene = Scene.TIMBER,
        headlines = listOf("One", "Two"),
        teasers = listOf("First", "Second"),
        lead = "Something in the woods.",
        clues = ClueType.entries.map { Clue(it.name.lowercase(), it, "") },
        solvedNote = "",
    )

    @Test
    fun aRegionalCreatureJoinsOnlyTheGuidesInItsRange() {
        val regional = listOf(critter, elsewhere)
        assertEquals(local.map { it.id } + "critter", fieldGuide("AA", local, regional).map { it.id })
        assertEquals(listOf("elsewhere"), fieldGuide("DD", emptyList(), regional).map { it.id })
        assertTrue(fieldGuide("ZZ", local, regional).none { it.id == "critter" || it.id == "elsewhere" })
    }

    @Test
    fun aStateKeepsItsOwnEntryForARegionalCreature() {
        val own = suspect("critter", "deep", "howl", "upright", "man", "hair").copy(entry = "local lore")
        val guide = fieldGuide("AA", listOf(own), listOf(critter))
        assertEquals(1, guide.count { it.id == "critter" })
        assertEquals("local lore", guide.single().entry)
    }

    @Test
    fun aStoryRunsInEveryStateInRangeWithABigEnoughGuide() {
        val guides = allFieldGuides(mapOf("AA" to local, "BB" to local.take(3)), listOf(critter))
        // AA has four creatures of its own plus the critter. BB reaches four with it. CC has only
        // the critter, so nothing runs there.
        val cases = story.cases(guides)
        assertEquals(listOf("AA", "BB"), cases.map { it.state })
        assertEquals(listOf("critter-story-aa", "critter-story-bb"), cases.map { it.id })
        assertTrue(cases.all { it.story == "critter-story" && it.answer.id == "critter" })
        assertEquals(guides["BB"], cases.last().suspects)
        assertTrue(cases.all { it.settledBy(ClueType.entries.take(3)) })
    }

    @Test
    fun theRealGuidesAreAllNamedAndBigEnough() {
        for ((state, guide) in FIELD_GUIDES) {
            assertTrue(state in STATE_NAMES, "$state has a display name")
            assertEquals(guide.size, guide.map { it.id }.toSet().size, "$state guide has no duplicates")
        }
        for (case in ALL_CASES.filter { it.state in FIELD_GUIDES }) {
            assertTrue(case.suspects.size >= MIN_FIELD_GUIDE, "${case.id} has a full field guide")
            assertEquals(FIELD_GUIDES[case.state], case.suspects, "${case.id} uses its state's guide")
        }
    }
}
