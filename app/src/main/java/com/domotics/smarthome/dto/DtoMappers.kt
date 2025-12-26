package com.domotics.smarthome.dto

import com.domotics.smarthome.entities.Area
import com.domotics.smarthome.entities.Building
import com.domotics.smarthome.entities.Location
import com.domotics.smarthome.entities.Zone

/**
 * Centralized mapping functions between DTOs and domain entities
 */
object DtoMappers {
    fun LocationDTO.toDomain(): Location = Location(id = id, name = name, buildingIds = buildingIds)

    fun Location.toDto(): LocationDTO = LocationDTO(id = id, name = name, buildingIds = buildingIds)

    fun toArea(request: AreaCreateRequest): Area = Area(name = request.name, zoneId = request.zoneId)

    fun updateArea(area: Area, request: AreaUpdateRequest): Area =
        area.copy(
            name = request.name ?: area.name,
            zoneId = request.zoneId,
        )

    fun toAreaResponse(area: Area): AreaResponse {
        val response = AreaResponse(
            id = area.id,
            zoneId = area.zoneId,
            name = area.name,
        )
        DtoValidators.validateAreaBelongsToZone(response, area.zoneId)
        return response
    }

    fun toZone(request: ZoneCreateRequest): Zone =
        Zone(
            name = request.name,
            buildingId = request.buildingId,
        )

    fun updateZone(zone: Zone, request: ZoneUpdateRequest): Zone {
        return zone.copy(
            name = request.name ?: zone.name,
            buildingId = request.buildingId,
        )
    }

    fun toZoneResponse(zone: Zone): ZoneResponse {
        val response = ZoneResponse(
            id = zone.id,
            buildingId = zone.buildingId,
            name = zone.name,
            areaIds = zone.areaIds,
        )
        DtoValidators.validateZoneBelongsToBuilding(response, zone.buildingId)
        return response
    }

    fun toBuilding(request: BuildingCreateRequest): Building =
        Building(
            name = request.name,
            locationId = request.locationId,
        )

    fun updateBuilding(building: Building, request: BuildingUpdateRequest): Building =
        building.copy(
            name = request.name ?: building.name,
            locationId = request.locationId ?: building.locationId,
        )

    fun toBuildingResponse(building: Building): BuildingResponse =
        BuildingResponse(
            id = building.id,
            name = building.name,
            locationId = building.locationId,
            zoneIds = building.zoneIds,
        )
}
