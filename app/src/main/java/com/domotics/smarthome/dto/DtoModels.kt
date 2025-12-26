package com.domotics.smarthome.dto

import com.domotics.smarthome.entities.ZoneType

/**
 * DTO representing geographic location data
 */
data class LocationDTO(
    val latitude: Double,
    val longitude: Double,
    val reference: String? = null
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
    }
}

/**
 * Request payload for creating an area
 */
data class AreaCreateRequest(
    val zoneId: String,
    val name: String,
    val squareMeters: Double? = null
) {
    init {
        require(zoneId.isNotBlank()) { "Zone id is required" }
        require(name.isNotBlank()) { "Area name cannot be blank" }
        squareMeters?.let {
            require(it > 0) { "Square meters must be positive" }
        }
    }
}

/**
 * Request payload for updating an area
 */
data class AreaUpdateRequest(
    val zoneId: String,
    val name: String? = null,
    val squareMeters: Double? = null
) {
    init {
        require(zoneId.isNotBlank()) { "Zone id is required" }
        name?.let {
            require(it.isNotBlank()) { "Area name cannot be blank" }
        }
        squareMeters?.let {
            require(it > 0) { "Square meters must be positive" }
        }
    }
}

/**
 * Response payload for returning an area
 */
data class AreaResponse(
    val id: String,
    val zoneId: String,
    val name: String,
    val squareMeters: Double? = null
)

/**
 * Request payload for creating a zone
 */
data class ZoneCreateRequest(
    val buildingId: String,
    val name: String,
    val floor: Int,
    val area: AreaCreateRequest,
    val zoneType: ZoneType? = null
) {
    init {
        require(buildingId.isNotBlank()) { "Building id is required" }
        require(name.isNotBlank()) { "Zone name cannot be blank" }
    }
}

/**
 * Request payload for updating a zone
 */
data class ZoneUpdateRequest(
    val buildingId: String,
    val name: String? = null,
    val floor: Int? = null,
    val area: AreaUpdateRequest? = null,
    val zoneType: ZoneType? = null
) {
    init {
        require(buildingId.isNotBlank()) { "Building id is required" }
        name?.let {
            require(it.isNotBlank()) { "Zone name cannot be blank" }
        }
    }
}

/**
 * Response payload for returning a zone
 */
data class ZoneResponse(
    val id: String,
    val buildingId: String,
    val name: String,
    val floor: Int,
    val area: AreaResponse,
    val zoneType: ZoneType? = null
)

/**
 * Request payload for creating a building
 */
data class BuildingCreateRequest(
    val name: String,
    val location: LocationDTO,
    val description: String? = null
) {
    init {
        require(name.isNotBlank()) { "Building name cannot be blank" }
    }
}

/**
 * Request payload for updating a building
 */
data class BuildingUpdateRequest(
    val name: String? = null,
    val location: LocationDTO? = null,
    val description: String? = null
) {
    init {
        name?.let {
            require(it.isNotBlank()) { "Building name cannot be blank" }
        }
    }
}

/**
 * Response payload for returning a building
 */
data class BuildingResponse(
    val id: String,
    val name: String,
    val location: LocationDTO,
    val description: String? = null,
    val zones: List<ZoneResponse> = emptyList()
)
