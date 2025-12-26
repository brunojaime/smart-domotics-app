package com.domotics.smarthome.data.device

import com.domotics.smarthome.provisioning.DiscoveryMetadata
import kotlinx.coroutines.delay

class MockWifiScanner : DeviceScanner {
    override suspend fun discover(): List<DiscoveredDevice> {
        delay(200)
        return listOf(
            DiscoveredDevice(
                id = "wifi-1",
                name = "Wi‑Fi Bulb",
                capabilities = listOf("soft-ap", "wifi"),
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

class MockMdnsScanner : DeviceScanner {
    override suspend fun discover(): List<DiscoveredDevice> {
        delay(150)
        return listOf(
            DiscoveredDevice(
                id = "mdns-1",
                name = "mDNS Switch",
                capabilities = listOf("mdns", "wifi"),
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

class MockSsdpScanner : DeviceScanner {
    override suspend fun discover(): List<DiscoveredDevice> {
        delay(150)
        return emptyList()
    }
}

class MockBluetoothScanner : DeviceScanner {
    override suspend fun discover(): List<DiscoveredDevice> {
        delay(180)
        return listOf(
            DiscoveredDevice(
                id = "ble-1",
                name = "Nearby BLE Dimmer",
                capabilities = listOf("bluetooth", "fallback"),
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
