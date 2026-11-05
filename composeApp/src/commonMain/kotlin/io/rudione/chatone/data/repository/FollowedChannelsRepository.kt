package io.rudione.chatone.data.repository

import io.rudione.chatone.data.remote.FollowedChannelsPage
import io.rudione.chatone.data.remote.TwitchFollowsClient
import io.rudione.chatone.domain.follow.FollowedChannel
import io.rudione.chatone.domain.follow.FollowedChannelOrder
import io.rudione.chatone.domain.follow.FollowedChannelsResult
import io.rudione.chatone.domain.follow.FollowedChannelsSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

class FollowedChannelsRepository(
    private val client: TwitchFollowsClient,
    private val sessions: ViewerSessionProvider,
    private val clock: Clock = Clock.System
) : FollowedChannelsSource {

    private val mutex = Mutex()
    private var cached: CachedFollows? = null

    override suspend fun followedChannels(forceRefresh: Boolean): FollowedChannelsResult =
        mutex.withLock {
            val session = sessions.current() ?: return@withLock FollowedChannelsResult.SignedOut
            val now = clock.now().toEpochMilliseconds()
            cached?.takeIf { !forceRefresh && it.userId == session.userId && now - it.loadedAtMs < CACHE_TTL_MS }
                ?.let { return@withLock FollowedChannelsResult.Loaded(it.channels) }

            val loaded = collect { cursor -> client.viewerFollows(session.gqlToken, cursor) }
                ?: collect { cursor ->
                    client.helixFollows(
                        session.userId,
                        session.helixToken,
                        cursor
                    )
                }
                ?: return@withLock FollowedChannelsResult.Failed

            val sorted = FollowedChannelOrder.sort(loaded.distinctBy { it.login })
            cached = CachedFollows(session.userId, now, sorted)
            FollowedChannelsResult.Loaded(sorted)
        }

    private suspend fun collect(page: suspend (String?) -> FollowedChannelsPage?): List<FollowedChannel>? {
        val first = page(null) ?: return null
        val channels = first.channels.toMutableList()
        var cursor = first.nextCursor
        var pages = 1
        while (cursor != null && pages < MAX_PAGES) {
            val next = page(cursor) ?: break
            channels += next.channels
            cursor = next.nextCursor
            pages++
        }
        return channels
    }

    private class CachedFollows(
        val userId: String,
        val loadedAtMs: Long,
        val channels: List<FollowedChannel>
    )

    private companion object {
        const val CACHE_TTL_MS = 120_000L
        const val MAX_PAGES = 8
    }
}
