package io.rudione.chatone.domain.model

enum class ProxyType { HTTP, SOCKS5 }

data class AccountProxyConfig(
    val type: ProxyType = ProxyType.HTTP,
    val host: String = "",
    val port: Int = 0,
    val username: String? = null,
    val password: String? = null,
    val enabled: Boolean = true
) {
    val isValid: Boolean
        get() = host.isNotBlank() && port in 1..65535

    val requiresAuth: Boolean
        get() = !username.isNullOrBlank()
}

data class AccountSettingsOverride(
    val userId: String,
    val enabled: Boolean,
    val settingsJson: String?
)
