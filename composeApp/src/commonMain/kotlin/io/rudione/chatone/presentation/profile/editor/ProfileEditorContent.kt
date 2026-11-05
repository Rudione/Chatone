package io.rudione.chatone.presentation.profile.editor

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.profile_editor_avatar_and_banner
import chatone.composeapp.generated.resources.profile_editor_avatar_and_banner_hint
import chatone.composeapp.generated.resources.profile_editor_badge_channel
import chatone.composeapp.generated.resources.profile_editor_badge_global
import chatone.composeapp.generated.resources.profile_editor_badges_empty
import chatone.composeapp.generated.resources.profile_editor_bio
import chatone.composeapp.generated.resources.profile_editor_custom_color
import chatone.composeapp.generated.resources.profile_editor_custom_color_hint
import chatone.composeapp.generated.resources.profile_editor_display_name
import chatone.composeapp.generated.resources.profile_editor_display_name_hint
import chatone.composeapp.generated.resources.profile_editor_open_twitch_settings
import chatone.composeapp.generated.resources.profile_editor_preview_message
import chatone.composeapp.generated.resources.profile_editor_preview_title
import chatone.composeapp.generated.resources.profile_editor_section_color
import chatone.composeapp.generated.resources.profile_editor_section_profile
import chatone.composeapp.generated.resources.profile_editor_section_twitch_badge
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.profile.ChatColorPresets
import io.rudione.chatone.domain.profile.SevenTvVanity
import io.rudione.chatone.domain.profile.TwitchVanity
import io.rudione.chatone.domain.profile.VanityBadge
import io.rudione.chatone.domain.profile.VanityDraft
import io.rudione.chatone.icons.lucide.Ban
import io.rudione.chatone.icons.lucide.Check
import io.rudione.chatone.icons.lucide.ExternalLink
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.presentation.chat.ProfileDisplayName
import io.rudione.chatone.presentation.components.ChatoneTextField
import io.rudione.chatone.presentation.components.expressive.PillButton
import io.rudione.chatone.presentation.components.expressive.PillTone

@Composable
internal fun ChatPreviewLine(twitch: TwitchVanity, sevenTv: SevenTvVanity?, draft: VanityDraft, modifier: Modifier = Modifier) {
    val profileEditorPreviewTitleText = stringResource(Res.string.profile_editor_preview_title)
    val paint = sevenTv?.paints?.firstOrNull { it.id == draft.paintId }
    val badges = buildList {
        twitch.globalBadges.firstOrNull { it.key == draft.globalBadge }?.let { add(it.imageUrl) }
        twitch.channelBadges.firstOrNull { it.key == draft.channelBadge }?.let { add(it.imageUrl) }
        sevenTv?.badges?.firstOrNull { it.id == draft.sevenTvBadgeId }?.let { add(it.url2x.ifEmpty { it.url1x }) }
    }
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .semantics(mergeDescendants = true) { contentDescription = profileEditorPreviewTitleText },
        verticalAlignment = Alignment.CenterVertically
    ) {
        badges.forEach { url ->
            AsyncImage(model = url, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
        }
        ProfileDisplayName(
            displayName = draft.displayName.ifBlank { twitch.displayName },
            color = draft.colorHex?.let { "#$it" },
            paint = paint,
            style = MaterialTheme.typography.bodyMedium,
            fallbackLogin = twitch.login,
            modifier = Modifier.weight(1f, fill = false)
        )
        Text(": ${stringResource(Res.string.profile_editor_preview_message)}", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColorSection(selected: String?, onPick: (String) -> Unit) {
    var custom by remember { mutableStateOf("") }
    EditorSection(stringResource(Res.string.profile_editor_section_color)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ChatColorPresets.all.forEach { (_, hex) ->
                ColorDot(hexToColor(hex), selected.equals(hex, ignoreCase = true)) { onPick(hex) }
            }
        }
        ChatoneTextField(
            value = custom,
            onValueChange = { value ->
                custom = value.take(7)
                ChatColorPresets.normalize(custom)?.let(onPick)
            },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(Res.string.profile_editor_custom_color),
            placeholder = "#9147FF",
            hint = stringResource(Res.string.profile_editor_custom_color_hint),
            leading = {
                Box(
                    Modifier.size(18.dp).clip(CircleShape)
                        .background(selected?.let(::hexToColor) ?: MaterialTheme.colorScheme.surfaceContainerHighest)
                )
            }
        )
    }
}

@Composable
private fun ColorDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1.12f else 1f, spring(dampingRatio = 0.45f), label = "colorDot")
    val ring by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
        tween(180),
        label = "colorRing"
    )
    Box(
        Modifier.size(38.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .border(2.dp, ring, CircleShape).padding(4.dp).clip(CircleShape).background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = selected, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            Icon(Lucide.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TwitchBadgeSection(twitch: TwitchVanity, draft: VanityDraft, onGlobal: (String?) -> Unit, onChannel: (String) -> Unit) {
    EditorSection(stringResource(Res.string.profile_editor_section_twitch_badge)) {
        SubTitle(stringResource(Res.string.profile_editor_badge_global))
        if (twitch.globalBadges.isEmpty()) {
            Hint(stringResource(Res.string.profile_editor_badges_empty))
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NoneTile(draft.globalBadge == null) { onGlobal(null) }
                twitch.globalBadges.forEach { badge -> BadgeTile(badge, draft.globalBadge == badge.key) { onGlobal(badge.key) } }
            }
        }
        val channelName = twitch.channelName
        if (channelName != null && twitch.channelBadges.isNotEmpty()) {
            SubTitle(stringResource(Res.string.profile_editor_badge_channel, channelName))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                twitch.channelBadges.forEach { badge -> BadgeTile(badge, draft.channelBadge == badge.key) { onChannel(badge.key) } }
            }
        }
    }
}

@Composable
private fun BadgeTile(badge: VanityBadge, selected: Boolean, onClick: () -> Unit) {
    SelectableTile(selected, onClick) {
        AsyncImage(model = badge.imageUrl, contentDescription = badge.title, modifier = Modifier.size(28.dp))
    }
}

@Composable
internal fun NoneTile(selected: Boolean, onClick: () -> Unit) {
    SelectableTile(selected, onClick) {
        Icon(Lucide.Ban, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun SelectableTile(selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHighest,
        tween(180),
        label = "tileContainer"
    )
    val border by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        tween(180),
        label = "tileBorder"
    )
    val scale by animateFloatAsState(if (selected) 1.06f else 1f, spring(dampingRatio = 0.5f), label = "tileScale")
    Box(
        Modifier.size(52.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp)).background(container)
            .border(2.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
internal fun ProfileFieldsSection(twitch: TwitchVanity, draft: VanityDraft, onName: (String) -> Unit, onBio: (String) -> Unit, onOpenSettings: () -> Unit) {
    EditorSection(stringResource(Res.string.profile_editor_section_profile)) {
        ChatoneTextField(
            value = draft.displayName,
            onValueChange = onName,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(Res.string.profile_editor_display_name),
            hint = stringResource(Res.string.profile_editor_display_name_hint),
            isError = !draft.displayName.equals(twitch.login, ignoreCase = true)
        )
        ChatoneTextField(
            value = draft.bio,
            onValueChange = onBio,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(Res.string.profile_editor_bio),
            hint = "${draft.bio.length}/300",
            singleLine = false,
            minLines = 3
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.profile_editor_avatar_and_banner), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Hint(stringResource(Res.string.profile_editor_avatar_and_banner_hint))
            }
            PillButton(text = stringResource(Res.string.profile_editor_open_twitch_settings), onClick = onOpenSettings, icon = Lucide.ExternalLink, tone = PillTone.Tonal, height = 40.dp)
        }
    }
}

@Composable
internal fun EditorSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
internal fun SubTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

internal fun hexToColor(hex: String): Color =
    hex.removePrefix("#").toLongOrNull(16)?.let { Color(0xFF000000 or it) } ?: Color.Gray
