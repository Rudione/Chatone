package io.rudione.chatone.util.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import io.rudione.chatone.util.EmoteAnimationCache
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import io.rudione.chatone.domain.model.GenericEmote
import io.rudione.chatone.presentation.chat.AnimatedEmoteImageCore
import io.rudione.chatone.presentation.chat.EmoteTooltip
import kotlin.time.Clock

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
actual fun EmoteImageWithTooltip(
    emote: GenericEmote,
    modifier: Modifier,
    onShowContextMenu: (() -> Unit)?
) {
    LaunchedEffect(emote.id) {
        EmoteAnimationCache.recordTooltipTiming(emote.id)
    }

    val syncedDelay = remember(emote.id) {
        val firstTiming = EmoteAnimationCache.getTooltipTiming(emote.id)
        val now = Clock.System.now().toEpochMilliseconds()
        val elapsed = now - firstTiming
        (400 - elapsed).coerceAtLeast(0).toInt()
    }

    TooltipArea(
        tooltip = { EmoteTooltip(emote = emote) },
        delayMillis = syncedDelay
    ) {
        AnimatedEmoteImageCore(
            url = emote.url2x.ifEmpty { emote.url1x },
            contentDescription = emote.code,
            emoteId = emote.id,
            modifier = modifier
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type == PointerEventType.Press ||
                                event.type == PointerEventType.Move ||
                                event.type == PointerEventType.Enter) {
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
        )
    }
}
