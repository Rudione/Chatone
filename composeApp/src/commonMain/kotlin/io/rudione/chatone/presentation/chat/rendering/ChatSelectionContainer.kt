package io.rudione.chatone.presentation.chat.rendering

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.rememberSelectionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.text.AnnotatedString
import io.rudione.chatone.util.platform.readText
import io.rudione.chatone.util.platform.setText

@Composable
internal fun ChatSelectionContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val state = rememberSelectionState()
    val clipboard = LocalClipboard.current
    val copying = remember(clipboard, state) { InlineCopyClipboard(clipboard) { state.selectedTexts } }
    CompositionLocalProvider(LocalClipboard provides copying) {
        SelectionContainer(state = state, modifier = modifier) {
            CompositionLocalProvider(LocalClipboard provides clipboard, content = content)
        }
    }
}

internal class InlineCopyClipboard(
    private val delegate: Clipboard,
    private val selectedTexts: () -> List<AnnotatedString>
) : Clipboard {

    override suspend fun getClipEntry(): ClipEntry? = delegate.getClipEntry()

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        val restored = clipEntry?.let { restoredSelection(it.readText(), selectedTexts()) }
        if (restored != null) delegate.setText(restored) else delegate.setClipEntry(clipEntry)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Use platform-specific extension to get platform reference")
    override val nativeClipboard: NativeClipboard
        get() = delegate.nativeClipboard
}

internal fun restoredSelection(copied: String?, selected: List<AnnotatedString>): String? {
    if (copied == null || selected.isEmpty()) return null
    if (selected.joinToString(SELECTION_SEPARATOR) { it.text } != copied) return null
    val restored = selected.joinToString(SELECTION_SEPARATOR) { it.copyableText() }
    return restored.takeIf { it != copied }
}

private const val SELECTION_SEPARATOR = "\n"
