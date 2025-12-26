package com.domotics.smarthome.dto

/**
 * DTO representing location data aligned with the backend contract
 */
data class LocationDTO(
    val id: String,
    val name: String,
    val buildingIds: List<String> = emptyList(),
)

/**
 * Request payload for creating an area
 */
data class AreaCreateRequest(
    val zoneId: String,
    val name: String,
) {
    init {
        require(zoneId.isNotBlank()) { "Zone id is required" }
        require(name.isNotBlank()) { "Area name cannot be blank" }
    }
}

/**
 * Request payload for updating an area
 */
data class AreaUpdateRequest(
    val zoneId: String,
    val name: String? = null,
) {
    init {
        require(zoneId.isNotBlank()) { "Zone id is required" }
        name?.let {
            require(it.isNotBlank()) { "Area name cannot be blank" }
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
)

/**
 * Request payload for creating a zone
 */
data class ZoneCreateRequest(
    val buildingId: String,
    val name: String,
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
    val areaIds: List<String> = emptyList(),
)

/**
 * Request payload for creating a building
 */
data class BuildingCreateRequest(
    val name: String,
    val locationId: String,
) {
    init {
        require(name.isNotBlank()) { "Building name cannot be blank" }
        require(locationId.isNotBlank()) { "Building must belong to a location" }
    }
}

/**
 * Request payload for updating a building
 */
data class BuildingUpdateRequest(
    val name: String? = null,
    val locationId: String? = null,
) {
    init {
        name?.let {
            require(it.isNotBlank()) { "Building name cannot be blank" }
        }
        locationId?.let {
            require(it.isNotBlank()) { "Building must belong to a location" }
        }
    }
}

/**
 * Response payload for returning a building
 */
data class BuildingResponse(
    val id: String,
    val name: String,
    val locationId: String,
    val zoneIds: List<String> = emptyList(),
)
