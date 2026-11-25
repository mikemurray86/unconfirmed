package app.unconfirmed.game

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import app.unconfirmed.shared.GeoPoint
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

sealed interface StateLookup {
    /** [code] is the two-letter US state, or null when the point is outside the United States. */
    data class Found(val code: String?) : StateLookup

    /** No geocoder on this phone, no connection, or it gave no answer. */
    data object Unavailable : StateLookup
}

/**
 * Works out which US state a point is in with the phone's own reverse geocoder. Most phones need
 * a connection for this. The caller caches the answer, so a lookup only happens on moving into a
 * new cell.
 */
class StateLocator(private val context: Context) {
    suspend fun lookUp(point: GeoPoint): StateLookup {
        if (!Geocoder.isPresent()) return StateLookup.Unavailable
        val geocoder = Geocoder(context, Locale.US)
        val address = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                lookUpAsync(geocoder, point)
            } else {
                lookUpBlocking(geocoder, point)
            }
        } catch (_: Exception) {
            return StateLookup.Unavailable
        } ?: return StateLookup.Unavailable
        if (address.countryCode != "US") return StateLookup.Found(null)
        val code = address.adminArea?.let(::stateCode) ?: return StateLookup.Unavailable
        return StateLookup.Found(code)
    }

    @Suppress("DEPRECATION")
    private suspend fun lookUpBlocking(geocoder: Geocoder, point: GeoPoint): Address? = withContext(Dispatchers.IO) {
        geocoder.getFromLocation(point.lat, point.lon, 1)?.firstOrNull()
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun lookUpAsync(geocoder: Geocoder, point: GeoPoint): Address? =
        suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocation(
                point.lat,
                point.lon,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (continuation.isActive) continuation.resume(addresses.firstOrNull())
                    }

                    override fun onError(errorMessage: String?) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                },
            )
        }
}

/** Geocoders usually give the full state name, but some give the code itself. */
internal fun stateCode(adminArea: String): String? {
    val trimmed = adminArea.trim()
    if (trimmed.length == 2 && trimmed.uppercase() in US_STATES.values) return trimmed.uppercase()
    return US_STATES[trimmed.lowercase(Locale.US)]
}

private val US_STATES = mapOf(
    "alabama" to "AL", "alaska" to "AK", "arizona" to "AZ", "arkansas" to "AR", "california" to "CA",
    "colorado" to "CO", "connecticut" to "CT", "delaware" to "DE", "district of columbia" to "DC",
    "florida" to "FL", "georgia" to "GA", "hawaii" to "HI", "idaho" to "ID", "illinois" to "IL",
    "indiana" to "IN", "iowa" to "IA", "kansas" to "KS", "kentucky" to "KY", "louisiana" to "LA",
    "maine" to "ME", "maryland" to "MD", "massachusetts" to "MA", "michigan" to "MI", "minnesota" to "MN",
    "mississippi" to "MS", "missouri" to "MO", "montana" to "MT", "nebraska" to "NE", "nevada" to "NV",
    "new hampshire" to "NH", "new jersey" to "NJ", "new mexico" to "NM", "new york" to "NY",
    "north carolina" to "NC", "north dakota" to "ND", "ohio" to "OH", "oklahoma" to "OK", "oregon" to "OR",
    "pennsylvania" to "PA", "rhode island" to "RI", "south carolina" to "SC", "south dakota" to "SD",
    "tennessee" to "TN", "texas" to "TX", "utah" to "UT", "vermont" to "VT", "virginia" to "VA",
    "washington" to "WA", "west virginia" to "WV", "wisconsin" to "WI", "wyoming" to "WY",
)
