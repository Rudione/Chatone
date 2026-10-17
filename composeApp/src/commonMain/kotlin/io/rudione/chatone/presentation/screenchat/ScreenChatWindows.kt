package io.rudione.chatone.presentation.screenchat

import androidx.compose.runtime.Composable
import io.rudione.chatone.domain.model.ScreenChatSettings
import io.rudione.chatone.presentation.chat.ChatSessionIdentity
import io.rudione.chatone.presentation.settings.SettingsState

@Composable
expect fun ScreenChatWindows(
    identity: ChatSessionIdentity,
    chat: SettingsState,
    onSettingsChange: (ScreenChatSettings) -> Unit
)

expect val screenChatFloatsOverFullScreen: Boolean
