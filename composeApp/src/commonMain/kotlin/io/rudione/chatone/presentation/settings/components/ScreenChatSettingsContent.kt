package io.rudione.chatone.presentation.settings.components

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
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.theme.i18n.ScreenChatStrings
import kotlin.math.roundToInt

@Composable
internal fun ScreenChatSettingsContent(
    value: ScreenChatSettings,
    floatsOverFullScreen: Boolean,
    onChange: (ScreenChatSettings) -> Unit
) {
    val s = LocalStrings.current.screenChat
    Text(
        s.settingsHelp,
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
    val s = LocalStrings.current.screenChat
    SettingsGroup(s.groupWindow) {
        ListRow(
            title = s.layerTitle,
            value = s.layerLabel(value.layer),
            options = ScreenChatLayer.entries.map(s::layerLabel),
            help = s.layerHelp
        ) { index -> onChange(value.copy(layer = ScreenChatLayer.entries[index])) }
        if (floatsOverFullScreen && value.layer == ScreenChatLayer.ALL_WINDOWS) {
            RowDivider()
            SwitchRow(s.overFullScreenTitle, s.overFullScreenHelp, value.overFullScreen) {
                onChange(value.copy(overFullScreen = it))
            }
            if (value.overFullScreen) {
                RowDivider()
                SwitchRow(s.keepDockTitle, s.keepDockHelp, value.keepDockIcon) { onChange(value.copy(keepDockIcon = it)) }
            }
        }
        RowDivider()
        SwitchRow(title = s.lockedTitle, subtitle = s.lockedHelp, checked = value.locked) {
            onChange(value.copy(locked = it))
        }
        SliderRow(
            title = s.hideDelayTitle,
            value = value.hideDelaySeconds,
            valueRange = ScreenChatSettings.MIN_HIDE_DELAY_SECONDS.toFloat()..ScreenChatSettings.MAX_HIDE_DELAY_SECONDS.toFloat(),
            steps = stepsBetween(ScreenChatSettings.MIN_HIDE_DELAY_SECONDS, ScreenChatSettings.MAX_HIDE_DELAY_SECONDS, 1),
            valueLabel = s.hideDelayValue.replace("{0}", value.hideDelaySeconds.toString()),
            onValueChange = { onChange(value.copy(hideDelaySeconds = it.roundToInt())) }
        )
    }
}

@Composable
private fun ScreenChatMessagesGroup(value: ScreenChatSettings, onChange: (ScreenChatSettings) -> Unit) {
    val s = LocalStrings.current.screenChat
    SettingsGroup(s.groupMessages) {
        SwitchRow(s.replyAboveTitle, s.replyAboveHelp, value.replyAbove) { onChange(value.copy(replyAbove = it)) }
        RowDivider()
        SwitchRow(s.moderationTitle, s.moderationHelp, value.showModeration) { onChange(value.copy(showModeration = it)) }
    }
}

@Composable
private fun ScreenChatLookGroup(value: ScreenChatSettings, onChange: (ScreenChatSettings) -> Unit) {
    val s = LocalStrings.current.screenChat
    SettingsGroup(s.groupLook) {
        SliderRow(
            title = s.fontSizeTitle,
            value = value.fontSize,
            valueRange = ScreenChatSettings.MIN_FONT_SIZE.toFloat()..ScreenChatSettings.MAX_FONT_SIZE.toFloat(),
            steps = stepsBetween(ScreenChatSettings.MIN_FONT_SIZE, ScreenChatSettings.MAX_FONT_SIZE, 1),
            valueLabel = value.fontSize.toString(),
            onValueChange = { onChange(value.copy(fontSize = it.roundToInt())) }
        )
        SliderRow(
            title = s.scaleTitle,
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
        PercentSlider(s.textTitle, value.textPercent, ScreenChatSettings.MIN_TEXT_PERCENT) {
            onChange(value.copy(textPercent = it))
        }
        PercentSlider(s.backgroundTitle, value.backgroundPercent, 0) { onChange(value.copy(backgroundPercent = it)) }
        RowDivider()
        SwitchRow(s.backdropTitle, s.backdropHelp, value.backdrop) { onChange(value.copy(backdrop = it)) }
        AnimatedVisibility(
            visible = value.backdrop,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            PercentSlider(s.backdropOpacityTitle, value.backdropPercent, ScreenChatSettings.MIN_BACKDROP_PERCENT) {
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

internal fun ScreenChatStrings.layerLabel(layer: ScreenChatLayer): String = when (layer) {
    ScreenChatLayer.ALL_WINDOWS -> layerAll
    ScreenChatLayer.APP_WINDOWS -> layerApp
    ScreenChatLayer.NORMAL -> layerNormal
}

private const val PERCENT_STEP = 5
