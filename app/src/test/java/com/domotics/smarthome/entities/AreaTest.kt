package com.domotics.smarthome.entities

import org.junit.Assert.*
import org.junit.Test

class AreaTest {

    @Test
    fun `create area with all properties`() {
        val area = Area(
            name = "Living Room",
            squareMeters = 25.5
        )

        assertNotNull(area.id)
        assertEquals("Living Room", area.name)
        assertNotNull(area.squareMeters)
        assertEquals(25.5, area.squareMeters!!, 0.01)
    }

    @Test
    fun `create area without square meters`() {
        val area = Area(name = "Storage")

        assertNotNull(area.id)
        assertEquals("Storage", area.name)
        assertNull(area.squareMeters)
    }

    @Test
    fun `areas have unique ids`() {
        val area1 = Area(name = "Room 1")
        val area2 = Area(name = "Room 2")

        assertNotEquals(area1.id, area2.id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank name throws exception`() {
        Area(name = "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `whitespace name throws exception`() {
        Area(name = "   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative square meters throws exception`() {
        Area(name = "Kitchen", squareMeters = -10.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero square meters throws exception`() {
        Area(name = "Bathroom", squareMeters = 0.0)
    }

    @Test
    fun `custom id is preserved`() {
        val customId = "custom-area-id-123"
        val area = Area(id = customId, name = "Custom Area")

        assertEquals(customId, area.id)
    }
}
