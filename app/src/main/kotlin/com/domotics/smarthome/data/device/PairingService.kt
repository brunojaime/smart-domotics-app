package com.domotics.smarthome.data.device

import com.domotics.smarthome.entities.DeviceStatus
import com.domotics.smarthome.entities.Lighting

interface PairingClient {
    suspend fun sendCredentials(device: DiscoveredDevice, credentials: PairingCredentials): Boolean
}

interface PairingService {
    suspend fun pairDevice(device: DiscoveredDevice, credentials: PairingCredentials): PairingState
}

class PairingServiceImpl(private val client: PairingClient) : PairingService {
    override suspend fun pairDevice(
        device: DiscoveredDevice,
        credentials: PairingCredentials,
    ): PairingState {
        return try {
            val success = client.sendCredentials(device, credentials)
            if (success) {
                val pairedDevice = Lighting(
                    name = device.name,
                    status = DeviceStatus.OFF,
                    brightness = 0,
                    poweredOn = false,
                )
                PairingState.Success(pairedDevice)
            } else {
                PairingState.Failure("Device rejected credentials")
            }
        } catch (ex: Exception) {
            PairingState.Failure(ex.message ?: "Unable to pair")
        }
    }
}
