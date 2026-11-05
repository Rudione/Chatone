package io.rudione.chatone.data.repository

import io.rudione.chatone.data.remote.MinuteWatched
import io.rudione.chatone.data.remote.TwitchWatchClient
import io.rudione.chatone.data.remote.WatchBroadcast
import io.rudione.chatone.domain.points.WatchActivitySink
import io.rudione.chatone.domain.points.WatchMinutePlan
import io.rudione.chatone.domain.points.WatchStreakSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MinuteWatchedReporter(
    private val client: TwitchWatchClient,
    private val sessions: ViewerSessionProvider,
    private val streaks: WatchStreakSource,
    private val scope: CoroutineScope
) : WatchActivitySink {

    private var watching: String? = null
    private var job: Job? = null

    override fun onPlayback(channelLogin: String?, playing: Boolean) {
        val target = channelLogin?.trim()?.removePrefix("#")?.lowercase()
            ?.takeIf { playing && it.isNotEmpty() }
        if (target == watching) return
        job?.cancel()
        watching = target
        job = target?.let { login -> scope.launch { watch(login) } }
    }

    private suspend fun CoroutineScope.watch(login: String) {
        streaks.refresh(login)
        var broadcast: WatchBroadcast? = null
        var minutes = 0
        while (isActive) {
            delay(WatchMinutePlan.MINUTE_MS)
            val session = sessions.current() ?: continue
            val live = broadcast ?: client.broadcast(login)?.also { broadcast = it } ?: continue
            if (client.reportMinute(MinuteWatched(session.userId, login, live))) {
                minutes++
                if (WatchMinutePlan.shouldRefreshStreak(minutes)) streaks.refresh(login)
            } else {
                broadcast = null
            }
        }
    }
}
