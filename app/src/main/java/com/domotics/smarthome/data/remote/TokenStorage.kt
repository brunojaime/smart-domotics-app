package com.domotics.smarthome.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface TokenStorage {
    val accessToken: StateFlow<String?>
    val refreshToken: StateFlow<String?>

    suspend fun saveTokens(accessToken: String, refreshToken: String)
    suspend fun clear()
}

class InMemoryTokenStorage : TokenStorage {
    private val mutex = Mutex()
    private val _accessToken = MutableStateFlow<String?>(null)
    private val _refreshToken = MutableStateFlow<String?>(null)

    override val accessToken: StateFlow<String?> = _accessToken
    override val refreshToken: StateFlow<String?> = _refreshToken

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        mutex.withLock {
            _accessToken.value = accessToken
            _refreshToken.value = refreshToken
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            _accessToken.value = null
            _refreshToken.value = null
        }
    }
}
