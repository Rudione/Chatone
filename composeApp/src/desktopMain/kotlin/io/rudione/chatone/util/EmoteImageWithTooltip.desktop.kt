package io.rudione.chatone.util

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalUriHandler
import io.rudione.chatone.domain.model.EmoteProvider
import io.rudione.chatone.domain.model.GenericEmote
import io.rudione.chatone.presentation.chat.AnimatedEmoteImageCore
import io.rudione.chatone.presentation.chat.EmoteTooltip

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
actual fun EmoteImageWithTooltip(
    emote: GenericEmote,
    modifier: Modifier,
    onShowContextMenu: (() -> Unit)?
) {
    val uriHandler = LocalUriHandler.current

    TooltipArea(
        tooltip = { EmoteTooltip(emote = emote) },
        delayMillis = 500
    ) {
        AnimatedEmoteImageCore(
            url = emote.url2x.ifEmpty { emote.url1x },
            contentDescription = emote.code,
            modifier = modifier
                .onPointerEvent(PointerEventType.Press) { event ->
                    if (event.buttons.isSecondaryPressed) {
                        if (emote.provider == EmoteProvider.SEVEN_TV && emote.id.isNotEmpty()) {
                            try {
                                uriHandler.openUri("https://7tv.app/emotes/${emote.id}")
                            } catch (_: Exception) {}
                        }
                        onShowContextMenu?.invoke()
                    }
                }
        )
    }
}