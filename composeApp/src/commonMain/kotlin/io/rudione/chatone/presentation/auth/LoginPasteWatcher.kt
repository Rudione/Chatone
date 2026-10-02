package io.rudione.chatone.presentation.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import io.rudione.chatone.data.auth.LoginPayloadCodec
import io.rudione.chatone.data.repository.LoginFailure
import io.rudione.chatone.util.platform.DeviceFormFactor
import io.rudione.chatone.util.platform.currentFormFactor
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Stable
class LoginPasteWatcher internal constructor(
    private val clipboard: ClipboardManager,
    private val submit: (String, Boolean) -> Unit
) {
    var manualPayload by mutableStateOf("")
        private set

    var codeAccepted by mutableStateOf(false)
        internal set

    private var acceptedPayload = ""

    fun accept(raw: String): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed == acceptedPayload) return false
        if (!LoginPayloadCodec.canAutoSubmit(trimmed)) return false
        acceptedPayload = trimmed
        codeAccepted = true
        manualPayload = ""
        submit(trimmed, true)
        runCatching { clipboard.setText(AnnotatedString("")) }
        return true
    }

    fun pasteFromClipboard() {
        val text = readClipboard()
        if (!accept(text)) {
            submit(text, false)
            if (text.isNotBlank()) runCatching { clipboard.setText(AnnotatedString("")) }
        }
    }

    fun type(value: String) {
        if (!accept(value)) manualPayload = value
    }

    fun submitManual() {
        submit(manualPayload, false)
        manualPayload = ""
    }

    internal fun readClipboard(): String = runCatching { clipboard.getText()?.text }.getOrNull().orEmpty()
}

@Composable
fun rememberLoginPasteWatcher(
    awaitingPaste: Boolean,
    busy: Boolean,
    failure: LoginFailure?,
    onSubmitPayload: (String, Boolean) -> Unit
): LoginPasteWatcher {
    val clipboard = LocalClipboardManager.current
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    val pollsClipboard = remember { currentFormFactor() == DeviceFormFactor.DESKTOP }
    val submit by rememberUpdatedState(onSubmitPayload)
    val watcher = remember(clipboard) { LoginPasteWatcher(clipboard) { payload, auto -> submit(payload, auto) } }

    LaunchedEffect(watcher, awaitingPaste, windowFocused, busy) {
        if (!awaitingPaste || !windowFocused || busy) return@LaunchedEffect
        var reads = 0
        while (true) {
            if (watcher.accept(watcher.readClipboard())) break
            reads++
            val limit = if (pollsClipboard) DESKTOP_CLIPBOARD_READS else FOCUS_CLIPBOARD_READS
            if (reads >= limit) break
            delay((if (pollsClipboard) DESKTOP_POLL_MS else FOCUS_POLL_MS).milliseconds)
        }
    }

    LaunchedEffect(watcher, failure) {
        if (failure != null && failure != LoginFailure.Network) watcher.codeAccepted = false
    }

    return watcher
}

private const val DESKTOP_POLL_MS = 700L
private const val FOCUS_POLL_MS = 350L
private const val FOCUS_CLIPBOARD_READS = 4
private const val DESKTOP_CLIPBOARD_READS = 430
