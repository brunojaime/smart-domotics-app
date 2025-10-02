package com.domotics.smarthome.entities

import java.util.UUID

/**
 * Represents an area within a zone
 */
data class Area(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val squareMeters: Double? = null
) {
    init {
        require(name.isNotBlank()) { "Area name cannot be blank" }
        squareMeters?.let {
            require(it > 0) { "Square meters must be positive" }
        }
    }
}
