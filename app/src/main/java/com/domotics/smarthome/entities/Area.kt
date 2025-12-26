package com.domotics.smarthome.entities

import java.util.UUID

/**
 * Represents an area within a zone
 */
data class Area(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val zoneId: String,
) {
    init {
        require(name.isNotBlank()) { "Area name cannot be blank" }
        require(zoneId.isNotBlank()) { "Area must belong to a zone" }
    }
}
