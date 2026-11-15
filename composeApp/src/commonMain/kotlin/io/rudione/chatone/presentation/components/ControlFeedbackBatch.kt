package io.rudione.chatone.presentation.components

import androidx.compose.ui.hapticfeedback.HapticFeedbackType

internal class ControlFeedbackBatch {
    private var pending: HapticFeedbackType? = null
    private var lastFeedbackAt = Double.NEGATIVE_INFINITY
    private var lastPriority = -1

    fun offer(type: HapticFeedbackType) {
        val previous = pending
        if (previous == null || priority(type) > priority(previous)) pending = type
    }

    fun take(nowSeconds: Double): HapticFeedbackType? {
        val type = pending ?: return null
        pending = null
        val importance = priority(type)
        if (nowSeconds - lastFeedbackAt < 0.07 && importance <= lastPriority) return null
        lastFeedbackAt = nowSeconds
        lastPriority = importance
        return type
    }

    fun clear() {
        pending = null
    }

    private fun priority(type: HapticFeedbackType): Int = when (type) {
        HapticFeedbackType.Reject -> 4
        HapticFeedbackType.Confirm -> 3
        HapticFeedbackType.LongPress, HapticFeedbackType.ContextClick, HapticFeedbackType.GestureThresholdActivate -> 2
        HapticFeedbackType.ToggleOn, HapticFeedbackType.ToggleOff, HapticFeedbackType.SegmentTick, HapticFeedbackType.SegmentFrequentTick -> 1
        else -> 0
    }
}
