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
        emit(executeRequest { apiService.listLocations().items.map { it.toEntity() } })
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

    fun fetchBuildings(): Flow<ApiResult<List<Building>>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest { apiService.listBuildings().items.map { it.toEntity() } })
    }

    fun createBuilding(request: CreateBuildingRequest): Flow<ApiResult<Building>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest { apiService.createBuilding(request).toEntity() })
    }

    fun updateBuilding(id: String, request: CreateBuildingRequest): Flow<ApiResult<Building>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest { apiService.updateBuilding(id, request).toEntity() })
    }

    fun deleteBuilding(id: String): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest {
            val response = apiService.deleteBuilding(id)
            if (!response.isSuccessful) {
                throw IllegalStateException("Failed to delete building (${response.code()})")
            }
        })
    }

    fun fetchAreas(): Flow<ApiResult<List<Area>>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest { apiService.listAreas().items.map { it.toEntity() } })
    }

    fun createArea(request: CreateAreaRequest): Flow<ApiResult<Area>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest { apiService.createArea(request).toEntity() })
    }

    fun updateArea(id: String, request: CreateAreaRequest): Flow<ApiResult<Area>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest { apiService.updateArea(id, request).toEntity() })
    }

    fun deleteArea(id: String): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest {
            val response = apiService.deleteArea(id)
            if (!response.isSuccessful) {
                throw IllegalStateException("Failed to delete area (${response.code()})")
            }
        })
    }

    fun fetchZones(): Flow<ApiResult<List<Zone>>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest { apiService.listZones().items.map { it.toEntity() } })
    }

    fun createZone(request: CreateZoneRequest): Flow<ApiResult<Zone>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest { apiService.createZone(request).toEntity() })
    }

    fun updateZone(id: String, request: CreateZoneRequest): Flow<ApiResult<Zone>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest { apiService.updateZone(id, request).toEntity() })
    }

    fun deleteZone(id: String): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(executeRequest {
            val response = apiService.deleteZone(id)
            if (!response.isSuccessful) {
                throw IllegalStateException("Failed to delete zone (${response.code()})")
            }
        })
    }

    private suspend fun <T> executeRequest(block: suspend () -> T): ApiResult<T> = withContext(ioDispatcher) {
        runCatching { block() }.toApiResult()
    }
}
