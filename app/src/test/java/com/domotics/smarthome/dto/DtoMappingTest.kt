package com.domotics.smarthome.dto

import com.domotics.smarthome.entities.Area
import com.domotics.smarthome.entities.Building
import com.domotics.smarthome.entities.Location
import com.domotics.smarthome.entities.Zone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DtoMappingTest {

    @Test
    fun `location dto maps to domain`() {
        val dto = LocationDTO(id = "loc-1", name = "Campus", buildingIds = listOf("b1"))

        val domain = dto.toDomain()
        assertEquals("Campus", domain.name)
        assertEquals(listOf("b1"), domain.buildingIds)
    }

    @Test
    fun `area create request validates parent`() {
        assertThrows(IllegalArgumentException::class.java) {
            AreaCreateRequest(zoneId = "", name = "Area")
        }
        assertThrows(IllegalArgumentException::class.java) {
            AreaCreateRequest(zoneId = "zone-1", name = "")
        }
    }

    @Test
    fun `zone create mapper preserves building link`() {
        val request = ZoneCreateRequest(
            buildingId = "building-1",
            name = "Lobby Zone",
        )

        val zone = DtoMappers.toZone(request)
        assertEquals("building-1", zone.buildingId)
        assertEquals("Lobby Zone", zone.name)
    }

    @Test
    fun `area response validates belonging to zone`() {
        val area = Area(name = "Main", zoneId = "zone-1")

        val response = DtoMappers.toAreaResponse(area)
        assertEquals(area.zoneId, response.zoneId)

        val mismatched = response.copy(zoneId = "other-zone")
        assertThrows(IllegalArgumentException::class.java) {
            DtoValidators.validateAreaBelongsToZone(mismatched, area.zoneId)
        }
    }

    @Test
    fun `zone response preserves building references`() {
        val zone = Zone(name = "Ground Floor", buildingId = "building-1")
        zone.areaIds.add("area-1")

        val response = DtoMappers.toZoneResponse(zone)
        assertEquals(zone.buildingId, response.buildingId)
        assertEquals(zone.areaIds, response.areaIds)
    }

    @Test
    fun `update mappers override only provided fields`() {
        val zone = Zone(name = "Storage", buildingId = "building-1")
        val building = Building(name = "Warehouse", locationId = "loc-1")
        building.addZone(zone.id)

        val updatedBuilding = DtoMappers.updateBuilding(
            building,
            BuildingUpdateRequest(name = "Warehouse North")
        )
        assertEquals("Warehouse North", updatedBuilding.name)
        assertEquals(building.locationId, updatedBuilding.locationId)

        val updatedZone = DtoMappers.updateZone(
            zone,
            ZoneUpdateRequest(
                buildingId = building.id,
                name = "Cold Storage",
            )
        )
        assertEquals("Cold Storage", updatedZone.name)
        assertEquals(building.id, updatedZone.buildingId)
    }

    @Test
    fun `zone validator enforces building relationship`() {
        val zone = Zone(name = "First Floor", buildingId = "building-1")
        val response = DtoMappers.toZoneResponse(zone)

        assertThrows(IllegalArgumentException::class.java) {
            DtoValidators.validateZoneBelongsToBuilding(response, "other-building")
        }
        assertEquals("building-1", response.buildingId)
    }
}
