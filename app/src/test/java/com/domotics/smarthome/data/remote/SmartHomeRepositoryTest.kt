package com.domotics.smarthome.data.remote

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class SmartHomeRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var api: SmartHomeApiService
    private lateinit var repository: SmartHomeRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SmartHomeApiService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `create building hits nested endpoint`() = runTest {
        repository = SmartHomeRepository(api, StandardTestDispatcher(testScheduler))
        val responseBody = """
            {"id":"b1","name":"HQ","location_id":"loc-1","zone_ids":["z1"]}
        """.trimIndent()
        server.enqueue(MockResponse().setBody(responseBody))

        val result = repository.createBuilding("loc-1", CreateBuildingRequest(name = "HQ"))
            .filterIsInstance<ApiResult.Success<*>>()
            .first()

        val request = server.takeRequest()
        assertEquals("/api/v1/locations/loc-1/buildings", request.path)
        assertTrue(request.body.readUtf8().contains("\"name\":\"HQ\""))
        val building = result.data as com.domotics.smarthome.entities.Building
        assertEquals("loc-1", building.locationId)
        assertEquals(listOf("z1"), building.zoneIds)
    }

    @Test
    fun `create zone includes building and location in path`() = runTest {
        repository = SmartHomeRepository(api, StandardTestDispatcher(testScheduler))
        val responseBody = """
            {"id":"z1","name":"Lobby","building_id":"b1","area_ids":[]}
        """.trimIndent()
        server.enqueue(MockResponse().setBody(responseBody))

        val result = repository.createZone("loc-1", "b1", CreateZoneRequest(name = "Lobby"))
            .filterIsInstance<ApiResult.Success<*>>()
            .first()

        val request = server.takeRequest()
        assertEquals("/api/v1/locations/loc-1/buildings/b1/zones", request.path)
        val zone = result.data as com.domotics.smarthome.entities.Zone
        assertEquals("b1", zone.buildingId)
    }

    @Test
    fun `create device threads hierarchy`() = runTest {
        repository = SmartHomeRepository(api, StandardTestDispatcher(testScheduler))
        val responseBody = """
            {"device_id":"dev1","name":"Sensor","zone_id":"zone-1"}
        """.trimIndent()
        server.enqueue(MockResponse().setBody(responseBody))

        val result = repository.createDevice(
            "loc-1",
            "b1",
            "zone-1",
            CreateDeviceRequest(deviceId = "dev1", name = "Sensor")
        ).filterIsInstance<ApiResult.Success<*>>()
            .first()

        val request = server.takeRequest()
        assertEquals("/api/v1/locations/loc-1/buildings/b1/zones/zone-1/devices", request.path)
        assertTrue(request.body.readUtf8().contains("\"device_id\":\"dev1\""))
        val device = result.data as DeviceMetadata
        assertEquals("zone-1", device.zoneId)
    }
}
