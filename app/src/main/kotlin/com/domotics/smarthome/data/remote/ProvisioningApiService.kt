package com.domotics.smarthome.data.remote

import com.domotics.smarthome.BuildConfig
import com.domotics.smarthome.data.auth.AuthInterceptor
import com.domotics.smarthome.data.auth.TokenProvider
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit client for the provisioning pipeline exposed by the backend.
 */
interface ProvisioningApiService {
    @POST("/api/v1/provisioning/devices/{deviceId}")
    suspend fun provisionDevice(
        @Path("deviceId") deviceId: String,
        @Body request: ProvisioningRequestDto,
    ): ProvisioningResultDto

    companion object {
        fun create(
            baseUrl: String = BuildConfig.API_BASE_URL,
            tokenProvider: TokenProvider? = null,
            enableLogging: Boolean = true,
        ): ProvisioningApiService {
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
                .create(ProvisioningApiService::class.java)
        }
    }
}

data class ProvisioningRequestDto(
    @SerializedName("device_id") val deviceId: String,
    val strategy: String?,
    val adapter: String? = null,
    @SerializedName("device_type") val deviceType: String,
    val capabilities: List<String> = emptyList(),
    val payload: Map<String, Any?> = emptyMap(),
)

data class ProvisioningResultDto(
    @SerializedName("device_id") val deviceId: String,
    val strategy: String,
    val adapter: String,
    val status: ProvisioningStatusDto,
    val steps: List<ProvisioningStepDto>,
)

enum class ProvisioningStatusDto {
    @SerializedName("pending")
    PENDING,

    @SerializedName("in_progress")
    IN_PROGRESS,

    @SerializedName("succeeded")
    SUCCEEDED,

    @SerializedName("failed")
    FAILED;

    companion object {
        fun from(raw: String?): ProvisioningStatusDto? =
            values().firstOrNull { it.name.equals(raw, ignoreCase = true) }
    }
}

data class ProvisioningStepDto(
    val stage: ProvisioningStageDto?,
    val status: ProvisioningStatusDto?,
    val detail: String?,
    val metadata: Map<String, Any?> = emptyMap(),
)

enum class ProvisioningStageDto {
    @SerializedName("detect")
    DETECT,

    @SerializedName("precheck")
    PRECHECK,

    @SerializedName("provision")
    PROVISION,

    @SerializedName("confirm")
    CONFIRM,

    @SerializedName("rollback")
    ROLLBACK;

    companion object {
        fun from(raw: String?): ProvisioningStageDto? =
            values().firstOrNull { it.name.equals(raw, ignoreCase = true) }
    }
}
