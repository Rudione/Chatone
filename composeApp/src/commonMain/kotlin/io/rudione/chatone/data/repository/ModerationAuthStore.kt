package io.rudione.chatone.data.repository

import com.russhwolf.settings.Settings
import io.github.aakira.napier.Napier
import io.rudione.chatone.data.remote.GqlTokenIdentity
import io.rudione.chatone.data.remote.TwitchGqlClient
import io.rudione.chatone.util.security.getSecret
import io.rudione.chatone.util.security.putSecret
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ModerationAuthStore(
    private val settings: Settings,
    private val gqlClient: TwitchGqlClient,
    private val accountManager: AccountManager,
    scope: CoroutineScope
) {
    data class Identity(val userId: String, val login: String, val displayName: String)

    private val _identity = MutableStateFlow<Identity?>(null)
    val identity: StateFlow<Identity?> = _identity.asStateFlow()

    init {
        migrateLegacyGlobalToken()
        _identity.value = loadIdentity(activeAccountId())
        scope.launch {
            accountManager.activeAccountId.collect { userId ->
                _identity.value = loadIdentity(userId)
            }
        }
    }

    fun customToken(): String = tokenFor(activeAccountId())

    fun hasCustomToken(): Boolean = customToken().isNotEmpty()

    fun resolveToken(accountToken: String): String = customToken().ifEmpty { accountToken }

    fun tokenFor(userId: String): String {
        if (userId.isBlank()) return ""
        return settings.getSecret(KEY_TOKEN + userId).trim()
    }

    fun hasTokenFor(userId: String): Boolean = tokenFor(userId).isNotEmpty()

    suspend fun setAndValidate(token: String, userId: String = activeAccountId()): Identity? {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) {
            clear(userId)
            return null
        }
        val resolved: GqlTokenIdentity = gqlClient.validateCustomToken(trimmed) ?: return null
        val owner = userId.ifBlank { resolved.userId }
        if (userId.isNotBlank() && resolved.userId.isNotBlank() && resolved.userId != userId) {
            Napier.w(
                "First-party token belongs to ${resolved.login}, not to the account it was requested for",
                tag = TAG
            )
            return null
        }

        settings.putSecret(KEY_TOKEN + owner, trimmed)
        settings.putString(KEY_USER_ID + owner, resolved.userId)
        settings.putString(KEY_LOGIN + owner, resolved.login)
        settings.putString(KEY_DISPLAY + owner, resolved.displayName)

        val identity = Identity(resolved.userId, resolved.login, resolved.displayName)
        if (owner == activeAccountId()) _identity.value = identity
        return identity
    }

    fun clear(userId: String = activeAccountId()) {
        if (userId.isBlank()) return
        settings.remove(KEY_TOKEN + userId)
        settings.remove(KEY_USER_ID + userId)
        settings.remove(KEY_LOGIN + userId)
        settings.remove(KEY_DISPLAY + userId)
        if (userId == activeAccountId()) _identity.value = null
    }

    private fun activeAccountId(): String = accountManager.activeAccountId.value

    private fun loadIdentity(userId: String): Identity? {
        if (userId.isBlank()) return null
        val ownerId = settings.getStringOrNull(KEY_USER_ID + userId) ?: return null
        if (ownerId.isBlank()) return null
        return Identity(
            userId = ownerId,
            login = settings.getStringOrNull(KEY_LOGIN + userId).orEmpty(),
            displayName = settings.getStringOrNull(KEY_DISPLAY + userId).orEmpty()
        )
    }

    private fun migrateLegacyGlobalToken() {
        val legacyToken = settings.getSecret(LEGACY_KEY_TOKEN).trim()
        val legacyUserId = settings.getStringOrNull(LEGACY_KEY_USER_ID).orEmpty()
        if (legacyToken.isEmpty() || legacyUserId.isBlank()) {
            clearLegacyKeys()
            return
        }

        settings.putSecret(KEY_TOKEN + legacyUserId, legacyToken)
        settings.putString(KEY_USER_ID + legacyUserId, legacyUserId)
        settings.putString(KEY_LOGIN + legacyUserId, settings.getStringOrNull(LEGACY_KEY_LOGIN).orEmpty())
        settings.putString(KEY_DISPLAY + legacyUserId, settings.getStringOrNull(LEGACY_KEY_DISPLAY).orEmpty())
        clearLegacyKeys()
        Napier.d("First-party token migrated to per-account storage", tag = TAG)
    }

    private fun clearLegacyKeys() {
        settings.remove(LEGACY_KEY_TOKEN)
        settings.remove(LEGACY_KEY_USER_ID)
        settings.remove(LEGACY_KEY_LOGIN)
        settings.remove(LEGACY_KEY_DISPLAY)
    }

    companion object {
        private const val TAG = "ModerationAuthStore"

        private const val KEY_TOKEN = "moderation_fp_token_"
        private const val KEY_USER_ID = "moderation_fp_user_id_"
        private const val KEY_LOGIN = "moderation_fp_login_"
        private const val KEY_DISPLAY = "moderation_fp_display_"

        private const val LEGACY_KEY_TOKEN = "moderation_first_party_token"
        private const val LEGACY_KEY_USER_ID = "moderation_first_party_user_id"
        private const val LEGACY_KEY_LOGIN = "moderation_first_party_login"
        private const val LEGACY_KEY_DISPLAY = "moderation_first_party_display"
    }
}
