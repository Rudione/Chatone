package io.rudione.chatone.presentation.chat.multichat

import io.rudione.chatone.domain.model.ChatPanel


data class PanelStateSnapshot(
    val panels: List<ChatPanel>,
    val activePanelId: String?,
    val timestamp: Long
)


class PanelStateHistory(private val capacity: Int = 20) {
    private val undoStack = ArrayDeque<PanelStateSnapshot>()
    private val redoStack = ArrayDeque<PanelStateSnapshot>()

    fun push(snapshot: PanelStateSnapshot) {
        undoStack.addLast(snapshot)
        if (undoStack.size > capacity) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(): PanelStateSnapshot? {
        if (undoStack.isEmpty()) return null
        val top = undoStack.removeLast()
        redoStack.addLast(top)
        return undoStack.lastOrNull()
    }

    fun redo(): PanelStateSnapshot? {
        if (redoStack.isEmpty()) return null
        val top = redoStack.removeLast()
        undoStack.addLast(top)
        return top
    }

    fun canUndo(): Boolean = undoStack.size > 1
    fun canRedo(): Boolean = redoStack.isNotEmpty()
    fun clear() { undoStack.clear(); redoStack.clear() }
}
