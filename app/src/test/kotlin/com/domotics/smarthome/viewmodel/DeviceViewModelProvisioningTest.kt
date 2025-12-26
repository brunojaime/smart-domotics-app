package com.domotics.smarthome.viewmodel

import com.domotics.smarthome.data.device.DeviceDiscoveryRepository
import com.domotics.smarthome.data.device.DiscoveredDevice
import com.domotics.smarthome.data.device.DiscoveryState
import com.domotics.smarthome.provisioning.DiscoveryMetadata
import com.domotics.smarthome.provisioning.ProvisioningOrchestrator
import com.domotics.smarthome.provisioning.ProvisioningProgress
import com.domotics.smarthome.provisioning.ProvisioningResult
import com.domotics.smarthome.provisioning.WifiCredentials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceViewModelProvisioningTest {
    @Test
    fun `provisioning uses backend orchestrator when provided`() = runTest {
        val device = DiscoveredDevice(
            id = "device-1",
            name = "Demo",
            capabilities = listOf("wifi"),
            metadata = DiscoveryMetadata(supportsSoftAp = true),
        )
        val discoveryRepository = object : DeviceDiscoveryRepository {
            override fun discoverDevices(): Flow<DiscoveryState> = flow {
                emit(DiscoveryState.Results(listOf(device)))
            }
        }
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
            discoveryRepository = discoveryRepository,
            provisioningOrchestrator = orchestrator,
            startMqttBridgeOnInit = false,
        )

        viewModel.startDiscovery()
        advanceUntilIdle()
        viewModel.provisionSelectedStrategy(WifiCredentials(ssid = "Home", password = "pass"))
        advanceUntilIdle()

        val state = viewModel.provisioningState.value
        assertTrue(state.lastResult is ProvisioningResult.Success)
        assertTrue(recordedProgress.contains(ProvisioningProgress.SendingCredentials))
        assertEquals(null, state.progress)
    }
}
