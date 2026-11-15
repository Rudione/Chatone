package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.rudione.chatone.presentation.nativecontext.SuspendNativeMessageMenus

@Composable
actual fun UserProfileContainer(
    isPinned: Boolean,
    width: Dp,
    height: Dp,
    onResize: (Dp, Dp) -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    SuspendNativeMessageMenus()
    Popup(
        onDismissRequest = { if (!isPinned) onDismiss() },
        properties = PopupProperties(focusable = true, dismissOnClickOutside = !isPinned)
    ) {
        ProfileCardSurface(width = width, height = height, onResize = null) { content() }
    }
}
