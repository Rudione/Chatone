package io.rudione.chatone.presentation.profile.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.lab_editor_account
import chatone.composeapp.generated.resources.lab_editor_badges
import chatone.composeapp.generated.resources.lab_editor_colors
import chatone.composeapp.generated.resources.lab_editor_links
import chatone.composeapp.generated.resources.lab_editor_stream
import chatone.composeapp.generated.resources.profile_editor_section_seven_tv
import io.rudione.chatone.icons.lucide.Link
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Palette
import io.rudione.chatone.icons.lucide.Radio
import io.rudione.chatone.icons.lucide.ShieldCheck
import io.rudione.chatone.icons.lucide.Sparkles
import io.rudione.chatone.icons.lucide.User
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal val LocalCompactProfileEditor = staticCompositionLocalOf { false }

private enum class EditorArea(val label: StringResource, val icon: ImageVector) {
    Account(Res.string.lab_editor_account, Lucide.User),
    Colors(Res.string.lab_editor_colors, Lucide.Palette),
    Badges(Res.string.lab_editor_badges, Lucide.ShieldCheck),
    SevenTv(Res.string.profile_editor_section_seven_tv, Lucide.Sparkles),
    Links(Res.string.lab_editor_links, Lucide.Link),
    Stream(Res.string.lab_editor_stream, Lucide.Radio)
}

@Composable
internal fun CompactProfileEditorContent(
    state: ProfileEditorState,
    onEvent: (ProfileEditorEvent) -> Unit,
    onOpenSettings: () -> Unit
) {
    val twitch = state.twitch ?: return
    val draft = state.draft ?: return
    var areaName by rememberSaveable(twitch.userId) { mutableStateOf(EditorArea.Account.name) }
    val areas = EditorArea.entries.filter { it != EditorArea.Stream || draft.stream != null }
    val area = areas.firstOrNull { it.name == areaName } ?: EditorArea.Account
    CompositionLocalProvider(LocalCompactProfileEditor provides true) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp, 8.dp, 22.dp, 8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(14.dp, 6.dp, 14.dp, 6.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Lucide.User, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.weight(1f)) {
                    Text(draft.displayName.ifBlank { twitch.displayName }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("@${twitch.login}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = if (maxWidth >= 600.dp) 3 else 2
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    areas.chunked(columns).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { choice ->
                                EditorAreaTile(choice, choice == area, { areaName = choice.name }, Modifier.weight(1f))
                            }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            AnimatedContent(
                targetState = area,
                transitionSpec = { fadeIn(tween(180, 40)) togetherWith fadeOut(tween(120)) },
                label = "profileEditorArea"
            ) { selected ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (selected) {
                        EditorArea.Account -> ProfileFieldsSection(twitch, draft,
                            onName = { onEvent(ProfileEditorEvent.EditDisplayName(it)) },
                            onBio = { onEvent(ProfileEditorEvent.EditBio(it)) }, onOpenSettings = onOpenSettings)
                        EditorArea.Colors -> {
                            ColorSection(draft.colorHex) { onEvent(ProfileEditorEvent.PickColor(it)) }
                            AccentSection(draft.accentHex) { onEvent(ProfileEditorEvent.PickAccent(it)) }
                        }
                        EditorArea.Badges -> TwitchBadgeSection(twitch, draft,
                            onGlobal = { onEvent(ProfileEditorEvent.PickGlobalBadge(it)) },
                            onChannel = { onEvent(ProfileEditorEvent.PickChannelBadge(it)) })
                        EditorArea.SevenTv -> SevenTvVanitySection(state.sevenTv, state.sevenTvBusy, draft, twitch.displayName,
                            onConnect = { onEvent(ProfileEditorEvent.ConnectSevenTv(it)) },
                            onDisconnect = { onEvent(ProfileEditorEvent.DisconnectSevenTv) },
                            onPaint = { onEvent(ProfileEditorEvent.PickPaint(it)) },
                            onBadge = { onEvent(ProfileEditorEvent.PickSevenTvBadge(it)) })
                        EditorArea.Links -> LinksSection(draft.links,
                            onAdd = { title, url -> onEvent(ProfileEditorEvent.AddLink(title, url)) },
                            onRemove = { onEvent(ProfileEditorEvent.RemoveLink(it)) })
                        EditorArea.Stream -> draft.stream?.let { stream ->
                            StreamSection(stream, state.categoryResults,
                                onTitle = { onEvent(ProfileEditorEvent.EditStreamTitle(it)) },
                                onSearch = { onEvent(ProfileEditorEvent.SearchCategory(it)) },
                                onPick = { onEvent(ProfileEditorEvent.PickCategory(it)) },
                                onAddTag = { onEvent(ProfileEditorEvent.AddTag(it)) },
                                onRemoveTag = { onEvent(ProfileEditorEvent.RemoveTag(it)) },
                                onLanguage = { onEvent(ProfileEditorEvent.SetLanguage(it)) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorAreaTile(area: EditorArea, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val color by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        tween(180), label = "editorTileColor"
    )
    val corner by animateDpAsState(if (selected) 5.dp else 12.dp, tween(180), label = "editorTileCorner")
    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = corner, bottomEnd = 20.dp, bottomStart = corner)
    val content = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier.height(52.dp).clip(shape).background(color)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.outlineVariant else Color.Transparent, shape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(area.icon, null, Modifier.size(18.dp), tint = content)
        Text(stringResource(area.label), style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
