package com.domotics.smarthome.connection

import com.domotics.smarthome.data.device.DiscoveredDevice
import com.domotics.smarthome.data.device.DiscoveryFinding
import com.domotics.smarthome.data.device.DiscoverySessionState
import com.domotics.smarthome.data.device.DiscoveryStrategy
import com.domotics.smarthome.data.device.PairingCapability
import com.domotics.smarthome.data.device.PairingState
import com.domotics.smarthome.provisioning.DiscoveryMetadata
import com.domotics.smarthome.provisioning.ProvisioningProgress
import com.domotics.smarthome.provisioning.ProvisioningResult
import com.domotics.smarthome.provisioning.ProvisioningStrategy
import com.domotics.smarthome.provisioning.WifiCredentials
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeDiscoveryStrategy(
    override val id: String,
    private val findings: List<DiscoveryFinding>
) : DiscoveryStrategy {
    override suspend fun discover(): List<DiscoveryFinding> = findings
}

private class FakeProvisioningStrategy(
    override val id: String,
    override val name: String,
    override val requiredUserAction: String,
    private val supportsSoftAp: Boolean = false,
    private val supportsBluetooth: Boolean = false,
    private val result: ProvisioningResult,
) : ProvisioningStrategy {
    var wasInvoked: Boolean = false

    override fun supports(metadata: DiscoveryMetadata): Boolean =
        (supportsSoftAp && metadata.supportsSoftAp) || (supportsBluetooth && metadata.supportsBluetoothFallback)

    override suspend fun provision(
        metadata: DiscoveryMetadata,
        credentials: WifiCredentials,
        onProgress: (ProvisioningProgress) -> Unit,
    ): ProvisioningResult {
        wasInvoked = true
        onProgress(ProvisioningProgress.ConnectingToDeviceAp)
        return result
    }

    override fun cancel() {}
}

class ConnectionOrchestratorTest {

    @Test
    fun `orchestrator builds pairing session and provisions with soft ap`() = runBlocking {
        val finding = DiscoveryFinding(
            device = DiscoveredDevice(
                id = "1",
                name = "Bulb",
                pairingCapabilities = setOf(PairingCapability.SOFT_AP)
            ),
            metadata = DiscoveryMetadata(supportsSoftAp = true)
        )

        val softApStrategy = FakeProvisioningStrategy(
            id = "soft_ap",
            name = "Soft AP",
            requiredUserAction = "",
            supportsSoftAp = true,
            result = ProvisioningResult.Success("ok"),
        )

        val orchestrator = ConnectionOrchestrator(
            discoveryStrategies = listOf(FakeDiscoveryStrategy("wifi", listOf(finding))),
            provisioningStrategies = listOf(softApStrategy)
        )

        val states = orchestrator.discoveryStates().toList()
        val results = states.last() as DiscoverySessionState.Results
        val pairingSession = orchestrator.pairingSessionFor(results.devices.first())

        val pairingResult = pairingSession!!.provision(WifiCredentials("ssid", "pass"))

        assertTrue(softApStrategy.wasInvoked)
        assertTrue(pairingResult is PairingState.Success)
    }

    @Test
    fun `returns failure when no strategy supports metadata`() = runBlocking {
        val finding = DiscoveryFinding(
            device = DiscoveredDevice(
                id = "2",
                name = "BLE Plug",
                pairingCapabilities = setOf(PairingCapability.BLUETOOTH)
            ),
            metadata = DiscoveryMetadata(supportsSoftAp = false, supportsBluetoothFallback = false)
        )

        val orchestrator = ConnectionOrchestrator(
            discoveryStrategies = listOf(FakeDiscoveryStrategy("ble", listOf(finding))),
            provisioningStrategies = emptyList()
        )

        val states = orchestrator.discoveryStates().toList()
        val results = states.last() as DiscoverySessionState.Results
        val pairingSession = orchestrator.pairingSessionFor(results.devices.first())

        val pairingResult = pairingSession!!.provision(WifiCredentials("home", "secret"))
        assertTrue(pairingResult is PairingState.Failure)
    }
}
