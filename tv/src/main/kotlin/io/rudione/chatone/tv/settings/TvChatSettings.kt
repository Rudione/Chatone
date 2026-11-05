package io.rudione.chatone.tv.settings

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.player_chat_background
import chatone.composeapp.generated.resources.tv_settings_overlay_idle
import chatone.composeapp.generated.resources.tv_settings_overlay_idle_desc
import chatone.composeapp.generated.resources.tv_settings_overlay_hide_delay
import chatone.composeapp.generated.resources.player_overlay_seconds
import chatone.composeapp.generated.resources.settings_emote_size
import chatone.composeapp.generated.resources.settings_font_size
import chatone.composeapp.generated.resources.settings_message_spacing
import chatone.composeapp.generated.resources.settings_spacing_high
import chatone.composeapp.generated.resources.settings_spacing_low
import chatone.composeapp.generated.resources.settings_spacing_medium
import chatone.composeapp.generated.resources.settings_spacing_none
import chatone.composeapp.generated.resources.settings_size_large
import chatone.composeapp.generated.resources.settings_size_medium
import chatone.composeapp.generated.resources.settings_size_small
import chatone.composeapp.generated.resources.settings_smooth_chat
import chatone.composeapp.generated.resources.settings_smooth_chat_desc
import chatone.composeapp.generated.resources.tv_settings_badges
import chatone.composeapp.generated.resources.tv_settings_chat_size
import chatone.composeapp.generated.resources.tv_settings_chat_width
import chatone.composeapp.generated.resources.actions_auto_claim
import chatone.composeapp.generated.resources.actions_auto_claim_hint
import chatone.composeapp.generated.resources.blur
import chatone.composeapp.generated.resources.off
import chatone.composeapp.generated.resources.on
import chatone.composeapp.generated.resources.settings_alternate_row_bg
import chatone.composeapp.generated.resources.settings_alternate_row_bg_desc
import chatone.composeapp.generated.resources.settings_readable_nick_colors
import chatone.composeapp.generated.resources.settings_readable_nick_colors_desc
import chatone.composeapp.generated.resources.settings_show_chat_gifs
import chatone.composeapp.generated.resources.settings_show_chat_gifs_desc
import chatone.composeapp.generated.resources.settings_show_deleted_messages
import chatone.composeapp.generated.resources.settings_show_deleted_messages_desc
import chatone.composeapp.generated.resources.settings_show_inline_images
import chatone.composeapp.generated.resources.settings_timestamp12h
import chatone.composeapp.generated.resources.settings_timestamp24h
import chatone.composeapp.generated.resources.settings_timestamp_format
import chatone.composeapp.generated.resources.settings_timestamp_off
import io.rudione.chatone.presentation.settings.InlineImageMode
import io.rudione.chatone.domain.stream.StreamPlayerPreferences
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.stream.StreamPlayerEvent
import io.rudione.chatone.presentation.stream.StreamPlayerHost
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val ChatScaleOptions = listOf(0.7f, 0.85f, 1f, 1.2f, 1.4f)
private val ChatWidthOptions = listOf(0.22f, 0.26f, 0.3f, 0.36f, 0.42f)
private val OverlayOpacityOptions = listOf(0f, 0.25f, 0.4f, 0.55f, 0.7f, 0.9f)
private val OverlayIdleOptions = listOf(0f, 0.15f, 0.3f, 0.45f, 0.6f)
private val OverlayDelayOptions = listOf(1, 2, 3, 5, 10)

@Composable
internal fun TvChatSettings(
    settings: SettingsState,
    send: (SettingsEvent) -> Unit,
    host: StreamPlayerHost?
) {
    val preferences = host?.state?.collectAsState()?.value?.preferences
    val scale = preferences?.overlayScale ?: 1f
    val width = preferences?.sideChatFraction ?: StreamPlayerPreferences.DEFAULT_SIDE_CHAT_FRACTION
    TvSettingsList {
        if (host != null) {
            item(key = "scale") {
                val current = ChatScaleOptions.minBy { kotlin.math.abs(it - scale) }
                TvCycleRow(
                    title = stringResource(Res.string.tv_settings_chat_size),
                    value = "${(current * 100).toInt()}%",
                    options = ChatScaleOptions,
                    current = current,
                    onChange = { host.onEvent(StreamPlayerEvent.SetOverlayScale(it)) }
                )
            }
            item(key = "width") {
                val current = ChatWidthOptions.minBy { kotlin.math.abs(it - width) }
                TvCycleRow(
                    title = stringResource(Res.string.tv_settings_chat_width),
                    value = "${(current * 100).toInt()}%",
                    options = ChatWidthOptions,
                    current = current,
                    onChange = { host.onEvent(StreamPlayerEvent.SetSideChatFraction(it)) }
                )
            }
            item(key = "overlayBackground") {
                val opacity = preferences?.overlayOpacity ?: StreamPlayerPreferences.DEFAULT_OVERLAY_OPACITY
                val current = OverlayOpacityOptions.minBy { kotlin.math.abs(it - opacity) }
                TvCycleRow(
                    title = stringResource(Res.string.player_chat_background),
                    value = percent(current),
                    options = OverlayOpacityOptions,
                    current = current,
                    onChange = { host.onEvent(StreamPlayerEvent.SetOverlayOpacity(it)) }
                )
            }
            item(key = "overlayIdle") {
                val opacity = preferences?.overlayIdleOpacity ?: StreamPlayerPreferences.DEFAULT_OVERLAY_IDLE_OPACITY
                val current = OverlayIdleOptions.minBy { kotlin.math.abs(it - opacity) }
                TvCycleRow(
                    title = stringResource(Res.string.tv_settings_overlay_idle),
                    value = percent(current),
                    options = OverlayIdleOptions,
                    current = current,
                    onChange = { host.onEvent(StreamPlayerEvent.SetOverlayIdleOpacity(it)) },
                    description = stringResource(Res.string.tv_settings_overlay_idle_desc)
                )
            }
            item(key = "overlayDelay") {
                val seconds = preferences?.overlayHideDelaySeconds ?: StreamPlayerPreferences.DEFAULT_OVERLAY_HIDE_DELAY_SECONDS
                val current = OverlayDelayOptions.minBy { kotlin.math.abs(it - seconds) }
                TvCycleRow(
                    title = stringResource(Res.string.tv_settings_overlay_hide_delay),
                    value = stringResource(Res.string.player_overlay_seconds, current),
                    options = OverlayDelayOptions,
                    current = current,
                    onChange = { host.onEvent(StreamPlayerEvent.SetOverlayHideDelay(it)) }
                )
            }
        }
        item(key = "font") {
            TvCycleRow(
                title = stringResource(Res.string.settings_font_size),
                value = stringResource(settings.fontSize.label()),
                options = SettingsState.FontSize.entries,
                current = settings.fontSize,
                onChange = { send(SettingsEvent.OnFontSizeChanged(it)) }
            )
        }
        item(key = "emotes") {
            TvCycleRow(
                title = stringResource(Res.string.settings_emote_size),
                value = stringResource(settings.emoteSize.label()),
                options = SettingsState.EmoteSize.entries,
                current = settings.emoteSize,
                onChange = { send(SettingsEvent.OnEmoteSizeChanged(it)) }
            )
        }
        item(key = "spacing") {
            TvCycleRow(
                title = stringResource(Res.string.settings_message_spacing),
                value = stringResource(settings.messageSpacing.label()),
                options = SettingsState.MessageSpacing.entries,
                current = settings.messageSpacing,
                onChange = { send(SettingsEvent.OnMessageSpacingChanged(it)) }
            )
        }
        item(key = "badges") {
            TvSwitchRow(stringResource(Res.string.tv_settings_badges), null, settings.showBadges) {
                send(SettingsEvent.OnShowBadgesChanged(it))
            }
        }
        item(key = "smooth") {
            TvSwitchRow(
                stringResource(Res.string.settings_smooth_chat),
                stringResource(Res.string.settings_smooth_chat_desc),
                settings.smoothChatEnabled
            ) { send(SettingsEvent.OnSmoothChatEnabledChanged(it)) }
        }
        item(key = "timestamps") {
            TvCycleRow(
                title = stringResource(Res.string.settings_timestamp_format),
                value = stringResource(settings.timestampFormat.label()),
                options = SettingsState.TimestampFormat.entries,
                current = settings.timestampFormat,
                onChange = { send(SettingsEvent.OnTimestampFormatChanged(it)) }
            )
        }
        item(key = "readableNicks") {
            TvSwitchRow(
                stringResource(Res.string.settings_readable_nick_colors),
                stringResource(Res.string.settings_readable_nick_colors_desc),
                settings.readableNickColors
            ) { send(SettingsEvent.OnReadableNickColorsChanged(it)) }
        }
        item(key = "alternateRows") {
            TvSwitchRow(
                stringResource(Res.string.settings_alternate_row_bg),
                stringResource(Res.string.settings_alternate_row_bg_desc),
                settings.alternateRowBackground
            ) { send(SettingsEvent.OnAlternateRowBackgroundChanged(it)) }
        }
        item(key = "deleted") {
            TvSwitchRow(
                stringResource(Res.string.settings_show_deleted_messages),
                stringResource(Res.string.settings_show_deleted_messages_desc),
                settings.showDeletedMessages
            ) { send(SettingsEvent.OnShowDeletedChanged(it)) }
        }
        item(key = "images") {
            TvCycleRow(
                title = stringResource(Res.string.settings_show_inline_images),
                value = stringResource(settings.showInlineImages.label()),
                options = InlineImageMode.entries,
                current = settings.showInlineImages,
                onChange = { send(SettingsEvent.OnShowInlineImagesChanged(it)) }
            )
        }
        item(key = "gifs") {
            TvSwitchRow(
                stringResource(Res.string.settings_show_chat_gifs),
                stringResource(Res.string.settings_show_chat_gifs_desc),
                settings.showChatGifs
            ) { send(SettingsEvent.OnShowChatGifsChanged(it)) }
        }
        item(key = "autoClaim") {
            TvSwitchRow(
                stringResource(Res.string.actions_auto_claim),
                stringResource(Res.string.actions_auto_claim_hint),
                settings.autoClaimPoints
            ) { send(SettingsEvent.OnAutoClaimPointsChanged(it)) }
        }
    }
}

private fun SettingsState.FontSize.label(): StringResource = when (this) {
    SettingsState.FontSize.SMALL -> Res.string.settings_size_small
    SettingsState.FontSize.MEDIUM -> Res.string.settings_size_medium
    SettingsState.FontSize.LARGE -> Res.string.settings_size_large
}

private fun SettingsState.MessageSpacing.label(): StringResource = when (this) {
    SettingsState.MessageSpacing.NONE -> Res.string.settings_spacing_none
    SettingsState.MessageSpacing.LOW -> Res.string.settings_spacing_low
    SettingsState.MessageSpacing.MEDIUM -> Res.string.settings_spacing_medium
    SettingsState.MessageSpacing.HIGH -> Res.string.settings_spacing_high
}

private fun SettingsState.TimestampFormat.label(): StringResource = when (this) {
    SettingsState.TimestampFormat.H24 -> Res.string.settings_timestamp24h
    SettingsState.TimestampFormat.H12 -> Res.string.settings_timestamp12h
    SettingsState.TimestampFormat.OFF -> Res.string.settings_timestamp_off
}

private fun InlineImageMode.label(): StringResource = when (this) {
    InlineImageMode.ON -> Res.string.on
    InlineImageMode.OFF -> Res.string.off
    InlineImageMode.BLUR -> Res.string.blur
}

private fun SettingsState.EmoteSize.label(): StringResource = when (this) {
    SettingsState.EmoteSize.SMALL -> Res.string.settings_size_small
    SettingsState.EmoteSize.MEDIUM -> Res.string.settings_size_medium
    SettingsState.EmoteSize.LARGE -> Res.string.settings_size_large
}

private fun percent(value: Float): String = "${(value * 100).toInt()}%"
