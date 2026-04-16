package io.rudione.chatone.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

expect class PlatformAuthHandler() {
    fun getClientId(): String
    fun getRedirectUri(): String
    fun startAuth(authUrl: String)
    suspend fun awaitToken(): String?
    fun cleanup()
}

object AuthTokenBridge {
    private val _tokenFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val tokenFlow: Flow<String> = _tokenFlow

    fun emitToken(token: String) {
        _tokenFlow.tryEmit(token)
    }
}
