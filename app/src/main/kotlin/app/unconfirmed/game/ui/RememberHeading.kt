package app.unconfirmed.game.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.atan2
import kotlin.math.hypot

/** How much of each new reading to take. Lower is steadier but slower to follow a turn. */
private const val SMOOTHING = 0.15f

/** Below this, the back of the phone is pointing too far up or down to give a heading. */
private const val MIN_LEVEL = 0.45f

/**
 * Which way the player is facing, clockwise from north, from the phone's rotation sensor. Held up
 * like a camera, it's the way the back of the phone points. Held flat, it's the way the top points.
 * Only listens while the screen is resumed. Phones with no rotation sensor fall back to
 * [fallbackDegrees], normally the GPS direction of travel, and null means nobody knows yet.
 */
@Composable
fun rememberHeading(fallbackDegrees: Double?): Double? {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val sensors = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val rotation = remember { sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    var heading by remember { mutableStateOf<Double?>(null) }

    DisposableEffect(lifecycle, rotation) {
        if (rotation == null) return@DisposableEffect onDispose {}
        val matrix = FloatArray(9)
        // Smoothed as a direction, not an angle, so turning through north doesn't spin it round.
        var east = 0f
        var north = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
                // Columns of the matrix are the phone's axes in world terms: x east, y north, z up.
                var x = -matrix[2]
                var y = -matrix[5]
                if (hypot(x, y) < MIN_LEVEL) {
                    x = matrix[1]
                    y = matrix[4]
                }
                val length = hypot(x, y).takeIf { it > 0f } ?: return
                east += (x / length - east) * SMOOTHING
                north += (y / length - north) * SMOOTHING
                heading = (Math.toDegrees(atan2(east, north).toDouble()) + 360.0) % 360.0
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME ->
                    sensors.registerListener(listener, rotation, SensorManager.SENSOR_DELAY_UI)
                Lifecycle.Event.ON_PAUSE -> sensors.unregisterListener(listener)
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            sensors.unregisterListener(listener)
        }
    }
    return if (rotation == null) fallbackDegrees else heading
}

/** The signed turn from [from] to [to], in degrees from -180 to 180. */
fun turnDegrees(from: Double, to: Double): Double {
    val diff = (to - from) % 360.0
    return when {
        diff > 180.0 -> diff - 360.0
        diff < -180.0 -> diff + 360.0
        else -> diff
    }
}
