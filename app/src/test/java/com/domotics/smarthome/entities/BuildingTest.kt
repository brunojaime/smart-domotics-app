package com.domotics.smarthome.entities

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BuildingTest {

    private lateinit var location: Location

    @Before
    fun setup() {
        location = Location(latitude = 40.7128, longitude = -74.0060, reference = "NYC")
    }

    @Test
    fun `create building with all properties`() {
        val building = Building(
            name = "Smart Building",
            location = location,
            description = "A modern smart building"
        )

        assertNotNull(building.id)
        assertEquals("Smart Building", building.name)
        assertEquals(location, building.location)
        assertEquals("A modern smart building", building.description)
        assertTrue(building.zones.isEmpty())
    }

    @Test
    fun `create building without description`() {
        val building = Building(
            name = "Office Tower",
            location = location
        )

        assertNotNull(building.id)
        assertEquals("Office Tower", building.name)
        assertNull(building.description)
    }

    @Test
    fun `buildings have unique ids`() {
        val building1 = Building(name = "Building 1", location = location)
        val building2 = Building(name = "Building 2", location = location)

        assertNotEquals(building1.id, building2.id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank name throws exception`() {
        Building(name = "", location = location)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `whitespace name throws exception`() {
        Building(name = "   ", location = location)
    }

    @Test
    fun `add zone to building`() {
        val building = Building(name = "Residential Building", location = location)
        val area = Area(name = "Main Area")
        val zone = Zone(name = "First Floor", floor = 1, area = area)

        building.addZone(zone)

        assertEquals(1, building.zones.size)
        assertTrue(building.zones.contains(zone))
    }

    @Test
    fun `remove zone from building`() {
        val building = Building(name = "Commercial Building", location = location)
        val area1 = Area(name = "Area 1")
        val area2 = Area(name = "Area 2")
        val zone1 = Zone(name = "Floor 1", floor = 1, area = area1)
        val zone2 = Zone(name = "Floor 2", floor = 2, area = area2)

        building.addZone(zone1)
        building.addZone(zone2)
        assertEquals(2, building.zones.size)

        building.removeZone(zone1)
        assertEquals(1, building.zones.size)
        assertFalse(building.zones.contains(zone1))
        assertTrue(building.zones.contains(zone2))
    }

    @Test
    fun `add multiple zones to building`() {
        val building = Building(name = "Multi-Floor Building", location = location)
        val area = Area(name = "Standard Area")

        val zone1 = Zone(name = "Ground Floor", floor = 0, area = area)
        val zone2 = Zone(name = "First Floor", floor = 1, area = area)
        val zone3 = Zone(name = "Second Floor", floor = 2, area = area)

        building.addZone(zone1)
        building.addZone(zone2)
        building.addZone(zone3)

        assertEquals(3, building.zones.size)
    }

    @Test
    fun `custom id is preserved`() {
        val customId = "custom-building-id-789"
        val building = Building(
            id = customId,
            name = "Custom Building",
            location = location
        )

        assertEquals(customId, building.id)
    }

    @Test
    fun `building with different locations`() {
        val londonLocation = Location(latitude = 51.5074, longitude = -0.1278, reference = "London")
        val tokyoLocation = Location(latitude = 35.6762, longitude = 139.6503, reference = "Tokyo")

        val building1 = Building(name = "London Tower", location = londonLocation)
        val building2 = Building(name = "Tokyo Center", location = tokyoLocation)

        assertEquals(londonLocation, building1.location)
        assertEquals(tokyoLocation, building2.location)
    }
}
