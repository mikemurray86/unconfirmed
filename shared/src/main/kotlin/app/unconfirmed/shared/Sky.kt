package app.unconfirmed.shared

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.serialization.Serializable

/** Where the sun is in the player's day. Only changes how the map looks. */
@Serializable
enum class TimeOfDay { NIGHT, DAWN, DAY, DUSK }

/** The sky over the player, as the map's atmosphere shows it. */
@Serializable
enum class Weather { CLEAR, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, STORM }

data class SkyState(val timeOfDay: TimeOfDay = TimeOfDay.DAY, val weather: Weather = Weather.CLEAR)

/** The sun's height above the horizon and whether it's past its highest point for the day. */
data class SunPosition(val elevationDegrees: Double, val afternoon: Boolean)

private const val MILLIS_PER_DAY = 86_400_000.0
private const val J2000 = 2451545.0
private const val UNIX_EPOCH_JULIAN = 2440587.5

/** Twilight starts here, a little after the sun has fully set. */
private const val TWILIGHT_DEGREES = 6.0

private fun Double.mod360(): Double = ((this % 360) + 360) % 360

private fun Double.radians(): Double = Math.toRadians(this)

/**
 * The sun's position from the low-precision formula in the Astronomical Almanac. It's within
 * about a degree, which is far more than a change of map colours needs.
 */
fun sunPosition(point: GeoPoint, epochMillis: Long): SunPosition {
    val n = epochMillis / MILLIS_PER_DAY + UNIX_EPOCH_JULIAN - J2000
    val meanLongitude = (280.460 + 0.9856474 * n).mod360()
    val meanAnomaly = (357.528 + 0.9856003 * n).mod360().radians()
    val eclipticLongitude =
        (meanLongitude + 1.915 * sin(meanAnomaly) + 0.020 * sin(2 * meanAnomaly)).radians()
    val obliquity = (23.439 - 0.0000004 * n).radians()

    val declination = asin(sin(obliquity) * sin(eclipticLongitude))
    val rightAscension =
        Math.toDegrees(atan2(cos(obliquity) * sin(eclipticLongitude), cos(eclipticLongitude)))
    val siderealDegrees = (280.46061837 + 360.98564736629 * n + point.lon).mod360()
    // From -180 to 180: negative before local noon, positive after.
    val hourAngle = (siderealDegrees - rightAscension + 180).mod360() - 180

    val lat = point.lat.radians()
    val elevation = asin(
        sin(lat) * sin(declination) + cos(lat) * cos(declination) * cos(hourAngle.radians()),
    )
    return SunPosition(Math.toDegrees(elevation), afternoon = hourAngle > 0)
}

fun timeOfDay(sun: SunPosition): TimeOfDay = when {
    sun.elevationDegrees < -TWILIGHT_DEGREES -> TimeOfDay.NIGHT
    sun.elevationDegrees < TWILIGHT_DEGREES -> if (sun.afternoon) TimeOfDay.DUSK else TimeOfDay.DAWN
    else -> TimeOfDay.DAY
}

fun timeOfDay(point: GeoPoint, epochMillis: Long): TimeOfDay = timeOfDay(sunPosition(point, epochMillis))

/**
 * Turns a MET Norway symbol code, such as "lightrainshowers_day" or "heavysnowandthunder", into
 * the weather the scene can show. MET has no drizzle, so light rain stands in for it, and sleet
 * shows as rain.
 */
fun weatherFromMetSymbol(code: String): Weather {
    val symbol = code.substringBefore('_')
    return when {
        "thunder" in symbol -> Weather.STORM
        "snow" in symbol -> Weather.SNOW
        symbol.startsWith("lightrain") -> Weather.DRIZZLE
        "rain" in symbol || "sleet" in symbol -> Weather.RAIN
        symbol == "fog" -> Weather.FOG
        symbol == "cloudy" || symbol == "partlycloudy" -> Weather.CLOUDY
        else -> Weather.CLEAR
    }
}
