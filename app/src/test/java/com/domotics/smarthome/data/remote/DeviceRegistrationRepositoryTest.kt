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
            serialNumber = "serial-xyz",
            macAddress = "00:11:22:33:44:55",
            accessToken = "token-abc",
            name = "Living Room Lamp",
            ownerId = "user-1"
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody(Gson().toJson(responseBody)))

        repository = DeviceRegistrationRepository(
            apiService,
            localDataSource,
            testSchedulerCoroutineDispatcher(testScheduler)
        )
        val result = repository.registerDevice(
            DeviceRegistrationRequest("serial-xyz", "00:11:22:33:44:55", "token-abc"),
            maxRetries = 0
        )

        assertTrue(result is DeviceRegistrationResult.Success)
        val metadata = (result as DeviceRegistrationResult.Success).metadata
        assertEquals(responseBody.deviceId, metadata.deviceId)
        assertEquals(responseBody.serialNumber, metadata.serialNumber)
        assertEquals(responseBody.macAddress, metadata.macAddress)
        assertEquals(responseBody.accessToken, metadata.accessToken)
        assertEquals(responseBody.name, metadata.name)
        assertEquals(1, localDataSource.savedDevices.size)
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
            DeviceRegistrationRequest("serial-conflict", "AA:BB:CC:DD:EE:FF", "token-conflict"),
            maxRetries = 0
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
                        serialNumber = "serial-retry",
                        macAddress = "11:22:33:44:55:66",
                        accessToken = "token-retry",
                        name = "Garage Sensor",
                        ownerId = "user-2"
                    )
                )
            )
        )

        val result = repository.registerDevice(
            DeviceRegistrationRequest("serial-retry", "11:22:33:44:55:66", "token-retry"),
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
