package io.rudione.chatone.presentation.chat

internal fun ChatState.hasHiddenEvents(): Boolean =
    (pinnedMessage != null && pinLocallyHidden) ||
        livePoll?.id in hiddenEventIds ||
        livePrediction?.id in hiddenEventIds

internal fun ChatState.withHiddenEventsToggled(memory: Int): ChatState {
    val liveIds = setOfNotNull(livePoll?.id, livePrediction?.id)
    return if (hasHiddenEvents()) {
        copy(pinLocallyHidden = false, hiddenEventIds = hiddenEventIds - liveIds)
    } else {
        copy(
            pinLocallyHidden = true,
            hiddenEventIds = (hiddenEventIds + liveIds).toList().takeLast(memory).toSet()
        )
    }
}
