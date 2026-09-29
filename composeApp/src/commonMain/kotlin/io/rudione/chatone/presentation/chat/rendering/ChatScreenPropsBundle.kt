package io.rudione.chatone.presentation.chat.rendering

import androidx.compose.runtime.Stable
import io.rudione.chatone.presentation.settings.SettingsState

@Stable
data class ChatScreenPropsBundle(
    val channelLogin: String,
    val accessToken: String,
    val currentUserId: String,
    val currentUserLogin: String,
    val currentDisplayName: String,
    val isCompact: Boolean,
    val renderParams: MessageRenderParams,
    val style: StableChatStyle
)

fun buildChatScreenProps(
    channelLogin: String,
    accessToken: String,
    currentUserId: String,
    currentUserLogin: String,
    currentDisplayName: String,
    isCompact: Boolean,
    settings: SettingsState,
    renderParams: MessageRenderParams,
    style: StableChatStyle
): ChatScreenPropsBundle = ChatScreenPropsBundle(
    channelLogin = channelLogin,
    accessToken = accessToken,
    currentUserId = currentUserId,
    currentUserLogin = currentUserLogin,
    currentDisplayName = currentDisplayName,
    isCompact = isCompact,
    renderParams = renderParams,
    style = style
)
