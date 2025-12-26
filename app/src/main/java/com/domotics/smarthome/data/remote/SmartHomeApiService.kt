package com.domotics.smarthome.data.remote

import com.domotics.smarthome.entities.Area
import com.domotics.smarthome.entities.Building
import com.domotics.smarthome.entities.Location
import com.domotics.smarthome.entities.Zone
import com.domotics.smarthome.entities.ZoneType
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
    @GET("/api/locations")
    suspend fun listLocations(): PagedResponse<LocationDto>

    @POST("/api/locations")
    suspend fun createLocation(@Body request: CreateLocationRequest): LocationDto

    @PUT("/api/locations/{id}")
    suspend fun updateLocation(
        @Path("id") id: String,
        @Body request: CreateLocationRequest,
    ): LocationDto

    @DELETE("/api/locations/{id}")
    suspend fun deleteLocation(@Path("id") id: String): Response<Unit>

    // Buildings
    @GET("/api/buildings")
    suspend fun listBuildings(): PagedResponse<BuildingDto>

    @POST("/api/buildings")
    suspend fun createBuilding(@Body request: CreateBuildingRequest): BuildingDto

    @PUT("/api/buildings/{id}")
    suspend fun updateBuilding(
        @Path("id") id: String,
        @Body request: CreateBuildingRequest,
    ): BuildingDto

    @DELETE("/api/buildings/{id}")
    suspend fun deleteBuilding(@Path("id") id: String): Response<Unit>

    // Areas
    @GET("/api/areas")
    suspend fun listAreas(): PagedResponse<AreaDto>

    @POST("/api/areas")
    suspend fun createArea(@Body request: CreateAreaRequest): AreaDto

    @PUT("/api/areas/{id}")
    suspend fun updateArea(
        @Path("id") id: String,
        @Body request: CreateAreaRequest,
    ): AreaDto

    @DELETE("/api/areas/{id}")
    suspend fun deleteArea(@Path("id") id: String): Response<Unit>

    // Zones
    @GET("/api/zones")
    suspend fun listZones(): PagedResponse<ZoneDto>

    @POST("/api/zones")
    suspend fun createZone(@Body request: CreateZoneRequest): ZoneDto

    @PUT("/api/zones/{id}")
    suspend fun updateZone(
        @Path("id") id: String,
        @Body request: CreateZoneRequest,
    ): ZoneDto

    @DELETE("/api/zones/{id}")
    suspend fun deleteZone(@Path("id") id: String): Response<Unit>
}

data class PagedResponse<T>(
    val items: List<T>,
)

data class LocationDto(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val reference: String?,
) {
    fun toEntity(): Location = Location(
        latitude = latitude,
        longitude = longitude,
        reference = reference,
    )
}

data class CreateLocationRequest(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val reference: String?,
)

data class BuildingDto(
    val id: String,
    val name: String,
    val description: String?,
    val location: LocationDto,
    val zones: List<ZoneDto>,
) {
    fun toEntity(): Building = Building(
        id = id,
        name = name,
        location = location.toEntity(),
        zones = zones.map { it.toEntity() }.toMutableList(),
        description = description,
    )
}

data class CreateBuildingRequest(
    val name: String,
    val description: String?,
    @SerializedName("location_id")
    val locationId: String,
)

data class ZoneDto(
    val id: String,
    val name: String,
    val floor: Int,
    val area: AreaDto,
    @SerializedName("zone_type")
    val zoneType: String?,
) {
    fun toEntity(): Zone = Zone(
        id = id,
        name = name,
        floor = floor,
        area = area.toEntity(),
        zoneType = zoneType?.let { type ->
            ZoneType.values().firstOrNull { it.name.equals(type, ignoreCase = true) }
        }
    )
}

data class CreateZoneRequest(
    val name: String,
    val floor: Int,
    @SerializedName("area_id")
    val areaId: String,
    @SerializedName("zone_type")
    val zoneType: String?,
)

data class AreaDto(
    val id: String,
    val name: String,
    @SerializedName("square_meters")
    val squareMeters: Double?,
) {
    fun toEntity(): Area = Area(
        id = id,
        name = name,
        squareMeters = squareMeters,
    )
}

data class CreateAreaRequest(
    val name: String,
    @SerializedName("square_meters")
    val squareMeters: Double?,
)
