package com.domotics.smarthome.entities

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ZoneTest {

    private lateinit var area: Area

    @Before
    fun setup() {
        area = Area(name = "Main Area", squareMeters = 50.0)
    }

    @Test
    fun `create zone with all properties`() {
        val zone = Zone(
            name = "First Floor",
            floor = 1,
            area = area,
            zoneType = ZoneType.RESIDENTIAL
        )

        assertNotNull(zone.id)
        assertEquals("First Floor", zone.name)
        assertEquals(1, zone.floor)
        assertEquals(area, zone.area)
        assertEquals(ZoneType.RESIDENTIAL, zone.zoneType)
        assertTrue(zone.devices.isEmpty())
    }

    @Test
    fun `create zone without zone type`() {
        val zone = Zone(
            name = "Basement",
            floor = -1,
            area = area
        )

        assertNotNull(zone.id)
        assertEquals("Basement", zone.name)
        assertEquals(-1, zone.floor)
        assertNull(zone.zoneType)
    }

    @Test
    fun `zones have unique ids`() {
        val zone1 = Zone(name = "Zone 1", floor = 1, area = area)
        val zone2 = Zone(name = "Zone 2", floor = 2, area = area)

        assertNotEquals(zone1.id, zone2.id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank name throws exception`() {
        Zone(name = "", floor = 1, area = area)
    }

    @Test
    fun `add device to zone`() {
        val zone = Zone(name = "Office", floor = 2, area = area)
        val light = Lighting(name = "Ceiling Light")

        zone.addDevice(light)

        assertEquals(1, zone.devices.size)
        assertTrue(zone.devices.contains(light))
    }

    @Test
    fun `remove device from zone`() {
        val zone = Zone(name = "Kitchen", floor = 1, area = area)
        val light = Lighting(name = "Kitchen Light")
        val sensor = Sensor(name = "Motion Sensor", sensorType = SensorType.MOTION)

        zone.addDevice(light)
        zone.addDevice(sensor)
        assertEquals(2, zone.devices.size)

        zone.removeDevice(light)
        assertEquals(1, zone.devices.size)
        assertFalse(zone.devices.contains(light))
        assertTrue(zone.devices.contains(sensor))
    }

    @Test
    fun `add multiple devices to zone`() {
        val zone = Zone(name = "Living Room", floor = 1, area = area)
        val light1 = Lighting(name = "Light 1")
        val light2 = Lighting(name = "Light 2")
        val sensor = Sensor(name = "Temperature Sensor", sensorType = SensorType.TEMPERATURE)

        zone.addDevice(light1)
        zone.addDevice(light2)
        zone.addDevice(sensor)

        assertEquals(3, zone.devices.size)
    }

    @Test
    fun `custom id is preserved`() {
        val customId = "custom-zone-id-456"
        val zone = Zone(id = customId, name = "Custom Zone", floor = 1, area = area)

        assertEquals(customId, zone.id)
    }
}
