package com.domotics.smarthome.entities

import java.util.UUID

/**
 * Represents a logical location that contains one or more buildings
 */
data class Location(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val buildingIds: List<String> = emptyList(),
) {
    init {
        require(name.isNotBlank()) { "Location name cannot be blank" }
    }
}
