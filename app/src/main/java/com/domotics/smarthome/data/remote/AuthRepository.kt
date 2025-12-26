package com.domotics.smarthome.data.remote

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val authApiService: AuthApiService,
    private val tokenStorage: TokenStorage,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun login(username: String, password: String): ApiResult<Unit> = withContext(ioDispatcher) {
        runCatching {
            authApiService.login(LoginRequest(username, password))
        }.mapCatching { tokens ->
            tokenStorage.saveTokens(tokens.accessToken, tokens.refreshToken)
        }.toApiResult()
    }

    suspend fun refresh(): ApiResult<Unit> = withContext(ioDispatcher) {
        val refreshToken = tokenStorage.refreshToken.value
            ?: return@withContext ApiResult.Error("No refresh token available")

        runCatching {
            authApiService.refresh(RefreshRequest(refreshToken))
        }.mapCatching { tokens ->
            tokenStorage.saveTokens(tokens.accessToken, tokens.refreshToken)
        }.toApiResult()
    }

    suspend fun logout() {
        tokenStorage.clear()
    }
}
