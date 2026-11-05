package io.rudione.chatone.presentation.settings.sections

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.blur
import chatone.composeapp.generated.resources.off
import chatone.composeapp.generated.resources.on
import chatone.composeapp.generated.resources.settings_clip_preview_width
import chatone.composeapp.generated.resources.settings_default_browser
import chatone.composeapp.generated.resources.settings_giphy_api_key
import chatone.composeapp.generated.resources.settings_giphy_api_key_desc
import chatone.composeapp.generated.resources.settings_hub_group_links_media
import chatone.composeapp.generated.resources.settings_image_max_height
import chatone.composeapp.generated.resources.settings_image_max_height_unit
import chatone.composeapp.generated.resources.settings_incognito_mode
import chatone.composeapp.generated.resources.settings_open_links
import chatone.composeapp.generated.resources.settings_show_chat_gifs
import chatone.composeapp.generated.resources.settings_show_chat_gifs_desc
import chatone.composeapp.generated.resources.settings_show_inline_images
import chatone.composeapp.generated.resources.settings_show_inline_images_desc
import org.jetbrains.compose.resources.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rudione.chatone.icons.lucide.Check
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.presentation.components.ChatoneSecretField
import io.rudione.chatone.presentation.components.rows.DropdownRow
import io.rudione.chatone.presentation.components.rows.HighlightedSettingsText
import io.rudione.chatone.presentation.components.rows.ListRow
import io.rudione.chatone.presentation.components.rows.RowDivider
import io.rudione.chatone.presentation.components.rows.SettingsRowMetrics
import io.rudione.chatone.presentation.components.rows.SliderRow
import io.rudione.chatone.presentation.components.rows.SwitchRow
import io.rudione.chatone.presentation.settings.InlineImageMode
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.settings.components.SettingsGroup
import io.rudione.chatone.presentation.settings.components.UploadsContent
import io.rudione.chatone.presentation.settings.settingsAnchor
import io.rudione.chatone.util.system.isDesktopPlatform

@Composable
internal fun MediaContent(state: SettingsState, vm: SettingsViewModel) {
    LinksMediaGroup(state, vm)
    if (isDesktopPlatform) UploadsContent(state) { vm.sendEvent(it) }
}

@Composable
private fun LinksMediaGroup(state: SettingsState, vm: SettingsViewModel) {
    SettingsGroup(stringResource(Res.string.settings_hub_group_links_media)) {
        ListRow(
            stringResource(Res.string.settings_open_links),
            when (state.linkOpenMode) {
                SettingsState.LinkOpenMode.DEFAULT -> stringResource(Res.string.settings_default_browser)
                SettingsState.LinkOpenMode.INCOGNITO -> stringResource(Res.string.settings_incognito_mode)
            },
            listOf(stringResource(Res.string.settings_default_browser), stringResource(Res.string.settings_incognito_mode))
        ) { vm.sendEvent(SettingsEvent.OnLinkOpenModeChanged(SettingsState.LinkOpenMode.entries[it])) }
        RowDivider()
        DropdownRow(
            label = stringResource(Res.string.settings_show_inline_images),
            description = stringResource(Res.string.settings_show_inline_images_desc),
            options = listOf(stringResource(Res.string.on), stringResource(Res.string.off), stringResource(Res.string.blur)),
            selected = state.showInlineImages.ordinal
        ) { idx -> vm.sendEvent(SettingsEvent.OnShowInlineImagesChanged(InlineImageMode.entries[idx])) }
        if (state.showInlineImages != InlineImageMode.OFF) {
            SliderRow(
                label = stringResource(Res.string.settings_image_max_height),
                value = state.inlineImageMaxHeight.toFloat(),
                valueRange = 50f..500f,
                steps = 8,
                valueLabel = stringResource(Res.string.settings_image_max_height_unit, state.inlineImageMaxHeight)
            ) { vm.sendEvent(SettingsEvent.OnInlineImageMaxHeightChanged(it.toInt())) }
        }
        SliderRow(
            label = stringResource(Res.string.settings_clip_preview_width),
            value = state.clipPreviewWidth.toFloat(),
            valueRange = 90f..320f,
            steps = 22,
            valueLabel = "${state.clipPreviewWidth} dp"
        ) { vm.sendEvent(SettingsEvent.OnClipPreviewWidthChanged(it.toInt())) }
        RowDivider()
        SwitchRow(stringResource(Res.string.settings_show_chat_gifs), stringResource(Res.string.settings_show_chat_gifs_desc), state.showChatGifs) {
            vm.sendEvent(SettingsEvent.OnShowChatGifsChanged(it))
        }
        AnimatedVisibility(
            visible = state.showChatGifs,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            GiphyApiKeyRow(state.giphyApiKey) { vm.sendEvent(SettingsEvent.OnGiphyApiKeyChanged(it)) }
        }
    }
}

@Composable
private fun GiphyApiKeyRow(currentKey: String, onSave: (String) -> Unit) {
    var draft by remember(currentKey) { mutableStateOf(currentKey) }
    Column(
        modifier = Modifier
            .settingsAnchor(stringResource(Res.string.settings_giphy_api_key))
            .fillMaxWidth()
            .padding(horizontal = SettingsRowMetrics.horizontal, vertical = SettingsRowMetrics.vertical)
    ) {
        HighlightedSettingsText(stringResource(Res.string.settings_giphy_api_key), style = MaterialTheme.typography.bodyMedium)
        HighlightedSettingsText(
            stringResource(Res.string.settings_giphy_api_key_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ChatoneSecretField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = stringResource(Res.string.settings_giphy_api_key)
            )
            FilledIconButton(
                onClick = { onSave(draft) },
                enabled = draft.trim() != currentKey
            ) {
                Icon(Lucide.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}
