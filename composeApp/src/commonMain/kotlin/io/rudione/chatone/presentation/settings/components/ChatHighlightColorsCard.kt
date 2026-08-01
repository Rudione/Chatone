package io.rudione.chatone.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.ChatRuleType
import io.rudione.chatone.domain.model.ChatoneColorTokens
import io.rudione.chatone.presentation.components.ChatoneFieldLabel
import io.rudione.chatone.presentation.components.ChatoneTextField
import io.rudione.chatone.presentation.theme.i18n.LocalStrings

private class ColorEntry(
    val label: String,
    val get: (ChatoneColorTokens) -> Long,
    val set: (ChatoneColorTokens, Long) -> ChatoneColorTokens,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorTokensEditor(
    tokens: ChatoneColorTokens,
    onChange: (ChatoneColorTokens) -> Unit,
) {
    val s = LocalStrings.current
    var pickerEntry by remember { mutableStateOf<ColorEntry?>(null) }
    var pickerHex by remember { mutableStateOf("") }

    val highlights = listOf(
        ColorEntry(s.colorMentionBg, { it.mentionBg }) { t, v -> t.copy(mentionBg = v) },
        ColorEntry(s.highlightRuleMentionAccent, { it.mentionAccent }) { t, v -> t.copy(mentionAccent = v) },
        ColorEntry(s.colorHighlightBg, { it.highlightBg }) { t, v -> t.copy(highlightBg = v) },
        ColorEntry(s.highlightRuleFirstMessage, { it.firstMessageColor }) { t, v -> t.copy(firstMessageColor = v) },
        ColorEntry(s.highlightRuleSearchMatch, { it.searchMatchBg }) { t, v -> t.copy(searchMatchBg = v) },
        ColorEntry(s.colorOwnMessageBg, { it.ownMessageBg }) { t, v -> t.copy(ownMessageBg = v) },
    )
    val moderation = listOf(
        ColorEntry(s.colorModDelete, { it.modDelete }) { t, v -> t.copy(modDelete = v) },
        ColorEntry(s.profileTimeout, { it.modTimeout }) { t, v -> t.copy(modTimeout = v) },
        ColorEntry(s.profileBan, { it.modBan }) { t, v -> t.copy(modBan = v) },
        ColorEntry(s.profileUnban, { it.modUnban }) { t, v -> t.copy(modUnban = v) },
    )
    val automod = listOf(
        ColorEntry(s.chatRuleTypeSpam, { it.automodSpam }) { t, v -> t.copy(automodSpam = v) },
        ColorEntry(s.chatRuleTypeCaps, { it.automodCaps }) { t, v -> t.copy(automodCaps = v) },
        ColorEntry(s.chatRuleTypeLinks, { it.automodLinks }) { t, v -> t.copy(automodLinks = v) },
        ColorEntry(s.chatRuleTypeEmotes, { it.automodEmotes }) { t, v -> t.copy(automodEmotes = v) },
        ColorEntry(s.chatRuleTypeNewUser, { it.automodNewAccount }) { t, v -> t.copy(automodNewAccount = v) },
        ColorEntry(s.chatRuleTypeDuplicate, { it.automodDuplicate }) { t, v -> t.copy(automodDuplicate = v) },
        ColorEntry(s.chatRuleTypeConsecutiveNumbers, { it.automodNumbers }) { t, v -> t.copy(automodNumbers = v) },
        ColorEntry(s.chatRuleTypeStreamOnline, { it.automodStreamOnline }) { t, v -> t.copy(automodStreamOnline = v) },
        ColorEntry(s.chatRuleTypeStreamOffline, { it.automodStreamOffline }) { t, v -> t.copy(automodStreamOffline = v) },
        ColorEntry(s.chatRuleTypeFirstMessageGreeting, { it.automodFirstGreeting }) { t, v -> t.copy(automodFirstGreeting = v) },
        ColorEntry(s.chatRuleTypeRaidWelcome, { it.automodRaid }) { t, v -> t.copy(automodRaid = v) },
    )

    fun openPicker(entry: ColorEntry) {
        pickerEntry = entry
        pickerHex = entry.get(tokens).toHex()
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ColorGroup(s.colorsGroupHighlights, s.colorsGroupHighlightsHint, highlights, tokens, ::openPicker) {
            onChange(highlights.fold(tokens) { acc, e -> e.set(acc, e.get(ChatoneColorTokens())) })
        }
        ColorGroup(s.colorsGroupModeration, s.colorsGroupModerationHint, moderation, tokens, ::openPicker) {
            onChange(moderation.fold(tokens) { acc, e -> e.set(acc, e.get(ChatoneColorTokens())) })
        }
        ColorGroup(s.colorsGroupAutomod, s.colorsGroupAutomodHint, automod, tokens, ::openPicker) {
            onChange(automod.fold(tokens) { acc, e -> e.set(acc, e.get(ChatoneColorTokens())) })
        }
    }

    pickerEntry?.let { entry ->
        ColorPickerDialog(
            title = entry.label,
            hex = pickerHex,
            onHexChange = { pickerHex = it },
            onConfirm = {
                runCatching { pickerHex.fromHex() }.getOrNull()?.let { onChange(entry.set(tokens, it)) }
                pickerEntry = null
            },
            onDismiss = { pickerEntry = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorGroup(
    title: String,
    hint: String,
    entries: List<ColorEntry>,
    tokens: ChatoneColorTokens,
    onPick: (ColorEntry) -> Unit,
    onResetGroup: () -> Unit,
) {
    val s = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                ChatoneFieldLabel(text = title, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(2.dp))
                ChatoneFieldLabel(text = hint, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onResetGroup, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text(s.colorsResetGroup, style = MaterialTheme.typography.labelSmall)
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            entries.forEach { entry -> ColorSwatchChip(entry.label, Color(entry.get(tokens))) { onPick(entry) } }
        }
    }
}

@Composable
private fun ColorSwatchChip(label: String, color: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier.size(18.dp).clip(CircleShape).background(color)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), CircleShape)
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 130.dp)
            )
        }
    }
}

@Composable
private fun ColorPickerDialog(
    title: String,
    hex: String,
    onHexChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val preview = runCatching { Color(hex.fromHex()) }.getOrDefault(Color.Gray)
                Box(
                    Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(8.dp)).background(preview)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                )
                ChatoneTextField(
                    value = hex,
                    onValueChange = { v -> if (v.length <= 8) onHexChange(v.filter { it.isLetterOrDigit() }.uppercase()) },
                    label = "AARRGGBB",
                    placeholder = "FF7C4DFF",
                    modifier = Modifier.fillMaxWidth()
                )
                val palette = listOf(
                    0xFFFF6B6B, 0xFFFFD700, 0xFF4FC3F7, 0xFF9B59B6,
                    0xFF2ECC71, 0xFFFF8C00, 0xFF7B2FBE, 0xFF00BCD4,
                    0xFFE91E63, 0xFF607D8B, 0xFF8BC34A, 0xFFFF5722
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(palette.size) { i ->
                        val c = palette[i]
                        Box(
                            Modifier.size(30.dp).clip(CircleShape).background(Color(c))
                                .border(
                                    if (hex == c.toHex()) 2.dp else 0.dp,
                                    MaterialTheme.colorScheme.onSurface, CircleShape
                                )
                                .clickable { onHexChange(c.toHex()) }
                        )
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text(s.themeApply) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } }
    )
}

private fun Long.toHex(): String = toString(16).uppercase().padStart(8, '0')
private fun String.fromHex(): Long = trimStart('#').toLong(16).let {
    if (length <= 6) it or 0xFF000000L else it
}
