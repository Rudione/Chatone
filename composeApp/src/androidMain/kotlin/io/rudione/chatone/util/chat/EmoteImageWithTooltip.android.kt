package io.rudione.chatone.util.chat

import io.rudione.chatone.presentation.chat.emote.EmoteInfoPopup
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import io.rudione.chatone.domain.model.EmoteProvider
import io.rudione.chatone.domain.model.GenericEmote
import io.rudione.chatone.presentation.chat.emote.SharedEmoteImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun EmoteImageWithTooltip(
    emote: GenericEmote,
    modifier: Modifier,
    onShowContextMenu: (() -> Unit)?
) {
    val uriHandler = LocalUriHandler.current
    val interactionSource = remember { MutableInteractionSource() }
    var infoShown by remember(emote.id) { mutableStateOf(false) }
    val emoteImage: @Composable (Modifier) -> Unit = { imageModifier ->
        SharedEmoteImage(
            url = emote.url2x.ifEmpty { emote.url1x },
            contentDescription = emote.code,
            modifier = imageModifier
        )
    }
    Box {
        emoteImage(
            modifier.combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { if (emote.provider != EmoteProvider.TWITCH) infoShown = true },
                onLongClick = {
                    infoShown = false
                    if (emote.provider == EmoteProvider.SEVEN_TV && emote.id.isNotEmpty()) {
                        try {
                            uriHandler.openUri("https://7tv.app/emotes/${emote.id}")
                        } catch (_: Exception) {}
                    }
                    onShowContextMenu?.invoke()
                }
            )
        )
        if (infoShown) {
            EmoteInfoPopup(emote = emote, onDismiss = { infoShown = false }, image = emoteImage)
        }
    }
}
