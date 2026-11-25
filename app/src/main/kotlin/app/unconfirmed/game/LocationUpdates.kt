package app.unconfirmed.game

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import app.unconfirmed.shared.GeoPoint
import app.unconfirmed.shared.LocationFix
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// Every second: walking is slow, so more fixes help the tracker see past the GPS noise.
private const val UPDATE_INTERVAL_MILLIS = 1_000L
private const val FASTEST_INTERVAL_MILLIS = 500L

/**
 * Foreground location only: updates stop when collection stops. The caller ties collection
 * to the screen being visible, and must already hold the fine location permission.
 */
@SuppressLint("MissingPermission")
fun locationUpdates(context: Context): Flow<LocationFix> = callbackFlow {
    val client = LocationServices.getFusedLocationProviderClient(context)
    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MILLIS)
        .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MILLIS)
        .setMinUpdateDistanceMeters(0f)
        .setGranularity(Granularity.GRANULARITY_FINE)
        .build()
    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { trySend(it.toFix()) }
        }
    }
    client.requestLocationUpdates(request, callback, Looper.getMainLooper())
    awaitClose { client.removeLocationUpdates(callback) }
}

private fun Location.toFix() = LocationFix(
    point = GeoPoint(latitude, longitude),
    accuracyMeters = if (hasAccuracy()) accuracy.toDouble() else Double.MAX_VALUE,
    timeMillis = time,
    speedMetersPerSecond = if (hasSpeed()) speed.toDouble() else null,
    // Standing still, the bearing is noise, so the map shows no direction.
    bearingDegrees = if (hasBearing() && hasSpeed() && speed >= MIN_HEADING_SPEED) bearing.toDouble() else null,
    speedAccuracyMetersPerSecond = if (hasSpeedAccuracy()) speedAccuracyMetersPerSecond.toDouble() else null,
)

private const val MIN_HEADING_SPEED = 0.5f
