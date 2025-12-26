package com.domotics.smarthome.data.device

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface DeviceScanner {
    suspend fun discover(): List<DiscoveredDevice>
}

interface DeviceDiscoveryRepository {
    fun discoverDevices(): Flow<DiscoveryState>
}

class DeviceDiscoveryRepositoryImpl(
    private val scanners: List<DeviceScanner>,
) : DeviceDiscoveryRepository {

    override fun discoverDevices(): Flow<DiscoveryState> = flow {
        if (scanners.isEmpty()) {
            emit(DiscoveryState.NoResults)
            return@flow
        }

        emit(DiscoveryState.Scanning(progress = 0))
        val allDevices = mutableListOf<DiscoveredDevice>()

        scanners.forEachIndexed { index, scanner ->
            try {
                val devices = scanner.discover()
                allDevices += devices
                val progress = ((index + 1) * 100) / scanners.size
                emit(DiscoveryState.Scanning(progress = progress))
            } catch (ex: Exception) {
                emit(DiscoveryState.Error(ex.message ?: "Discovery failed"))
                return@flow
            }
        }

        if (allDevices.isNotEmpty()) {
            emit(DiscoveryState.Results(allDevices))
        } else {
            emit(DiscoveryState.NoResults)
        }
    }
}
