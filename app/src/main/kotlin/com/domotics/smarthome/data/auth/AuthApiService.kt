package com.domotics.smarthome.data.auth

import com.domotics.smarthome.BuildConfig
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("/api/auth/login")
    suspend fun login(@Body payload: LoginRequest): AuthResponse

    @POST("/api/auth/register")
    suspend fun register(@Body payload: RegisterRequest): AuthResponse

    @POST("/api/auth/google")
    suspend fun googleSignIn(@Body payload: GoogleAuthRequest): AuthResponse

    @POST("/api/auth/oauth/callback")
    suspend fun exchangeCode(@Body payload: OAuthCallbackRequest): AuthResponse

    companion object {
        fun create(
            baseUrl: String = BuildConfig.API_BASE_URL,
            tokenProvider: TokenProvider? = null,
            enableLogging: Boolean = true,
        ): AuthApiService {
            val clientBuilder = OkHttpClient.Builder()
            if (tokenProvider != null) {
                clientBuilder.addInterceptor(AuthInterceptor(tokenProvider))
            }
            if (enableLogging) {
                val logger = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
                clientBuilder.addInterceptor(logger)
            }

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(clientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AuthApiService::class.java)
        }
    }
}

data class LoginRequest(
    val email: String,
    val password: String,
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
)

data class GoogleAuthRequest(
    @SerializedName("id_token") val idToken: String,
)

data class OAuthCallbackRequest(
    val code: String,
    val state: String?,
)

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String?,
    val user: AuthUser?,
)

data class AuthUser(
    val id: String?,
    val email: String?,
    val name: String?,
)
