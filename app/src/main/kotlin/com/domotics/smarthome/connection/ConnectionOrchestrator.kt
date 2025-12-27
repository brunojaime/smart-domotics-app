package com.domotics.smarthome.connection

import com.domotics.smarthome.data.device.DiscoveredDevice
import com.domotics.smarthome.data.device.DiscoverySession
import com.domotics.smarthome.data.device.DiscoverySessionState
import com.domotics.smarthome.data.device.DiscoveryStrategy
import com.domotics.smarthome.data.device.PairingCapability
import com.domotics.smarthome.data.device.PairingState
import com.domotics.smarthome.provisioning.DiscoveryMetadata
import com.domotics.smarthome.provisioning.ProvisioningProgress
import com.domotics.smarthome.provisioning.ProvisioningResult
import com.domotics.smarthome.provisioning.ProvisioningStrategy
import com.domotics.smarthome.provisioning.WifiCredentials
import kotlinx.coroutines.flow.Flow

/**
 * Orchestrates the discovery and provisioning pipeline. The UI interacts with this
 * orchestrator to keep transports hidden: discovery first, provisioning second.
 */
class ConnectionOrchestrator(
    discoveryStrategies: List<DiscoveryStrategy>,
    private val provisioningStrategies: List<ProvisioningStrategy>,
) {
    private val discoverySession = DiscoverySession(discoveryStrategies)

    fun discoveryStates(): Flow<DiscoverySessionState> = discoverySession.discoverDevices()

    fun pairingSessionFor(device: DiscoveredDevice): PairingSession? {
        val metadata = discoverySession.metadataFor(device.id) ?: return null
        return PairingSession(device, metadata, provisioningStrategies)
    }
}

/**
 * Represents the onboarding lifecycle for a single device selection.
 */
class PairingSession(
    val device: DiscoveredDevice,
    private val metadata: DiscoveryMetadata,
    private val provisioningStrategies: List<ProvisioningStrategy>,
) {
    private var activeStrategy: ProvisioningStrategy? = null

    suspend fun provision(
        credentials: WifiCredentials,
        onProgress: (ProvisioningProgress) -> Unit = {},
    ): PairingState {
        val strategy = selectProvisioningStrategy()
            ?: return PairingState.Failure("No provisioning strategy available for ${device.name}")

        activeStrategy = strategy
        return when (val result = strategy.provision(metadata, credentials, onProgress)) {
            is ProvisioningResult.Success -> PairingState.Success(device = device.toLightingPlaceholder())
            is ProvisioningResult.Cancelled -> PairingState.Failure(result.message)
            is ProvisioningResult.Failure -> PairingState.Failure(result.message)
        }
    }

    fun cancel() {
        activeStrategy?.cancel()
    }

    private fun selectProvisioningStrategy(): ProvisioningStrategy? {
        val preferredOrder = listOf(
            PairingCapability.SOFT_AP,
            PairingCapability.BLUETOOTH,
            PairingCapability.ONBOARDING_CODE,
        )

        val strategiesByCapability = provisioningStrategies.associateBy { strategy ->
            when (strategy.id) {
                "soft_ap" -> PairingCapability.SOFT_AP
                "bluetooth_fallback" -> PairingCapability.BLUETOOTH
                "onboarding_code" -> PairingCapability.ONBOARDING_CODE
                else -> null
            }
        }

        preferredOrder.forEach { capability ->
            if (device.pairingCapabilities.contains(capability)) {
                val candidate = strategiesByCapability[capability]?.takeIf { it.supports(metadata) }
                if (candidate != null) return candidate
            }
        }

        return provisioningStrategies.firstOrNull { it.supports(metadata) }
    }
}

private fun DiscoveredDevice.toLightingPlaceholder(): com.domotics.smarthome.entities.Lighting =
    com.domotics.smarthome.entities.Lighting(id = id, name = name)
