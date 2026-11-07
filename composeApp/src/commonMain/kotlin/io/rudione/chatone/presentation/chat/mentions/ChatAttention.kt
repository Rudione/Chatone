package io.rudione.chatone.presentation.chat.mentions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalWindowInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

const val CHAT_IDLE_AFTER_MS = 60_000L

@Stable
class ChatAttention internal constructor(
    private val clock: () -> Long,
    private val idleAfterMs: Long
) {
    private var lastActivityMs = clock()

    var isIdle by mutableStateOf(false)
        private set

    internal var isWindowFocused by mutableStateOf(true)
        private set

    val isAttentive: Boolean get() = isWindowFocused && !isIdle

    fun onActivity() {
        lastActivityMs = clock()
        if (isIdle) isIdle = false
    }

    internal fun onWindowFocusChanged(focused: Boolean) {
        if (focused && !isWindowFocused) onActivity()
        isWindowFocused = focused
    }

    internal suspend fun watchIdle() {
        while (true) {
            val left = lastActivityMs + idleAfterMs - clock()
            if (left > 0L) {
                delay(left)
                continue
            }
            isIdle = true
            snapshotFlow { isIdle }.first { idle -> !idle }
        }
    }
}

@Composable
fun rememberChatAttention(idleAfterMs: Long = CHAT_IDLE_AFTER_MS): ChatAttention {
    val attention = remember(idleAfterMs) {
        ChatAttention(clock = { Clock.System.now().toEpochMilliseconds() }, idleAfterMs = idleAfterMs)
    }
    val focused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(attention, focused) { attention.onWindowFocusChanged(focused) }
    LaunchedEffect(attention) { attention.watchIdle() }
    return attention
}

fun Modifier.reportsActivity(attention: ChatAttention): Modifier = pointerInput(attention) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial)
            attention.onActivity()
        }
    }
}
