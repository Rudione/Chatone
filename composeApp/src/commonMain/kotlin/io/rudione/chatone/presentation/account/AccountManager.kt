package io.rudione.chatone.presentation.account

import com.russhwolf.settings.Settings
import io.rudione.chatone.domain.model.AccountProxyConfig
import io.rudione.chatone.domain.model.ProxyType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AccountManager(private val settings: Settings) {

    companion object {
        private const val KEY_ACTIVE_USER_ID = "account_active_user_id"
        private const val KEY_PROXY_PREFIX = "account_proxy_"
        private const val KEY_OVERRIDE_PREFIX = "account_settings_override_"
        private const val KEY_USE_OVERRIDE_PREFIX = "account_use_override_"
    }

    private val _activeAccountId = MutableStateFlow(settings.getStringOrNull(KEY_ACTIVE_USER_ID) ?: "")
    val activeAccountId: StateFlow<String> = _activeAccountId.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun setActiveAccount(userId: String) {
        settings.putString(KEY_ACTIVE_USER_ID, userId)
        _activeAccountId.value = userId
    }

    fun getProxy(userId: String): AccountProxyConfig? {
        if (userId.isBlank()) return null
        val raw = settings.getStringOrNull(KEY_PROXY_PREFIX + userId) ?: return null
        return runCatching {
            val obj = json.parseToJsonElement(raw).jsonObject
            AccountProxyConfig(
                type = ProxyType.valueOf(obj["type"]?.jsonPrimitive?.contentOrNull ?: "HTTP"),
                host = obj["host"]?.jsonPrimitive?.contentOrNull ?: "",
                port = obj["port"]?.jsonPrimitive?.int ?: 0,
                username = obj["username"]?.jsonPrimitive?.contentOrNull,
                password = obj["password"]?.jsonPrimitive?.contentOrNull,
                enabled = obj["enabled"]?.jsonPrimitive?.boolean ?: true
            )
        }.getOrNull()
    }

    fun saveProxy(userId: String, proxy: AccountProxyConfig?) {
        if (userId.isBlank()) return
        if (proxy == null) {
            settings.remove(KEY_PROXY_PREFIX + userId)
            return
        }
        val payload = buildString {
            append("{")
            append("\"type\":\"${proxy.type.name}\",")
            append("\"host\":\"${proxy.host.replace("\"", "\\\"")}\",")
            append("\"port\":${proxy.port},")
            append("\"username\":${proxy.username?.let { "\"${it.replace("\"", "\\\"")}\"" } ?: "null"},")
            append("\"password\":${proxy.password?.let { "\"${it.replace("\"", "\\\"")}\"" } ?: "null"},")
            append("\"enabled\":${proxy.enabled}")
            append("}")
        }
        settings.putString(KEY_PROXY_PREFIX + userId, payload)
    }

    fun isOverrideEnabled(userId: String): Boolean {
        if (userId.isBlank()) return false
        return settings.getBoolean(KEY_USE_OVERRIDE_PREFIX + userId, false)
    }

    fun setOverrideEnabled(userId: String, enabled: Boolean) {
        if (userId.isBlank()) return
        settings.putBoolean(KEY_USE_OVERRIDE_PREFIX + userId, enabled)
    }

    fun getSettingsOverrideJson(userId: String): String? {
        if (userId.isBlank()) return null
        return settings.getStringOrNull(KEY_OVERRIDE_PREFIX + userId)
    }

    fun saveSettingsOverrideJson(userId: String, json: String?) {
        if (userId.isBlank()) return
        if (json == null) settings.remove(KEY_OVERRIDE_PREFIX + userId)
        else settings.putString(KEY_OVERRIDE_PREFIX + userId, json)
    }

    fun deleteAllPerAccountData(userId: String) {
        if (userId.isBlank()) return
        settings.remove(KEY_PROXY_PREFIX + userId)
        settings.remove(KEY_OVERRIDE_PREFIX + userId)
        settings.remove(KEY_USE_OVERRIDE_PREFIX + userId)
    }
}
