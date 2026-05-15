package io.rudione.chatone.presentation.account

import io.github.aakira.napier.Napier
import io.rudione.chatone.data.remote.TwitchIrcClient
import io.rudione.chatone.data.remote.TwitchPubSubClient
import io.rudione.chatone.data.repository.EmoteRepository
import io.rudione.chatone.domain.model.TwitchAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class AccountSwitchCoordinator(
    private val accountManager: AccountManager,
    private val ircClient: TwitchIrcClient,
    private val pubSubClient: TwitchPubSubClient,
    private val emoteRepository: EmoteRepository,
    private val scope: CoroutineScope
) {
    companion object { private const val TAG = "AccountSwitch" }

    private val _isSwitching = MutableStateFlow(false)
    val isSwitching: StateFlow<Boolean> = _isSwitching.asStateFlow()

    private val _lastSwitchedAt = MutableStateFlow(0L)
    val lastSwitchedAt: StateFlow<Long> = _lastSwitchedAt.asStateFlow()

    fun switchTo(account: TwitchAccount, onComplete: (Boolean) -> Unit = {}) {
        if (_isSwitching.value) {
            Napier.w("Switch already in progress", tag = TAG)
            return
        }
        scope.launch {
            _isSwitching.value = true
            try {
                Napier.d("Switching to account ${account.login}", tag = TAG)


                emoteRepository.invalidatePersonalEmotes()


                try { pubSubClient.disconnect() } catch (e: Exception) {
                    Napier.w("PubSub disconnect failed: ${e.message}", tag = TAG)
                }
                try { ircClient.disconnect() } catch (e: Exception) {
                    Napier.w("IRC disconnect failed: ${e.message}", tag = TAG)
                }


                accountManager.setActiveAccount(account.userId)


                ircClient.connect(username = account.login, oauthToken = account.accessToken)

                _lastSwitchedAt.value = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                Napier.d("Switched to ${account.login}", tag = TAG)
                onComplete(true)
            } catch (e: Exception) {
                Napier.e("Switch failed: ${e.message}", e, tag = TAG)
                onComplete(false)
            } finally {
                _isSwitching.value = false
            }
        }
    }
}
