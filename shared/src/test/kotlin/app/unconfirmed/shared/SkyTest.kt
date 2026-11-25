package app.unconfirmed.shared

import java.time.Instant
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SkyTest {
    private val missoula = GeoPoint(46.87, -113.99)

    private fun at(iso: String) = Instant.parse(iso).toEpochMilli()

    private fun assertNear(expected: Double, actual: Double, tolerance: Double = 1.0) =
        assertTrue(abs(expected - actual) <= tolerance, "expected about $expected, got $actual")

    @Test
    fun `midsummer noon puts the sun high over Missoula`() {
        // Local solar noon in Missoula is about 19:36 UTC.
        val sun = sunPosition(missoula, at("2026-06-21T19:36:00Z"))
        assertNear(90 - 46.87 + 23.44, sun.elevationDegrees)
    }

    @Test
    fun `midsummer midnight keeps the sun below the horizon`() {
        val sun = sunPosition(missoula, at("2026-06-21T07:36:00Z"))
        assertNear(-(90 - 46.87 - 23.44), sun.elevationDegrees)
        assertEquals(TimeOfDay.NIGHT, timeOfDay(sun))
    }

    @Test
    fun `equinox noon has the sun at the complement of the latitude`() {
        val sun = sunPosition(missoula, at("2026-03-20T19:43:00Z"))
        assertNear(90 - 46.87, sun.elevationDegrees)
        assertEquals(TimeOfDay.DAY, timeOfDay(sun))
    }

    @Test
    fun `morning twilight is dawn and evening twilight is dusk`() {
        assertEquals(TimeOfDay.DAWN, timeOfDay(SunPosition(2.0, afternoon = false)))
        assertEquals(TimeOfDay.DUSK, timeOfDay(SunPosition(2.0, afternoon = true)))
        assertEquals(TimeOfDay.DUSK, timeOfDay(SunPosition(-5.9, afternoon = true)))
        assertEquals(TimeOfDay.NIGHT, timeOfDay(SunPosition(-6.1, afternoon = true)))
        assertEquals(TimeOfDay.DAY, timeOfDay(SunPosition(6.0, afternoon = false)))
    }

    @Test
    fun `a real summer evening in Missoula reads as dusk`() {
        // Sunset is around 03:50 UTC the next day.
        assertEquals(TimeOfDay.DUSK, timeOfDay(missoula, at("2026-06-22T03:55:00Z")))
        assertEquals(TimeOfDay.DAWN, timeOfDay(missoula, at("2026-06-21T11:50:00Z")))
    }

    @Test
    fun `MET symbol codes map to what the scene can show`() {
        assertEquals(Weather.CLEAR, weatherFromMetSymbol("clearsky_day"))
        assertEquals(Weather.CLEAR, weatherFromMetSymbol("fair_night"))
        assertEquals(Weather.CLOUDY, weatherFromMetSymbol("partlycloudy_polartwilight"))
        assertEquals(Weather.CLOUDY, weatherFromMetSymbol("cloudy"))
        assertEquals(Weather.FOG, weatherFromMetSymbol("fog"))
        assertEquals(Weather.DRIZZLE, weatherFromMetSymbol("lightrain"))
        assertEquals(Weather.DRIZZLE, weatherFromMetSymbol("lightrainshowers_day"))
        assertEquals(Weather.RAIN, weatherFromMetSymbol("heavyrainshowers_night"))
        assertEquals(Weather.RAIN, weatherFromMetSymbol("sleet"))
        assertEquals(Weather.SNOW, weatherFromMetSymbol("lightsnowshowers_day"))
        assertEquals(Weather.STORM, weatherFromMetSymbol("lightrainandthunder"))
        assertEquals(Weather.STORM, weatherFromMetSymbol("heavysnowshowersandthunder_night"))
        assertEquals(Weather.CLEAR, weatherFromMetSymbol("something_new"))
    }
}
