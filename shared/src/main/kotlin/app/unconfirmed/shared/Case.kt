package app.unconfirmed.shared

import kotlinx.serialization.Serializable

enum class ClueType(val title: String) {
    FOOTPRINTS("Footprints"),
    AUDIO("Field recording"),
    TRAIL_CAM("Trail-cam still"),
    WITNESS_SKETCH("Witness sketch"),
    SAMPLE("Hair or feather sample"),
}

/** How many of a case's clues one playthrough hands out. */
const val CLUES_PER_CASE = 3

/** What a save made before clue types were drawn at random holds. */
val LEGACY_CLUE_TYPES = listOf(ClueType.FOOTPRINTS, ClueType.AUDIO, ClueType.TRAIL_CAM)

data class Clue(val id: String, val type: ClueType, val text: String)

/**
 * [traits] says what each clue type shows for this creature. Two suspects with the same trait for
 * a type can't be told apart by that clue. A suspect with no trait for a type can't be ruled out by it.
 */
data class Suspect(val id: String, val name: String, val entry: String, val traits: Map<ClueType, String> = emptyMap())

/**
 * A case is only offered in [state], the US state its cryptid is reported in, so every creature a
 * player meets is local lore. [scene] is the kind of place the lead describes, drawn as the
 * first-person view while the player investigates. [headlines] and [teasers] are the ways the paper
 * can run the story, so a regular reader can't tell the creature from the front page alone. The
 * first headline is the one saves from before there was a choice get. [place] names where the story
 * is datelined, when the scene's usual name doesn't fit. [story] is shared by the copies of a
 * [RegionalStory] that run in different states, and is otherwise the case's own id.
 */
data class CaseDefinition(
    val id: String,
    val state: String,
    val scene: Scene,
    val place: String? = null,
    val headlines: List<String>,
    val teasers: List<String>,
    val lead: String,
    val clues: List<Clue>,
    val suspects: List<Suspect>,
    val answerId: String,
    val solvedNote: String,
    val story: String = id,
) {
    init {
        require(headlines.isNotEmpty()) { "$id needs at least one headline" }
        require(teasers.isNotEmpty()) { "$id needs at least one teaser" }
    }

    /** Headline number [index], wrapping round so an old save's number always finds one. */
    fun headline(index: Int): String = headlines[Math.floorMod(index, headlines.size)]

    /** Teaser number [index], wrapping round like [headline]. */
    fun teaser(index: Int): String = teasers[Math.floorMod(index, teasers.size)]

    val answer: Suspect get() = suspects.first { it.id == answerId }

    /** Where in the country the case is, which sets how its scenery looks. */
    val region: Region get() = regionOf(state)
    fun clue(id: String) = clues.first { it.id == id }

    /** True when clues of [types] leave only the answer standing. */
    fun settledBy(types: Collection<ClueType>): Boolean {
        val traits = answer.traits
        return suspects.none { suspect ->
            suspect.id != answerId &&
                types.all { type ->
                    val trait = suspect.traits[type]
                    trait == null || trait == traits[type]
                }
        }
    }
}

/**
 * Draws which of [definition]'s clue types a playthrough gets. Any type can come up, but only sets
 * that settle the case are drawn, so a player who finds every clue can always be sure. A case with
 * no more clues than are handed out gets all of them.
 */
fun pickClueTypes(definition: CaseDefinition, seed: Long): List<ClueType> {
    val available = definition.clues.map { it.type }
    if (available.size <= CLUES_PER_CASE) return available
    val options = combinations(available, CLUES_PER_CASE).filter(definition::settledBy)
    require(options.isNotEmpty()) { "${definition.id} has no set of clues that settles it" }
    val random = SeededRandom(seed xor definition.id.hashCode().toLong())
    return options[random.nextInt(options.size)]
}

/**
 * Which of [definition]'s headlines the paper runs. [seed] should change from day to day and with
 * each case the player takes, so the same story isn't always printed the same way.
 */
fun pickHeadline(definition: CaseDefinition, seed: Long): Int =
    SeededRandom(seed xor definition.id.hashCode().toLong() xor 0x68656164L).nextInt(definition.headlines.size)

/** Which of [definition]'s teasers runs under the headline, drawn apart from it so the pairs mix. */
fun pickTeaser(definition: CaseDefinition, seed: Long): Int =
    SeededRandom(seed xor definition.id.hashCode().toLong() xor 0x7465617365L).nextInt(definition.teasers.size)

/**
 * The stories the paper runs from [cases]: one for each creature, in the order the creatures first
 * appear. When a creature has more than one story, [edition] picks which runs, so the same creature
 * turns up in a different story from one edition to the next.
 */
fun runToday(cases: List<CaseDefinition>, edition: Long): List<CaseDefinition> =
    cases.groupBy { it.answerId }.map { (creature, stories) ->
        val random = SeededRandom(edition xor creature.hashCode().toLong() xor 0x72756EL)
        stories[random.nextInt(stories.size)]
    }

private fun <T> combinations(items: List<T>, size: Int): List<List<T>> = when {
    size == 0 -> listOf(emptyList())
    items.size < size -> emptyList()
    else -> combinations(items.drop(1), size - 1).map { listOf(items.first()) + it } +
        combinations(items.drop(1), size)
}

@Serializable
enum class JournalRank(val label: String) {
    RUMORED("Rumored"),
    SIGHTED("Sighted"),
    DOCUMENTED("Documented"),
    CONFIRMED("Confirmed"),
}

/** A wrong deduction caps the entry at Sighted, however good the photo is. */
fun journalRank(correctDeduction: Boolean, photoScore: Int): JournalRank = when {
    !correctDeduction -> JournalRank.SIGHTED
    photoScore >= 75 -> JournalRank.CONFIRMED
    photoScore >= 35 -> JournalRank.DOCUMENTED
    else -> JournalRank.SIGHTED
}

@Serializable
enum class CasePhase { LEAD, INVESTIGATING, ENCOUNTER, DONE }

/**
 * A player's progress through a case. Saved after every change, so a closed app loses nothing.
 * Clues turn up one at a time as the player walks, in the order of [clueTypes].
 */
@Serializable
data class ActiveCase(
    val caseId: String,
    /** The two-letter US state the case was opened in. */
    val state: String,
    val slot: Int,
    /** A suggested heading for each clue. Walking any other way finds it just as well. */
    val bearings: List<Double>,
    val cluesFound: Int = 0,
    val phase: CasePhase = CasePhase.LEAD,
    val deductionId: String? = null,
    val photoScore: Int? = null,
    val walkedMeters: Double = 0.0,
    /** Metres walked since the last clue turned up. */
    val searchedMeters: Double = 0.0,
    val startedAtMillis: Long? = null,
    val finishedAtMillis: Long? = null,
    /** The clue types drawn for this playthrough, in the order they turn up. */
    val clueTypes: List<ClueType> = LEGACY_CLUE_TYPES,
    /** Every photo taken in the encounter, in the order they were taken. */
    val photos: List<Photo> = emptyList(),
    /** Which of the case's headlines the paper ran when the player took it. */
    val headline: Int = 0,
    /**
     * The region the player was in when they took the case, which sets its scenery. Null for saves
     * from before regions, which use the case's own.
     */
    val region: Region? = null,
) {
    /** The headline the player took the case under, kept for the whole case and its journal entry. */
    fun title(definition: CaseDefinition): String = definition.headline(headline)

    /** The clues this playthrough hands out. Types the case has no clue for are skipped. */
    fun clues(definition: CaseDefinition): List<Clue> =
        clueTypes.mapNotNull { type -> definition.clues.firstOrNull { it.type == type } }

    fun allCluesFound(definition: CaseDefinition): Boolean = cluesFound >= clues(definition).size
}

/** How many investigations a player can set aside at once. */
const val MAX_PAUSED_CASES = 3

/**
 * Only an investigation under way can be paused: a lead can just be turned down, and once the
 * creature is named the case plays out. A case is never paused twice over.
 */
fun canPause(case: ActiveCase?, paused: List<ActiveCase>): Boolean = case != null &&
    case.phase == CasePhase.INVESTIGATING &&
    paused.size < MAX_PAUSED_CASES &&
    paused.none { it.caseId == case.caseId }

/** The clues the player has turned up so far, oldest first. */
fun foundClues(case: ActiveCase, definition: CaseDefinition): List<Clue> = case.clues(definition).take(case.cluesFound)
