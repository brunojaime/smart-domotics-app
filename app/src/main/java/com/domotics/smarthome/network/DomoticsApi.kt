package com.domotics.smarthome.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit definitions for the hierarchical Location -> Building -> Zone -> Device API.
 */
interface DomoticsApi {
    @GET("/api/v1/locations")
    suspend fun listLocations(): List<LocationDto>

    @POST("/api/v1/locations")
    suspend fun createLocation(@Body payload: LocationCreate): LocationDto

    @GET("/api/v1/locations/{locationId}/buildings")
    suspend fun listBuildings(@Path("locationId") locationId: String): List<BuildingDto>

    @POST("/api/v1/locations/{locationId}/buildings")
    suspend fun createBuilding(
        @Path("locationId") locationId: String,
        @Body payload: BuildingCreate,
    ): BuildingDto

    @GET("/api/v1/locations/{locationId}/buildings/{buildingId}/zones")
    suspend fun listZones(
        @Path("locationId") locationId: String,
        @Path("buildingId") buildingId: String,
    ): List<ZoneDto>

    @POST("/api/v1/locations/{locationId}/buildings/{buildingId}/zones")
    suspend fun createZone(
        @Path("locationId") locationId: String,
        @Path("buildingId") buildingId: String,
        @Body payload: ZoneCreate,
    ): ZoneDto

    @GET("/api/v1/locations/{locationId}/buildings/{buildingId}/zones/{zoneId}/devices")
    suspend fun listZoneDevices(
        @Path("locationId") locationId: String,
        @Path("buildingId") buildingId: String,
        @Path("zoneId") zoneId: String,
    ): List<ZoneDeviceDto>

    @POST("/api/v1/locations/{locationId}/buildings/{buildingId}/zones/{zoneId}/devices")
    suspend fun createZoneDevice(
        @Path("locationId") locationId: String,
        @Path("buildingId") buildingId: String,
        @Path("zoneId") zoneId: String,
        @Body payload: ZoneDeviceCreate,
    ): ZoneDeviceDto

    @DELETE("/api/v1/locations/{locationId}/buildings/{buildingId}/zones/{zoneId}/devices/{deviceId}")
    suspend fun deleteZoneDevice(
        @Path("locationId") locationId: String,
        @Path("buildingId") buildingId: String,
        @Path("zoneId") zoneId: String,
        @Path("deviceId") deviceId: String,
    )
}

// DTOs mirror the backend payloads for the hierarchy

data class LocationDto(val id: String, val name: String)

data class LocationCreate(val name: String)

data class BuildingDto(val id: String, val name: String, val location_id: String)

data class BuildingCreate(val name: String)

data class ZoneDto(val id: String, val name: String, val building_id: String)

data class ZoneCreate(val name: String)

data class ZoneDeviceDto(val device_id: String, val name: String, val zone_id: String)

data class ZoneDeviceCreate(val device_id: String, val name: String)
