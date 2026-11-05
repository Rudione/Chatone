package io.rudione.chatone.data.repository

import io.rudione.chatone.data.remote.TwitchWatchClient
import io.rudione.chatone.domain.points.WatchStreak
import io.rudione.chatone.domain.points.WatchStreakSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WatchStreakRepository(
    private val client: TwitchWatchClient,
    private val sessions: ViewerSessionProvider
) : WatchStreakSource {

    private val _streaks = MutableStateFlow<Map<String, WatchStreak>>(emptyMap())
    override val streaks: StateFlow<Map<String, WatchStreak>> = _streaks.asStateFlow()

    override suspend fun refresh(channelLogin: String) {
        val login =
            channelLogin.trim().removePrefix("#").lowercase().takeIf { it.isNotEmpty() } ?: return
        val session = sessions.current() ?: return
        val streak = client.streak(login, session.gqlToken) ?: return
        _streaks.update { it + (login to streak) }
    }
}
