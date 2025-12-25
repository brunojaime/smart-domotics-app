package com.domotics.smarthome.data.remote

import com.google.gson.annotations.SerializedName

data class DeviceRegistrationRequest(
    @SerializedName("serialNumber") val serialNumber: String,
    @SerializedName("macAddress") val macAddress: String,
    @SerializedName("accessToken") val accessToken: String
)

data class DeviceRegistrationResponse(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("serialNumber") val serialNumber: String,
    @SerializedName("macAddress") val macAddress: String,
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("name") val name: String?,
    @SerializedName("ownerId") val ownerId: String?
)

data class DeviceMetadata(
    val deviceId: String,
    val serialNumber: String,
    val macAddress: String,
    val accessToken: String,
    val name: String? = null,
    val ownerId: String? = null
)

data class DeviceConflictResponse(
    @SerializedName("message") val message: String?
)
