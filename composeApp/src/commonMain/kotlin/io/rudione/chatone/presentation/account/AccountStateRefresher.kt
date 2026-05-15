package io.rudione.chatone.presentation.account

import io.rudione.chatone.data.repository.AuthRepository
import io.rudione.chatone.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class AccountStateRefresher(
    private val authRepository: AuthRepository,
    private val accountManager: AccountManager,
    private val scope: CoroutineScope
) {
    private val _validationStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val validationStatus: StateFlow<Map<String, Boolean>> = _validationStatus.asStateFlow()

    private var refreshJob: Job? = null

    fun startPeriodicRefresh(intervalMs: Long = 60 * 60_000L) {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            while (true) {
                refreshAll()
                delay(intervalMs)
            }
        }
    }

    fun stop() {
        refreshJob?.cancel()
        refreshJob = null
    }

    suspend fun refreshAll() {
        try {
            val accounts = authRepository.getAccounts().first()
            val results = mutableMapOf<String, Boolean>()
            accounts.forEach { acc ->
                val isValid = when (val r = authRepository.validateToken(acc)) {
                    is Result.Success -> r.data
                    is Result.Error -> false
                    Result.Loading -> true
                }
                results[acc.userId] = isValid
            }
            _validationStatus.value = results
        } catch (_: Exception) {

        }
    }
}
