package com.domotics.smarthome.entities

import java.util.UUID

/**
 * Represents a building in the domotics system
 */
data class Building(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val locationId: String,
    val zoneIds: MutableList<String> = mutableListOf(),
) {
    init {
        require(name.isNotBlank()) { "Building name cannot be blank" }
        require(locationId.isNotBlank()) { "Building must belong to a location" }
    }

    fun addZone(zoneId: String) {
        zoneIds.add(zoneId)
    }

    fun removeZone(zoneId: String) {
        zoneIds.remove(zoneId)
    }
}
