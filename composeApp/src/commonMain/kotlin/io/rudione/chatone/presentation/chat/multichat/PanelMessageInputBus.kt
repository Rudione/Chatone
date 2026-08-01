package io.rudione.chatone.presentation.chat.multichat

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PanelMessageInputBus {
    private val inputs = mutableMapOf<String, MutableStateFlow<String>>()

    fun inputFor(panelId: String): StateFlow<String> {
        return inputs.getOrPut(panelId) { MutableStateFlow("") }.asStateFlow()
    }

    fun setInput(panelId: String, text: String) {
        inputs.getOrPut(panelId) { MutableStateFlow("") }.value = text
    }

    fun clearInput(panelId: String) {
        inputs.remove(panelId)
    }

    fun clearAll() {
        inputs.clear()
    }
}
