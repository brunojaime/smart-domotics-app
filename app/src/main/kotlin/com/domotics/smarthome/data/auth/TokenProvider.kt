package com.domotics.smarthome.data.auth

/**
 * Provides access tokens for authenticated API calls.
 */
interface TokenProvider {
    fun currentToken(): String?
}

class InMemoryTokenProvider(private var token: String? = null) : TokenProvider {
    override fun currentToken(): String? = token

    fun update(tokenValue: String?) {
        token = tokenValue
    }
}
