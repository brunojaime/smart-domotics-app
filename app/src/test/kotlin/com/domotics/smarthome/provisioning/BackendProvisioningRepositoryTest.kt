package com.domotics.smarthome.provisioning

import com.domotics.smarthome.data.device.DiscoveredDevice
import com.domotics.smarthome.data.device.PairingCapability
import com.domotics.smarthome.data.remote.ProvisioningApiService
import com.domotics.smarthome.provisioning.ProvisioningProgress as UiProgress
import com.domotics.smarthome.provisioning.ProvisioningResult as UiResult
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackendProvisioningRepositoryTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `provision emits progress and maps successful response`() = runTest {
        val responseBody = """
            {
              "device_id": "device-123",
              "strategy": "wifi",
              "adapter": "wifi_local",
              "status": "succeeded",
              "steps": [
                {"stage": "detect", "status": "succeeded", "detail": "Adapter connected"},
                {"stage": "precheck", "status": "succeeded", "detail": "Wi-Fi credentials validated"},
                {"stage": "provision", "status": "succeeded", "detail": "Payload delivered"},
                {"stage": "confirm", "status": "succeeded", "detail": "Provisioning confirmed"}
              ]
            }
        """.trimIndent()
        server.enqueue(MockResponse().setBody(responseBody).setResponseCode(200))

        val api = ProvisioningApiService.create(
            baseUrl = server.url("/").toString(),
            enableLogging = false,
        )
        val repository = BackendProvisioningRepository(api)
        val metadata = DiscoveryMetadata(supportsSoftAp = true)
        val device = DiscoveredDevice(
            id = "device-123",
            name = "Demo",
            pairingCapabilities = setOf(PairingCapability.SOFT_AP),
        )

        val progressUpdates = mutableListOf<UiProgress>()
        val result = repository.provision(
            device = device,
            metadata = metadata,
            credentials = WifiCredentials(ssid = "Home", password = "supersecret"),
            strategyId = "wifi",
        ) { progressUpdates.add(it) }

        val recordedRequest = server.takeRequest()
        assertEquals("/api/v1/provisioning/devices/device-123", recordedRequest.path)
        val body = recordedRequest.body.readUtf8()
        assertTrue(body.contains("\"device_type\":\"wifi\""))
        assertTrue(body.contains("\"capabilities\":[\"soft_ap\"]"))
        assertTrue(progressUpdates.isNotEmpty())
        assertTrue(result is UiResult.Success)
    }

    @Test
    fun `failed response is mapped to failure`() = runTest {
        val responseBody = """
            {
              "device_id": "device-456",
              "strategy": "wifi",
              "adapter": "wifi_local",
              "status": "failed",
              "steps": [
                {"stage": "detect", "status": "succeeded", "detail": "Adapter connected"},
                {"stage": "precheck", "status": "failed", "detail": "Adapter missing wifi capability"}
              ]
            }
        """.trimIndent()
        server.enqueue(MockResponse().setBody(responseBody).setResponseCode(200))

        val api = ProvisioningApiService.create(
            baseUrl = server.url("/").toString(),
            enableLogging = false,
        )
        val repository = BackendProvisioningRepository(api)
        val metadata = DiscoveryMetadata(supportsSoftAp = false)
        val device = DiscoveredDevice(
            id = "device-456",
            name = "Demo",
            pairingCapabilities = emptySet(),
        )

        val result = repository.provision(
            device = device,
            metadata = metadata,
            credentials = WifiCredentials(ssid = "Lab", password = "badpass"),
            strategyId = "wifi",
        ) { }

        assertTrue(result is UiResult.Failure)
        assertEquals("Adapter missing wifi capability", (result as UiResult.Failure).message)
    }
}
