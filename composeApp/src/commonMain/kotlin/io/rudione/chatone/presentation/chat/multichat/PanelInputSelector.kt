package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue


@Composable
fun rememberSharedInputState(): SharedInputState = remember { SharedInputState() }


class SharedInputState {
    private val perPanel = mutableMapOf<String, String>()
    var unifiedMode by mutableStateOf(false)
    var unifiedText by mutableStateOf("")

    fun get(panelId: String): String =
        if (unifiedMode) unifiedText else perPanel[panelId] ?: ""

    fun set(panelId: String, text: String) {
        if (unifiedMode) unifiedText = text
        else perPanel[panelId] = text
    }

    fun clear(panelId: String) {
        if (unifiedMode) unifiedText = ""
        else perPanel.remove(panelId)
    }

    fun clearAll() {
        perPanel.clear()
        unifiedText = ""
    }
}
