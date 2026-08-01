package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Modifier.panelKeyboardShortcuts(panelManager: ChatPanelManager): Modifier {
    return this.onPreviewKeyEvent { ke: KeyEvent ->
        if (ke.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        val mod = ke.isCtrlPressed || ke.isMetaPressed
        if (!mod) return@onPreviewKeyEvent false
        when (ke.key) {
            Key.W -> {
                PanelKeyboardShortcuts.closeActive(panelManager)
            }
            Key.Tab -> {
                if (ke.isShiftPressed) PanelKeyboardShortcuts.cyclePrev(panelManager)
                else PanelKeyboardShortcuts.cycleNext(panelManager)
            }
            else -> false
        }
    }
}
