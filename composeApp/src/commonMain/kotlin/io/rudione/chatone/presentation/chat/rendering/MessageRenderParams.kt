package io.rudione.chatone.presentation.chat.rendering

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.HighlightRule
import io.rudione.chatone.presentation.settings.SettingsState


@Stable
data class MessageRenderParams(
    val timestampFormat: SettingsState.TimestampFormat,
    val showBadges: Boolean,
    val emoteSize: SettingsState.EmoteSize,
    val fontSizeSp: Float,
    val extraVerticalPaddingDp: Dp,
    val spacingDp: Dp,
    val alternateRowBackground: Boolean,
    val confirmModActions: Boolean,
    val defaultTimeoutDuration: Int,
    val showDefaultDeleteButton: Boolean,
    val showDefaultTimeoutButton: Boolean,
    val showDefaultBanButton: Boolean,
    val highlightRules: List<HighlightRule>
)


@Composable
fun rememberMessageRenderParams(settings: SettingsState): MessageRenderParams {
    return remember(
        settings.timestampFormat,
        settings.showBadges,
        settings.emoteSize,
        settings.fontSize,
        settings.messageSpacing,
        settings.alternateRowBackground,
        settings.confirmModActions,
        settings.defaultTimeoutDuration,
        settings.showDefaultDeleteButton,
        settings.showDefaultTimeoutButton,
        settings.showDefaultBanButton,
        settings.highlightRules
    ) {
        val fontSize = when (settings.fontSize) {
            SettingsState.FontSize.SMALL -> 12f
            SettingsState.FontSize.MEDIUM -> 15f
            SettingsState.FontSize.LARGE -> 18f
        }
        val (spacing, extraPad) = when (settings.messageSpacing) {
            SettingsState.MessageSpacing.NONE -> 0.dp to 0.dp
            SettingsState.MessageSpacing.LOW -> 1.dp to 1.dp
            SettingsState.MessageSpacing.MEDIUM -> 3.dp to 2.dp
            SettingsState.MessageSpacing.HIGH -> 5.dp to 4.dp
        }
        MessageRenderParams(
            timestampFormat = settings.timestampFormat,
            showBadges = settings.showBadges,
            emoteSize = settings.emoteSize,
            fontSizeSp = fontSize,
            extraVerticalPaddingDp = extraPad,
            spacingDp = spacing,
            alternateRowBackground = settings.alternateRowBackground,
            confirmModActions = settings.confirmModActions,
            defaultTimeoutDuration = settings.defaultTimeoutDuration,
            showDefaultDeleteButton = settings.showDefaultDeleteButton,
            showDefaultTimeoutButton = settings.showDefaultTimeoutButton,
            showDefaultBanButton = settings.showDefaultBanButton,
            highlightRules = settings.highlightRules
        )
    }
}
