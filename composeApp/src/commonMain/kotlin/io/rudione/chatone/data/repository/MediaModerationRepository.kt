package io.rudione.chatone.data.repository

import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.domain.media.MediaModeration
import io.rudione.chatone.util.Result
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MediaModerationRepository(
    private val helix: TwitchApiClient,
    private val sessions: ViewerSessionProvider
) : MediaModeration {

    private val mutex = Mutex()
    private var cached: Pair<String, Set<String>>? = null

    override suspend fun canModerate(broadcasterId: String): Boolean {
        val session = sessions.current() ?: return false
        if (session.userId == broadcasterId) return true
        return broadcasterId in moderated(session)
    }

    override suspend fun ban(broadcasterId: String, userId: String, reason: String): Boolean {
        val session = sessions.current() ?: return false
        return helix.banUser(session.helixToken, broadcasterId, session.userId, userId, reason = reason) is Result.Success
    }

    private suspend fun moderated(session: ViewerSession): Set<String> = mutex.withLock {
        cached?.takeIf { it.first == session.userId }?.second ?: run {
            val result = helix.getModeratedChannels(session.helixToken, session.userId)
            val ids = (result as? Result.Success)?.data.orEmpty()
            if (result is Result.Success) cached = session.userId to ids
            ids
        }
    }
}
