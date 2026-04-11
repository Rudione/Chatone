package io.rudione.chatone.util

import androidx.compose.ui.input.key.KeyEvent


object GlobalKeyDispatcher {
    private val handlers = mutableListOf<(KeyEvent) -> Boolean>()

    fun register(handler: (KeyEvent) -> Boolean): () -> Unit {
        handlers.add(handler)
        return { handlers.remove(handler) }
    }

    fun dispatch(event: KeyEvent): Boolean {
       
        val snapshot = handlers.toList()
        for (i in snapshot.indices.reversed()) {
            if (snapshot[i](event)) return true
        }
        return false
    }
}
