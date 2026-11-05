package io.rudione.chatone.presentation.stream.ambient

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import io.rudione.chatone.presentation.stream.StreamPlayerHost
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun rememberStreamAmbience(host: StreamPlayerHost): StreamAmbience {
    val ambience = remember(host) { StreamAmbience() }
    LaunchedEffect(ambience) { ambience.animate() }
    LaunchedEffect(host, ambience) {
        host.state
            .map { it.isOpen && !it.needsAttention && it.preferences.ambientLight }
            .distinctUntilChanged()
            .collect { showing -> if (!showing) ambience.fadeOut() }
    }
    return ambience
}
