package app.unconfirmed.shared

import kotlin.math.sqrt

data class LocationFix(
    val point: GeoPoint,
    val accuracyMeters: Double,
    val timeMillis: Long,
    val speedMetersPerSecond: Double? = null,
    /** Direction of travel, clockwise from north. Only set while the player is actually moving. */
    val bearingDegrees: Double? = null,
    /** One standard deviation of [speedMetersPerSecond], when the phone reports it. */
    val speedAccuracyMetersPerSecond: Double? = null,
)

/**
 * Measures distance walked and blocks play at driving speed.
 *
 * GPS positions wander by 5–40 m even when you stand still, which swamps a walk of 1.4 m/s. The
 * speed that comes with a fix is mostly measured from the satellite signals themselves, so it
 * stays accurate when the position doesn't. Distance is speed × time while that speed can be
 * trusted. Otherwise it falls back to the distance moved by a smoothed position, which has to
 * clear the noise before it counts.
 */
class MovementTracker {
    private var previous: LocationFix? = null
    private var smoothed: SmoothedPosition? = null
    private var anchor: GeoPoint? = null
    private var anchorMillis = 0L
    private var fastReadings = 0
    private var firstFastMillis = 0L
    private var lastFastMillis = 0L

    var isTooFast = false
        private set

    /** Handles a new fix and returns the metres walked since the last one. */
    fun update(fix: LocationFix): Double {
        val last = previous
        if (last != null && fix.timeMillis <= last.timeMillis) return 0.0
        updateSpeedBlock(fix, last)
        previous = fix
        if (fix.accuracyMeters > MAX_USABLE_ACCURACY_METERS) return 0.0

        val position = smoothed?.apply { update(fix) } ?: SmoothedPosition(fix).also { smoothed = it }
        val start = anchor
        if (start == null || last == null) {
            moveAnchor(position.point, fix)
            return 0.0
        }

        val seconds = (fix.timeMillis - last.timeMillis) / 1000.0
        val moved = when {
            seconds <= MAX_SPEED_GAP_SECONDS && fix.hasTrustedSpeed() -> {
                moveAnchor(position.point, fix)
                // Average with the previous speed when there is one, to follow a speed-up or stop.
                val lastSpeed = if (last.hasTrustedSpeed()) last.speedMetersPerSecond!! else fix.speedMetersPerSecond!!
                val speed = (lastSpeed + fix.speedMetersPerSecond!!) / 2
                if (speed < MIN_MOVING_SPEED || speed > MAX_PLAY_SPEED) 0.0 else speed * seconds
            }
            fix.speedMetersPerSecond != null &&
                fix.speedMetersPerSecond < MIN_MOVING_SPEED &&
                fix.speedAccuracyMetersPerSecond.let { it == null || it < MIN_MOVING_SPEED } -> {
                // A trusted reading of standing still: keep the anchor where the player stands.
                moveAnchor(position.point, fix)
                0.0
            }
            else -> {
                val distance = distanceMeters(start, position.point)
                if (distance < maxOf(MIN_COUNTED_MOVE_METERS, 2 * position.errorMeters)) return 0.0
                val sinceAnchor = (fix.timeMillis - anchorMillis) / 1000.0
                moveAnchor(position.point, fix)
                if (distance / sinceAnchor > MAX_PLAY_SPEED) 0.0 else distance
            }
        }
        return if (isTooFast) 0.0 else moved
    }

    private fun moveAnchor(point: GeoPoint, fix: LocationFix) {
        anchor = point
        anchorMillis = fix.timeMillis
    }

    /** Forgets the last position, so a debug teleport isn't counted as a journey. */
    fun reset() {
        previous = null
        smoothed = null
        anchor = null
        fastReadings = 0
        isTooFast = false
    }

    private fun updateSpeedBlock(fix: LocationFix, last: LocationFix?) {
        val measured = if (last !=
            null
        ) {
            distanceMeters(last.point, fix.point) / ((fix.timeMillis - last.timeMillis) / 1000.0)
        } else {
            0.0
        }
        val reliableMeasured = fix.accuracyMeters <= MAX_USABLE_ACCURACY_METERS &&
            (last?.accuracyMeters ?: Double.MAX_VALUE) <= MAX_USABLE_ACCURACY_METERS
        val speed = fix.speedMetersPerSecond ?: if (reliableMeasured) measured else 0.0

        if (speed > MAX_PLAY_SPEED) {
            if (fastReadings == 0) firstFastMillis = fix.timeMillis
            fastReadings += 1
            lastFastMillis = fix.timeMillis
            // Fixes come every second, so ask for fast readings over a few seconds, not just two.
            if (fastReadings >= 2 && fix.timeMillis - firstFastMillis >= CONFIRM_FAST_MILLIS) isTooFast = true
        } else {
            fastReadings = 0
            if (isTooFast && fix.timeMillis - lastFastMillis >= SLOW_DOWN_MILLIS) isTooFast = false
        }
    }

    private fun LocationFix.hasTrustedSpeed(): Boolean {
        val speed = speedMetersPerSecond ?: return false
        val error = speedAccuracyMetersPerSecond ?: return true
        return error <= MAX_SPEED_ERROR && error < speed
    }

    private companion object {
        const val MAX_PLAY_SPEED = 8.5 // Metres per second, about 30 km/h: a quick cyclist.
        const val MAX_USABLE_ACCURACY_METERS = 50.0
        const val MIN_COUNTED_MOVE_METERS = 8.0
        const val MIN_MOVING_SPEED = 0.5 // Metres per second. Slower than this is GPS noise.
        const val MAX_SPEED_ERROR = 1.5 // Metres per second.
        const val MAX_SPEED_GAP_SECONDS = 10.0 // Longer gaps fall back to the distance moved.
        const val CONFIRM_FAST_MILLIS = 3_000L
        const val SLOW_DOWN_MILLIS = 15_000L
    }
}

/**
 * A one-state Kalman filter on position. Each fix pulls the estimate towards it by how much it
 * can be trusted, and the estimate's uncertainty grows with time to allow for walking on.
 */
private class SmoothedPosition(fix: LocationFix) {
    var point = fix.point
        private set
    private var variance = fix.accuracyMeters * fix.accuracyMeters
    private var timeMillis = fix.timeMillis

    /** One standard deviation of the estimate, in metres. */
    val errorMeters: Double get() = sqrt(variance)

    fun update(fix: LocationFix) {
        val seconds = (fix.timeMillis - timeMillis).coerceAtLeast(0) / 1000.0
        timeMillis = fix.timeMillis
        variance += seconds * WALKING_SPREAD * WALKING_SPREAD
        val gain = variance / (variance + fix.accuracyMeters * fix.accuracyMeters)
        point = GeoPoint(
            point.lat + gain * (fix.point.lat - point.lat),
            point.lon + gain * (fix.point.lon - point.lon),
        )
        variance *= 1 - gain
    }

    private companion object {
        const val WALKING_SPREAD = 3.0 // Metres per second the player might have moved, unseen.
    }
}
