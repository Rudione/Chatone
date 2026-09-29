package io.rudione.chatone.presentation.stream

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import io.rudione.chatone.domain.stream.StreamPlaybackEngine
import io.rudione.chatone.domain.stream.StreamPlaybackEngineFactory
import io.rudione.chatone.domain.stream.StreamStats
import io.rudione.chatone.util.platform.currentFormFactor
import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Stable
class StreamPlayerHost internal constructor(private val viewModel: StreamPlayerViewModel) {
    val state: StateFlow<StreamPlayerUiState> get() = viewModel.state
    val stats: StateFlow<StreamStats?> get() = viewModel.stats
    val latencySeconds: StateFlow<Float?> get() = viewModel.latencySeconds
    val engine: StateFlow<StreamPlaybackEngine?> get() = viewModel.playbackEngine

    fun onEvent(event: StreamPlayerEvent) = viewModel.onEvent(event)

    fun reloadIfWatching(channelLogin: String) {
        val login = channelLogin.trim().lowercase()
        if (login.isNotEmpty() && viewModel.state.value.channelLogin == login) {
            viewModel.onEvent(StreamPlayerEvent.Reload)
        }
    }
}

val LocalStreamPlayerHost = staticCompositionLocalOf<StreamPlayerHost?> { null }

@Composable
fun rememberStreamPlayerHost(): StreamPlayerHost? {
    val isHandheld = remember { currentFormFactor().isHandheld }
    if (!isHandheld) return null
    val factory = koinInject<StreamPlaybackEngineFactory>()
    if (!factory.isSupported) return null
    val viewModel: StreamPlayerViewModel = koinViewModel()
    return remember(viewModel) { StreamPlayerHost(viewModel) }
}
