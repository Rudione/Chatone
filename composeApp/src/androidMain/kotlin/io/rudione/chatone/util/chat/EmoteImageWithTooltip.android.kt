package io.rudione.chatone.util.chat

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

    SharedEmoteImage(
        url = emote.url2x.ifEmpty { emote.url1x },
        contentDescription = emote.code,
        modifier = modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {},
            onLongClick = {
                if (emote.provider == EmoteProvider.SEVEN_TV && emote.id.isNotEmpty()) {
                    try {
                        uriHandler.openUri("https://7tv.app/emotes/${emote.id}")
                    } catch (_: Exception) {}
                }
                onShowContextMenu?.invoke()
            }
        )
    )
}
