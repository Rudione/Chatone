package io.rudione.chatone.presentation.settings.sections

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Plus
import io.rudione.chatone.presentation.components.ChatoneTextField
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.settings.components.HighlightRuleRow
import io.rudione.chatone.presentation.settings.components.SettingsGrid
import io.rudione.chatone.presentation.settings.components.SettingsGroup
import io.rudione.chatone.presentation.settings.components.isCustom
import io.rudione.chatone.presentation.settings.settingsAnchor
import io.rudione.chatone.presentation.theme.i18n.LocalStrings

@Composable
internal fun HighlightContent(state: SettingsState, vm: SettingsViewModel) {
    val hub = LocalStrings.current.settingsHub
    val (custom, builtIn) = remember(state.highlightRules) { state.highlightRules.partition { it.isCustom } }
    SettingsGroup(hub.highlightBuiltIn) {
        SettingsGrid(
            items = builtIn,
            minCellWidth = BUILT_IN_CELL,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) { rule -> HighlightRuleRow(rule, vm) }
    }
    SettingsGroup(hub.highlightCustom) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AddHighlightRuleField(vm)
            if (custom.isEmpty()) {
                Text(
                    hub.highlightCustomEmpty,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                )
            } else {
                custom.forEach { rule -> key(rule.id) { HighlightRuleRow(rule, vm) } }
            }
            Text(
                LocalStrings.current.settingsHighlightRulesHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
    }
}

@Composable
private fun AddHighlightRuleField(vm: SettingsViewModel) {
    val s = LocalStrings.current
    var pattern by remember { mutableStateOf("") }
    val submit = {
        if (pattern.isNotBlank()) {
            vm.sendEvent(SettingsEvent.OnAddHighlightRule(pattern.trim()))
            pattern = ""
        }
    }
    Row(
        modifier = Modifier
            .settingsAnchor(s.settingsAddRule)
            .fillMaxWidth()
            .clip(FieldShape)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), FieldShape)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ChatoneTextField(
            value = pattern,
            onValueChange = { pattern = it },
            modifier = Modifier
                .weight(1f)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                        submit()
                        true
                    } else false
                },
            placeholder = s.settingsAddHighlightPattern,
            singleLine = true
        )
        FilledIconButton(onClick = submit, enabled = pattern.isNotBlank(), modifier = Modifier.size(36.dp)) {
            Icon(Lucide.Plus, contentDescription = s.settingsAddRule, modifier = Modifier.size(18.dp))
        }
    }
}

private val BUILT_IN_CELL = 260.dp
private val FieldShape = RoundedCornerShape(14.dp)
