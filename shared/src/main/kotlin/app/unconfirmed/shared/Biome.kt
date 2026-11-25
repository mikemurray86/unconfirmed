package app.unconfirmed.shared

import kotlinx.serialization.Serializable

/** The kind of country a scene shows. It sets the atmosphere drawn over the scene. */
enum class Biome { FOREST, WETLAND, WATERSIDE, URBAN, GRASSLAND, ROCKY, OPEN }

/**
 * The place a case is set in, drawn as the first-person view. It follows the lead, not the answer,
 * so the scenery never gives the deduction away.
 */
enum class Scene(val biome: Biome) {
    LAKE_SHORE(Biome.WATERSIDE),
    TIMBER(Biome.FOREST),
    PASTURE(Biome.GRASSLAND),
    RIDGE(Biome.ROCKY),
    RIVER_TOWN(Biome.URBAN),
    PINE_BARRENS(Biome.WETLAND),
}

/**
 * The part of the country a case is in, which sets what the scenery looks like: the skyline, the
 * trees and the colour of the land. The [Scene] says what kind of place it is; this says where.
 */
@Serializable
enum class Region {
    /** Conifers and mountains: the Pacific Northwest, California and the northern Rockies. */
    NORTHWEST,

    /** Mesas, junipers and sagebrush. */
    DESERT,

    /** Flat grassland, with cottonwoods along the creeks. */
    PLAINS,

    /** Farm country and hardwood woods round the Great Lakes. */
    MIDWEST,

    /** Hardwood forest over long, rolling ridges: Appalachia, the Mid-Atlantic and New England. */
    EAST,

    /** Live oaks hung with moss, southern pines and palmettos. */
    SOUTH,

    /** Spruce, tundra and snow-capped peaks. */
    ALASKA,

    /** Palms, lush green and volcanic mountains above the sea. */
    HAWAII,
}

/** The region a US state's scenery is drawn from. Anywhere unknown looks like the Northwest. */
fun regionOf(state: String?): Region = when (state) {
    "AZ", "NM", "NV", "UT" -> Region.DESERT
    "ND", "SD", "NE", "KS", "OK", "TX" -> Region.PLAINS
    "MN", "WI", "MI", "IL", "IN", "OH", "IA", "MO" -> Region.MIDWEST
    "ME", "NH", "VT", "MA", "RI", "CT", "NY", "NJ", "PA", "DE", "MD", "VA", "WV", "KY", "TN", "NC" -> Region.EAST
    "SC", "GA", "FL", "AL", "MS", "LA", "AR" -> Region.SOUTH
    "AK" -> Region.ALASKA
    "HI" -> Region.HAWAII
    else -> Region.NORTHWEST
}

/**
 * The region at [point] in [state]. A few states are split where their country changes, using rough
 * lines of longitude and latitude: the Mojave and the low deserts of southern California, the
 * piney woods of East Texas and the desert of the Trans-Pecos, Oregon's high desert, the mesas of
 * western Colorado and the plains east of the Front Range, and the eastern plains of Montana,
 * Wyoming and New Mexico. Without a point, it's the state's usual region.
 *
 * Only the region is ever kept: the point never leaves this function.
 */
fun regionAt(state: String?, point: GeoPoint?): Region {
    val usual = regionOf(state)
    if (point == null) return usual
    val lat = point.lat
    val lon = point.lon
    return when (state) {
        "CA" -> if ((lon > -118.0 && lat < 37.5) || (lon > -116.7 && lat < 34.3)) Region.DESERT else usual
        "TX" -> when {
            lon > -95.8 && lat > 29.6 -> Region.SOUTH
            lon < -102.0 && lat < 32.1 -> Region.DESERT
            else -> usual
        }
        "OR" -> if (lon > -121.3 && lat < 44.8) Region.DESERT else usual
        "CO" -> when {
            lon < -107.6 -> Region.DESERT
            lon > -104.6 -> Region.PLAINS
            else -> usual
        }
        "MT" -> if (lon > -109.5) Region.PLAINS else usual
        "WY" -> if (lon > -105.3) Region.PLAINS else usual
        "NM" -> if (lon > -103.8) Region.PLAINS else usual
        else -> usual
    }
}
