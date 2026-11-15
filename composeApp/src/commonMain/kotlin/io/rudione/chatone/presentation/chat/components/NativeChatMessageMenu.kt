package io.rudione.chatone.presentation.chat.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.chat_copy_message
import chatone.composeapp.generated.resources.chat_reply_to
import chatone.composeapp.generated.resources.chat_translate
import chatone.composeapp.generated.resources.ui_pin
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.presentation.nativecontext.NativeMessageAction
import io.rudione.chatone.presentation.nativecontext.NativeMessageMenu
import io.rudione.chatone.presentation.nativecontext.NativeMessageMenus
import io.rudione.chatone.presentation.nativecontext.nativeMessageMenu
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun Modifier.nativeChatMessageMenu(
    message: DisplayMessage.PrivMsg,
    plainText: String,
    annotatedText: AnnotatedString,
    layout: () -> TextLayoutResult?,
    enabled: Boolean,
    isMod: Boolean,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onTranslate: () -> Unit,
    onPin: () -> Unit
): Modifier {
    if (!NativeMessageMenus.enabled || !enabled) return this
    val actions = buildList {
        add(NativeMessageAction(stringResource(Res.string.chat_reply_to), "arrowshape.turn.up.left", action = onReply))
        add(NativeMessageAction(stringResource(Res.string.chat_copy_message), "doc.on.doc", action = onCopy))
        add(NativeMessageAction(stringResource(Res.string.chat_translate), "character.bubble", action = onTranslate))
        if (isMod) add(NativeMessageAction(stringResource(Res.string.ui_pin), "pin", action = onPin))
    }
    return nativeMessageMenu(NativeMessageMenu(message.displayName, plainText, actions), true) { point ->
        layout()?.let { result ->
            val index = result.getOffsetForPosition(point)
            protectedTags.none { annotatedText.getStringAnnotations(it, index, index).isNotEmpty() }
        } == true
    }
}

private val protectedTags = listOf("username", "mention", "url", "androidx.compose.foundation.text.inlineContent")
