package com.domotics.smarthome.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface DeviceApiService {
    @POST("/api/v1/locations/{locationId}/buildings/{buildingId}/zones/{zoneId}/devices")
    suspend fun registerDevice(
        @Path("locationId") locationId: String,
        @Path("buildingId") buildingId: String,
        @Path("zoneId") zoneId: String,
        @Body request: DeviceRegistrationRequest,
    ): Response<DeviceRegistrationResponse>
}
