package com.domotics.smarthome.data.device

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.domotics.smarthome.provisioning.DiscoveryMetadata
import com.domotics.smarthome.data.device.PairingCapability

private class FakeDiscoveryStrategy(
    override val id: String,
    private val findings: List<DiscoveryFinding>,
    private val shouldThrow: Boolean = false,
) : DiscoveryStrategy {
    override suspend fun discover(): List<DiscoveryFinding> {
        if (shouldThrow) throw IllegalStateException("scan failed")
        return findings
    }
}

class DeviceDiscoveryRepositoryTest {

    @Test
    fun `emits progress, deduplicates devices, and retains metadata`() = runBlocking {
        val wifiFinding = DiscoveryFinding(
            device = DiscoveredDevice("1", "Wi‑Fi Bulb", pairingCapabilities = setOf(PairingCapability.SOFT_AP)),
            metadata = DiscoveryMetadata(supportsSoftAp = true, simulated = true),
        )
        val mdnsFinding = DiscoveryFinding(
            device = DiscoveredDevice("1", "Wi‑Fi Bulb", pairingCapabilities = setOf(PairingCapability.SOFT_AP)),
            metadata = DiscoveryMetadata(supportsSoftAp = true, simulated = true),
        )
        val bluetoothFinding = DiscoveryFinding(
            device = DiscoveredDevice("2", "BLE Plug", pairingCapabilities = setOf(PairingCapability.BLUETOOTH)),
            metadata = DiscoveryMetadata(supportsBluetoothFallback = true, simulated = true),
        )

        val session = DiscoverySession(
            listOf(
                FakeDiscoveryStrategy("wifi", listOf(wifiFinding)),
                FakeDiscoveryStrategy("mdns", listOf(mdnsFinding)),
                FakeDiscoveryStrategy("bluetooth", listOf(bluetoothFinding)),
            )
        )

        val emissions = session.discoverDevices().toList()
        assertTrue(emissions[0] is DiscoverySessionState.Discovering)
        assertTrue(emissions[1] is DiscoverySessionState.Discovering)
        assertTrue(emissions[2] is DiscoverySessionState.Discovering)
        val results = emissions[3]
        assertTrue(results is DiscoverySessionState.Results)
        assertEquals(2, (results as DiscoverySessionState.Results).devices.size)
        assertTrue(session.metadataFor("1")?.supportsSoftAp == true)
        assertTrue(session.metadataFor("2")?.supportsBluetoothFallback == true)
    }

    @Test
    fun `emits error when scanner fails`() = runBlocking {
        val session = DiscoverySession(
            listOf(FakeDiscoveryStrategy("wifi", emptyList(), shouldThrow = true)),
        )

        val emissions = session.discoverDevices().toList()
        assertTrue(emissions[0] is DiscoverySessionState.Discovering)
        val error = emissions[1]
        assertTrue(error is DiscoverySessionState.Error)
    }
}
