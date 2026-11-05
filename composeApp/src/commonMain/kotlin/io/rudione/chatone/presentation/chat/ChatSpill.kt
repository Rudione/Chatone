package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.staticCompositionLocalOf

@Stable
class ChatSpill {
    private val requests = mutableStateSetOf<String>()

    val active: Boolean get() = requests.isNotEmpty()

    fun request(key: String) {
        requests.add(key)
    }

    fun release(key: String) {
        requests.remove(key)
    }
}

val LocalChatSpill = staticCompositionLocalOf<ChatSpill?> { null }

@Composable
fun ChatSpillRequest(key: String, active: Boolean = true) {
    val spill = LocalChatSpill.current ?: return
    DisposableEffect(spill, key, active) {
        if (active) spill.request(key)
        onDispose { spill.release(key) }
    }
}
