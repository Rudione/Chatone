package io.rudione.chatone.presentation.chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import io.rudione.chatone.presentation.components.crumbleBurst
import io.rudione.chatone.presentation.components.rememberCrumbleState
import kotlinx.coroutines.launch

internal class NicknameMentionFeedback(
    val decoration: Modifier,
    val mention: (String, Offset) -> Unit
)

@Composable
internal fun rememberNicknameMentionFeedback(onMention: (String) -> Unit): NicknameMentionFeedback {
    val crumble = rememberCrumbleState()
    val origin = remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val mentionLatest by rememberUpdatedState(onMention)
    val palette = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onSurfaceVariant)
    return remember(crumble, scope, haptics, palette) {
        NicknameMentionFeedback(
            decoration = Modifier.crumbleBurst(crumble, palette) { origin.value },
            mention = { name, position ->
                mentionLatest(name)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                if (!crumble.isCrumbling) {
                    origin.value = position
                    scope.launch {
                        try {
                            crumble.crumble()
                        } finally {
                            crumble.reset()
                        }
                    }
                }
            }
        )
    }
}

internal fun AnnotatedString.nicknameAt(
    layout: TextLayoutResult?,
    position: Offset,
    authorName: String
): String? {
    if (layout == null || isEmpty()) return null
    val nearest = layout.getOffsetForPosition(position).coerceAtMost(length - 1)
    val index = when {
        layout.getBoundingBox(nearest).contains(position) -> nearest
        nearest > 0 && layout.getBoundingBox(nearest - 1).contains(position) -> nearest - 1
        else -> return null
    }
    if (getStringAnnotations("username", index, index).isNotEmpty()) return authorName
    return getStringAnnotations("mention", index, index).firstOrNull()?.item
}

@Composable
internal fun Modifier.nicknameTouchGestures(
    text: AnnotatedString,
    layout: () -> TextLayoutResult?,
    authorName: String,
    onClick: (String) -> Unit,
    onMention: (String, Offset) -> Unit
): Modifier {
    val layoutLatest by rememberUpdatedState(layout)
    val clickLatest by rememberUpdatedState(onClick)
    val mentionLatest by rememberUpdatedState(onMention)
    return pointerInput(text, authorName) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (down.type == PointerType.Mouse || down.isConsumed) return@awaitEachGesture
            val nickname = text.nicknameAt(layoutLatest(), down.position, authorName) ?: return@awaitEachGesture
            down.consume()
            awaitPointerEvent(PointerEventPass.Final)
            val held = awaitLongPressOrCancellation(down.id)
            if (held != null) {
                held.consume()
                mentionLatest(nickname, held.position)
            } else {
                val up = currentEvent.changes.firstOrNull { it.id == down.id && it.changedToUp() }
                if (up != null) {
                    up.consume()
                    clickLatest(nickname)
                }
            }
        }
    }
}
