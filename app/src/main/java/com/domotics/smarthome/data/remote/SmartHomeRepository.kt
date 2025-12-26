package com.domotics.smarthome.data.remote

import com.domotics.smarthome.entities.Area
import com.domotics.smarthome.entities.Building
import com.domotics.smarthome.entities.Location
import com.domotics.smarthome.entities.Zone
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class SmartHomeRepository(
    private val apiService: SmartHomeApiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    fun fetchLocations(): Flow<ApiResult<List<Location>>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest { apiService.listLocations().map { it.toEntity() } })
    }

    fun createLocation(request: CreateLocationRequest): Flow<ApiResult<Location>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest { apiService.createLocation(request).toEntity() })
    }

    fun updateLocation(id: String, request: CreateLocationRequest): Flow<ApiResult<Location>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest { apiService.updateLocation(id, request).toEntity() })
    }

    fun deleteLocation(id: String): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest {
            val response = apiService.deleteLocation(id)
            if (!response.isSuccessful) {
                throw IllegalStateException("Failed to delete location (${response.code()})")
            }
        })
    }

    fun fetchBuildings(locationId: String): Flow<ApiResult<List<Building>>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest { apiService.listBuildings(locationId).map { it.toEntity() } })
    }

    fun createBuilding(locationId: String, request: CreateBuildingRequest): Flow<ApiResult<Building>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest { apiService.createBuilding(locationId, request).toEntity() })
    }

    fun updateBuilding(
        locationId: String,
        id: String,
        request: CreateBuildingRequest,
    ): Flow<ApiResult<Building>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest { apiService.updateBuilding(locationId, id, request).toEntity() })
    }

    fun deleteBuilding(locationId: String, id: String): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest {
            val response = apiService.deleteBuilding(locationId, id)
            if (!response.isSuccessful) {
                throw IllegalStateException("Failed to delete building (${response.code()})")
            }
        })
    }

    fun fetchZones(locationId: String, buildingId: String): Flow<ApiResult<List<Zone>>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest { apiService.listZones(locationId, buildingId).map { it.toEntity() } })
    }

    fun createZone(
        locationId: String,
        buildingId: String,
        request: CreateZoneRequest,
    ): Flow<ApiResult<Zone>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest { apiService.createZone(locationId, buildingId, request).toEntity() })
    }

    fun updateZone(
        locationId: String,
        buildingId: String,
        id: String,
        request: CreateZoneRequest,
    ): Flow<ApiResult<Zone>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest { apiService.updateZone(locationId, buildingId, id, request).toEntity() })
    }

    fun deleteZone(locationId: String, buildingId: String, id: String): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest {
            val response = apiService.deleteZone(locationId, buildingId, id)
            if (!response.isSuccessful) {
                throw IllegalStateException("Failed to delete zone (${response.code()})")
            }
        })
    }

    fun fetchAreas(locationId: String, buildingId: String, zoneId: String): Flow<ApiResult<List<Area>>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest { apiService.listAreas(locationId, buildingId, zoneId).map { it.toEntity() } })
    }

    fun createArea(
        locationId: String,
        buildingId: String,
        zoneId: String,
        request: CreateAreaRequest,
    ): Flow<ApiResult<Area>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest { apiService.createArea(locationId, buildingId, zoneId, request).toEntity() })
    }

    fun updateArea(
        locationId: String,
        buildingId: String,
        zoneId: String,
        id: String,
        request: CreateAreaRequest,
    ): Flow<ApiResult<Area>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest { apiService.updateArea(locationId, buildingId, zoneId, id, request).toEntity() })
    }

    fun deleteArea(locationId: String, buildingId: String, zoneId: String, id: String): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest {
            val response = apiService.deleteArea(locationId, buildingId, zoneId, id)
            if (!response.isSuccessful) {
                throw IllegalStateException("Failed to delete area (${response.code()})")
            }
        })
    }

    fun fetchDevices(locationId: String, buildingId: String, zoneId: String): Flow<ApiResult<List<DeviceMetadata>>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest { apiService.listDevices(locationId, buildingId, zoneId).map { it.toEntity() } })
    }

    fun createDevice(
        locationId: String,
        buildingId: String,
        zoneId: String,
        request: CreateDeviceRequest,
    ): Flow<ApiResult<DeviceMetadata>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest { apiService.createDevice(locationId, buildingId, zoneId, request).toEntity() })
    }

    fun deleteDevice(locationId: String, buildingId: String, zoneId: String, deviceId: String): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest {
            val response = apiService.deleteDevice(locationId, buildingId, zoneId, deviceId)
            if (!response.isSuccessful) {
                throw IllegalStateException("Failed to delete device (${response.code()})")
            }
        })
    }

    private suspend fun <T> executeRequest(block: suspend () -> T): ApiResult<T> = withContext(ioDispatcher) {
        runCatching { block() }.toApiResult()
    }
}
