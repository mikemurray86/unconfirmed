package app.unconfirmed.shared

/** The fewest suspects a state's field guide needs before its cases are offered. */
const val MIN_FIELD_GUIDE = 4

/**
 * A creature reported across a whole region rather than in one state, like the lumberjacks'
 * fearsome critters across logging country. It joins the field guide of every state in [range],
 * where it can be the answer to a case or a wrong guess in any other.
 */
class RegionalCreature(val suspect: Suspect, val range: Set<String>)

/**
 * The field guide for [state]: its own [local] creatures, then every [regional] one whose range
 * takes in [state]. A state with its own entry for a creature keeps that, so local lore wins.
 */
fun fieldGuide(state: String, local: List<Suspect>, regional: List<RegionalCreature>): List<Suspect> =
    (local + regional.filter { state in it.range }.map { it.suspect }).distinctBy { it.id }

/**
 * A case that can be reported anywhere in [creature]'s range. It runs as a separate case in each
 * state, against that state's field guide, so its words mustn't name a place. Each copy's id is
 * this [id] with the state on the end, and they share [id] as their story.
 */
class RegionalStory(
    val id: String,
    val creature: RegionalCreature,
    val scene: Scene,
    val place: String? = null,
    val headlines: List<String>,
    val teasers: List<String>,
    val lead: String,
    val clues: List<Clue>,
    val solvedNote: String,
) {
    /** One case for each state in range whose field guide in [guides] is big enough to play. */
    fun cases(guides: Map<String, List<Suspect>>): List<CaseDefinition> = creature.range.sorted().mapNotNull { state ->
        val guide = guides[state]?.takeIf { it.size >= MIN_FIELD_GUIDE } ?: return@mapNotNull null
        CaseDefinition(
            id = "$id-${state.lowercase()}",
            story = id,
            state = state,
            scene = scene,
            place = place,
            headlines = headlines,
            teasers = teasers,
            lead = lead,
            clues = clues,
            suspects = guide,
            answerId = creature.suspect.id,
            solvedNote = solvedNote,
        )
    }
}

/**
 * Every state's field guide: the [local] guides as they are, plus a guide for any state that only
 * [regional] creatures reach.
 */
fun allFieldGuides(local: Map<String, List<Suspect>>, regional: List<RegionalCreature>): Map<String, List<Suspect>> {
    val states = local.keys + regional.flatMap { it.range }
    return states.associateWith { state -> fieldGuide(state, local[state].orEmpty(), regional) }
}
