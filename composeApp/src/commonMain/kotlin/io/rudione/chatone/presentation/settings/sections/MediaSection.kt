package io.rudione.chatone.presentation.settings.sections

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
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.system.isDesktopPlatform

@Composable
internal fun MediaContent(state: SettingsState, vm: SettingsViewModel) {
    LinksMediaGroup(state, vm)
    if (isDesktopPlatform) UploadsContent(state) { vm.sendEvent(it) }
}

@Composable
private fun LinksMediaGroup(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    SettingsGroup(s.settingsHub.groupLinksMedia) {
        ListRow(
            s.settingsOpenLinks,
            when (state.linkOpenMode) {
                SettingsState.LinkOpenMode.DEFAULT -> s.settingsDefaultBrowser
                SettingsState.LinkOpenMode.INCOGNITO -> s.settingsIncognitoMode
            },
            listOf(s.settingsDefaultBrowser, s.settingsIncognitoMode)
        ) { vm.sendEvent(SettingsEvent.OnLinkOpenModeChanged(SettingsState.LinkOpenMode.entries[it])) }
        RowDivider()
        DropdownRow(
            label = s.settingsShowInlineImages,
            description = s.settingsShowInlineImagesDesc,
            options = listOf(s.on, s.off, s.blur),
            selected = state.showInlineImages.ordinal
        ) { idx -> vm.sendEvent(SettingsEvent.OnShowInlineImagesChanged(InlineImageMode.entries[idx])) }
        if (state.showInlineImages != InlineImageMode.OFF) {
            SliderRow(
                label = s.settingsImageMaxHeight,
                value = state.inlineImageMaxHeight.toFloat(),
                valueRange = 50f..500f,
                steps = 8,
                valueLabel = s.settingsImageMaxHeightUnit.replace("{0}", state.inlineImageMaxHeight.toString())
            ) { vm.sendEvent(SettingsEvent.OnInlineImageMaxHeightChanged(it.toInt())) }
        }
        SliderRow(
            label = s.settingsClipPreviewWidth,
            value = state.clipPreviewWidth.toFloat(),
            valueRange = 90f..320f,
            steps = 22,
            valueLabel = "${state.clipPreviewWidth} dp"
        ) { vm.sendEvent(SettingsEvent.OnClipPreviewWidthChanged(it.toInt())) }
        RowDivider()
        SwitchRow(s.settingsShowChatGifs, s.settingsShowChatGifsDesc, state.showChatGifs) {
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
    val s = LocalStrings.current
    var draft by remember(currentKey) { mutableStateOf(currentKey) }
    Column(
        modifier = Modifier
            .settingsAnchor(s.settingsGiphyApiKey)
            .fillMaxWidth()
            .padding(horizontal = SettingsRowMetrics.horizontal, vertical = SettingsRowMetrics.vertical)
    ) {
        HighlightedSettingsText(s.settingsGiphyApiKey, style = MaterialTheme.typography.bodyMedium)
        HighlightedSettingsText(
            s.settingsGiphyApiKeyDesc,
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
                placeholder = s.settingsGiphyApiKey
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
