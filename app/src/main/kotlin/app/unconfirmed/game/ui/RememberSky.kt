package app.unconfirmed.game.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.unconfirmed.game.WeatherRepository
import app.unconfirmed.shared.GeoPoint
import app.unconfirmed.shared.SkyState
import app.unconfirmed.shared.TimeOfDay
import app.unconfirmed.shared.Weather
import app.unconfirmed.shared.timeOfDay
import kotlinx.coroutines.delay

private const val CLOCK_TICK_MILLIS = 60_000L

/**
 * The sky over [point]: time of day from the sun's position, and the current weather. The
 * overrides let debug builds preview every combination without waiting for one.
 */
@Composable
fun rememberSky(point: GeoPoint?, timeOverride: TimeOfDay? = null, weatherOverride: Weather? = null): SkyState {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(CLOCK_TICK_MILLIS)
            now = System.currentTimeMillis()
        }
    }

    var weather by remember { mutableStateOf(Weather.CLEAR) }
    val cell = point?.let { WeatherRepository.cellOf(it) }
    // Asks again when the player moves into a new cell or the last answer has gone stale.
    LaunchedEffect(cell, now / WeatherRepository.CACHE_MILLIS) {
        if (point != null) weather = WeatherRepository.weatherAt(point, now)
    }

    return SkyState(
        timeOfDay = timeOverride ?: point?.let { timeOfDay(it, now) } ?: TimeOfDay.DAY,
        weather = weatherOverride ?: weather,
    )
}
