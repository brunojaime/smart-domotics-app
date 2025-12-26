package com.domotics.smarthome.data.remote

import com.domotics.smarthome.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object DomoticsApiClient {
    fun create(
        baseUrl: String = BuildConfig.API_BASE_URL,
        tokenStorage: TokenStorage = InMemoryTokenStorage(),
        enableLogging: Boolean = true,
    ): ApiClients {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val authClientBuilder = OkHttpClient.Builder()
        if (enableLogging) {
            authClientBuilder.addInterceptor(loggingInterceptor)
        }

        val authRetrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(authClientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val authApiService = authRetrofit.create(AuthApiService::class.java)

        val authedClientBuilder = OkHttpClient.Builder()
        if (enableLogging) {
            authedClientBuilder.addInterceptor(loggingInterceptor)
        }

        authedClientBuilder.addInterceptor(JwtAuthorizationInterceptor(tokenStorage))
        authedClientBuilder.authenticator(JwtRefreshAuthenticator(tokenStorage, authApiService))

        val authedRetrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(authedClientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val smartHomeApiService = authedRetrofit.create(SmartHomeApiService::class.java)

        return ApiClients(
            authApiService = authApiService,
            smartHomeApiService = smartHomeApiService,
            tokenStorage = tokenStorage,
        )
    }
}

data class ApiClients(
    val authApiService: AuthApiService,
    val smartHomeApiService: SmartHomeApiService,
    val tokenStorage: TokenStorage,
)
