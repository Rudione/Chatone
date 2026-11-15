package io.rudione.chatone.presentation.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.streamer_mode_hide_data
import chatone.composeapp.generated.resources.ui_close
import chatone.composeapp.generated.resources.upload_history
import chatone.composeapp.generated.resources.upload_history_clear
import chatone.composeapp.generated.resources.upload_history_empty
import chatone.composeapp.generated.resources.upload_history_hint
import chatone.composeapp.generated.resources.upload_preset_default
import chatone.composeapp.generated.resources.upload_preset_name
import chatone.composeapp.generated.resources.upload_preset_remove
import chatone.composeapp.generated.resources.upload_preset_save
import chatone.composeapp.generated.resources.upload_presets
import chatone.composeapp.generated.resources.uploader_copy_link
import io.rudione.chatone.data.remote.ImageUploaderClient
import io.rudione.chatone.data.repository.UploadHistoryRepository
import io.rudione.chatone.data.repository.UploaderPresetsRepository
import io.rudione.chatone.domain.model.ImageUploaderConfig
import io.rudione.chatone.icons.lucide.Check
import io.rudione.chatone.icons.lucide.Copy
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.components.ChatoneButton
import io.rudione.chatone.presentation.components.ChatoneButtonTone
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.CompactOutlinedField
import io.rudione.chatone.presentation.components.LocalStreamerPrivacy
import io.rudione.chatone.util.platform.setText
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
internal fun UploadLibraryControls(config: ImageUploaderConfig, update: (ImageUploaderConfig) -> Unit) {
    val history = koinInject<UploadHistoryRepository>()
    val entries by history.entries.collectAsState()
    var servicesOpen by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChatoneButton(
            text = stringResource(Res.string.upload_presets),
            onClick = { servicesOpen = true },
            tone = ChatoneButtonTone.Neutral,
            modifier = Modifier.weight(1f)
        )
        ChatoneButton(
            text = "${stringResource(Res.string.upload_history)} · ${entries.size}",
            onClick = { historyOpen = true },
            tone = ChatoneButtonTone.Neutral,
            modifier = Modifier.weight(1f)
        )
    }
    if (servicesOpen) UploadServicesDialog(config, update) { servicesOpen = false }
    if (historyOpen) UploadHistoryDialog { historyOpen = false }
}

@Composable
private fun UploadServicesDialog(config: ImageUploaderConfig, update: (ImageUploaderConfig) -> Unit, onDismiss: () -> Unit) {
    val repository = koinInject<UploaderPresetsRepository>()
    val presets by repository.presets.collectAsState()
    var name by remember { mutableStateOf(ImageUploaderClient.destinationHost(config.requestUrl).orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.upload_presets)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = {
                    update(ImageUploaderConfig(enabled = config.enabled, askOnUpload = config.askOnUpload))
                    onDismiss()
                }) { Text("${stringResource(Res.string.upload_preset_default)} · p.ftk.li") }
                LazyColumn(Modifier.heightIn(max = 220.dp)) {
                    items(presets, key = { it.id }) { preset ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                update(preset.config.copy(enabled = config.enabled, askOnUpload = config.askOnUpload))
                                onDismiss()
                            }.padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(preset.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    ImageUploaderClient.destinationHost(preset.config.requestUrl).orEmpty(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            ChatoneIconButton({ repository.remove(preset.id) }, Modifier.size(32.dp)) {
                                Icon(Lucide.X, stringResource(Res.string.upload_preset_remove), Modifier.size(15.dp))
                            }
                        }
                    }
                }
                CompactOutlinedField(name, { name = it }, label = stringResource(Res.string.upload_preset_name), modifier = Modifier.fillMaxWidth())
                ChatoneButton(
                    text = stringResource(Res.string.upload_preset_save),
                    onClick = { repository.save(name, config) },
                    enabled = name.isNotBlank() && config.requestUrl.isNotBlank() && config.formField.isNotBlank() &&
                        (presets.size < UploaderPresetsRepository.MAX_PRESETS || presets.any { it.name == name.trim() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.ui_close)) } }
    )
}

@Composable
private fun UploadHistoryDialog(onDismiss: () -> Unit) {
    val repository = koinInject<UploadHistoryRepository>()
    val entries by repository.entries.collectAsState()
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val hidden = LocalStreamerPrivacy.current.tokensHidden
    var copied by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.upload_history)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(Res.string.upload_history_hint), style = MaterialTheme.typography.bodySmall)
                when {
                    hidden -> Text(stringResource(Res.string.streamer_mode_hide_data))
                    entries.isEmpty() -> Text(stringResource(Res.string.upload_history_empty))
                    else -> LazyColumn(Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(entries, key = { it.link }) { entry ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(entry.fileName, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(entry.link, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                ChatoneIconButton(onClick = {
                                    scope.launch { clipboard.setText(entry.link); copied = entry.link }
                                }) {
                                    Icon(if (copied == entry.link) Lucide.Check else Lucide.Copy, stringResource(Res.string.uploader_copy_link), Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.ui_close)) } },
        dismissButton = {
            if (entries.isNotEmpty()) TextButton(onClick = { scope.launch { repository.clear() } }) {
                Text(stringResource(Res.string.upload_history_clear))
            }
        }
    )
}
