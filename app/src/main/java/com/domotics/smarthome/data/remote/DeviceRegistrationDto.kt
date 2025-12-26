package com.domotics.smarthome.data.remote

import com.google.gson.annotations.SerializedName

data class DeviceRegistrationRequest(
    @SerializedName("device_id") val deviceId: String,
    val name: String,
)

data class DeviceRegistrationResponse(
    @SerializedName("device_id") val deviceId: String,
    val name: String,
    @SerializedName("zone_id") val zoneId: String,
)

data class DeviceMetadata(
    val deviceId: String,
    val name: String,
    val zoneId: String,
)

data class DeviceConflictResponse(
    @SerializedName("message") val message: String?,
)
