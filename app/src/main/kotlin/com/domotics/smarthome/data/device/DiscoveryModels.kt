package com.domotics.smarthome.data.device

import com.domotics.smarthome.entities.Device
import com.domotics.smarthome.provisioning.DiscoveryMetadata

/**
 * Pure domain representation of a device discovered during a discovery session.
 * It intentionally omits transport details and chip metadata; those are tracked
 * separately by the orchestrator so the UI only sees a unified device surface.
 */
data class DiscoveredDevice(
    val id: String,
    val name: String,
    val signalStrength: Int? = null,
    val pairingCapabilities: Set<PairingCapability> = emptySet(),
)

/** Capabilities that indicate which provisioning strategies are viable. */
enum class PairingCapability { SOFT_AP, BLUETOOTH, ONBOARDING_CODE }

/**
 * Internal tuple that keeps discovery metadata adjacent to the domain object
 * without leaking transport details to the UI.
 */
data class DiscoveryFinding(
    val device: DiscoveredDevice,
    val metadata: DiscoveryMetadata,
)

sealed class DiscoverySessionState {
    data object Idle : DiscoverySessionState()
    data class Discovering(val progress: Int) : DiscoverySessionState()
    data class Results(val devices: List<DiscoveredDevice>) : DiscoverySessionState()
    data object NoResults : DiscoverySessionState()
    data class Error(val message: String) : DiscoverySessionState()
}

data class RadioState(
    val wifiEnabled: Boolean = true,
    val bluetoothEnabled: Boolean = true,
)

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
