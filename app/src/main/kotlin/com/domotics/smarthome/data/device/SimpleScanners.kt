package com.domotics.smarthome.data.device

import kotlinx.coroutines.delay

class MockWifiScanner : DeviceScanner {
    override suspend fun discover(): List<DiscoveredDevice> {
        delay(200)
        return listOf(
            DiscoveredDevice(id = "wifi-1", name = "Wi‑Fi Bulb", capabilities = listOf("soft-ap")),
        )
    }
}

class MockMdnsScanner : DeviceScanner {
    override suspend fun discover(): List<DiscoveredDevice> {
        delay(150)
        return listOf(
            DiscoveredDevice(id = "mdns-1", name = "mDNS Switch", capabilities = listOf("mdns")),
        )
    }
}

class MockSsdpScanner : DeviceScanner {
    override suspend fun discover(): List<DiscoveredDevice> {
        delay(150)
        return emptyList()
    }
}
