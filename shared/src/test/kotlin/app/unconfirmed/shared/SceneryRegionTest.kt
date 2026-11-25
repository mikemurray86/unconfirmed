package app.unconfirmed.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class SceneryRegionTest {
    @Test
    fun statesHaveTheirUsualRegion() {
        assertEquals(Region.DESERT, regionOf("AZ"))
        assertEquals(Region.SOUTH, regionOf("AL"))
        assertEquals(Region.EAST, regionOf("WV"))
        assertEquals(Region.ALASKA, regionOf("AK"))
        assertEquals(Region.HAWAII, regionOf("HI"))
        assertEquals(Region.NORTHWEST, regionOf(null))
    }

    @Test
    fun withoutAPointAStateKeepsItsUsualRegion() {
        assertEquals(Region.PLAINS, regionAt("TX", null))
        assertEquals(Region.NORTHWEST, regionAt("CA", null))
    }

    @Test
    fun aPointSplitsStatesWhereTheCountryChanges() {
        // Lufkin is in the piney woods, El Paso in the desert, Amarillo on the plains.
        assertEquals(Region.SOUTH, regionAt("TX", GeoPoint(31.34, -94.73)))
        assertEquals(Region.DESERT, regionAt("TX", GeoPoint(31.76, -106.49)))
        assertEquals(Region.PLAINS, regionAt("TX", GeoPoint(35.22, -101.83)))
        // Barstow and Palm Springs are desert. Eureka and Sacramento aren't.
        assertEquals(Region.DESERT, regionAt("CA", GeoPoint(34.9, -117.02)))
        assertEquals(Region.DESERT, regionAt("CA", GeoPoint(33.83, -116.55)))
        assertEquals(Region.NORTHWEST, regionAt("CA", GeoPoint(40.8, -124.16)))
        assertEquals(Region.NORTHWEST, regionAt("CA", GeoPoint(38.58, -121.49)))
        // Bend is high desert. Portland isn't.
        assertEquals(Region.DESERT, regionAt("OR", GeoPoint(44.06, -121.31 + 0.02)))
        assertEquals(Region.NORTHWEST, regionAt("OR", GeoPoint(45.52, -122.68)))
        // Grand Junction is mesa country, Denver's at the edge of the plains, and Aspen's in the mountains.
        assertEquals(Region.DESERT, regionAt("CO", GeoPoint(39.06, -108.55)))
        assertEquals(Region.PLAINS, regionAt("CO", GeoPoint(39.74, -104.5)))
        assertEquals(Region.NORTHWEST, regionAt("CO", GeoPoint(39.19, -106.82)))
        // Billings and beyond are plains. Missoula isn't.
        assertEquals(Region.PLAINS, regionAt("MT", GeoPoint(46.4, -105.84)))
        assertEquals(Region.NORTHWEST, regionAt("MT", GeoPoint(46.87, -113.99)))
    }
}
