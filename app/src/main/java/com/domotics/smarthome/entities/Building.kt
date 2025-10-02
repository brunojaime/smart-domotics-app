package com.domotics.smarthome.entities

import java.util.UUID

/**
 * Represents a building in the domotics system
 */
data class Building(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val location: Location,
    val zones: MutableList<Zone> = mutableListOf(),
    val description: String? = null
) {
    init {
        require(name.isNotBlank()) { "Building name cannot be blank" }
    }

    fun addZone(zone: Zone) {
        zones.add(zone)
    }

    fun removeZone(zone: Zone) {
        zones.remove(zone)
    }
}
