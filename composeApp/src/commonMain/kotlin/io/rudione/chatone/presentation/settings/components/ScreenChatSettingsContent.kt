package io.rudione.chatone.presentation.settings.components

import chatone.composeapp.generated.resources.screen_chat_layer_all
import chatone.composeapp.generated.resources.screen_chat_layer_app
import chatone.composeapp.generated.resources.screen_chat_layer_normal
import org.jetbrains.compose.resources.StringResource
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.screen_chat_backdrop_help
import chatone.composeapp.generated.resources.screen_chat_backdrop_opacity_title
import chatone.composeapp.generated.resources.screen_chat_backdrop_title
import chatone.composeapp.generated.resources.screen_chat_background_title
import chatone.composeapp.generated.resources.screen_chat_font_size_title
import chatone.composeapp.generated.resources.screen_chat_group_look
import chatone.composeapp.generated.resources.screen_chat_group_messages
import chatone.composeapp.generated.resources.screen_chat_group_window
import chatone.composeapp.generated.resources.screen_chat_hide_delay_title
import chatone.composeapp.generated.resources.screen_chat_hide_delay_value
import chatone.composeapp.generated.resources.screen_chat_keep_dock_help
import chatone.composeapp.generated.resources.screen_chat_keep_dock_title
import chatone.composeapp.generated.resources.screen_chat_layer_help
import chatone.composeapp.generated.resources.screen_chat_layer_title
import chatone.composeapp.generated.resources.screen_chat_locked_help
import chatone.composeapp.generated.resources.screen_chat_locked_title
import chatone.composeapp.generated.resources.screen_chat_moderation_help
import chatone.composeapp.generated.resources.screen_chat_moderation_title
import chatone.composeapp.generated.resources.screen_chat_over_full_screen_help
import chatone.composeapp.generated.resources.screen_chat_over_full_screen_title
import chatone.composeapp.generated.resources.screen_chat_reply_above_help
import chatone.composeapp.generated.resources.screen_chat_reply_above_title
import chatone.composeapp.generated.resources.screen_chat_scale_title
import chatone.composeapp.generated.resources.screen_chat_settings_help
import chatone.composeapp.generated.resources.screen_chat_text_title
import org.jetbrains.compose.resources.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.ScreenChatLayer
import io.rudione.chatone.domain.model.ScreenChatSettings
import io.rudione.chatone.presentation.components.rows.ListRow
import io.rudione.chatone.presentation.components.rows.RowDivider
import io.rudione.chatone.presentation.components.rows.SliderRow
import io.rudione.chatone.presentation.components.rows.SwitchRow
import kotlin.math.roundToInt

@Composable
internal fun ScreenChatSettingsContent(
    value: ScreenChatSettings,
    floatsOverFullScreen: Boolean,
    onChange: (ScreenChatSettings) -> Unit
) {
    Text(
        stringResource(Res.string.screen_chat_settings_help),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 6.dp)
    )
    SettingsPair(
        first = { ScreenChatWindowGroup(value, floatsOverFullScreen, onChange) },
        second = { ScreenChatMessagesGroup(value, onChange) }
    )
    ScreenChatLookGroup(value, onChange)
}

@Composable
private fun ScreenChatWindowGroup(
    value: ScreenChatSettings,
    floatsOverFullScreen: Boolean,
    onChange: (ScreenChatSettings) -> Unit
) {
    SettingsGroup(stringResource(Res.string.screen_chat_group_window)) {
        ListRow(
            title = stringResource(Res.string.screen_chat_layer_title),
            value = layerLabel(value.layer),
            options = ScreenChatLayer.entries.map { layerLabel(it) },
            help = stringResource(Res.string.screen_chat_layer_help)
        ) { index -> onChange(value.copy(layer = ScreenChatLayer.entries[index])) }
        if (floatsOverFullScreen && value.layer == ScreenChatLayer.ALL_WINDOWS) {
            RowDivider()
            SwitchRow(stringResource(Res.string.screen_chat_over_full_screen_title), stringResource(Res.string.screen_chat_over_full_screen_help), value.overFullScreen) {
                onChange(value.copy(overFullScreen = it))
            }
            if (value.overFullScreen) {
                RowDivider()
                SwitchRow(stringResource(Res.string.screen_chat_keep_dock_title), stringResource(Res.string.screen_chat_keep_dock_help), value.keepDockIcon) { onChange(value.copy(keepDockIcon = it)) }
            }
        }
        RowDivider()
        SwitchRow(title = stringResource(Res.string.screen_chat_locked_title), subtitle = stringResource(Res.string.screen_chat_locked_help), checked = value.locked) {
            onChange(value.copy(locked = it))
        }
        SliderRow(
            title = stringResource(Res.string.screen_chat_hide_delay_title),
            value = value.hideDelaySeconds,
            valueRange = ScreenChatSettings.MIN_HIDE_DELAY_SECONDS.toFloat()..ScreenChatSettings.MAX_HIDE_DELAY_SECONDS.toFloat(),
            steps = stepsBetween(ScreenChatSettings.MIN_HIDE_DELAY_SECONDS, ScreenChatSettings.MAX_HIDE_DELAY_SECONDS, 1),
            valueLabel = stringResource(Res.string.screen_chat_hide_delay_value, value.hideDelaySeconds),
            onValueChange = { onChange(value.copy(hideDelaySeconds = it.roundToInt())) }
        )
    }
}

@Composable
private fun ScreenChatMessagesGroup(value: ScreenChatSettings, onChange: (ScreenChatSettings) -> Unit) {
    SettingsGroup(stringResource(Res.string.screen_chat_group_messages)) {
        SwitchRow(stringResource(Res.string.screen_chat_reply_above_title), stringResource(Res.string.screen_chat_reply_above_help), value.replyAbove) { onChange(value.copy(replyAbove = it)) }
        RowDivider()
        SwitchRow(stringResource(Res.string.screen_chat_moderation_title), stringResource(Res.string.screen_chat_moderation_help), value.showModeration) { onChange(value.copy(showModeration = it)) }
    }
}

@Composable
private fun ScreenChatLookGroup(value: ScreenChatSettings, onChange: (ScreenChatSettings) -> Unit) {
    SettingsGroup(stringResource(Res.string.screen_chat_group_look)) {
        SliderRow(
            title = stringResource(Res.string.screen_chat_font_size_title),
            value = value.fontSize,
            valueRange = ScreenChatSettings.MIN_FONT_SIZE.toFloat()..ScreenChatSettings.MAX_FONT_SIZE.toFloat(),
            steps = stepsBetween(ScreenChatSettings.MIN_FONT_SIZE, ScreenChatSettings.MAX_FONT_SIZE, 1),
            valueLabel = value.fontSize.toString(),
            onValueChange = { onChange(value.copy(fontSize = it.roundToInt())) }
        )
        SliderRow(
            title = stringResource(Res.string.screen_chat_scale_title),
            value = value.scalePercent,
            valueRange = ScreenChatSettings.MIN_SCALE_PERCENT.toFloat()..ScreenChatSettings.MAX_SCALE_PERCENT.toFloat(),
            steps = stepsBetween(
                ScreenChatSettings.MIN_SCALE_PERCENT,
                ScreenChatSettings.MAX_SCALE_PERCENT,
                ScreenChatSettings.SCALE_STEP_PERCENT
            ),
            valueLabel = "${value.scalePercent}%",
            onValueChange = { onChange(value.copy(scalePercent = it.roundToInt())) }
        )
        PercentSlider(stringResource(Res.string.screen_chat_text_title), value.textPercent, ScreenChatSettings.MIN_TEXT_PERCENT) {
            onChange(value.copy(textPercent = it))
        }
        PercentSlider(stringResource(Res.string.screen_chat_background_title), value.backgroundPercent, 0) { onChange(value.copy(backgroundPercent = it)) }
        RowDivider()
        SwitchRow(stringResource(Res.string.screen_chat_backdrop_title), stringResource(Res.string.screen_chat_backdrop_help), value.backdrop) { onChange(value.copy(backdrop = it)) }
        AnimatedVisibility(
            visible = value.backdrop,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            PercentSlider(stringResource(Res.string.screen_chat_backdrop_opacity_title), value.backdropPercent, ScreenChatSettings.MIN_BACKDROP_PERCENT) {
                onChange(value.copy(backdropPercent = it))
            }
        }
    }
}

@Composable
private fun PercentSlider(title: String, percent: Int, min: Int, onChange: (Int) -> Unit) {
    SliderRow(
        title = title,
        value = percent,
        valueRange = min.toFloat()..ScreenChatSettings.MAX_PERCENT.toFloat(),
        steps = stepsBetween(min, ScreenChatSettings.MAX_PERCENT, PERCENT_STEP),
        valueLabel = "$percent%",
        onValueChange = { onChange(it.roundToInt()) }
    )
}

private fun stepsBetween(min: Int, max: Int, step: Int): Int = ((max - min) / step - 1).coerceAtLeast(0)

internal fun ScreenChatLayer.labelRes(): StringResource = when (this) {
    ScreenChatLayer.ALL_WINDOWS -> Res.string.screen_chat_layer_all
    ScreenChatLayer.APP_WINDOWS -> Res.string.screen_chat_layer_app
    ScreenChatLayer.NORMAL -> Res.string.screen_chat_layer_normal
}

@Composable
internal fun layerLabel(layer: ScreenChatLayer): String = stringResource(layer.labelRes())

private const val PERCENT_STEP = 5
