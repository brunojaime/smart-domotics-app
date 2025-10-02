package com.domotics.smarthome.entities

import org.junit.Assert.*
import org.junit.Test

class LocationTest {

    @Test
    fun `create location with valid coordinates`() {
        val location = Location(
            latitude = 40.7128,
            longitude = -74.0060,
            reference = "New York City"
        )

        assertEquals(40.7128, location.latitude, 0.0001)
        assertEquals(-74.0060, location.longitude, 0.0001)
        assertEquals("New York City", location.reference)
    }

    @Test
    fun `create location without reference`() {
        val location = Location(
            latitude = 51.5074,
            longitude = -0.1278
        )

        assertEquals(51.5074, location.latitude, 0.0001)
        assertEquals(-0.1278, location.longitude, 0.0001)
        assertNull(location.reference)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `latitude above 90 throws exception`() {
        Location(latitude = 91.0, longitude = 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `latitude below -90 throws exception`() {
        Location(latitude = -91.0, longitude = 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `longitude above 180 throws exception`() {
        Location(latitude = 0.0, longitude = 181.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `longitude below -180 throws exception`() {
        Location(latitude = 0.0, longitude = -181.0)
    }

    @Test
    fun `edge case latitude values are valid`() {
        val northPole = Location(latitude = 90.0, longitude = 0.0)
        val southPole = Location(latitude = -90.0, longitude = 0.0)

        assertEquals(90.0, northPole.latitude, 0.0001)
        assertEquals(-90.0, southPole.latitude, 0.0001)
    }

    @Test
    fun `edge case longitude values are valid`() {
        val eastEdge = Location(latitude = 0.0, longitude = 180.0)
        val westEdge = Location(latitude = 0.0, longitude = -180.0)

        assertEquals(180.0, eastEdge.longitude, 0.0001)
        assertEquals(-180.0, westEdge.longitude, 0.0001)
    }
}
