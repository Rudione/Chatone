package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

@Composable
fun horizontalMouseWheelScrollModifier(scrollState: ScrollState): Modifier {
    val scope = rememberCoroutineScope()
    return Modifier.pointerInput(scrollState) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type == PointerEventType.Scroll) {
                    val totalScroll = event.changes.sumOf {
                        val delta = it.scrollDelta
                        (delta.x + delta.y).toDouble()
                    }.toFloat()
                    if (totalScroll != 0f && scrollState.maxValue > 0) {
                        scope.launch {
                            scrollState.scrollBy(totalScroll * 60f)
                        }
                        event.changes.forEach { it.consume() }
                    }
                }
            }
        }
    }
}
