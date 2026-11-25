package app.unconfirmed.shared

/** Each hint turns at least this far from the last one, so the trail feels like it wanders. */
const val MIN_TURN_DEGREES = 60.0

/**
 * A suggested heading, clockwise from north, for each of a case's clues. They're only a
 * suggestion: a clue turns up after walking far enough in any direction, so the game never sends
 * anyone towards a particular spot.
 */
fun suggestedBearings(seed: Long, count: Int): List<Double> {
    val random = SeededRandom(seed xor 0x747261696CL)
    val bearings = mutableListOf<Double>()
    var heading = random.nextDouble() * 360.0
    repeat(count) {
        bearings += heading
        val turn = MIN_TURN_DEGREES + random.nextDouble() * (360.0 - 2 * MIN_TURN_DEGREES)
        heading = (heading + turn) % 360.0
    }
    return bearings
}

/** The direction hint for the next clue, or null once every clue is found. */
fun nextHint(case: ActiveCase): Double? = case.bearings.getOrNull(case.cluesFound)
