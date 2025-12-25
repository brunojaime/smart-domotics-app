package com.domotics.smarthome.data.remote

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import kotlinx.coroutines.delay

sealed interface DeviceRegistrationResult {
    data class Success(val metadata: DeviceMetadata) : DeviceRegistrationResult
    data class Conflict(val reason: String?) : DeviceRegistrationResult
    data class TransientFailure(val lastError: Throwable) : DeviceRegistrationResult
    data class UnexpectedFailure(val lastError: Throwable) : DeviceRegistrationResult
}

class DeviceRegistrationRepository(
    private val apiService: DeviceApiService,
    private val localDataSource: DeviceLocalDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val delayProvider: suspend (Long) -> Unit = { delay(it) }
) {

    suspend fun registerDevice(
        request: DeviceRegistrationRequest,
        maxRetries: Int = 3,
        initialDelayMs: Long = 500L
    ): DeviceRegistrationResult = withContext(ioDispatcher) {
        var delayMs = initialDelayMs
        var lastError: Throwable? = null

        repeat(maxRetries + 1) { attempt ->
            try {
                val response = apiService.registerDevice(request)
                if (response.isSuccessful) {
                    val body = response.body()
                        ?: return@withContext DeviceRegistrationResult.UnexpectedFailure(
                            IllegalStateException("Empty response from registerDevice")
                        )

                    val metadata = DeviceMetadata(
                        deviceId = body.deviceId,
                        serialNumber = body.serialNumber,
                        macAddress = body.macAddress,
                        accessToken = body.accessToken,
                        name = body.name,
                        ownerId = body.ownerId
                    )
                    localDataSource.saveDevice(metadata)
                    localDataSource.refreshDevices()
                    return@withContext DeviceRegistrationResult.Success(metadata)
                }

                if (response.code() == 409) {
                    return@withContext DeviceRegistrationResult.Conflict(response.errorBody()?.string())
                }

                if (response.code() >= 500) {
                    lastError = HttpException(response)
                } else {
                    return@withContext DeviceRegistrationResult.UnexpectedFailure(HttpException(response))
                }
            } catch (ioException: IOException) {
                lastError = ioException
            }

            if (attempt < maxRetries) {
                delayProvider(delayMs)
                delayMs *= 2
            }
        }

        val error = lastError ?: IllegalStateException("Unknown transient failure")
        if (error is IOException || (error is HttpException && error.code() >= 500)) {
            DeviceRegistrationResult.TransientFailure(error)
        } else {
            DeviceRegistrationResult.UnexpectedFailure(error)
        }
    }
}
