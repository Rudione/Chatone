package io.rudione.chatone.presentation.account

import io.rudione.chatone.domain.model.AccountProxyConfig


object ProxyValidator {
    private val IPV4 = Regex("^(\\d{1,3}\\.){3}\\d{1,3}$")
    private val HOSTNAME = Regex("^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?)*$")

    fun validate(config: AccountProxyConfig): Result {
        if (config.host.isBlank()) return Result.Failure("Host is empty")
        val host = config.host.trim()
        val isValid = when {
            IPV4.matches(host) -> host.split(".").all { it.toIntOrNull() in 0..255 }
            HOSTNAME.matches(host) -> true
            else -> false
        }
        if (!isValid) return Result.Failure("Invalid host: $host")
        if (config.port !in 1..65535) return Result.Failure("Port out of range: ${config.port}")
        if (config.username != null && config.username.contains(":")) {
            return Result.Failure("Username cannot contain ':'")
        }
        return Result.Valid
    }

    sealed class Result {
        object Valid : Result()
        data class Failure(val reason: String) : Result()
    }
}
