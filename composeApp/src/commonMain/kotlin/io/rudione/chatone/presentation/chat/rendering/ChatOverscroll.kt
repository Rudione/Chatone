package io.rudione.chatone.presentation.chat.rendering

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import io.rudione.chatone.util.system.isDesktopPlatform

@Composable
internal fun rememberChatOverscrollEffect(desktop: Boolean = isDesktopPlatform): OverscrollEffect? =
    if (desktop) rememberOverscrollEffect() else null
