package io.rudione.chatone.data.repository

import io.github.aakira.napier.Napier
import io.rudione.chatone.data.remote.TwitchIrcClient
import io.rudione.chatone.data.remote.proxy.IrcConnectionFactory
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.presentation.account.AccountManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class MultiAccountConnectionRegistry(
    private val ircFactory: IrcConnectionFactory,
    private val accountManager: AccountManager,
    private val scope: CoroutineScope
) {
    companion object { private const val TAG = "MultiAccConn" }

    private val _connectedAccountIds = MutableStateFlow<Set<String>>(emptySet())
    val connectedAccountIds: StateFlow<Set<String>> = _connectedAccountIds.asStateFlow()

    fun connect(account: TwitchAccount) {
        scope.launch {
            try {
                val irc = ircFactory.forAccount(account.userId)
                irc.connect(username = account.login, oauthToken = account.accessToken)
                _connectedAccountIds.value = _connectedAccountIds.value + account.userId
                Napier.d("Connected IRC for ${account.login}", tag = TAG)
            } catch (e: Exception) {
                Napier.e("IRC connect failed for ${account.login}: ${e.message}", e, tag = TAG)
            }
        }
    }

    fun disconnect(userId: String) {
        scope.launch {
            try {
                ircFactory.disconnect(userId)
                _connectedAccountIds.value = _connectedAccountIds.value - userId
            } catch (e: Exception) {
                Napier.w("IRC disconnect failed: ${e.message}", tag = TAG)
            }
        }
    }

    fun isConnected(userId: String): Boolean = userId in _connectedAccountIds.value

    fun ircForAccount(userId: String): TwitchIrcClient? = ircFactory.forAccount(userId)

    fun ircForActive(): TwitchIrcClient? = ircFactory.activeAccountIrc()
}
