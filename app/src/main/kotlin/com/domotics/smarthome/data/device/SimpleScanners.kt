package com.domotics.smarthome.data.device

import com.domotics.smarthome.provisioning.DiscoveryMetadata
import kotlinx.coroutines.delay

class MockWifiScanner : DiscoveryStrategy {
    override val id: String = "wifi"

    override suspend fun discover(): List<DiscoveryFinding> {
        delay(200)
        return listOf(
            DiscoveryFinding(
                device = DiscoveredDevice(
                    id = "wifi-1",
                    name = "Wi‑Fi Bulb",
                    pairingCapabilities = setOf(PairingCapability.SOFT_AP, PairingCapability.BLUETOOTH),
                ),
                metadata = DiscoveryMetadata(
                    deviceSsid = "SmartBulb-Setup",
                    supportsSoftAp = true,
                    supportsBluetoothFallback = true,
                    expectedWifiPassword = "password123",
                    respondsToHeartbeat = true,
                    simulated = true,
                ),
            ),
        )
    }
}

class MockMdnsScanner : DiscoveryStrategy {
    override val id: String = "mdns"

    override suspend fun discover(): List<DiscoveryFinding> {
        delay(150)
        return listOf(
            DiscoveryFinding(
                device = DiscoveredDevice(
                    id = "mdns-1",
                    name = "mDNS Switch",
                    pairingCapabilities = setOf(PairingCapability.SOFT_AP),
                ),
                metadata = DiscoveryMetadata(
                    supportsSoftAp = true,
                    supportsBluetoothFallback = false,
                    deviceApReachable = true,
                    expectedWifiPassword = null,
                    respondsToHeartbeat = true,
                    simulated = true,
                ),
            ),
        )
    }
}

class MockSsdpScanner : DiscoveryStrategy {
    override val id: String = "ssdp"

    override suspend fun discover(): List<DiscoveryFinding> {
        delay(150)
        return emptyList()
    }
}

class MockBluetoothScanner : DiscoveryStrategy {
    override val id: String = "bluetooth"

    override suspend fun discover(): List<DiscoveryFinding> {
        delay(180)
        return listOf(
            DiscoveryFinding(
                device = DiscoveredDevice(
                    id = "ble-1",
                    name = "Nearby BLE Dimmer",
                    pairingCapabilities = setOf(PairingCapability.BLUETOOTH),
                ),
                metadata = DiscoveryMetadata(
                    supportsSoftAp = false,
                    supportsBluetoothFallback = true,
                    respondsToHeartbeat = true,
                    simulated = true,
                ),
            ),
        )
    }
}
