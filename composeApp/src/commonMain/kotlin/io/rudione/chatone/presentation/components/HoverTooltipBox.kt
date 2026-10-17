package io.rudione.chatone.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SHOW_DELAY_MS = 350L
private val TooltipProperties = PopupProperties(focusable = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoverTooltipBox(
    tooltip: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider()
    if (LocalPopupPortal.current == null) {
        TooltipBox(positionProvider = positionProvider, tooltip = { tooltip() }, state = rememberTooltipState()) {
            content()
        }
        return
    }
    var visible by remember { mutableStateOf(false) }
    Box(Modifier.hoverReveal { visible = it }) {
        content()
        if (visible) PortalPopup(positionProvider, properties = TooltipProperties, content = tooltip)
    }
}

private fun Modifier.hoverReveal(onVisibleChange: (Boolean) -> Unit): Modifier = pointerInput(Unit) {
    coroutineScope {
        var pending: Job? = null
        awaitPointerEventScope {
            while (true) {
                when (awaitPointerEvent().type) {
                    PointerEventType.Enter -> {
                        pending?.cancel()
                        pending = launch {
                            delay(SHOW_DELAY_MS)
                            onVisibleChange(true)
                        }
                    }

                    PointerEventType.Exit, PointerEventType.Press, PointerEventType.Scroll -> {
                        pending?.cancel()
                        onVisibleChange(false)
                    }

                    else -> Unit
                }
            }
        }
    }
}
