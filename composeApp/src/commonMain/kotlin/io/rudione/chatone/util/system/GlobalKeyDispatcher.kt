package io.rudione.chatone.util.system

import androidx.compose.ui.input.key.KeyEvent

object GlobalKeyDispatcher {
    private val handlers = mutableListOf<(KeyEvent) -> Boolean>()

    private var textInputDepth = 0

    val isTextInputActive: Boolean get() = textInputDepth > 0

    fun beginTextInput() {
        textInputDepth++
    }

    fun endTextInput() {
        if (textInputDepth > 0) textInputDepth--
    }

    fun register(handler: (KeyEvent) -> Boolean): () -> Unit {
        handlers.add(handler)
        return { handlers.remove(handler) }
    }

    fun dispatch(event: KeyEvent): Boolean {
        if (textInputDepth > 0) return false

        val snapshot = handlers.toList()
        for (i in snapshot.indices.reversed()) {
            if (snapshot[i](event)) return true
        }
        return false
    }
}
