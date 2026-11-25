package app.unconfirmed.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class JournalTest {
    private fun finished(definition: CaseDefinition, named: String, score: Int, found: Int = 3) = ActiveCase(
        caseId = definition.id,
        state = "MT",
        slot = 0,
        bearings = listOf(0.0, 120.0, 240.0),
        cluesFound = found,
        phase = CasePhase.DONE,
        deductionId = named,
        photoScore = score,
        walkedMeters = 1200.0,
        startedAtMillis = 0L,
        finishedAtMillis = 30 * 60_000L,
    )

    private fun entry(definition: CaseDefinition, named: String, score: Int, id: String = "e", found: Int = 3) =
        journalEntryFor(finished(definition, named, score, found), definition, id)!!

    @Test
    fun aFinishedCaseBecomesAnEntry() {
        val entry = entry(CARRIES_OFF_DOGS, "ringdocus", 80, found = 2)
        assertEquals("Ringdocus", entry.creatureName)
        assertTrue(entry.correct)
        assertEquals(JournalRank.CONFIRMED, entry.rank)
        assertEquals(2, entry.cluesFound)
        assertEquals(3, entry.cluesTotal)
        assertEquals("MT", entry.state)
    }

    @Test
    fun aWrongNameRecordsTheRealCreatureAndCapsTheRank() {
        val entry = entry(FLAT_WATER, "bigfoot", 100)
        assertFalse(entry.correct)
        assertEquals("Flathead Lake Monster", entry.creatureName)
        assertEquals("Bigfoot", entry.namedName)
        assertEquals(JournalRank.SIGHTED, entry.rank)
    }

    @Test
    fun anUnfinishedCaseIsNotAnEntry() {
        val open = finished(FLAT_WATER, "bigfoot", 50).copy(phase = CasePhase.ENCOUNTER)
        assertNull(journalEntryFor(open, FLAT_WATER, "e"))
    }

    @Test
    fun entriesSurviveSaving() {
        val entries = listOf(entry(FLAT_WATER, "flathead-lake-monster", 40))
        val json = Json { ignoreUnknownKeys = true }
        val serializer = ListSerializer(JournalEntry.serializer())
        assertEquals(entries, json.decodeFromString(serializer, json.encodeToString(serializer, entries)))
    }

    @Test
    fun anEntryFromTheMapVersionStillLoads() {
        val json = Json { ignoreUnknownKeys = true }
        val saved = json.encodeToString(JournalEntry.serializer(), entry(FLAT_WATER, "bigfoot", 10))
            .replace("\"state\":", "\"areaName\":\"lincoln\",\"state\":")
        assertEquals("MT", json.decodeFromString(JournalEntry.serializer(), saved).state)
    }

    @Test
    fun pagesListLocalCreaturesAndPutTheBestRankFirst() {
        val entries = listOf(
            entry(FLAT_WATER, "bigfoot", 90, "a"),
            entry(FLAT_WATER, "flathead-lake-monster", 50, "b"),
            entry(KNOCKS_IN_THE_TIMBER, "bigfoot", 90, "c"),
        )
        val pages = journalPages(entries, "MT")
        assertEquals(7, pages.size, "every Montana creature has a page")
        assertEquals("Bigfoot", pages[0].name)
        assertEquals(JournalRank.CONFIRMED, pages[0].best)
        val lake = pages.first { it.name == "Flathead Lake Monster" }
        assertEquals(JournalRank.DOCUMENTED, lake.best)
        assertEquals(2, lake.entries.size)
        assertTrue(pages.takeLast(4).all { it.best == null }, "rumored creatures come last")
    }

    @Test
    fun creaturesMetElsewhereKeepTheirPage() {
        val devil = journalEntryFor(finished(DUSK_CROSSING, "jersey-devil", 60), DUSK_CROSSING, "d")!!
        val pages = journalPages(listOf(devil), "MT")
        assertEquals(8, pages.size)
        assertEquals("NJ", pages.first { it.name == "Jersey Devil" }.state)
        assertEquals(1, journalPages(listOf(devil), null).size)
    }

    private fun photo(score: Int, at: Long = 0L) = Photo(
        score = score,
        headingDegrees = 10.0,
        creatureBearing = 14.0,
        distanceMeters = 12.0,
        visibility = 0.8,
        movingRight = true,
        stride = 3.2,
        sceneSeconds = 9.5,
        timeOfDay = TimeOfDay.DUSK,
        weather = Weather.FOG,
        frameWidthDp = 411f,
        frameHeightDp = 891f,
        takenAtMillis = at,
    )

    @Test
    fun photosAreFiledWithTheEntryAndSurviveSaving() {
        val photos = listOf(photo(20, 1), photo(70, 2))
        val case = finished(FLAT_WATER, "flathead-lake-monster", 70).copy(photos = photos)
        val entry = journalEntryFor(case, FLAT_WATER, "e")!!
        assertEquals(photos, entry.photos)
        val json = Json { ignoreUnknownKeys = true }
        assertEquals(
            entry,
            json.decodeFromString(JournalEntry.serializer(), json.encodeToString(JournalEntry.serializer(), entry)),
        )
    }

    @Test
    fun anEntryFromBeforePhotosLoadsWithNone() {
        val json = Json { ignoreUnknownKeys = true }
        val saved = json.encodeToString(JournalEntry.serializer(), entry(FLAT_WATER, "bigfoot", 10))
        assertFalse("photos" in saved, "an entry with no photos saves as it always did")
        assertTrue(json.decodeFromString(JournalEntry.serializer(), saved).photos.isEmpty())
    }

    @Test
    fun theBestPhotoIsTheHighestScoringShotWithAnythingOnIt() {
        val a = journalEntryFor(
            finished(FLAT_WATER, "bigfoot", 40).copy(photos = listOf(photo(40, 1))),
            FLAT_WATER,
            "a",
        )!!
        val b = journalEntryFor(
            finished(FLAT_WATER, "flathead-lake-monster", 85).copy(photos = listOf(photo(0, 2), photo(85, 3))),
            FLAT_WATER,
            "b",
        )!!
        val best = bestPhoto(listOf(a, b))
        assertEquals("b", best?.first?.id)
        assertEquals(85, best?.second?.score)
        val blank = a.copy(photos = listOf(photo(0)))
        assertNull(bestPhoto(listOf(blank)), "a shot of nothing isn't a photo of the creature")
    }

    @Test
    fun everyClueHasATraitForItsArt() {
        ALL_CASES.forEach { case ->
            case.clues.forEach { clue ->
                assertTrue(case.traitOf(clue) != null, "${case.id}'s ${clue.type} clue has no trait to draw")
            }
        }
    }

    @Test
    fun anEntryKeepsTheCluesThatTurnedUp() {
        val entry = entry(FLAT_WATER, "flathead-lake-monster", 50, found = 2)
        val case = finished(FLAT_WATER, "flathead-lake-monster", 50, found = 2)
        assertEquals(foundClues(case, FLAT_WATER).map { it.id }, entry.clueIds)
        assertEquals(2, entry.clueIds.size)
    }

    @Test
    fun aCreaturePageGathersOneClueOfEachType() {
        val older = entry(FLAT_WATER, "flathead-lake-monster", 50, "old", found = 3).copy(finishedAtMillis = 1L)
        val newer = journalEntryFor(
            finished(FLAT_WATER, "flathead-lake-monster", 50, found = 1)
                .copy(clueTypes = listOf(ClueType.TRAIL_CAM), finishedAtMillis = 2L),
            FLAT_WATER,
            "new",
        )!!
        val page = journalPages(listOf(older, newer), "MT").first { it.name == "Flathead Lake Monster" }
        val types = page.evidence.map { it.clue.type }
        assertEquals(types.distinct(), types, "one clue per type")
        assertEquals(LEGACY_CLUE_TYPES.sortedBy { it.ordinal }, types)
        assertTrue(page.evidence.all { it.trait != null })
        assertTrue(page.clueTypes.containsAll(types), "found clues are among the creature's clue types")
    }

    @Test
    fun anEntryFromBeforeCluesWereKeptLoadsWithNone() {
        val json = Json { ignoreUnknownKeys = true }
        val saved = json.encodeToString(JournalEntry.serializer(), entry(FLAT_WATER, "bigfoot", 10))
            .replace(Regex(""",?"clueIds":\[[^\]]*\]"""), "")
        assertFalse("clueIds" in saved)
        val loaded = json.decodeFromString(JournalEntry.serializer(), saved)
        assertTrue(loaded.clueIds.isEmpty())
        assertTrue(evidenceIn(listOf(loaded)).isEmpty())
    }
}
