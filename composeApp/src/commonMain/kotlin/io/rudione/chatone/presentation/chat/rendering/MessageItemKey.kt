package io.rudione.chatone.presentation.chat.rendering

import io.rudione.chatone.domain.model.DisplayMessage

fun DisplayMessage.lazyKey(): String = id

fun DisplayMessage.contentType(): String = when (this) {
    is DisplayMessage.PrivMsg -> if (isHighlighted) "priv-highlight" else "priv"
    is DisplayMessage.SystemMsg -> "system"
    is DisplayMessage.ModerationMsg -> "mod"
    is DisplayMessage.AutoModMsg -> "automod"
    else -> "other"
}
