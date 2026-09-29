package io.rudione.chatone.presentation.stream

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import io.rudione.chatone.domain.stream.StreamPlayerPreferences
import io.rudione.chatone.domain.stream.StreamVariant
import io.rudione.chatone.util.system.openInStreamlink

enum class StreamOpenQuality(val label: String, val playerPreference: String) {
    BEST("best", StreamVariant.SOURCE_GROUP),
    HD_60("720p60", "720p60"),
    SD("480p", "480p"),
    AUDIO_ONLY("audio_only", StreamPlayerPreferences.QUALITY_AUDIO_ONLY)
}

@Stable
class StreamLauncher internal constructor(private val host: StreamPlayerHost?) {

    val opensInApp: Boolean get() = host != null

    fun open(channelLogin: String, quality: StreamOpenQuality) {
        val player = host
        if (player != null) {
            player.onEvent(StreamPlayerEvent.Open(channelLogin, quality.playerPreference))
        } else {
            openInStreamlink(channelLogin, quality.label)
        }
    }
}

@Composable
fun rememberStreamLauncher(): StreamLauncher {
    val host = LocalStreamPlayerHost.current
    return remember(host) { StreamLauncher(host) }
}
