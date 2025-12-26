package com.domotics.smarthome.data.remote

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Error(val message: String, val cause: Throwable? = null) : ApiResult<Nothing>
    data object Loading : ApiResult<Nothing>
}

fun <T> Result<T>.toApiResult(): ApiResult<T> = fold(
    onSuccess = { ApiResult.Success(it) },
    onFailure = { ApiResult.Error(it.message ?: "Unexpected error", it) },
)
