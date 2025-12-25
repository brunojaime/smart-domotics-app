package com.domotics.smarthome.provisioning

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SoftApProvisioningStrategyTest {

    private val metadata = DiscoveryMetadata(
        deviceSsid = "Device_AP",
        supportsSoftAp = true,
        expectedWifiPassword = "correct-password",
        respondsToHeartbeat = true,
        deviceApReachable = true
    )

    @Test
    fun `provisions successfully with correct password`() = runTest {
        val strategy = SoftApProvisioningStrategy(
            timeouts = SoftApTimeouts(
                connectTimeoutMillis = 1_000,
                credentialAckTimeoutMillis = 1_000,
                heartbeatTimeoutMillis = 1_000
            )
        )

        val progressEvents = mutableListOf<ProvisioningProgress>()
        val result = strategy.provision(
            metadata,
            WifiCredentials(ssid = "Home", password = "correct-password")
        ) { progressEvents.add(it) }

        assertTrue(result is ProvisioningResult.Success)
        assertEquals(
            listOf(
                ProvisioningProgress.ConnectingToDeviceAp,
                ProvisioningProgress.SendingCredentials,
                ProvisioningProgress.WaitingForDevice
            ),
            progressEvents
        )
    }

    @Test
    fun `fails when device access point is unreachable`() = runTest {
        val strategy = SoftApProvisioningStrategy()
        val result = strategy.provision(
            metadata.copy(deviceApReachable = false),
            WifiCredentials(ssid = "Home", password = "correct-password")
        ) {}

        val failure = result as ProvisioningResult.Failure
        assertEquals(ProvisioningFailureReason.AP_UNREACHABLE, failure.reason)
    }

    @Test
    fun `fails when password rejected`() = runTest {
        val strategy = SoftApProvisioningStrategy()
        val result = strategy.provision(
            metadata,
            WifiCredentials(ssid = "Home", password = "wrong")
        ) {}

        val failure = result as ProvisioningResult.Failure
        assertEquals(ProvisioningFailureReason.BAD_PASSWORD, failure.reason)
    }

    @Test
    fun `fails when device stops responding`() = runTest {
        val strategy = SoftApProvisioningStrategy()
        val result = strategy.provision(
            metadata.copy(respondsToHeartbeat = false),
            WifiCredentials(ssid = "Home", password = "correct-password")
        ) {}

        val failure = result as ProvisioningResult.Failure
        assertEquals(ProvisioningFailureReason.DEVICE_TIMEOUT, failure.reason)
    }

    @Test
    fun `cancellation returns cancelled result`() = runTest {
        val strategy = SoftApProvisioningStrategy(
            timeouts = SoftApTimeouts(
                connectTimeoutMillis = 5_000,
                credentialAckTimeoutMillis = 5_000,
                heartbeatTimeoutMillis = 5_000
            )
        )

        val job: Job = launch {
            val result = strategy.provision(
                metadata,
                WifiCredentials("Home", "correct-password")
            ) {}
            assertTrue(result is ProvisioningResult.Cancelled)
        }

        advanceTimeBy(400)
        strategy.cancel()
        job.join()
    }
}
