package io.rudione.chatone.data.repository

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock

class AccountTokenKeeper(
    private val authRepository: AuthRepository,
    private val scope: CoroutineScope,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                refreshDue()
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    suspend fun refreshDue() {
        authRepository.getAccounts().first()
            .filter { it.refreshToken.isNotBlank() && it.expiresAt - now() < REFRESH_MARGIN_MS }
            .forEach { account ->
                when (authRepository.refreshAccount(account)) {
                    is TokenRefreshOutcome.Refreshed -> Napier.d("Token refreshed for ${account.login}", tag = TAG)
                    TokenRefreshOutcome.Rejected -> Napier.w("Refresh token rejected for ${account.login}", tag = TAG)
                    TokenRefreshOutcome.Unreachable -> Unit
                }
            }
    }

    private companion object {
        const val TAG = "AccountTokenKeeper"
        const val CHECK_INTERVAL_MS = 5 * 60 * 1000L
        const val REFRESH_MARGIN_MS = 30 * 60 * 1000L
    }
}
