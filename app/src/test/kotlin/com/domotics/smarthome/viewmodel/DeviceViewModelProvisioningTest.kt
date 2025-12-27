package com.domotics.smarthome.viewmodel

import com.domotics.smarthome.data.device.DiscoveredDevice
import com.domotics.smarthome.data.device.PairingCapability
import com.domotics.smarthome.provisioning.DiscoveryMetadata
import com.domotics.smarthome.provisioning.ProvisioningOrchestrator
import com.domotics.smarthome.provisioning.ProvisioningProgress
import com.domotics.smarthome.provisioning.ProvisioningResult
import com.domotics.smarthome.provisioning.WifiCredentials
import com.domotics.smarthome.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceViewModelProvisioningTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `provisioning uses backend orchestrator when provided`() = runTest(mainDispatcherRule.testDispatcher) {
        val device = DiscoveredDevice(
            id = "device-1",
            name = "Demo",
            pairingCapabilities = setOf(PairingCapability.SOFT_AP),
        )
        val metadata = DiscoveryMetadata(supportsSoftAp = true)
        val recordedProgress = mutableListOf<ProvisioningProgress>()
        val orchestrator = object : ProvisioningOrchestrator {
            override suspend fun provision(
                device: DiscoveredDevice,
                metadata: DiscoveryMetadata,
                credentials: WifiCredentials,
                strategyId: String,
                onProgress: (ProvisioningProgress) -> Unit,
            ): ProvisioningResult {
                onProgress(ProvisioningProgress.SendingCredentials)
                recordedProgress.add(ProvisioningProgress.SendingCredentials)
                return ProvisioningResult.Success("ok")
            }
        }

        val viewModel = DeviceViewModel(
            provisioningOrchestrator = orchestrator,
            startMqttBridgeOnInit = false,
        )

        viewModel.selectDevice(device)
        viewModel.updateDiscovery(metadata)
        viewModel.provisionSelectedStrategy(WifiCredentials(ssid = "Home", password = "pass"))
        advanceUntilIdle()

        val state = viewModel.provisioningState.value
        assertTrue(state.lastResult is ProvisioningResult.Success)
        assertTrue(recordedProgress.contains(ProvisioningProgress.SendingCredentials))
        assertEquals(null, state.progress)
    }
}
