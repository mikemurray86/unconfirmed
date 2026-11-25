package app.unconfirmed.game

import app.unconfirmed.shared.GeoPoint
import app.unconfirmed.shared.Weather
import app.unconfirmed.shared.weatherFromMetSymbol
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Current weather from MET Norway's Locationforecast, which is free for commercial use under
 * CC BY 4.0 and needs no key. It only sets the scene's mood, so any failure just means clear skies.
 *
 * Their terms ask for a User-Agent naming the app and a contact, and for clients to respect the
 * cache: a forecast is fetched at most once per cell every [CACHE_MILLIS].
 */
object WeatherRepository {
    const val CACHE_MILLIS = 30 * 60_000L
    private const val TIMEOUT_MILLIS = 8_000

    private val userAgent = "Unconfirmed/${BuildConfig.VERSION_NAME} (${BuildConfig.GAME_CONTACT})"

    private data class Cached(val weather: Weather, val atMillis: Long)

    private val cache = mutableMapOf<Pair<Int, Int>, Cached>()

    /** About a kilometre across. The service never sees the player's position closer than that. */
    fun cellOf(point: GeoPoint): Pair<Int, Int> = (point.lat * 100).roundToInt() to (point.lon * 100).roundToInt()

    suspend fun weatherAt(point: GeoPoint, nowMillis: Long = System.currentTimeMillis()): Weather {
        // MET blocks requests without a contact, so don't send any until there is one.
        if (BuildConfig.GAME_CONTACT.isBlank()) return Weather.CLEAR
        val cell = cellOf(point)
        synchronized(cache) { cache[cell] }?.let { if (nowMillis - it.atMillis < CACHE_MILLIS) return it.weather }

        val weather = withContext(Dispatchers.IO) { runCatching { fetch(cell) }.getOrNull() } ?: return Weather.CLEAR
        synchronized(cache) { cache[cell] = Cached(weather, nowMillis) }
        return weather
    }

    private fun fetch(cell: Pair<Int, Int>): Weather {
        val url = URL(
            String.format(
                Locale.US,
                "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.2f&lon=%.2f",
                cell.first / 100.0,
                cell.second / 100.0,
            ),
        )
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.setRequestProperty("User-Agent", userAgent)
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            // The first time step is the current hour, and its next hour's summary stands for "now".
            val symbol = Json.parseToJsonElement(body).jsonObject
                .getValue("properties").jsonObject
                .getValue("timeseries").jsonArray.first().jsonObject
                .getValue("data").jsonObject
                .getValue("next_1_hours").jsonObject
                .getValue("summary").jsonObject
                .getValue("symbol_code").jsonPrimitive.content
            return weatherFromMetSymbol(symbol)
        } finally {
            connection.disconnect()
        }
    }
}
