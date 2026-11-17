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

internal fun ChatState.liveEventId(key: String): String? = when (key) {
    "poll" -> livePoll?.id
    "prediction" -> livePrediction?.id
    else -> null
}

internal fun ChatState.withEventCollapseToggled(key: String, memory: Int): ChatState {
    val id = liveEventId(key) ?: return this
    val next = if (id in collapsedEventIds) collapsedEventIds - id
    else (collapsedEventIds + id).toList().takeLast(memory).toSet()
    return copy(collapsedEventIds = next)
}

internal fun ChatState.withEventShown(key: String): ChatState {
    val id = liveEventId(key) ?: return this
    return copy(
        hiddenEventIds = hiddenEventIds - id,
        collapsedEventIds = collapsedEventIds - id,
        showPointsBitsPanel = false
    )
}
