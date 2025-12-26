package com.domotics.smarthome.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthTokensDto

    @POST("/api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): AuthTokensDto
}

data class LoginRequest(
    val username: String,
    val password: String,
)

data class RefreshRequest(
    @SerializedName("refresh_token")
    val refreshToken: String,
)

data class AuthTokensDto(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("expires_in_seconds")
    val expiresInSeconds: Long,
)
