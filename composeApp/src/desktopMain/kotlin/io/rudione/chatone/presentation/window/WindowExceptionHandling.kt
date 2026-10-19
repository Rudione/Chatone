package io.rudione.chatone.presentation.window

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.DefaultWindowExceptionHandlerFactory
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.WindowExceptionHandlerFactory
import io.github.aakira.napier.Napier
import java.awt.Window

@OptIn(ExperimentalComposeUiApi::class)
internal object ChatoneWindowExceptionHandlerFactory : WindowExceptionHandlerFactory {
    override fun exceptionHandler(window: Window): WindowExceptionHandler {
        val fallback = DefaultWindowExceptionHandlerFactory.exceptionHandler(window)
        return WindowExceptionHandler { throwable ->
            if (DisposedLayerRecovery.recover(throwable)) {
                Napier.w(
                    "Skipped a frame after a popup layer closed mid-frame",
                    throwable,
                    tag = TAG
                )
            } else {
                fallback.onException(throwable)
            }
        }
    }

    private const val TAG = "WindowException"
}

private object DisposedLayerRecovery {
    private var burstStartedAtMs = 0L
    private var burstCount = 0

    fun recover(throwable: Throwable): Boolean {
        if (throwable !is IllegalArgumentException || throwable.message != DISPOSED_OWNER_MESSAGE) return false
        val now = System.currentTimeMillis()
        if (now - burstStartedAtMs > BURST_WINDOW_MS) {
            burstStartedAtMs = now
            burstCount = 0
        }
        return ++burstCount <= MAX_RECOVERIES_PER_BURST
    }

    private const val DISPOSED_OWNER_MESSAGE = "RootNodeOwner is already disposed"
    private const val BURST_WINDOW_MS = 2_000L
    private const val MAX_RECOVERIES_PER_BURST = 20
}
