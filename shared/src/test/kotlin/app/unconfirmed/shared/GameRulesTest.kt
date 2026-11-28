package app.unconfirmed.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameRulesTest {
    private val start = GeoPoint(45.0, -122.0)

    /** A point [meters] north of the start. */
    private fun north(meters: Double) = GeoPoint(start.lat + meters / METERS_PER_DEGREE_LAT, start.lon)

    private fun fix(meters: Double, seconds: Int, accuracy: Double = 8.0) =
        LocationFix(north(meters), accuracy, seconds * 1000L)

    @Test
    fun walkingAddsUpDistance() {
        val tracker = MovementTracker()
        val walked = (0..10).sumOf { step -> tracker.update(fix(step * 15.0, step * 10)) }
        // Smoothing trails a few metres behind a moving player, so the last few aren't counted yet.
        assertEquals(150.0, walked, 10.0)
        assertFalse(tracker.isTooFast)
    }

    @Test
    fun gpsJitterWhileStandingStillDoesNotCount() {
        val tracker = MovementTracker()
        val wobble = listOf(0.0, 4.0, -3.0, 5.0, -5.0, 2.0, 6.0, -2.0)
        val walked = wobble.withIndex().sumOf { (i, m) -> tracker.update(fix(m, i * 3)) }
        assertEquals(0.0, walked)
    }

    /** A fix that comes with a speed reading, as real GPS fixes do. */
    private fun fixWithSpeed(
        meters: Double,
        seconds: Int,
        speed: Double,
        accuracy: Double = 25.0,
        speedError: Double? = 0.3,
    ) = LocationFix(north(meters), accuracy, seconds * 1000L, speed, speedAccuracyMetersPerSecond = speedError)

    /** Repeatable GPS position noise of up to [size] metres either way. */
    private fun noise(i: Int, size: Double) = size * kotlin.math.sin(i * 2.3 + 0.7)

    @Test
    fun walkingCountsFromSpeedEvenWhenPositionsWander() {
        val tracker = MovementTracker()
        // Five minutes at 1.4 m/s with positions off by up to 25 m.
        val walked = (0..300).sumOf { s -> tracker.update(fixWithSpeed(s * 1.4 + noise(s, 25.0), s, 1.4)) }
        assertEquals(420.0, walked, 5.0)
        assertFalse(tracker.isTooFast)
    }

    @Test
    fun speedNoiseWhileStandingStillDoesNotCount() {
        val tracker = MovementTracker()
        val walked = (0..120).sumOf { s ->
            tracker.update(fixWithSpeed(noise(s, 15.0), s, speed = 0.2 + 0.2 * kotlin.math.sin(s * 1.7)))
        }
        assertEquals(0.0, walked)
    }

    @Test
    fun untrustedSpeedFallsBackToPosition() {
        val tracker = MovementTracker()
        // The phone claims 2 m/s, give or take 3, while standing still.
        val walked = (0..60).sumOf { s ->
            tracker.update(fixWithSpeed(noise(s, 4.0), s, speed = 2.0, accuracy = 8.0, speedError = 3.0))
        }
        assertEquals(0.0, walked)
    }

    @Test
    fun walkingWithoutSpeedReadingsStillCounts() {
        val tracker = MovementTracker()
        val walked = (0..200).sumOf { s -> tracker.update(fix(s * 1.4 + noise(s, 5.0), s, accuracy = 15.0)) }
        assertEquals(280.0, walked, 30.0)
    }

    @Test
    fun poorFixesDoNotCount() {
        val tracker = MovementTracker()
        tracker.update(fix(0.0, 0))
        assertEquals(0.0, tracker.update(fix(100.0, 60, accuracy = 80.0)))
    }

    @Test
    fun drivingSpeedBlocksPlayAndDoesNotCountAsWalking() {
        val tracker = MovementTracker()
        tracker.update(fix(0.0, 0))
        tracker.update(fix(150.0, 10))
        val walked = tracker.update(fix(300.0, 20))
        assertTrue(tracker.isTooFast)
        assertEquals(0.0, walked)
    }

    @Test
    fun cyclingIsAllowed() {
        val tracker = MovementTracker()
        repeat(6) { step -> tracker.update(fix(step * 60.0, step * 10)) }
        assertFalse(tracker.isTooFast)
    }

    @Test
    fun oneGpsJumpDoesNotBlockPlay() {
        val tracker = MovementTracker()
        tracker.update(fix(0.0, 0))
        tracker.update(fix(400.0, 3))
        tracker.update(fix(400.0, 6))
        assertFalse(tracker.isTooFast)
    }

    @Test
    fun blockLiftsAfterSlowingDown() {
        val tracker = MovementTracker()
        tracker.update(fix(0.0, 0))
        tracker.update(fix(150.0, 10))
        tracker.update(fix(300.0, 20))
        assertTrue(tracker.isTooFast)
        tracker.update(fix(301.0, 30))
        assertTrue(tracker.isTooFast, "a stop at a junction shouldn't lift it")
        tracker.update(fix(302.0, 40))
        assertFalse(tracker.isTooFast)
    }

    @Test
    fun standingStillNeverFindsAClue() {
        assertFalse(searchFindsClue(searchedMeters = 0.0, movedMeters = 0.0, roll = 0.0))
        assertFalse(searchFindsClue(searchedMeters = MAX_SEARCH_METERS, movedMeters = 0.0, roll = 0.0))
    }

    @Test
    fun walkingAlwaysFindsTheClueInTheEnd() {
        assertFalse(searchFindsClue(searchedMeters = 0.0, movedMeters = 20.0, roll = 0.99))
        assertTrue(searchFindsClue(searchedMeters = MAX_SEARCH_METERS - 20.0, movedMeters = 20.0, roll = 0.99))
    }

    @Test
    fun findsComeAtRandomPointsOnTheWalk() {
        val random = kotlin.random.Random(42)
        val walks = (0 until 2000).map {
            var searched = 0.0
            while (!searchFindsClue(searched, 20.0, random.nextDouble())) searched += 20.0
            searched + 20.0
        }
        assertTrue(walks.min() <= 40.0, "some clues turn up almost at once")
        assertTrue(walks.max() <= MAX_SEARCH_METERS)
        assertTrue(walks.average() in 100.0..200.0, "average ${walks.average()}")
    }

    @Test
    fun cluesTurnUpInTheOrderTheyWereDrawn() {
        for (case in ALL_CASES) {
            val drawn = pickClueTypes(case, 5L)
            val active = ActiveCase(case.id, case.state, 0, suggestedBearings(5L, CLUES_PER_CASE), clueTypes = drawn)
            assertTrue(foundClues(active, case).isEmpty(), case.id)
            val all = active.copy(cluesFound = drawn.size)
            assertEquals(drawn, foundClues(all, case).map { it.type }, case.id)
            assertTrue(all.allCluesFound(case), case.id)
            assertFalse(active.copy(cluesFound = 1).allCluesFound(case), case.id)
        }
    }

    @Test
    fun anyThreeCluesSettleEveryFullCase() {
        val types = ClueType.entries
        for (case in ALL_CASES.filter { it.state in FIELD_GUIDES }) {
            assertEquals(types.toSet(), case.clues.map { it.type }.toSet(), "${case.id} has every clue type")
            for (a in types) {
                for (b in types) {
                    for (c in types) {
                        if (setOf(a, b, c).size ==
                            3
                        ) {
                            assertTrue(case.settledBy(listOf(a, b, c)), "${case.id}: $a $b $c")
                        }
                    }
                }
            }
        }
    }

    @Test
    fun drawnCluesVaryButAlwaysSettleTheCase() {
        for (case in ALL_CASES.filter { it.state in FIELD_GUIDES }) {
            val draws = (0L until 200L).map { pickClueTypes(case, it) }
            assertTrue(draws.all { it.size == CLUES_PER_CASE && case.settledBy(it) }, case.id)
            assertEquals(ClueType.entries.toSet(), draws.flatten().toSet(), "${case.id} draws every type")
            assertEquals(draws.first(), pickClueTypes(case, 0L), "${case.id} draw is repeatable")
        }
        val threeClues = FLAT_WATER.copy(clues = FLAT_WATER.clues.filter { it.type in LEGACY_CLUE_TYPES })
        assertEquals(LEGACY_CLUE_TYPES, pickClueTypes(threeClues, 7L), "a three-clue case uses all three")
    }

    @Test
    fun theSameStoryRunsUnderDifferentHeadlinesAndTeasers() {
        // A regional story runs the same words in each state, so headlines are unique per story.
        val stories = ALL_CASES.distinctBy { it.story }
        val allHeadlines = stories.flatMap { it.headlines }
        assertEquals(allHeadlines.size, allHeadlines.toSet().size, "no headline is shared between stories")
        for (case in ALL_CASES) {
            assertTrue(case.headlines.size > 1, "${case.id} has more than one headline")
            val draws = (0L until 30L).flatMap { day -> (0 until 5).map { slot -> editionSeed(day, slot) } }
                .map { pickHeadline(case, it) }
            assertEquals(case.headlines.indices.toSet(), draws.toSet(), "${case.id} runs every headline")
            assertEquals(draws.first(), pickHeadline(case, editionSeed(0L, 0)), "${case.id} draw is repeatable")
        }
        val allTeasers = stories.flatMap { it.teasers }
        assertEquals(allTeasers.size, allTeasers.toSet().size, "no teaser is shared between stories")
        for (case in ALL_CASES) {
            assertTrue(case.teasers.size > 1, "${case.id} has more than one teaser")
            val seeds = (0L until 30L).flatMap { day -> (0 until 5).map { slot -> editionSeed(day, slot) } }
            val teasers = seeds.map { pickTeaser(case, it) }.toSet()
            assertEquals(case.teasers.indices.toSet(), teasers, "${case.id} runs every teaser")
            val pairs = seeds.map { pickHeadline(case, it) to pickTeaser(case, it) }.toSet()
            assertTrue(pairs.size > case.headlines.size, "${case.id} teasers aren't tied to headlines")
        }
        val legacy = ActiveCase(FLAT_WATER.id, "MT", 0, listOf(0.0))
        assertEquals(FLAT_WATER.headlines.first(), legacy.title(FLAT_WATER), "an old save keeps its headline")
        val wrapped = FLAT_WATER.headline(FLAT_WATER.headlines.size)
        assertEquals(FLAT_WATER.headlines.first(), wrapped, "an out-of-range index wraps")
    }

    @Test
    fun onlyAnInvestigationUnderWayCanBePaused() {
        fun case(id: String, phase: CasePhase) = ActiveCase(id, "MT", 0, listOf(0.0), phase = phase)
        val investigating = case("flat-water", CasePhase.INVESTIGATING)
        assertTrue(canPause(investigating, emptyList()))
        assertFalse(canPause(null, emptyList()))
        for (phase in listOf(CasePhase.LEAD, CasePhase.ENCOUNTER, CasePhase.DONE)) {
            assertFalse(canPause(case("flat-water", phase), emptyList()), "$phase")
        }
        assertFalse(canPause(investigating, listOf(investigating)), "already paused")
        val full = (0 until MAX_PAUSED_CASES).map { case("other-$it", CasePhase.INVESTIGATING) }
        assertFalse(canPause(investigating, full), "too many paused")
        assertTrue(canPause(investigating, full.drop(1)))
    }

    @Test
    fun aSaveFromTheMapVersionIsDropped() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val saved = """{"caseId":"flat-water","areaName":"a","slot":0,"sites":[]}"""
        assertTrue(runCatching { json.decodeFromString(ActiveCase.serializer(), saved) }.isFailure)
    }

    @Test
    fun everyCaseIsWellFormed() {
        assertEquals(ALL_CASES.size, ALL_CASES.map { it.id }.toSet().size, "case ids are unique")
        for (case in ALL_CASES) {
            assertTrue(case.suspects.any { it.id == case.answerId }, "${case.id} answer is a suspect")
            assertEquals(case.suspects.size, case.suspects.map { it.id }.toSet().size, "${case.id} suspects")
            assertFalse("%s" in case.lead, "${case.id} lead stands on its own")
            assertTrue(case.state in STATE_NAMES, "${case.id} state has a display name")
            assertEquals(case, caseById(case.id))
        }
        assertEquals("Jersey Devil", DUSK_CROSSING.answer.name)
    }

    @Test
    fun thePaperRunsOneStoryPerCreatureAndRotatesThem() {
        val editions = (0L until 30L).flatMap { day -> (0 until 5).map { slot -> editionSeed(day, slot) } }
        for (state in ALL_CASES.map { it.state }.distinct()) {
            val cases = casesIn(state)
            val creatures = cases.map { it.answerId }.distinct()
            val runs = editions.map { runToday(cases, it) }
            for (run in runs) assertEquals(creatures, run.map { it.answerId }, "$state runs every creature once")
            assertEquals(cases.toSet(), runs.flatten().toSet(), "$state runs every story in time")
            assertEquals(runs.first(), runToday(cases, editions.first()), "$state run is repeatable")
        }
        // Every creature with a case anywhere has more than one story there to rotate through.
        for ((state, cases) in ALL_CASES.groupBy { it.state }) {
            for ((creature, stories) in cases.groupBy { it.answerId }) {
                assertTrue(stories.size > 1, "$creature has a second story in $state")
            }
        }
        assertEquals(3, casesIn("AL").count { it.answerId == "bigfoot" }, "Bigfoot has three stories")
    }

    @Test
    fun casesAreOnlyOfferedInTheirOwnState() {
        val montana = casesIn("MT")
        assertEquals(
            setOf(
                "Flathead Lake Monster",
                "Ringdocus",
                "Bigfoot",
                "Prairie Giant",
                "Blacktail Monster",
                "Thunderbird",
                "Jackalope",
            ),
            montana.map { it.answer.name }.toSet(),
        )
        assertTrue(montana.all { it.state == "MT" })
        assertEquals(
            setOf("Bigfoot", "Batsquatch", "Cadborosaurus", "Lake Chelan Monster", "Gumberoo"),
            casesIn("WA").map { it.answer.name }.toSet(),
        )
        assertEquals(
            setOf("Bigfoot", "Dark Watchers", "Tahoe Tessie", "Fresno Nightcrawler", "Billiwhack Monster"),
            casesIn("CA").map { it.answer.name }.toSet(),
        )
        assertEquals(
            setOf("Jackalope", "Smetty", "Bigfoot", "Wimpus"),
            casesIn("WY").map { it.answer.name }.toSet(),
        )
        assertEquals(
            setOf("Lake Worth Monster", "Bigfoot", "Big Bird", "Chupacabra", "Black Panther", "Hoop Snake"),
            casesIn("TX").map { it.answer.name }.toSet(),
        )
        assertEquals(
            setOf("Red Ghost", "Mogollon Monster", "Winged Monster", "Chupacabra", "Tommyknocker"),
            casesIn("AZ").map { it.answer.name }.toSet(),
        )
        assertEquals(
            setOf("Mogollon Monster", "Winged Monster", "Chupacabra", "Tommyknocker"),
            casesIn("NM").map { it.answer.name }.toSet(),
        )
        assertEquals(
            setOf("Jersey Devil", "Bigfoot", "Black Panther", "Hidebehind", "Atlantic Sea Serpent"),
            casesIn("NJ").map { it.answer.name }.toSet(),
        )
        assertEquals(
            setOf("Mothman", "Bigfoot", "Black Panther", "Hoop Snake"),
            casesIn("WV").map { it.answer.name }.toSet(),
        )
        assertEquals(
            setOf("Chupacabra", "Bigfoot", "Thunderbird", "Black Panther", "Hoop Snake"),
            casesIn("OK").map { it.answer.name }.toSet(),
        )
        assertEquals(
            setOf("Bigfoot", "Thunderbird", "Iliamna Lake Monster"),
            casesIn("AK").map { it.answer.name }.toSet(),
        )
        assertEquals(
            setOf("Menehune", "Island Big Cat", "Nuuanu Caiman", "Kraken"),
            casesIn("HI").map { it.answer.name }.toSet(),
        )
        assertTrue(casesIn(null).isEmpty())
    }

    @Test
    fun everyStateHasCases() {
        for (state in STATE_NAMES.keys) {
            assertTrue(casesIn(state).isNotEmpty(), "$state has cases")
            assertTrue(state in FIELD_GUIDES, "$state has a field guide")
        }
    }

    @Test
    fun wrongDeductionCapsTheJournalEntry() {
        assertEquals(JournalRank.SIGHTED, journalRank(correctDeduction = false, photoScore = 100))
    }

    @Test
    fun betterPhotosEarnBetterEntries() {
        assertEquals(JournalRank.SIGHTED, journalRank(true, 0))
        assertEquals(JournalRank.DOCUMENTED, journalRank(true, 50))
        assertEquals(JournalRank.CONFIRMED, journalRank(true, 90))
    }

    @Test
    fun shotScoreRewardsAimVisibilityAndCloseness() {
        assertEquals(100, shotScore(offAimDegrees = 0.0, distanceMeters = 6.0, visibility = 1.0))
        assertEquals(0, shotScore(0.0, 6.0, visibility = 0.0))
        assertTrue(shotScore(20.0, 6.0, 1.0) < shotScore(3.0, 6.0, 1.0), "aim matters")
        assertTrue(shotScore(0.0, 22.0, 1.0) < shotScore(0.0, 8.0, 1.0), "closer is better")
        assertTrue(shotScore(0.0, 100.0, 1.0) > 0, "a distant clear shot still counts")
    }

    @Test
    fun theCreatureRoamsAroundThePlayerWithinReach() {
        val setup = EncounterSetup.forSighting(guessedRight = true)
        val spots = (0 until 45 * 30).map { creatureSpot(setup, it / 30.0, TRACKING_WANDER_DEGREES) }
        assertTrue(spots.all { kotlin.math.abs(it.offsetDegrees) <= TRACKING_WANDER_DEGREES })
        assertTrue(
            spots.minOf {
                it.offsetDegrees
            } < -40 &&
                spots.maxOf { it.offsetDegrees } > 40,
            "it crosses the view",
        )
        assertTrue(spots.all { it.distanceMeters in 5.9..22.1 })
        assertTrue(spots.any { it.movingRight } && spots.any { !it.movingRight })
    }

    private fun bestPossibleScore(setup: EncounterSetup) = (0 until (setup.durationSeconds * 30).toInt())
        .maxOf { frame ->
            val spot = creatureSpot(setup, frame / 30.0, TRACKING_WANDER_DEGREES)
            shotScore(offAimDegrees = 0.0, distanceMeters = spot.distanceMeters, visibility = spot.visibility)
        }

    @Test
    fun correctDeductionMakesConfirmedReachable() {
        val setup = EncounterSetup.forSighting(guessedRight = true)
        assertTrue(bestPossibleScore(setup) >= 75, "best was ${bestPossibleScore(setup)}")
    }

    @Test
    fun withoutACompassThePathStillPassesThroughTheMiddle() {
        val setup = EncounterSetup.forSighting(guessedRight = true)
        val best = (0 until (setup.durationSeconds * 30).toInt()).maxOf { frame ->
            val spot = creatureSpot(setup, frame / 30.0, ON_SCREEN_WANDER_DEGREES)
            shotScore(spot.offsetDegrees, spot.distanceMeters, spot.visibility)
        }
        assertTrue(best >= 75, "best was $best")
    }

    @Test
    fun aWatcherNeverMovesWhileItCanBeSeen() {
        val setup = EncounterSetup.forSighting(guessedRight = true)
        val spots = (0 until (setup.durationSeconds * 30).toInt()).map {
            creatureSpot(setup, it / 30.0, TRACKING_WANDER_DEGREES, EncounterPath.WATCHES)
        }
        for ((a, b) in spots.zipWithNext()) {
            if (a.visibility > 0 && b.visibility > 0) {
                assertEquals(a.offsetDegrees, b.offsetDegrees, "it doesn't move while seen")
                assertEquals(a.distanceMeters, b.distanceMeters, "it doesn't come closer while seen")
            }
        }
        assertTrue(spots.any { it.visibility == 0.0 }, "it vanishes between posts")
        assertTrue(spots.map { it.offsetDegrees }.toSet().size >= 5, "it turns up in different places")
        assertTrue(spots.all { kotlin.math.abs(it.offsetDegrees) <= TRACKING_WANDER_DEGREES })
        assertTrue(spots.all { it.distanceMeters in 11.9..24.1 }, "it keeps its distance")
    }

    @Test
    fun aWatcherCanStillBeConfirmedWithoutACompass() {
        val setup = EncounterSetup.forSighting(guessedRight = true)
        val best = (0 until (setup.durationSeconds * 30).toInt()).maxOf { frame ->
            val spot = creatureSpot(setup, frame / 30.0, ON_SCREEN_WANDER_DEGREES, EncounterPath.WATCHES)
            shotScore(spot.offsetDegrees, spot.distanceMeters, spot.visibility)
        }
        assertTrue(best >= 75, "best was $best")
    }

    @Test
    fun eachClueFoundAddsAShotAndTime() {
        val setups = (1..CLUES_PER_CASE).map { EncounterSetup.forSighting(guessedRight = true, cluesFound = it) }
        assertEquals(listOf(1, 2, 3), setups.map { it.shots })
        assertEquals(listOf(35.0, 40.0, 45.0), setups.map { it.durationSeconds })
        assertEquals(EncounterSetup.forSighting(guessedRight = true), setups.last(), "every clue gives the full roll")
    }

    @Test
    fun aSingleClueStillMakesConfirmedReachable() {
        val setup = EncounterSetup.forSighting(guessedRight = true, cluesFound = 1)
        assertTrue(bestPossibleScore(setup) >= 75, "best was ${bestPossibleScore(setup)}")
        val watcher = (0 until (setup.durationSeconds * 30).toInt()).maxOf { frame ->
            val spot = creatureSpot(setup, frame / 30.0, ON_SCREEN_WANDER_DEGREES, EncounterPath.WATCHES)
            shotScore(spot.offsetDegrees, spot.distanceMeters, spot.visibility)
        }
        assertTrue(watcher >= 75, "best watcher shot was $watcher")
    }

    @Test
    fun aRightGuessGivesAnEasierEncounter() {
        val right = EncounterSetup.forSighting(guessedRight = true)
        val unsure = EncounterSetup.forSighting(guessedRight = false)
        assertEquals(right.shots, unsure.shots, "film comes from clues, not guesses")
        assertTrue(right.speed < unsure.speed)
        assertTrue(right.visibilityBias > unsure.visibilityBias)
    }

    @Test
    fun withoutARightGuessASingleClueCanStillBeConfirmed() {
        val setup = EncounterSetup.forSighting(guessedRight = false, cluesFound = 1)
        assertTrue(bestPossibleScore(setup) >= 75, "best was ${bestPossibleScore(setup)}")
    }
}

/** States whose cases have a full field guide and every clue type. */
