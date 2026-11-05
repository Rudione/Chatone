package io.rudione.chatone.presentation.browse

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.browse_all_languages
import chatone.composeapp.generated.resources.browse_any_viewers
import chatone.composeapp.generated.resources.browse_apply_filters
import chatone.composeapp.generated.resources.browse_custom_tag_hint
import chatone.composeapp.generated.resources.browse_filters
import chatone.composeapp.generated.resources.browse_hide_mature
import chatone.composeapp.generated.resources.browse_hide_mature_description
import chatone.composeapp.generated.resources.browse_language_label
import chatone.composeapp.generated.resources.browse_min_viewers_label
import chatone.composeapp.generated.resources.browse_reset_filters
import chatone.composeapp.generated.resources.browse_sort_label
import chatone.composeapp.generated.resources.browse_tags_label
import org.jetbrains.compose.resources.stringResource
import io.rudione.chatone.presentation.components.expressive.PillButton
import io.rudione.chatone.presentation.components.expressive.ExpressiveSheet
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.browse.StreamFilter
import io.rudione.chatone.domain.browse.StreamSort
import io.rudione.chatone.presentation.components.ChatoneChip

private const val MAX_TAG_LENGTH = 25

@Composable
internal fun StreamFilterSheet(
    initial: StreamFilter,
    tagSuggestions: List<String>,
    onApply: (StreamFilter) -> Unit,
    onDismiss: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var draft by remember { mutableStateOf(initial) }
    var customTag by remember { mutableStateOf("") }

    val tags = remember(tagSuggestions, draft.tag) {
        (listOfNotNull(draft.tag) + tagSuggestions).distinctBy { it.lowercase() }
    }

    fun result(): StreamFilter {
        val typed = customTag.trim().take(MAX_TAG_LENGTH).takeIf { it.isNotEmpty() }
        return if (typed != null) draft.copy(tag = typed) else draft
    }

    ExpressiveSheet(onDismissRequest = onDismiss) { sheet ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 2.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(Res.string.browse_filters),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    draft = StreamFilter()
                    customTag = ""
                },
                enabled = !draft.isDefault || customTag.isNotBlank()
            ) { Text(stringResource(Res.string.browse_reset_filters)) }
        }
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            FilterSection(stringResource(Res.string.browse_sort_label)) {
                StreamSort.entries.forEach { sort ->
                    ChatoneChip(
                        label = sortLabel(sort),
                        selected = draft.sort == sort,
                        showCheckWhenSelected = true,
                        onClick = { draft = draft.copy(sort = sort) }
                    )
                }
            }

            FilterSection(stringResource(Res.string.browse_language_label)) {
                ChatoneChip(
                    label = stringResource(Res.string.browse_all_languages),
                    selected = draft.language == null,
                    showCheckWhenSelected = true,
                    onClick = { draft = draft.copy(language = null) }
                )
                StreamFilter.LANGUAGES.forEach { language ->
                    ChatoneChip(
                        label = language,
                        selected = draft.language == language,
                        showCheckWhenSelected = true,
                        onClick = { draft = draft.copy(language = language.takeUnless { it == draft.language }) }
                    )
                }
            }

            FilterSection(
                label = stringResource(Res.string.browse_tags_label),
                footer = {
                    OutlinedTextField(
                        value = customTag,
                        onValueChange = { customTag = it.take(MAX_TAG_LENGTH) },
                        placeholder = { Text(stringResource(Res.string.browse_custom_tag_hint)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            ) {
                tags.forEach { tag ->
                    val selected = tag.equals(draft.tag, ignoreCase = true)
                    ChatoneChip(
                        label = tag,
                        selected = selected,
                        showCheckWhenSelected = true,
                        accent = MaterialTheme.colorScheme.tertiary,
                        onClick = {
                            customTag = ""
                            draft = draft.copy(tag = tag.takeUnless { selected })
                        }
                    )
                }
            }

            FilterSection(stringResource(Res.string.browse_min_viewers_label)) {
                StreamFilter.MIN_VIEWER_STEPS.forEach { step ->
                    ChatoneChip(
                        label = if (step == 0) stringResource(Res.string.browse_any_viewers) else "≥ " + BrowseFormatting.compact(step),
                        selected = draft.minViewers == step,
                        showCheckWhenSelected = true,
                        onClick = { draft = draft.copy(minViewers = step) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { draft = draft.copy(hideMature = !draft.hideMature) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.browse_hide_mature),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        stringResource(Res.string.browse_hide_mature_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = draft.hideMature, onCheckedChange = { draft = draft.copy(hideMature = it) })
            }

        }
        PillButton(
            text = stringResource(Res.string.browse_apply_filters),
            onClick = {
                val applied = result()
                sheet.close { onApply(applied) }
            },
            height = 52.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    label: String,
    footer: (@Composable () -> Unit)? = null,
    chips: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) { chips() }
        footer?.invoke()
    }
}
