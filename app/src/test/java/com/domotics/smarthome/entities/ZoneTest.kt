package com.domotics.smarthome.entities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoneTest {

    @Test
    fun `create zone with defaults`() {
        val zone = Zone(name = "First Floor", buildingId = "b1")

        assertNotEquals("", zone.id)
        assertEquals("First Floor", zone.name)
        assertEquals("b1", zone.buildingId)
        assertTrue(zone.areaIds.isEmpty())
        assertTrue(zone.devices.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank name throws exception`() {
        Zone(name = "", buildingId = "b1")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank building id throws exception`() {
        Zone(name = "Zone", buildingId = " ")
    }

    @Test
    fun `add and remove devices`() {
        val device = object : Device(name = "Sensor") {}
        val zone = Zone(name = "Ground", buildingId = "b1")

        zone.addDevice(device)
        assertEquals(1, zone.devices.size)

        zone.removeDevice(device)
        assertTrue(zone.devices.isEmpty())
    }
}
