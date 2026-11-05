package io.rudione.chatone.presentation.profile.editor

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.cancel
import chatone.composeapp.generated.resources.profile_editor_accent_hint
import chatone.composeapp.generated.resources.profile_editor_category
import chatone.composeapp.generated.resources.profile_editor_category_search
import chatone.composeapp.generated.resources.profile_editor_language
import chatone.composeapp.generated.resources.profile_editor_link_add
import chatone.composeapp.generated.resources.profile_editor_link_title
import chatone.composeapp.generated.resources.profile_editor_link_url
import chatone.composeapp.generated.resources.profile_editor_nothing_changed
import chatone.composeapp.generated.resources.profile_editor_review_hint
import chatone.composeapp.generated.resources.profile_editor_review_save
import chatone.composeapp.generated.resources.profile_editor_review_title
import chatone.composeapp.generated.resources.profile_editor_section_accent
import chatone.composeapp.generated.resources.profile_editor_section_links
import chatone.composeapp.generated.resources.profile_editor_section_stream
import chatone.composeapp.generated.resources.profile_editor_stream_title
import chatone.composeapp.generated.resources.profile_editor_tag_add
import chatone.composeapp.generated.resources.profile_editor_tags
import org.jetbrains.compose.resources.stringResource
import io.rudione.chatone.presentation.profile.editor.vanityFieldLabel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.profile.ChatColorPresets
import io.rudione.chatone.domain.profile.SocialLink
import io.rudione.chatone.domain.profile.StreamCategory
import io.rudione.chatone.domain.profile.StreamSettings
import io.rudione.chatone.domain.profile.VanityChange
import io.rudione.chatone.icons.lucide.ArrowRight
import io.rudione.chatone.icons.lucide.Check
import io.rudione.chatone.icons.lucide.Link
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Plus
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.components.ChatoneTextField
import io.rudione.chatone.presentation.components.expressive.PillButton
import io.rudione.chatone.presentation.components.expressive.PillTone

private val LANGUAGES = listOf("ru", "en", "uk", "es", "de", "fr", "pt", "ko", "ja", "other")

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StreamSection(
    stream: StreamSettings,
    results: List<StreamCategory>,
    onTitle: (String) -> Unit,
    onSearch: (String) -> Unit,
    onPick: (StreamCategory) -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onLanguage: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    EditorSection(stringResource(Res.string.profile_editor_section_stream)) {
        ChatoneTextField(value = stream.title, onValueChange = onTitle, modifier = Modifier.fillMaxWidth(), label = stringResource(Res.string.profile_editor_stream_title), singleLine = false, maxLines = 3)
        SubTitle(stringResource(Res.string.profile_editor_category))
        stream.category?.let { category -> CategoryRow(category, selected = true, onClick = {}) }
        ChatoneTextField(
            value = query,
            onValueChange = {
                query = it
                onSearch(it)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(Res.string.profile_editor_category_search)
        )
        AnimatedVisibility(visible = results.isNotEmpty(), enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                results.forEach { category ->
                    CategoryRow(category, selected = category.id == stream.category?.id) {
                        query = ""
                        onPick(category)
                    }
                }
            }
        }
        SubTitle(stringResource(Res.string.profile_editor_tags))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            stream.tags.forEach { value -> RemovableChip(value) { onRemoveTag(value) } }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChatoneTextField(value = tag, onValueChange = { tag = it.take(25) }, modifier = Modifier.weight(1f), placeholder = stringResource(Res.string.profile_editor_tag_add))
            Spacer(Modifier.width(8.dp))
            RoundAdd(enabled = tag.isNotBlank()) {
                onAddTag(tag)
                tag = ""
            }
        }
        SubTitle(stringResource(Res.string.profile_editor_language))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LANGUAGES.forEach { code ->
                SelectChip(code.uppercase(), selected = stream.language.equals(code, ignoreCase = true)) { onLanguage(code) }
            }
        }
    }
}

@Composable
internal fun LinksSection(links: List<SocialLink>, onAdd: (String, String) -> Unit, onRemove: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    EditorSection(stringResource(Res.string.profile_editor_section_links)) {
        links.forEach { link ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Lucide.Link, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(link.title.ifBlank { link.url }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(link.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(Modifier.size(32.dp).clip(CircleShape).clickable { onRemove(link.url) }, contentAlignment = Alignment.Center) {
                    Icon(Lucide.X, null, modifier = Modifier.size(16.dp))
                }
            }
        }
        if (links.size < MAX_LINKS) {
            ChatoneTextField(value = title, onValueChange = { title = it.take(25) }, modifier = Modifier.fillMaxWidth(), label = stringResource(Res.string.profile_editor_link_title))
            ChatoneTextField(value = url, onValueChange = { url = it.take(300) }, modifier = Modifier.fillMaxWidth(), label = stringResource(Res.string.profile_editor_link_url), placeholder = "https://")
            PillButton(
                text = stringResource(Res.string.profile_editor_link_add),
                onClick = {
                    onAdd(title, url)
                    title = ""
                    url = ""
                },
                icon = Lucide.Plus,
                tone = PillTone.Tonal,
                enabled = url.startsWith("http"),
                height = 42.dp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AccentSection(selected: String?, onPick: (String) -> Unit) {
    EditorSection(stringResource(Res.string.profile_editor_section_accent)) {
        Hint(stringResource(Res.string.profile_editor_accent_hint))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ACCENTS.forEach { hex ->
                val active = selected.equals(hex, ignoreCase = true)
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(hexToColor(hex))
                        .border(2.dp, if (active) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                        .clickable { onPick(hex) },
                    contentAlignment = Alignment.Center
                ) {
                    if (active) Icon(Lucide.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
internal fun ChangesReviewDialog(changes: List<VanityChange>, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (changes.isEmpty()) stringResource(Res.string.profile_editor_nothing_changed) else stringResource(Res.string.profile_editor_review_title)) },
        text = {
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (changes.isNotEmpty()) Hint(stringResource(Res.string.profile_editor_review_hint))
                changes.forEach { change ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(10.dp)
                    ) {
                        Text(vanityFieldLabel(change.field), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            change.before?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, textDecoration = TextDecoration.LineThrough, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            }
                            if (change.before != null && change.after != null) {
                                Icon(Lucide.ArrowRight, null, modifier = Modifier.padding(horizontal = 6.dp).size(14.dp))
                            }
                            change.after?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (changes.isNotEmpty()) TextButton(onClick = onConfirm) { Text(stringResource(Res.string.profile_editor_review_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } }
    )
}

@Composable
private fun CategoryRow(category: StreamCategory, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(width = 26.dp, height = 36.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)) {
            if (category.artUrl.isNotEmpty()) AsyncImage(model = category.artUrl, contentDescription = null, modifier = Modifier.size(width = 26.dp, height = 36.dp))
        }
        Text(category.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 10.dp).weight(1f))
        if (selected) Icon(Lucide.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun RemovableChip(label: String, onRemove: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest).clickable(onClick = onRemove)
            .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Icon(Lucide.X, null, modifier = Modifier.padding(start = 4.dp).size(14.dp))
    }
}

@Composable
private fun SelectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun RoundAdd(enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape)
            .background(if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Lucide.Plus, null, tint = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

private const val MAX_LINKS = 5
private val ACCENTS = listOf("9147FF") + ChatColorPresets.all.map { it.second } + listOf("0DD987", "00C8AF", "FF6905", "EB0400")
