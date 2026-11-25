package app.unconfirmed.shared

import kotlin.math.floor
import kotlinx.serialization.Serializable

private const val CELL_SIZE_DEGREES = 0.01
private const val MILLIS_PER_WEEK = 7L * 24 * 60 * 60 * 1000

/** A patch of the world roughly 1 km across. Everyone in it gets the same cases that week. */
@Serializable
data class Cell(val x: Int, val y: Int) {
    val center: GeoPoint
        get() = GeoPoint((y + 0.5) * CELL_SIZE_DEGREES, (x + 0.5) * CELL_SIZE_DEGREES)

    companion object {
        fun containing(point: GeoPoint) = Cell(
            x = floor(point.lon / CELL_SIZE_DEGREES).toInt(),
            y = floor(point.lat / CELL_SIZE_DEGREES).toInt(),
        )
    }
}

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

fun dayWindow(epochMillis: Long): Long = Math.floorDiv(epochMillis, MILLIS_PER_DAY)

/** Seeds the front page's headlines: a new edition every day, and again after each case taken. */
fun editionSeed(day: Long, slot: Int): Long {
    val daySeed = SeededRandom(0x6E657773L xor day).nextLong()
    return SeededRandom(daySeed xor slot.toLong()).nextLong()
}

fun weekWindow(epochMillis: Long): Long = Math.floorDiv(epochMillis, MILLIS_PER_WEEK)

/** SplitMix64, written out so results can't change with a library or platform update. */
class SeededRandom(seed: Long) {
    private var state = seed

    fun nextLong(): Long {
        state += -0x61c8864680b583ebL
        var z = state
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }

    fun nextInt(bound: Int): Int = Math.floorMod(nextLong(), bound.toLong()).toInt()

    /** A number from 0 up to but not including 1. */
    fun nextDouble(): Double = (nextLong() ushr 11) * (1.0 / (1L shl 53))
}

/** [slot] counts the cases a player has taken, so asking for another gives a new trail. */
fun caseSeed(cell: Cell, window: Long, slot: Int): Long {
    var seed = 0x756E636F6E66L
    for (value in longArrayOf(cell.x.toLong(), cell.y.toLong(), window, slot.toLong())) {
        seed = SeededRandom(seed xor value).nextLong()
    }
    return seed
}
