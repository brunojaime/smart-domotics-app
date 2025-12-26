package com.domotics.smarthome.dto

import com.domotics.smarthome.entities.Area
import com.domotics.smarthome.entities.Building
import com.domotics.smarthome.entities.Location
import com.domotics.smarthome.entities.Zone
import com.domotics.smarthome.entities.ZoneType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DtoMappingTest {

    @Test
    fun `location dto enforces coordinate ranges`() {
        assertThrows(IllegalArgumentException::class.java) {
            LocationDTO(latitude = 95.0, longitude = 10.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LocationDTO(latitude = 10.0, longitude = 200.0)
        }
    }

    @Test
    fun `area create request validates parent and dimensions`() {
        assertThrows(IllegalArgumentException::class.java) {
            AreaCreateRequest(zoneId = "", name = "Area", squareMeters = 10.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AreaCreateRequest(zoneId = "zone-1", name = "", squareMeters = 10.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AreaCreateRequest(zoneId = "zone-1", name = "Area", squareMeters = -5.0)
        }
    }

    @Test
    fun `zone create mapper reuses area zone id`() {
        val request = ZoneCreateRequest(
            buildingId = "building-1",
            name = "Lobby Zone",
            floor = 1,
            area = AreaCreateRequest(zoneId = "zone-123", name = "Lobby"),
            zoneType = ZoneType.COMMERCIAL
        )

        val zone = DtoMappers.toZone(request)
        assertEquals("zone-123", zone.id)
        assertEquals("Lobby", zone.area.name)
        assertEquals(ZoneType.COMMERCIAL, zone.zoneType)
    }

    @Test
    fun `area response validates belonging to zone`() {
        val area = Area(name = "Main", squareMeters = 20.0)
        val zone = Zone(name = "Zone", floor = 1, area = area)

        val response = DtoMappers.toAreaResponse(area, zone.id)
        assertEquals(zone.id, response.zoneId)

        val mismatched = response.copy(zoneId = "other-zone")
        assertThrows(IllegalArgumentException::class.java) {
            DtoValidators.validateAreaBelongsToZone(mismatched, zone.id)
        }
    }

    @Test
    fun `zone response preserves building and nested area references`() {
        val area = Area(name = "Lobby", squareMeters = 15.0)
        val zone = Zone(name = "Ground Floor", floor = 0, area = area, zoneType = ZoneType.COMMERCIAL)
        val building = Building(name = "HQ", location = Location(latitude = 10.0, longitude = 10.0))
        building.addZone(zone)

        val response = DtoMappers.toBuildingResponse(building)
        assertEquals(building.id, response.id)
        assertEquals(1, response.zones.size)

        val zoneResponse = response.zones.first()
        assertEquals(building.id, zoneResponse.buildingId)
        assertEquals(zone.id, zoneResponse.area.zoneId)
        assertEquals(ZoneType.COMMERCIAL, zoneResponse.zoneType)
    }

    @Test
    fun `update mappers override only provided fields`() {
        val area = Area(name = "Basement", squareMeters = 30.0)
        val zone = Zone(name = "Storage", floor = -1, area = area)
        val building = Building(name = "Warehouse", location = Location(latitude = 5.0, longitude = 5.0))
        building.addZone(zone)

        val updatedBuilding = DtoMappers.updateBuilding(
            building,
            BuildingUpdateRequest(name = "Warehouse North", description = "Updated")
        )
        assertEquals("Warehouse North", updatedBuilding.name)
        assertEquals(building.location, updatedBuilding.location)
        assertEquals("Updated", updatedBuilding.description)

        val updatedZone = DtoMappers.updateZone(
            zone,
            ZoneUpdateRequest(
                buildingId = building.id,
                name = "Cold Storage",
                area = AreaUpdateRequest(zoneId = zone.id, squareMeters = 45.0)
            )
        )
        assertEquals("Cold Storage", updatedZone.name)
        assertEquals(45.0, updatedZone.area.squareMeters, 0.0)
        assertEquals(zone.floor, updatedZone.floor)
    }

    @Test
    fun `area update enforces existing zone relationship`() {
        val area = Area(name = "Hall", squareMeters = 12.0)
        val zone = Zone(name = "Entrance", floor = 0, area = area)

        assertThrows(IllegalArgumentException::class.java) {
            DtoMappers.updateZone(
                zone,
                ZoneUpdateRequest(
                    buildingId = "building-1",
                    area = AreaUpdateRequest(zoneId = "different-zone", name = "Updated Hall")
                )
            )
        }
    }

    @Test
    fun `zone validator enforces building relationship`() {
        val area = Area(name = "Meeting Room", squareMeters = 25.0)
        val zone = Zone(name = "First Floor", floor = 1, area = area)
        val response = DtoMappers.toZoneResponse(zone, buildingId = "building-1")

        assertThrows(IllegalArgumentException::class.java) {
            DtoValidators.validateZoneBelongsToBuilding(response, "other-building")
        }
        assertEquals("building-1", response.buildingId)
    }
}
