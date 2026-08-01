package io.rudione.chatone.data.repository

import com.russhwolf.settings.Settings
import io.rudione.chatone.data.remote.GqlTokenIdentity
import io.rudione.chatone.data.remote.TwitchGqlClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ModerationAuthStore(
    private val settings: Settings,
    private val gqlClient: TwitchGqlClient
) {
    data class Identity(val userId: String, val login: String, val displayName: String)

    private val _identity = MutableStateFlow(loadIdentity())
    val identity: StateFlow<Identity?> = _identity

    fun customToken(): String = settings.getStringOrNull(KEY_TOKEN)?.trim().orEmpty()

    fun hasCustomToken(): Boolean = customToken().isNotEmpty()

    fun resolveToken(accountToken: String): String =
        customToken().ifEmpty { accountToken }

    suspend fun setAndValidate(token: String): Identity? {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) {
            clear()
            return null
        }
        val id: GqlTokenIdentity = gqlClient.validateCustomToken(trimmed) ?: return null
        settings.putString(KEY_TOKEN, trimmed)
        settings.putString(KEY_USER_ID, id.userId)
        settings.putString(KEY_LOGIN, id.login)
        settings.putString(KEY_DISPLAY, id.displayName)
        val identity = Identity(id.userId, id.login, id.displayName)
        _identity.value = identity
        return identity
    }

    fun clear() {
        settings.remove(KEY_TOKEN)
        settings.remove(KEY_USER_ID)
        settings.remove(KEY_LOGIN)
        settings.remove(KEY_DISPLAY)
        _identity.value = null
    }

    private fun loadIdentity(): Identity? {
        val userId = settings.getStringOrNull(KEY_USER_ID) ?: return null
        if (userId.isBlank()) return null
        return Identity(
            userId = userId,
            login = settings.getStringOrNull(KEY_LOGIN).orEmpty(),
            displayName = settings.getStringOrNull(KEY_DISPLAY).orEmpty()
        )
    }

    companion object {
        private const val KEY_TOKEN = "moderation_first_party_token"
        private const val KEY_USER_ID = "moderation_first_party_user_id"
        private const val KEY_LOGIN = "moderation_first_party_login"
        private const val KEY_DISPLAY = "moderation_first_party_display"
    }
}
