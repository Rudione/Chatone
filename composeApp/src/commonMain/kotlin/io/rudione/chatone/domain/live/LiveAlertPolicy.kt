package io.rudione.chatone.domain.live

import io.rudione.chatone.domain.model.LiveStreamSnapshot
import kotlinx.serialization.Serializable

@Serializable
data class LiveAlertEntry(val lastStreamId: String? = null)

object LiveAlertPolicy {

    data class Outcome(
        val entries: Map<String, LiveAlertEntry>,
        val alerts: List<LiveStreamSnapshot>
    )

    fun evaluate(
        enabledChannels: Set<String>,
        entries: Map<String, LiveAlertEntry>,
        live: Map<String, LiveStreamSnapshot>
    ): Outcome {
        val next = HashMap<String, LiveAlertEntry>(enabledChannels.size)
        val alerts = ArrayList<LiveStreamSnapshot>()
        for (login in enabledChannels) {
            val stream = live[login]
            val known = entries[login]
            when {
                known == null -> next[login] = LiveAlertEntry(lastStreamId = stream?.streamId)
                stream != null && stream.streamId != known.lastStreamId -> {
                    alerts += stream
                    next[login] = LiveAlertEntry(lastStreamId = stream.streamId)
                }
                else -> next[login] = known
            }
        }
        return Outcome(next, alerts)
    }
}
