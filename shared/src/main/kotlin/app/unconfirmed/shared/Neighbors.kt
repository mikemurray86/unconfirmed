package app.unconfirmed.shared

/**
 * The states that share a land border with each state. Alaska and Hawaii have none, so they're
 * linked to the states their flights and ferries run to: Alaska to Washington, Hawaii to California.
 */
val STATE_NEIGHBORS: Map<String, List<String>> = mapOf(
    "AL" to listOf("FL", "GA", "MS", "TN"),
    "AK" to listOf("WA"),
    "AZ" to listOf("CA", "CO", "NM", "NV", "UT"),
    "AR" to listOf("LA", "MO", "MS", "OK", "TN", "TX"),
    "CA" to listOf("AZ", "HI", "NV", "OR"),
    "CO" to listOf("AZ", "KS", "NE", "NM", "OK", "UT", "WY"),
    "CT" to listOf("MA", "NY", "RI"),
    "DE" to listOf("MD", "NJ", "PA"),
    "FL" to listOf("AL", "GA"),
    "GA" to listOf("AL", "FL", "NC", "SC", "TN"),
    "HI" to listOf("CA"),
    "ID" to listOf("MT", "NV", "OR", "UT", "WA", "WY"),
    "IL" to listOf("IA", "IN", "KY", "MO", "WI"),
    "IN" to listOf("IL", "KY", "MI", "OH"),
    "IA" to listOf("IL", "MN", "MO", "NE", "SD", "WI"),
    "KS" to listOf("CO", "MO", "NE", "OK"),
    "KY" to listOf("IL", "IN", "MO", "OH", "TN", "VA", "WV"),
    "LA" to listOf("AR", "MS", "TX"),
    "ME" to listOf("NH"),
    "MD" to listOf("DE", "PA", "VA", "WV"),
    "MA" to listOf("CT", "NH", "NY", "RI", "VT"),
    "MI" to listOf("IN", "OH", "WI"),
    "MN" to listOf("IA", "ND", "SD", "WI"),
    "MS" to listOf("AL", "AR", "LA", "TN"),
    "MO" to listOf("AR", "IA", "IL", "KS", "KY", "NE", "OK", "TN"),
    "MT" to listOf("ID", "ND", "SD", "WY"),
    "NE" to listOf("CO", "IA", "KS", "MO", "SD", "WY"),
    "NV" to listOf("AZ", "CA", "ID", "OR", "UT"),
    "NH" to listOf("MA", "ME", "VT"),
    "NJ" to listOf("DE", "NY", "PA"),
    "NM" to listOf("AZ", "CO", "OK", "TX", "UT"),
    "NY" to listOf("CT", "MA", "NJ", "PA", "VT"),
    "NC" to listOf("GA", "SC", "TN", "VA"),
    "ND" to listOf("MN", "MT", "SD"),
    "OH" to listOf("IN", "KY", "MI", "PA", "WV"),
    "OK" to listOf("AR", "CO", "KS", "MO", "NM", "TX"),
    "OR" to listOf("CA", "ID", "NV", "WA"),
    "PA" to listOf("DE", "MD", "NJ", "NY", "OH", "WV"),
    "RI" to listOf("CT", "MA"),
    "SC" to listOf("GA", "NC"),
    "SD" to listOf("IA", "MN", "MT", "ND", "NE", "WY"),
    "TN" to listOf("AL", "AR", "GA", "KY", "MO", "MS", "NC", "VA"),
    "TX" to listOf("AR", "LA", "NM", "OK"),
    "UT" to listOf("AZ", "CO", "ID", "NM", "NV", "WY"),
    "VT" to listOf("MA", "NH", "NY"),
    "VA" to listOf("KY", "MD", "NC", "TN", "WV"),
    "WA" to listOf("AK", "ID", "OR"),
    "WV" to listOf("KY", "MD", "OH", "PA", "VA"),
    "WI" to listOf("IA", "IL", "MI", "MN"),
    "WY" to listOf("CO", "ID", "MT", "NE", "SD", "UT"),
)

/** How many papers from neighboring states the front page runs at once. */
const val MAX_WIRE_STORIES = 4

/** The creatures the player has a journal entry for, however well they did. */
fun metCreatures(journal: List<JournalEntry>): Set<String> = journal.map { it.creatureId }.toSet()

/** True once every creature reported in [state] has an entry in the journal. */
fun stateComplete(state: String, met: Set<String>): Boolean {
    val cases = casesIn(state)
    return cases.isNotEmpty() && cases.all { it.answerId in met }
}

/**
 * The states whose reports reach the player in [home], nearest first. Filling in a state opens up
 * its neighbors, so it spreads outward from [home] one finished state at a time: a state is reached
 * when it borders [home] or another reached state that the player has finished. [home] itself is
 * not in the list.
 */
fun reachedStates(home: String?, met: Set<String>): List<String> = reachedDistances(home, met).keys.toList()

/** [reachedStates], each with how many borders it is from [home]. */
private fun reachedDistances(home: String?, met: Set<String>): Map<String, Int> {
    if (home == null || home !in STATE_NEIGHBORS) return emptyMap()
    val distance = linkedMapOf(home to 0)
    val queue = ArrayDeque(listOf(home))
    while (queue.isNotEmpty()) {
        val state = queue.removeFirst()
        if (!stateComplete(state, met)) continue
        for (next in STATE_NEIGHBORS[state].orEmpty().sorted()) {
            if (next !in distance) {
                distance[next] = distance.getValue(state) + 1
                queue.addLast(next)
            }
        }
    }
    distance.remove(home)
    return distance
}

/**
 * The stories from neighboring states that today's paper runs alongside the local ones: cases from
 * [reachedStates] about creatures the player hasn't met yet, one per creature, nearest states first.
 * Among states the same distance away, which ones run changes with each [edition], so coming back
 * another day turns up different ones. At most [limit] are run.
 */
fun wireCases(
    home: String?,
    journal: List<JournalEntry>,
    edition: Long,
    limit: Int = MAX_WIRE_STORIES,
): List<CaseDefinition> {
    val met = metCreatures(journal)
    val local = casesIn(home).map { it.answerId }.toSet()
    val random = SeededRandom(edition xor 0x77697265L)
    return reachedDistances(home, met)
        .flatMap { (state, distance) -> casesIn(state).map { Triple(it, distance, random.nextLong()) } }
        .filter { (case, _, _) -> case.answerId !in met && case.answerId !in local }
        .sortedWith(compareBy({ it.second }, { it.third }))
        .distinctBy { it.first.answerId }
        .take(limit)
        .map { it.first }
}
