package com.domotics.smarthome.data.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val api: AuthApiService,
) {
    suspend fun login(email: String, password: String): AuthTokens =
        withContext(Dispatchers.IO) {
            api.login(LoginRequest(email = email, password = password)).toTokens()
        }

    suspend fun register(name: String, email: String, password: String): AuthTokens =
        withContext(Dispatchers.IO) {
            api.register(RegisterRequest(name = name, email = email, password = password)).toTokens()
        }

    suspend fun googleSignIn(idToken: String): AuthTokens =
        withContext(Dispatchers.IO) {
            api.googleSignIn(GoogleAuthRequest(idToken = idToken)).toTokens()
        }

    suspend fun exchangeOAuthCode(code: String, state: String?): AuthTokens =
        withContext(Dispatchers.IO) {
            api.exchangeCode(OAuthCallbackRequest(code = code, state = state)).toTokens()
        }
}

private fun AuthResponse.toTokens(): AuthTokens = AuthTokens(
    accessToken = accessToken,
    refreshToken = refreshToken,
)
