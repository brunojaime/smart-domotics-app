package com.domotics.smarthome.entities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildingTest {

    @Test
    fun `create building with minimal properties`() {
        val building = Building(
            name = "Smart Building",
            locationId = "loc-1",
        )

        assertNotEquals("", building.id)
        assertEquals("Smart Building", building.name)
        assertEquals("loc-1", building.locationId)
        assertTrue(building.zoneIds.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank name throws exception`() {
        Building(name = "", locationId = "loc-1")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank location id throws exception`() {
        Building(name = "Building", locationId = " ")
    }

    @Test
    fun `buildings have unique ids`() {
        val building1 = Building(name = "Building 1", locationId = "loc-1")
        val building2 = Building(name = "Building 2", locationId = "loc-1")

        assertNotEquals(building1.id, building2.id)
    }

    @Test
    fun `add and remove zones`() {
        val building = Building(name = "Residential Building", locationId = "loc-1")

        building.addZone("zone-1")
        building.addZone("zone-2")
        assertEquals(2, building.zoneIds.size)

        building.removeZone("zone-1")
        assertEquals(1, building.zoneIds.size)
        assertFalse(building.zoneIds.contains("zone-1"))
        assertTrue(building.zoneIds.contains("zone-2"))
    }

    @Test
    fun `custom id is preserved`() {
        val customId = "custom-building-id-789"
        val building = Building(
            id = customId,
            name = "Custom Building",
            locationId = "loc-2",
        )

        assertEquals(customId, building.id)
    }
}
