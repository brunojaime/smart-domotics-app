package com.domotics.smarthome.data.remote

import com.domotics.smarthome.BuildConfig
import com.domotics.smarthome.data.auth.AuthInterceptor
import com.domotics.smarthome.data.auth.TokenProvider
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Header
import retrofit2.http.POST

private val DEFAULT_BASE_URL = BuildConfig.API_BASE_URL
private const val DEFAULT_APP_TOKEN = "user_demo"

/**
 * Simple Retrofit client for the broker API used to mint MQTT credentials.
 */
interface BrokerApiService {
    @POST("/api/auth/mqtt")
    suspend fun issueMqttCredentials(
        @Header("Authorization") bearer: String,
    ): MqttCredentialsDto

    companion object {
        fun create(
            baseUrl: String = DEFAULT_BASE_URL,
            tokenProvider: TokenProvider? = null,
            enableLogging: Boolean = true,
        ): BrokerApiService {
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
                .create(BrokerApiService::class.java)
        }
    }
}

/**
 * Mirrors the backend `/api/auth/mqtt` payload.
 */
data class MqttCredentialsDto(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    @SerializedName("client_id")
    val clientId: String,
    val topics: List<String>,
    @SerializedName("expires_in_seconds")
    val expiresInSeconds: Int,
) {
    val isLocalBroker: Boolean
        get() = host == "localhost" || host == "127.0.0.1"
}

object BrokerApiDefaults {
    val baseUrl: String = DEFAULT_BASE_URL
    const val demoToken: String = DEFAULT_APP_TOKEN
}
