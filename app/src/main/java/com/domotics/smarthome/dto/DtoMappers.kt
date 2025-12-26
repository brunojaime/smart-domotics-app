package com.domotics.smarthome.dto

import com.domotics.smarthome.entities.Area
import com.domotics.smarthome.entities.Building
import com.domotics.smarthome.entities.Location
import com.domotics.smarthome.entities.Zone

/**
 * Centralized mapping functions between DTOs and domain entities
 */
object DtoMappers {
    fun LocationDTO.toDomain(): Location = Location(latitude = latitude, longitude = longitude, reference = reference)

    fun Location.toDto(): LocationDTO = LocationDTO(latitude = latitude, longitude = longitude, reference = reference)

    fun toArea(request: AreaCreateRequest): Area = Area(name = request.name, squareMeters = request.squareMeters)

    fun updateArea(area: Area, request: AreaUpdateRequest): Area =
        area.copy(
            name = request.name ?: area.name,
            squareMeters = request.squareMeters ?: area.squareMeters
        )

    fun toAreaResponse(area: Area, zoneId: String): AreaResponse {
        val response = AreaResponse(
            id = area.id,
            zoneId = zoneId,
            name = area.name,
            squareMeters = area.squareMeters
        )
        DtoValidators.validateAreaBelongsToZone(response, zoneId)
        return response
    }

    fun toZone(request: ZoneCreateRequest): Zone =
        Zone(
            id = request.area.zoneId,
            name = request.name,
            floor = request.floor,
            area = toArea(request.area),
            zoneType = request.zoneType
        )

    fun updateZone(zone: Zone, request: ZoneUpdateRequest): Zone {
        request.area?.let {
            require(it.zoneId == zone.id) { "Area ${zone.area.id} must belong to zone ${zone.id}" }
        }
        val updatedArea = request.area?.let { updateArea(zone.area, it) } ?: zone.area
        return zone.copy(
            name = request.name ?: zone.name,
            floor = request.floor ?: zone.floor,
            area = updatedArea,
            zoneType = request.zoneType ?: zone.zoneType
        )
    }

    fun toZoneResponse(zone: Zone, buildingId: String): ZoneResponse {
        val areaResponse = toAreaResponse(zone.area, zone.id)
        val response = ZoneResponse(
            id = zone.id,
            buildingId = buildingId,
            name = zone.name,
            floor = zone.floor,
            area = areaResponse,
            zoneType = zone.zoneType
        )
        DtoValidators.validateZoneBelongsToBuilding(response, buildingId)
        return response
    }

    fun toBuilding(request: BuildingCreateRequest): Building =
        Building(
            name = request.name,
            location = request.location.toDomain(),
            description = request.description
        )

    fun updateBuilding(building: Building, request: BuildingUpdateRequest): Building =
        building.copy(
            name = request.name ?: building.name,
            location = request.location?.toDomain() ?: building.location,
            description = request.description ?: building.description
        )

    fun toBuildingResponse(building: Building): BuildingResponse =
        BuildingResponse(
            id = building.id,
            name = building.name,
            location = building.location.toDto(),
            description = building.description,
            zones = building.zones.map { toZoneResponse(it, building.id) }
        )
}
