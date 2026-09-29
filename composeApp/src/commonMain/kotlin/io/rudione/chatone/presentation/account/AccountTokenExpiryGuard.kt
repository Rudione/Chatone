package io.rudione.chatone.presentation.account

import io.rudione.chatone.domain.model.TwitchAccount
import kotlin.time.Clock

object AccountTokenExpiryGuard {

    private const val WARNING_BUFFER_MS = 7 * 24 * 60 * 60 * 1000L
    private const val CRITICAL_BUFFER_MS = 24 * 60 * 60 * 1000L

    enum class Status { Valid, Warning, Critical, Expired }

    fun status(account: TwitchAccount): Status {
        val now = Clock.System.now().toEpochMilliseconds()
        val timeLeft = account.expiresAt - now
        return when {
            timeLeft <= 0 -> Status.Expired
            timeLeft <= CRITICAL_BUFFER_MS -> Status.Critical
            timeLeft <= WARNING_BUFFER_MS -> Status.Warning
            else -> Status.Valid
        }
    }

    fun isExpired(account: TwitchAccount): Boolean = status(account) == Status.Expired
    fun isExpiringSoon(account: TwitchAccount): Boolean = status(account) != Status.Valid
}
