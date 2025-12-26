package com.domotics.smarthome.data.remote

import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceRegistrationRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var apiService: DeviceApiService
    private lateinit var repository: DeviceRegistrationRepository
    private lateinit var localDataSource: TestDeviceLocalDataSource

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        apiService = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeviceApiService::class.java)

        localDataSource = TestDeviceLocalDataSource()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `registerDevice saves metadata and refreshes cache on success`() = runTest {
        val responseBody = DeviceRegistrationResponse(
            deviceId = "device-123",
            name = "Living Room Lamp",
            zoneId = "zone-1",
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody(Gson().toJson(responseBody)))

        repository = DeviceRegistrationRepository(
            apiService,
            localDataSource,
            testSchedulerCoroutineDispatcher(testScheduler)
        )
        val result = repository.registerDevice(
            locationId = "loc-1",
            buildingId = "bldg-1",
            zoneId = "zone-1",
            request = DeviceRegistrationRequest(deviceId = "device-123", name = "Living Room Lamp"),
            maxRetries = 0
        )

        assertTrue(result is DeviceRegistrationResult.Success)
        val metadata = (result as DeviceRegistrationResult.Success).metadata
        assertEquals(responseBody.deviceId, metadata.deviceId)
        assertEquals(responseBody.name, metadata.name)
        assertEquals(responseBody.zoneId, metadata.zoneId)
        val recordedRequest = server.takeRequest()
        assertEquals("/api/v1/locations/loc-1/buildings/bldg-1/zones/zone-1/devices", recordedRequest.path)
        assertTrue(localDataSource.refreshCount.get() > 0)
    }

    @Test
    fun `registerDevice returns conflict result without caching`() = runTest {
        server.enqueue(MockResponse().setResponseCode(409).setBody("{\"message\":\"Device already registered\"}"))

        repository = DeviceRegistrationRepository(
            apiService,
            localDataSource,
            testSchedulerCoroutineDispatcher(testScheduler)
        )
        val result = repository.registerDevice(
            locationId = "loc-1",
            buildingId = "bldg-1",
            zoneId = "zone-1",
            request = DeviceRegistrationRequest(deviceId = "serial-conflict", name = "Conflict"),
            maxRetries = 0,
        )

        assertTrue(result is DeviceRegistrationResult.Conflict)
        assertTrue(localDataSource.savedDevices.isEmpty())
        assertEquals(0, localDataSource.refreshCount.get())
    }

    @Test
    fun `registerDevice retries on transient errors then succeeds`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        repository = DeviceRegistrationRepository(
            apiService = apiService,
            localDataSource = localDataSource,
            ioDispatcher = dispatcher,
            delayProvider = { delayMs -> advanceTimeBy(delayMs) }
        )

        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                Gson().toJson(
                    DeviceRegistrationResponse(
                        deviceId = "device-retry",
                        name = "Garage Sensor",
                        zoneId = "zone-retry",
                    )
                )
            )
        )

        val result = repository.registerDevice(
            locationId = "loc-2",
            buildingId = "bldg-2",
            zoneId = "zone-retry",
            request = DeviceRegistrationRequest(deviceId = "device-retry", name = "Garage Sensor"),
            maxRetries = 2,
            initialDelayMs = 100
        )
        advanceUntilIdle()

        assertTrue(result is DeviceRegistrationResult.Success)
        assertEquals(2, server.requestCount)
        assertEquals(1, localDataSource.savedDevices.size)
        assertEquals(1, localDataSource.refreshCount.get())
    }

    private fun testSchedulerCoroutineDispatcher(
        testScheduler: TestCoroutineScheduler
    ) = StandardTestDispatcher(testScheduler)
}

class TestDeviceLocalDataSource : DeviceLocalDataSource {
    val savedDevices = mutableListOf<DeviceMetadata>()
    val refreshCount = AtomicInteger(0)

    override suspend fun saveDevice(metadata: DeviceMetadata) {
        savedDevices.add(metadata)
    }

    override suspend fun refreshDevices() {
        refreshCount.incrementAndGet()
    }
}
