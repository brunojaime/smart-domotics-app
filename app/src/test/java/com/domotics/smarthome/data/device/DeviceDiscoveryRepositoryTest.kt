package com.domotics.smarthome.data.device

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

private class FakeScanner(
    private val devices: List<DiscoveredDevice>,
    private val shouldThrow: Boolean = false,
) : DeviceScanner {
    override suspend fun discover(): List<DiscoveredDevice> {
        if (shouldThrow) throw IllegalStateException("scan failed")
        return devices
    }
}

class DeviceDiscoveryRepositoryTest {

    @Test
    fun `emits progress and results`() = runBlocking {
        val repository = DeviceDiscoveryRepositoryImpl(
            listOf(
                FakeScanner(listOf(DiscoveredDevice("1", "Wi‑Fi Bulb"))),
                FakeScanner(listOf(DiscoveredDevice("2", "mDNS Plug"))),
            ),
        )

        val emissions = repository.discoverDevices().toList()
        assertTrue(emissions[0] is DiscoveryState.Scanning)
        assertTrue(emissions[1] is DiscoveryState.Scanning)
        assertTrue(emissions[2] is DiscoveryState.Scanning)
        val results = emissions[3]
        assertTrue(results is DiscoveryState.Results)
        assertEquals(2, (results as DiscoveryState.Results).devices.size)
    }

    @Test
    fun `emits error when scanner fails`() = runBlocking {
        val repository = DeviceDiscoveryRepositoryImpl(
            listOf(FakeScanner(emptyList(), shouldThrow = true)),
        )

        val emissions = repository.discoverDevices().toList()
        assertTrue(emissions[0] is DiscoveryState.Scanning)
        val error = emissions[1]
        assertTrue(error is DiscoveryState.Error)
    }
}
