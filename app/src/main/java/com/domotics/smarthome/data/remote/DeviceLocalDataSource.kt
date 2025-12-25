package com.domotics.smarthome.data.remote

interface DeviceLocalDataSource {
    suspend fun saveDevice(metadata: DeviceMetadata)
    suspend fun refreshDevices()
}
