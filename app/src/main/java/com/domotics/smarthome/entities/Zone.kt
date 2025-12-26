package com.domotics.smarthome.entities

import java.util.UUID

/**
 * Represents a zone within a building
 */
data class Zone(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val buildingId: String,
    val areaIds: MutableList<String> = mutableListOf(),
    val devices: MutableList<Device> = mutableListOf(),
) {
    init {
        require(name.isNotBlank()) { "Zone name cannot be blank" }
        require(buildingId.isNotBlank()) { "Zone must belong to a building" }
    }

    fun addDevice(device: Device) {
        devices.add(device)
    }

    fun removeDevice(device: Device) {
        devices.remove(device)
    }
}
