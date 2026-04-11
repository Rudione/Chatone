package io.rudione.chatone.util

import androidx.compose.ui.input.key.KeyEvent

/**
 * Global key-event bus that lets a composable subscribe to Window-level
 * key events regardless of focus. The desktop entry point installs
 * [onPreviewKeyEvent] as its Window handler, which forwards every key
 * press / release through [dispatch]. Handlers are tried in reverse
 * registration order (LIFO) — the last-registered screen wins.
 *
 * Used by ChatScreen for the pause hotkey so the user can stop/resume
 * chat scroll even when focus has moved away from the chat box (e.g.
 * to a settings dialog, the channel sidebar, or nothing at all).
 */
object GlobalKeyDispatcher {
    private val handlers = mutableListOf<(KeyEvent) -> Boolean>()

    fun register(handler: (KeyEvent) -> Boolean): () -> Unit {
        handlers.add(handler)
        return { handlers.remove(handler) }
    }

    fun dispatch(event: KeyEvent): Boolean {
        // Iterate over a snapshot to tolerate concurrent unregisters.
        val snapshot = handlers.toList()
        for (i in snapshot.indices.reversed()) {
            if (snapshot[i](event)) return true
        }
        return false
    }
}
