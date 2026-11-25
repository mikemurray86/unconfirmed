package app.unconfirmed.shared

import kotlinx.serialization.Serializable

/**
 * One closed case, as the player's own record of it. Entries are kept on the phone for now. Each
 * has its own [id] and a [version], so they can move to the server later without a rewrite.
 *
 * Journal entries are proof of what the player walked and named, so they are never traded. Any
 * tradeable creature or egg should be a separate item that refers to an entry by [id].
 */
@Serializable
data class JournalEntry(
    val id: String,
    val caseId: String,
    val caseTitle: String,
    /** The creature that was really there, whatever the player named. */
    val creatureId: String,
    val creatureName: String,
    val namedId: String?,
    val namedName: String?,
    val rank: JournalRank,
    val photoScore: Int,
    val state: String,
    val cluesFound: Int,
    val cluesTotal: Int,
    val walkedMeters: Double,
    val startedAtMillis: Long?,
    val finishedAtMillis: Long,
    /** The photos taken in the encounter. Entries filed before photos were kept have none. */
    val photos: List<Photo> = emptyList(),
    val version: Int = CURRENT_VERSION,
    /** The region the case was played in, so its photos are redrawn in the same scenery. */
    val region: Region? = null,
    /**
     * The ids of the clues found, in [caseId]'s own clue list, in the order they turned up. Entries
     * filed before clues were kept have none, though [cluesFound] still counts them.
     */
    val clueIds: List<String> = emptyList(),
) {
    val correct: Boolean get() = namedId == creatureId

    companion object {
        const val CURRENT_VERSION = 1
    }
}

/** Builds the journal entry for a finished case, or null if it isn't finished. */
fun journalEntryFor(case: ActiveCase, definition: CaseDefinition, id: String): JournalEntry? {
    if (case.phase != CasePhase.DONE) return null
    val answer = definition.answer
    val named = definition.suspects.firstOrNull { it.id == case.deductionId }
    val score = case.photoScore ?: 0
    return JournalEntry(
        id = id,
        caseId = definition.id,
        caseTitle = case.title(definition),
        creatureId = answer.id,
        creatureName = answer.name,
        namedId = named?.id,
        namedName = named?.name,
        rank = journalRank(named?.id == answer.id, score),
        photoScore = score,
        state = case.state,
        cluesFound = foundClues(case, definition).size,
        cluesTotal = case.clues(definition).size,
        walkedMeters = case.walkedMeters,
        startedAtMillis = case.startedAtMillis,
        finishedAtMillis = case.finishedAtMillis ?: 0L,
        photos = case.photos,
        region = case.region,
        clueIds = foundClues(case, definition).map { it.id },
    )
}

/** A clue the player found on a case, with what it showed of the creature that was really there. */
data class Evidence(val caseId: String, val clue: Clue, val trait: String?)

/**
 * What the player has turned up about a creature across all their reports on it: one clue of each
 * type, from the newest report that found one. [entries] are newest first.
 */
fun evidenceIn(entries: List<JournalEntry>): List<Evidence> = entries
    .flatMap { entry ->
        val definition = caseById(entry.caseId) ?: return@flatMap emptyList()
        entry.clueIds.mapNotNull { id ->
            definition.clues.firstOrNull { it.id == id }?.let { Evidence(definition.id, it, definition.traitOf(it)) }
        }
    }
    .distinctBy { it.clue.type }
    .sortedBy { it.clue.type.ordinal }

/**
 * A creature's page in the journal. [best] is null for a creature still only rumored. [evidence]
 * is the clues found on it so far, and [clueTypes] every kind of clue its cases can turn up.
 */
data class CreaturePage(
    val creatureId: String,
    val name: String,
    val state: String,
    val fieldNotes: String,
    val best: JournalRank?,
    val entries: List<JournalEntry>,
    val evidence: List<Evidence> = emptyList(),
    val clueTypes: List<ClueType> = emptyList(),
)

/**
 * One page per creature that the player has met or could meet: every case answer in [state] and
 * the neighboring states its reports reach (see [reachedStates]), plus anything already in the
 * journal from elsewhere. Pages with entries come first, newest
 * report first within each page.
 */
fun journalPages(
    entries: List<JournalEntry>,
    state: String?,
    cases: List<CaseDefinition> = ALL_CASES,
): List<CreaturePage> {
    val byCreature = entries.groupBy { it.creatureId }
    val met = metCreatures(entries)
    // Neighboring states the player has opened up by filling in their own come after it.
    val reached = listOfNotNull(state) + reachedStates(state, met)
    val available = reached.flatMap { here -> cases.filter { it.state == here } }.map { it.answer to it.state }
    val elsewhere = cases.filter { it.answer.id in met && it.state != state }.map { it.answer to it.state }
    return (available + elsewhere)
        .distinctBy { (creature, _) -> creature.id }
        .map { (creature, creatureState) ->
            val own = byCreature[creature.id].orEmpty().sortedByDescending { it.finishedAtMillis }
            CreaturePage(
                creatureId = creature.id,
                name = creature.name,
                state = creatureState,
                fieldNotes = creature.entry,
                best = own.maxOfOrNull { it.rank },
                entries = own,
                evidence = evidenceIn(own),
                clueTypes = cases
                    .filter { it.answerId == creature.id }
                    .flatMap { case -> case.clues.map { it.type } }
                    .distinct()
                    .sortedBy { it.ordinal },
            )
        }
        .sortedWith(compareBy<CreaturePage> { it.best == null }.thenByDescending { it.best })
}
