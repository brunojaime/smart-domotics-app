package com.domotics.smarthome.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Interceptor that injects the latest access token into outbound requests.
 */
class JwtAuthorizationInterceptor(
    private val tokenStorage: TokenStorage,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val accessToken = tokenStorage.accessToken.value
        return if (accessToken.isNullOrBlank()) {
            chain.proceed(request)
        } else {
            val authorized = request.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
            chain.proceed(authorized)
        }
    }
}

/**
 * OkHttp authenticator that attempts to refresh JWTs when a request is rejected with 401.
 */
class JwtRefreshAuthenticator(
    private val tokenStorage: TokenStorage,
    private val authApiService: AuthApiService,
) : Authenticator {
    private val isRefreshing = AtomicBoolean(false)

    override fun authenticate(route: Route?, response: Response): Request? {
        // Avoid retry loops
        if (responseCount(response) >= 2) return null

        val refreshToken = tokenStorage.refreshToken.value ?: return null
        if (!isRefreshing.compareAndSet(false, true)) {
            // Another thread is refreshing; let it proceed and retry once with the new token
            return buildRequestWithLatestToken(response.request)
        }

        return try {
            val newTokens = runBlocking {
                authApiService.refresh(RefreshRequest(refreshToken))
            }
            runBlocking {
                tokenStorage.saveTokens(newTokens.accessToken, newTokens.refreshToken)
            }
            buildRequestWithLatestToken(response.request)
        } catch (_: Exception) {
            runBlocking { tokenStorage.clear() }
            null
        } finally {
            isRefreshing.set(false)
        }
    }

    private fun buildRequestWithLatestToken(request: Request): Request? {
        val latestToken = tokenStorage.accessToken.value ?: return null
        return request.newBuilder()
            .header("Authorization", "Bearer $latestToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var current = response.priorResponse
        var count = 1
        while (current != null) {
            count++
            current = current.priorResponse
        }
        return count
    }
}
