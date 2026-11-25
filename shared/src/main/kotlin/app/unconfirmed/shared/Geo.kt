package app.unconfirmed.shared

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.serialization.Serializable

private const val EARTH_RADIUS_METERS = 6_371_000.0
const val METERS_PER_DEGREE_LAT = 111_320.0

private fun Double.toRadians() = this * PI / 180.0

@Serializable
data class GeoPoint(val lat: Double, val lon: Double)

fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
    val dLat = (b.lat - a.lat).toRadians()
    val dLon = (b.lon - a.lon).toRadians()
    val h = sin(dLat / 2).pow(2) +
        cos(a.lat.toRadians()) * cos(b.lat.toRadians()) * sin(dLon / 2).pow(2)
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(h.coerceAtMost(1.0)))
}

/** The nearest of the eight compass points to [degrees], clockwise from north. */
fun compassPoint(degrees: Double): String {
    val points = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    return points[((degrees + 22.5) / 45.0).toInt() % points.size]
}
