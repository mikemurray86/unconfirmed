package app.unconfirmed.shared

import kotlin.math.exp
import kotlinx.serialization.Serializable

/**
 * Bait the player finds lying on the ground while they search, and can set out to draw the
 * creature in. The right lure for the creature warms the case and the wrong one cools it, by half
 * what a guess would: see [heatOf]. Like a guess, it never says which it was.
 */
@Serializable
enum class Lure(val title: String, val note: String, val used: String) {
    KNOCKING_STICK(
        "Knocking stick",
        "Rap it on a trunk. Some things knock back.",
        "You knock three times on a trunk, and listen.",
    ),
    RAW_MEAT(
        "Raw meat",
        "Leave it out for something that hunts.",
        "You leave the meat out and step back.",
    ),
    FISH(
        "Fish",
        "Leave it by the water.",
        "You leave the fish at the water's edge.",
    ),
    LANTERN(
        "Lantern",
        "Hang it up and let the light carry.",
        "You hang the lantern and let it burn.",
    ),
    WHISTLE(
        "Whistle",
        "A thin call that carries. Some odd things answer.",
        "You blow the whistle, long and thin.",
    ),
}

/** What each lure adds when it suits the creature, and takes away when it doesn't: half a guess. */
const val HEAT_PER_RIGHT_LURE = HEAT_PER_RIGHT_GUESS / 2
const val HEAT_PER_WRONG_LURE = HEAT_PER_WRONG_GUESS / 2

/** On average, how far the player goes before coming across bait. */
const val MEAN_BAIT_METERS = 220.0

/** How much bait the player can carry at once. More won't turn up until some is used. */
const val MAX_BAIT_CARRIED = 3

/** Bait left lying is lost once the player has gone this far past it. */
const val BAIT_LEFT_BEHIND_METERS = 40.0

/**
 * Which lure draws each creature in, by its id. Big upright walkers and knockers answer a stick,
 * hunters come to meat, anything in the water to fish, night fliers to a light, and the odd ones
 * out to a whistle.
 */
val CREATURE_LURES: Map<String, Lure> = buildMap {
    listOf(
        "bigfoot",
        "mogollon-monster",
        "prairie-giant",
        "tommyknocker",
        "menehune",
        "goatman",
        "lake-worth-monster",
        "billiwhack-monster",
    ).forEach { put(it, Lure.KNOCKING_STICK) }
    listOf(
        "chupacabra",
        "dogman",
        "black-panther",
        "island-big-cat",
        "ringdocus",
        "gumberoo",
        "hoop-snake",
        "thunderbird",
    ).forEach { put(it, Lure.RAW_MEAT) }
    listOf(
        "bear-lake-monster", "blacktail-monster", "cadborosaurus", "champ", "flathead-lake-monster",
        "iliamna-lake-monster", "kraken", "sea-serpent", "smetty", "south-bay-bessie", "tahoe-tessie",
        "lake-chelan-monster", "caiman", "wimpus",
    ).forEach { put(it, Lure.FISH) }
    listOf("batsquatch", "jersey-devil", "mothman", "winged-monster", "fresno-nightcrawler")
        .forEach { put(it, Lure.LANTERN) }
    listOf("jackalope", "dark-watchers", "red-ghost", "hidebehind").forEach { put(it, Lure.WHISTLE) }
}

/** Whether covering [movedMeters] more turns up bait. [roll] is a random number from 0 to 1. */
fun baitTurnsUp(movedMeters: Double, roll: Double): Boolean =
    movedMeters > 0.0 && roll < 1 - exp(-movedMeters / MEAN_BAIT_METERS)

/** Which lure is lying there. Any of them can turn up, whatever the case. [roll] is from 0 to 1. */
fun lureAt(roll: Double): Lure = Lure.entries[(roll * Lure.entries.size).toInt().coerceIn(0, Lure.entries.size - 1)]

/** Whether the player has room to pick up more bait. */
fun canCarryMoreBait(case: ActiveCase): Boolean = case.bait.size < MAX_BAIT_CARRIED
