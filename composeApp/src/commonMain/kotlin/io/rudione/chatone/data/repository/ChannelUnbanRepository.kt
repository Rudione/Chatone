package io.rudione.chatone.data.repository

import io.ktor.client.plugins.ResponseException
import io.rudione.chatone.data.remote.HelixApiException
import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.domain.moderation.UnbanTarget
import io.rudione.chatone.domain.moderation.UnbannedUser
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.util.Result
import io.rudione.chatone.util.concurrent.IoDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlin.time.Clock

interface ChannelUnbanGateway {
    suspend fun unban(actor: ModerationActor, target: UnbanTarget): Result<UnbannedUser>
}

class ChannelUnbanRepository(
    private val helix: TwitchApiClient,
    private val auth: AuthRepository,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) : ChannelUnbanGateway {
    override suspend fun unban(actor: ModerationActor, target: UnbanTarget): Result<UnbannedUser> = withContext(IoDispatcher) {
        try {
            val account = auth.getAccountById(actor.moderatorId)
            var token = account?.accessToken?.takeIf(String::isNotBlank) ?: actor.accessToken
            var refreshed = false
            if (account != null && account.expiresAt <= now() + 30_000L && account.refreshToken.isNotBlank()) {
                val updated = refresh(account) ?: return@withContext Result.Error(IllegalStateException("Session expired"))
                token = updated.accessToken
                refreshed = true
            }
            val first = perform(actor.copy(accessToken = token), target)
            val failure = first.exceptionOrNull()
            val unauthorized = (failure as? HelixApiException)?.status == 401 ||
                (failure as? ResponseException)?.response?.status?.value == 401
            if (!unauthorized || refreshed || account == null || account.refreshToken.isBlank()) return@withContext first
            val updated = refresh(account) ?: return@withContext first
            perform(actor.copy(accessToken = updated.accessToken), target)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun refresh(account: TwitchAccount): TwitchAccount? =
        (auth.refreshAccount(account) as? TokenRefreshOutcome.Refreshed)?.account

    private suspend fun perform(actor: ModerationActor, target: UnbanTarget): Result<UnbannedUser> {
        val id = target.userId.takeIf { it.isNotBlank() && it.all(Char::isDigit) }
        val login = target.login.trim().removePrefix("@").lowercase()
        val user = if (id != null && login.isNotBlank()) {
            UnbannedUser(id, login)
        } else {
            if (id == null && login.isBlank()) return Result.Error(IllegalArgumentException("Unknown user"))
            when (val result = helix.getUsers(
                actor.accessToken,
                ids = id?.let(::listOf) ?: emptyList(),
                logins = if (id == null) listOf(login) else emptyList()
            )) {
                is Result.Success -> result.data.data.firstOrNull()?.let { UnbannedUser(it.id, it.login) }
                    ?: return Result.Error(IllegalArgumentException("User not found"))
                is Result.Error -> return result
                Result.Loading -> return Result.Error(IllegalStateException("User lookup incomplete"))
            }
        }
        return when (val result = helix.unbanUser(actor.accessToken, actor.channelId, actor.moderatorId, user.userId)) {
            is Result.Success -> Result.Success(user)
            is Result.Error -> result
            Result.Loading -> Result.Error(IllegalStateException("Unban incomplete"))
        }
    }
}
