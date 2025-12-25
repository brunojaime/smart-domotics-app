package com.domotics.smarthome.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface DeviceApiService {
    @POST("/devices/register")
    suspend fun registerDevice(
        @Body request: DeviceRegistrationRequest
    ): Response<DeviceRegistrationResponse>
}
