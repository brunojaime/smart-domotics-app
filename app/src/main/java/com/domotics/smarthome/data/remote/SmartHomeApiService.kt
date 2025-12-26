package com.domotics.smarthome.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface SmartHomeApiService {
    // Locations
    @GET("/api/v1/locations")
    suspend fun listLocations(): List<LocationDto>

    @POST("/api/v1/locations")
    suspend fun createLocation(@Body request: CreateLocationRequest): LocationDto

    @PUT("/api/v1/locations/{id}")
    suspend fun updateLocation(
        @Path("id") id: String,
        @Body request: CreateLocationRequest,
    ): LocationDto

    @DELETE("/api/v1/locations/{id}")
    suspend fun deleteLocation(@Path("id") id: String): Response<Unit>

    // Buildings
    @GET("/api/v1/locations/{locationId}/buildings")
    suspend fun listBuildings(@Path("locationId") locationId: String): List<BuildingDto>

    @POST("/api/v1/locations/{locationId}/buildings")
    suspend fun createBuilding(
        @Path("locationId") locationId: String,
        @Body request: CreateBuildingRequest,
    ): BuildingDto

    @PUT("/api/v1/locations/{locationId}/buildings/{id}")
    suspend fun updateBuilding(
        @Path("locationId") locationId: String,
        @Path("id") id: String,
        @Body request: CreateBuildingRequest,
    ): BuildingDto

    @DELETE("/api/v1/locations/{locationId}/buildings/{id}")
    suspend fun deleteBuilding(
        @Path("locationId") locationId: String,
        @Path("id") id: String,
    ): Response<Unit>

    // Zones
    @GET("/api/v1/locations/{locationId}/buildings/{buildingId}/zones")
    suspend fun listZones(
        @Path("locationId") locationId: String,
        @Path("buildingId") buildingId: String,
    ): List<ZoneDto>

    @POST("/api/v1/locations/{locationId}/buildings/{buildingId}/zones")
    suspend fun createZone(
        @Path("locationId") locationId: String,
        @Path("buildingId") buildingId: String,
        @Body request: CreateZoneRequest,
    ): ZoneDto

    @PUT("/api/v1/locations/{locationId}/buildings/{buildingId}/zones/{id}")
    suspend fun updateZone(
        @Path("locationId") locationId: String,
        @Path("buildingId") buildingId: String,
        @Path("id") id: String,
        @Body request: CreateZoneRequest,
    ): ZoneDto

    @DELETE("/api/v1/locations/{locationId}/buildings/{buildingId}/zones/{id}")
    suspend fun deleteZone(
        @Path("locationId") locationId: String,
        @Path("buildingId") buildingId: String,
        @Path("id") id: String,
    ): Response<Unit>

    // Areas
    @GET("/api/v1/locations/{locationId}/buildings/{buildingId}/zones/{zoneId}/areas")
    suspend fun listAreas(
        @Path("locationId") locationId: String,
        @Path("buildingId") buildingId: String,
        @Path("zoneId") zoneId: String,
    ): List<AreaDto>

    @POST("/api/v1/locations/{locationId}/buildings/{buildingId}/zones/{zoneId}/areas")
    suspend fun createArea(
        @Path("locationId") locationId: String,
        @Path("buildingId") buildingId: String,
        @Path("zoneId") zoneId: String,
        @Body request: CreateAreaRequest,
    ): AreaDto

    @PUT("/api/v1/locations/{locationId}/buildings/{buildingId}/zones/{zoneId}/areas/{id}")
    suspend fun updateArea(
        @Path("locationId") locationId: String,
        @Path("buildingId") buildingId: String,
        @Path("zoneId") zoneId: String,
        @Path("id") id: String,
        @Body request: CreateAreaRequest,
    ): AreaDto

    @DELETE("/api/v1/locations/{locationId}/buildings/{buildingId}/zones/{zoneId}/areas/{id}")
    suspend fun deleteArea(
        @Path("locationId") locationId: String,
        @Path("buildingId") buildingId: String,
        @Path("zoneId") zoneId: String,
        @Path("id") id: String,
    ): Response<Unit>

    // Devices
    @GET("/api/v1/locations/{locationId}/buildings/{buildingId}/zones/{zoneId}/devices")
    suspend fun listDevices(
        @Path("locationId") locationId: String,
        @Path("buildingId") buildingId: String,
        @Path("zoneId") zoneId: String,
    ): List<DeviceDto>

    @POST("/api/v1/locations/{locationId}/buildings/{buildingId}/zones/{zoneId}/devices")
    suspend fun createDevice(
        @Path("locationId") locationId: String,
        @Path("buildingId") buildingId: String,
        @Path("zoneId") zoneId: String,
        @Body request: CreateDeviceRequest,
    ): DeviceDto

    @DELETE("/api/v1/locations/{locationId}/buildings/{buildingId}/zones/{zoneId}/devices/{deviceId}")
    suspend fun deleteDevice(
        @Path("locationId") locationId: String,
        @Path("buildingId") buildingId: String,
        @Path("zoneId") zoneId: String,
        @Path("deviceId") deviceId: String,
    ): Response<Unit>
}

data class LocationDto(
    val id: String,
    val name: String,
    @SerializedName("building_ids")
    val buildingIds: List<String> = emptyList(),
) {
    fun toEntity(): com.domotics.smarthome.entities.Location =
        com.domotics.smarthome.entities.Location(id = id, name = name, buildingIds = buildingIds)
}

data class CreateLocationRequest(
    val name: String,
)

data class BuildingDto(
    val id: String,
    val name: String,
    @SerializedName("location_id")
    val locationId: String,
    @SerializedName("zone_ids")
    val zoneIds: List<String> = emptyList(),
) {
    fun toEntity(): com.domotics.smarthome.entities.Building =
        com.domotics.smarthome.entities.Building(
            id = id,
            name = name,
            locationId = locationId,
            zoneIds = zoneIds.toMutableList(),
        )
}

data class CreateBuildingRequest(
    val name: String,
)

data class ZoneDto(
    val id: String,
    val name: String,
    @SerializedName("building_id")
    val buildingId: String,
    @SerializedName("area_ids")
    val areaIds: List<String> = emptyList(),
) {
    fun toEntity(): com.domotics.smarthome.entities.Zone =
        com.domotics.smarthome.entities.Zone(
            id = id,
            name = name,
            buildingId = buildingId,
            areaIds = areaIds.toMutableList(),
        )
}

data class CreateZoneRequest(
    val name: String,
)

data class AreaDto(
    val id: String,
    val name: String,
    @SerializedName("zone_id")
    val zoneId: String,
) {
    fun toEntity(): com.domotics.smarthome.entities.Area =
        com.domotics.smarthome.entities.Area(
            id = id,
            name = name,
            zoneId = zoneId,
        )
}

data class CreateAreaRequest(
    val name: String,
)

data class DeviceDto(
    @SerializedName("device_id")
    val deviceId: String,
    val name: String,
    @SerializedName("zone_id")
    val zoneId: String,
) {
    fun toEntity(): DeviceMetadata = DeviceMetadata(deviceId = deviceId, name = name, zoneId = zoneId)
}

data class CreateDeviceRequest(
    @SerializedName("device_id")
    val deviceId: String,
    val name: String,
)
