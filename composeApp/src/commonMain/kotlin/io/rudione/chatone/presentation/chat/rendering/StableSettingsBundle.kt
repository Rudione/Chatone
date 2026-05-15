package io.rudione.chatone.presentation.chat.rendering

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import io.rudione.chatone.presentation.settings.InlineImageMode
import io.rudione.chatone.presentation.settings.SettingsState


@Stable
data class StableChatStyle(
    val showBadges: Boolean,
    val timestampFormat: SettingsState.TimestampFormat,
    val pauseOnHover: Boolean,
    val alternateRowBackground: Boolean,
    val showChatHeader: Boolean,
    val smoothChatEnabled: Boolean,
    val mentionSoundEnabled: Boolean,
    val mentionSoundVolume: Float,
    val customMentionSoundPath: String?,
    val disableScrollOnAlt: Boolean,
    val showInlineImages: InlineImageMode,
    val closeEmotePickerOnMouseLeave: Boolean,
    val scrollbackLimit: Int
)


@Composable
fun rememberStableChatStyle(s: SettingsState): StableChatStyle {
    return remember(
        s.showBadges, s.timestampFormat, s.pauseOnHover, s.alternateRowBackground,
        s.showChatHeader, s.smoothChatEnabled, s.mentionSoundEnabled, s.mentionSoundVolume,
        s.customMentionSoundPath, s.disableScrollOnAlt, s.showInlineImages,
        s.closeEmotePickerOnMouseLeave, s.scrollbackLimit
    ) {
        StableChatStyle(
            showBadges = s.showBadges,
            timestampFormat = s.timestampFormat,
            pauseOnHover = s.pauseOnHover,
            alternateRowBackground = s.alternateRowBackground,
            showChatHeader = s.showChatHeader,
            smoothChatEnabled = s.smoothChatEnabled,
            mentionSoundEnabled = s.mentionSoundEnabled,
            mentionSoundVolume = s.mentionSoundVolume,
            customMentionSoundPath = s.customMentionSoundPath,
            disableScrollOnAlt = s.disableScrollOnAlt,
            showInlineImages = s.showInlineImages,
            closeEmotePickerOnMouseLeave = s.closeEmotePickerOnMouseLeave,
            scrollbackLimit = s.scrollbackLimit
        )
    }
}
