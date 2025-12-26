package com.domotics.smarthome.dto

/**
 * Utility validators to ensure relational integrity between DTOs
 */
object DtoValidators {
    fun validateAreaBelongsToZone(area: AreaResponse, zoneId: String) {
        require(area.zoneId == zoneId) { "Area ${area.id} must belong to zone $zoneId" }
    }

    fun validateZoneBelongsToBuilding(zone: ZoneResponse, buildingId: String) {
        require(zone.buildingId == buildingId) { "Zone ${zone.id} must belong to building $buildingId" }
    }
}
