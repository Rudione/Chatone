package io.rudione.chatone.presentation.settings.components

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.apply
import chatone.composeapp.generated.resources.cancel
import chatone.composeapp.generated.resources.highlight_rule_channel_points
import chatone.composeapp.generated.resources.highlight_rule_first_message
import chatone.composeapp.generated.resources.highlight_rule_mention_accent
import chatone.composeapp.generated.resources.highlight_rule_search_match
import chatone.composeapp.generated.resources.highlight_rule_subscriptions
import chatone.composeapp.generated.resources.highlight_rule_substring
import chatone.composeapp.generated.resources.highlight_rule_substring_desc
import chatone.composeapp.generated.resources.highlight_rule_username
import chatone.composeapp.generated.resources.highlight_rule_whispers
import chatone.composeapp.generated.resources.settings_highlight_color
import chatone.composeapp.generated.resources.settings_hub_highlight_remove
import chatone.composeapp.generated.resources.settings_hub_highlight_sound
import chatone.composeapp.generated.resources.settings_hue
import chatone.composeapp.generated.resources.settings_regex
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.HighlightRule
import io.rudione.chatone.icons.lucide.Bell
import io.rudione.chatone.icons.lucide.BellOff
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.chat.components.LiquidGlassTooltipBox
import io.rudione.chatone.presentation.components.ChatoneChip
import io.rudione.chatone.presentation.components.ChatoneChipDefaults
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneSlider
import io.rudione.chatone.presentation.components.ChatoneSwitch
import io.rudione.chatone.presentation.components.rows.HighlightedSettingsText
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.settings.settingsAnchor

@Composable
internal fun HighlightRuleRow(rule: HighlightRule, vm: SettingsViewModel, modifier: Modifier = Modifier) {
    val isCustom = rule.id.startsWith(CUSTOM_PREFIX)
    val label = highlightRuleLabel(rule)
    val color = Color(rule.color)
    var picking by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .settingsAnchor(label)
            .fillMaxWidth()
            .heightIn(min = ROW_HEIGHT)
            .clip(RowShape)
            .background(color.copy(alpha = if (rule.enabled) 0.08f else 0.03f))
            .border(1.dp, color.copy(alpha = if (rule.enabled) 0.3f else 0.12f), RowShape)
            .clickable { vm.sendEvent(SettingsEvent.OnHighlightRuleToggled(rule.id, !rule.enabled)) }
            .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), CircleShape)
                .clickable { picking = true }
                .pointerHoverIcon(PointerIcon.Hand)
        )
        Column(modifier = Modifier.weight(1f)) {
            HighlightedSettingsText(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (rule.enabled) 1f else 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isCustom && rule.isRegex) {
                Text(
                    stringResource(Res.string.settings_regex),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (isCustom && !rule.isRegex) {
            LiquidGlassTooltipBox(tooltip = stringResource(Res.string.highlight_rule_substring_desc)) {
                ChatoneChip(
                    label = stringResource(Res.string.highlight_rule_substring),
                    onClick = { vm.sendEvent(SettingsEvent.OnHighlightRuleSubstringToggled(rule.id, !rule.matchSubstring)) },
                    selected = rule.matchSubstring,
                    accent = color,
                    height = ChatoneChipDefaults.CompactHeight
                )
            }
        }
        LiquidGlassTooltipBox(tooltip = stringResource(Res.string.settings_hub_highlight_sound)) {
            ChatoneIconButton(
                onClick = { vm.sendEvent(SettingsEvent.OnHighlightRuleSoundToggled(rule.id, !rule.playSound)) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    if (rule.playSound) Lucide.Bell else Lucide.BellOff,
                    contentDescription = stringResource(Res.string.settings_hub_highlight_sound),
                    modifier = Modifier.size(15.dp),
                    tint = if (rule.playSound) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        ChatoneSwitch(
            checked = rule.enabled,
            onCheckedChange = { vm.sendEvent(SettingsEvent.OnHighlightRuleToggled(rule.id, it)) },
            modifier = Modifier.height(SWITCH_HEIGHT)
        )
        if (isCustom) {
            ChatoneIconButton(
                onClick = { vm.sendEvent(SettingsEvent.OnRemoveHighlightRule(rule.id)) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Lucide.X,
                    contentDescription = stringResource(Res.string.settings_hub_highlight_remove),
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    if (picking) {
        HighlightColorPickerDialog(
            initial = color,
            onConfirm = { picked ->
                vm.sendEvent(SettingsEvent.OnHighlightRuleColorChanged(rule.id, picked.toArgbLong()))
                picking = false
            },
            onDismiss = { picking = false }
        )
    }
}

@Composable
internal fun highlightRuleLabel(rule: HighlightRule): String = rule.pattern.ifEmpty {
    when (rule.id) {
        "username" -> stringResource(Res.string.highlight_rule_username)
        "whispers" -> stringResource(Res.string.highlight_rule_whispers)
        "subscriptions" -> stringResource(Res.string.highlight_rule_subscriptions)
        "first_message" -> stringResource(Res.string.highlight_rule_first_message)
        "search_match" -> stringResource(Res.string.highlight_rule_search_match)
        "mention_accent" -> stringResource(Res.string.highlight_rule_mention_accent)
        "channel_points" -> stringResource(Res.string.highlight_rule_channel_points)
        else -> rule.id.replace("_", " ").replaceFirstChar { it.uppercase() }
    }
}

internal val HighlightRule.isCustom: Boolean get() = id.startsWith(CUSTOM_PREFIX)

@Composable
private fun HighlightColorPickerDialog(
    initial: Color,
    onConfirm: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var picked by remember { mutableStateOf(initial) }
    val hue = remember(picked) { hueOf(picked) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_highlight_color)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(picked)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRESETS.forEach { preset ->
                        Box(
                            modifier = Modifier.weight(1f).aspectRatio(1f)
                                .clip(CircleShape).background(preset)
                                .border(if (picked == preset) 2.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                .clickable { picked = preset }
                        )
                    }
                }
                Text(stringResource(Res.string.settings_hue), style = MaterialTheme.typography.labelSmall)
                ChatoneSlider(
                    value = hue,
                    onValueChange = { h -> picked = Color.hsl(h, 0.85f, 0.55f) },
                    valueRange = 0f..360f,
                    trackHeight = 8.dp,
                    fullTrackBrush = Brush.horizontalGradient((0..12).map { Color.hsl(it * 30f % 360f, 0.85f, 0.55f) })
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(picked) }) { Text(stringResource(Res.string.apply)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } }
    )
}

private fun hueOf(color: Color): Float {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    if (max == min) return 0f
    val hue = when (max) {
        r -> ((g - b) / (max - min) % 6f) / 6f * 360f
        g -> ((b - r) / (max - min) + 2f) / 6f * 360f
        else -> ((r - g) / (max - min) + 4f) / 6f * 360f
    }
    return if (hue < 0f) hue + 360f else hue
}

private fun Color.toArgbLong(): Long {
    val opaque = copy(alpha = 1f)
    return ((opaque.red * 255).toLong() shl 16) or
        ((opaque.green * 255).toLong() shl 8) or
        (opaque.blue * 255).toLong() or 0xFF000000L
}

private const val CUSTOM_PREFIX = "custom_"
private val ROW_HEIGHT = 40.dp
private val SWITCH_HEIGHT = 28.dp
private val RowShape = RoundedCornerShape(12.dp)
private val PRESETS = listOf(
    Color(0xFFFF6B6B), Color(0xFFFF9F43), Color(0xFFFFD700), Color(0xFF2ECC71),
    Color(0xFF00BFFF), Color(0xFF9B59B6), Color(0xFFFF69B4), Color(0xFFFF4500)
)
