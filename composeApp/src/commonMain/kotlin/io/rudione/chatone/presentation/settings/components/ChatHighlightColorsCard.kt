package io.rudione.chatone.presentation.settings.components

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.cancel
import chatone.composeapp.generated.resources.chat_rule_type_caps
import chatone.composeapp.generated.resources.chat_rule_type_consecutive_numbers
import chatone.composeapp.generated.resources.chat_rule_type_duplicate
import chatone.composeapp.generated.resources.chat_rule_type_emotes
import chatone.composeapp.generated.resources.chat_rule_type_first_message_greeting
import chatone.composeapp.generated.resources.chat_rule_type_length
import chatone.composeapp.generated.resources.chat_rule_type_links
import chatone.composeapp.generated.resources.chat_rule_type_new_user
import chatone.composeapp.generated.resources.chat_rule_type_raid_welcome
import chatone.composeapp.generated.resources.chat_rule_type_spam
import chatone.composeapp.generated.resources.chat_rule_type_stream_offline
import chatone.composeapp.generated.resources.chat_rule_type_stream_online
import chatone.composeapp.generated.resources.color_highlight_bg
import chatone.composeapp.generated.resources.color_mention_bg
import chatone.composeapp.generated.resources.color_mod_delete
import chatone.composeapp.generated.resources.color_own_message_bg
import chatone.composeapp.generated.resources.colors_group_automod
import chatone.composeapp.generated.resources.colors_group_automod_hint
import chatone.composeapp.generated.resources.colors_group_highlights
import chatone.composeapp.generated.resources.colors_group_highlights_hint
import chatone.composeapp.generated.resources.colors_group_moderation
import chatone.composeapp.generated.resources.colors_group_moderation_hint
import chatone.composeapp.generated.resources.colors_reset_group
import chatone.composeapp.generated.resources.highlight_rule_first_message
import chatone.composeapp.generated.resources.highlight_rule_mention_accent
import chatone.composeapp.generated.resources.highlight_rule_search_match
import chatone.composeapp.generated.resources.profile_ban
import chatone.composeapp.generated.resources.profile_timeout
import chatone.composeapp.generated.resources.profile_unban
import chatone.composeapp.generated.resources.theme_apply
import org.jetbrains.compose.resources.stringResource
import io.rudione.chatone.presentation.components.FitText
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.delay
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.ChatoneColorTokens
import io.rudione.chatone.presentation.components.ChatoneFieldLabel
import io.rudione.chatone.presentation.components.ChatoneTextField

private class ColorEntry(
    val label: String,
    val get: (ChatoneColorTokens) -> Long,
    val set: (ChatoneColorTokens, Long) -> ChatoneColorTokens,
)

enum class ColorTokenGroup { HIGHLIGHTS, MODERATION, AUTOMOD }

private const val GROUP_FOCUS_DELAY_MS = 250L
private const val GROUP_FOCUS_FADE_MS = 1_800

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorTokensEditor(
    tokens: ChatoneColorTokens,
    onChange: (ChatoneColorTokens) -> Unit,
    highlightGroup: ColorTokenGroup? = null,
) {
    var pickerEntry by remember { mutableStateOf<ColorEntry?>(null) }
    var pickerHex by remember { mutableStateOf("") }

    val highlights = listOf(
        ColorEntry(stringResource(Res.string.color_mention_bg), { it.mentionBg }) { t, v -> t.copy(mentionBg = v) },
        ColorEntry(stringResource(Res.string.highlight_rule_mention_accent), { it.mentionAccent }) { t, v -> t.copy(mentionAccent = v) },
        ColorEntry(stringResource(Res.string.color_highlight_bg), { it.highlightBg }) { t, v -> t.copy(highlightBg = v) },
        ColorEntry(stringResource(Res.string.highlight_rule_first_message), { it.firstMessageColor }) { t, v -> t.copy(firstMessageColor = v) },
        ColorEntry(stringResource(Res.string.highlight_rule_search_match), { it.searchMatchBg }) { t, v -> t.copy(searchMatchBg = v) },
        ColorEntry(stringResource(Res.string.color_own_message_bg), { it.ownMessageBg }) { t, v -> t.copy(ownMessageBg = v) },
    )
    val moderation = listOf(
        ColorEntry(stringResource(Res.string.color_mod_delete), { it.modDelete }) { t, v -> t.copy(modDelete = v) },
        ColorEntry(stringResource(Res.string.profile_timeout), { it.modTimeout }) { t, v -> t.copy(modTimeout = v) },
        ColorEntry(stringResource(Res.string.profile_ban), { it.modBan }) { t, v -> t.copy(modBan = v) },
        ColorEntry(stringResource(Res.string.profile_unban), { it.modUnban }) { t, v -> t.copy(modUnban = v) },
    )
    val automod = listOf(
        ColorEntry(stringResource(Res.string.chat_rule_type_spam), { it.automodSpam }) { t, v -> t.copy(automodSpam = v) },
        ColorEntry(stringResource(Res.string.chat_rule_type_caps), { it.automodCaps }) { t, v -> t.copy(automodCaps = v) },
        ColorEntry(stringResource(Res.string.chat_rule_type_links), { it.automodLinks }) { t, v -> t.copy(automodLinks = v) },
        ColorEntry(stringResource(Res.string.chat_rule_type_emotes), { it.automodEmotes }) { t, v -> t.copy(automodEmotes = v) },
        ColorEntry(stringResource(Res.string.chat_rule_type_new_user), { it.automodNewAccount }) { t, v -> t.copy(automodNewAccount = v) },
        ColorEntry(stringResource(Res.string.chat_rule_type_duplicate), { it.automodDuplicate }) { t, v -> t.copy(automodDuplicate = v) },
        ColorEntry(stringResource(Res.string.chat_rule_type_consecutive_numbers), { it.automodNumbers }) { t, v -> t.copy(automodNumbers = v) },
        ColorEntry(stringResource(Res.string.chat_rule_type_length), { it.automodLength }) { t, v -> t.copy(automodLength = v) },
        ColorEntry(stringResource(Res.string.chat_rule_type_stream_online), { it.automodStreamOnline }) { t, v -> t.copy(automodStreamOnline = v) },
        ColorEntry(stringResource(Res.string.chat_rule_type_stream_offline), { it.automodStreamOffline }) { t, v -> t.copy(automodStreamOffline = v) },
        ColorEntry(stringResource(Res.string.chat_rule_type_first_message_greeting), { it.automodFirstGreeting }) { t, v -> t.copy(automodFirstGreeting = v) },
        ColorEntry(stringResource(Res.string.chat_rule_type_raid_welcome), { it.automodRaid }) { t, v -> t.copy(automodRaid = v) },
    )

    fun openPicker(entry: ColorEntry) {
        pickerEntry = entry
        pickerHex = entry.get(tokens).toHex()
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ColorGroup(
            stringResource(Res.string.colors_group_highlights), stringResource(Res.string.colors_group_highlights_hint), highlights, tokens, ::openPicker,
            highlighted = highlightGroup == ColorTokenGroup.HIGHLIGHTS
        ) {
            onChange(highlights.fold(tokens) { acc, e -> e.set(acc, e.get(ChatoneColorTokens())) })
        }
        ColorGroup(
            stringResource(Res.string.colors_group_moderation), stringResource(Res.string.colors_group_moderation_hint), moderation, tokens, ::openPicker,
            highlighted = highlightGroup == ColorTokenGroup.MODERATION
        ) {
            onChange(moderation.fold(tokens) { acc, e -> e.set(acc, e.get(ChatoneColorTokens())) })
        }
        ColorGroup(
            stringResource(Res.string.colors_group_automod), stringResource(Res.string.colors_group_automod_hint), automod, tokens, ::openPicker,
            highlighted = highlightGroup == ColorTokenGroup.AUTOMOD
        ) {
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun ColorGroup(
    title: String,
    hint: String,
    entries: List<ColorEntry>,
    tokens: ChatoneColorTokens,
    onPick: (ColorEntry) -> Unit,
    highlighted: Boolean = false,
    onResetGroup: () -> Unit,
) {
    val requester = remember { BringIntoViewRequester() }
    val pulse = remember { Animatable(0f) }
    val accent = MaterialTheme.colorScheme.primary
    LaunchedEffect(highlighted) {
        if (!highlighted) return@LaunchedEffect
        delay(GROUP_FOCUS_DELAY_MS)
        requester.bringIntoView()
        pulse.snapTo(1f)
        pulse.animateTo(0f, tween(GROUP_FOCUS_FADE_MS))
    }
    Column(
        modifier = Modifier
            .bringIntoViewRequester(requester)
            .drawBehind {
                val amount = pulse.value
                if (amount > 0f) {
                    val corner = CornerRadius(12.dp.toPx())
                    drawRoundRect(accent.copy(alpha = 0.14f * amount), cornerRadius = corner)
                    drawRoundRect(accent.copy(alpha = 0.7f * amount), cornerRadius = corner, style = Stroke(1.5.dp.toPx()))
                }
            },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                ChatoneFieldLabel(text = title, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(2.dp))
                ChatoneFieldLabel(text = hint, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onResetGroup, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text(stringResource(Res.string.colors_reset_group), style = MaterialTheme.typography.labelSmall)
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
            FitText(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                minFontSize = 10.sp
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
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HighlightPalette.forEach { c ->
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
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(Res.string.theme_apply)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } }
    )
}

private val HighlightPalette = listOf(
    0xFFFF6B6B, 0xFFFFD700, 0xFF4FC3F7, 0xFF9B59B6,
    0xFF2ECC71, 0xFFFF8C00, 0xFF7B2FBE, 0xFF00BCD4,
    0xFFE91E63, 0xFF607D8B, 0xFF8BC34A, 0xFFFF5722
)

private fun Long.toHex(): String = toString(16).uppercase().padStart(8, '0')
private fun String.fromHex(): Long = trimStart('#').toLong(16).let {
    if (length <= 6) it or 0xFF000000L else it
}
