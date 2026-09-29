package io.rudione.chatone.presentation.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import io.rudione.chatone.presentation.theme.i18n.BrowseStrings
import kotlinx.coroutines.launch

private const val MAX_TAG_LENGTH = 25

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StreamFilterSheet(
    initial: StreamFilter,
    tagSuggestions: List<String>,
    strings: BrowseStrings,
    onApply: (StreamFilter) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var draft by remember { mutableStateOf(initial) }
    var customTag by remember { mutableStateOf("") }

    val tags = remember(tagSuggestions, draft.tag) {
        (listOfNotNull(draft.tag) + tagSuggestions).distinctBy { it.lowercase() }
    }

    fun apply() {
        val typed = customTag.trim().take(MAX_TAG_LENGTH).takeIf { it.isNotEmpty() }
        val result = if (typed != null) draft.copy(tag = typed) else draft
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onApply(result)
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    strings.filters,
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
                ) { Text(strings.resetFilters) }
            }

            FilterSection(strings.sortLabel) {
                StreamSort.entries.forEach { sort ->
                    ChatoneChip(
                        label = sortLabel(sort, strings),
                        selected = draft.sort == sort,
                        showCheckWhenSelected = true,
                        onClick = { draft = draft.copy(sort = sort) }
                    )
                }
            }

            FilterSection(strings.languageLabel) {
                ChatoneChip(
                    label = strings.allLanguages,
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
                label = strings.tagsLabel,
                footer = {
                    OutlinedTextField(
                        value = customTag,
                        onValueChange = { customTag = it.take(MAX_TAG_LENGTH) },
                        placeholder = { Text(strings.customTagHint) },
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

            FilterSection(strings.minViewersLabel) {
                StreamFilter.MIN_VIEWER_STEPS.forEach { step ->
                    ChatoneChip(
                        label = if (step == 0) strings.anyViewers else "≥ " + BrowseFormatting.compact(step, strings),
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
                        strings.hideMature,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        strings.hideMatureDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = draft.hideMature, onCheckedChange = { draft = draft.copy(hideMature = it) })
            }

            Button(
                onClick = ::apply,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(strings.applyFilters, fontWeight = FontWeight.SemiBold) }
        }
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
