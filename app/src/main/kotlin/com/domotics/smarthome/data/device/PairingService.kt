package com.domotics.smarthome.data.device

import com.domotics.smarthome.entities.DeviceStatus
import com.domotics.smarthome.entities.Lighting
import com.domotics.smarthome.data.device.RadioState
import kotlinx.coroutines.delay

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

class LocalPairingClient(
    private val metadataProvider: (String) -> com.domotics.smarthome.provisioning.DiscoveryMetadata?,
    private val radioState: () -> RadioState,
) : PairingClient {
    override suspend fun sendCredentials(device: DiscoveredDevice, credentials: PairingCredentials): Boolean {
        val metadata = metadataProvider(device.id)
        val radios = radioState()

        if (!radios.wifiEnabled && !radios.bluetoothEnabled) {
            throw IllegalStateException("Enable Wi‑Fi or Bluetooth to continue pairing")
        }

        if (metadata?.supportsSoftAp == true && !radios.wifiEnabled) {
            throw IllegalStateException("Wi‑Fi must be enabled to provision this device")
        }

        if (metadata?.supportsBluetoothFallback == true && !metadata.supportsSoftAp && !radios.bluetoothEnabled) {
            throw IllegalStateException("Bluetooth must be enabled to provision this device")
        }

        if (metadata?.supportsSoftAp == true) {
            val ssid = credentials.ssid?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("Wi‑Fi SSID is required")
            val password = credentials.password?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("Wi‑Fi password is required")
            val expectedPassword = metadata.expectedWifiPassword
            if (expectedPassword != null && expectedPassword != password) {
                return false
            }
            if (!metadata.deviceApReachable) {
                throw IllegalStateException("Device access point is unreachable")
            }
            if (!metadata.respondsToHeartbeat) {
                throw IllegalStateException("Device stopped responding")
            }
            delay(400)
        } else if (metadata?.supportsBluetoothFallback == true) {
            delay(250)
        }

        return true
    }
}
