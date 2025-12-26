package com.domotics.smarthome.data.device

import com.domotics.smarthome.entities.Device

/** Represents a device discovered during scanning. */
data class DiscoveredDevice(
    val id: String,
    val name: String,
    val capabilities: List<String> = emptyList(),
)

sealed class DiscoveryState {
    data object Idle : DiscoveryState()
    data class Scanning(val progress: Int) : DiscoveryState()
    data class Results(val devices: List<DiscoveredDevice>) : DiscoveryState()
    data object NoResults : DiscoveryState()
    data class Error(val message: String) : DiscoveryState()
}

data class PairingCredentials(
    val ssid: String? = null,
    val password: String? = null,
)

sealed class PairingState {
    data object Idle : PairingState()
    data object Submitting : PairingState()
    data class Success(val device: Device) : PairingState()
    data class Failure(val reason: String) : PairingState()
}
