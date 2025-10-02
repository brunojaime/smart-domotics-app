package com.domotics.smarthome.entities

import java.util.UUID

/**
 * Represents a zone within a building
 */
data class Zone(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val floor: Int,
    val area: Area,
    val devices: MutableList<Device> = mutableListOf(),
    val zoneType: ZoneType? = null
) {
    init {
        require(name.isNotBlank()) { "Zone name cannot be blank" }
    }

    fun addDevice(device: Device) {
        devices.add(device)
    }

    fun removeDevice(device: Device) {
        devices.remove(device)
    }
}

/**
 * Types of zones in a building
 */
enum class ZoneType {
    RESIDENTIAL,
    COMMERCIAL,
    INDUSTRIAL,
    COMMON_AREA,
    STORAGE,
    OFFICE,
    OTHER
}
