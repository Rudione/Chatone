package io.rudione.chatone.presentation.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import io.rudione.chatone.presentation.components.CompactOutlinedField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.remote.ImageUploaderClient
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun ImageUploaderSection(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    uploaderClient: ImageUploaderClient = koinInject()
) {
    val s = LocalStrings.current
    val config = state.imageUploader
    val scope = rememberCoroutineScope()
    var statusText by remember { mutableStateOf<String?>(null) }
    var statusOk by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }

    SettingsCard(title = s.uploaderTitle) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                s.uploaderDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    s.uploaderEnable,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                io.rudione.chatone.presentation.components.ChatoneSwitch(
                    checked = config.enabled,
                    onCheckedChange = {
                        onEvent(SettingsEvent.OnImageUploaderChanged(config.copy(enabled = it)))
                    }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    s.uploaderAskOnUpload,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                io.rudione.chatone.presentation.components.ChatoneSwitch(
                    checked = config.askOnUpload,
                    enabled = config.enabled,
                    onCheckedChange = {
                        onEvent(SettingsEvent.OnImageUploaderChanged(config.copy(askOnUpload = it)))
                    }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactOutlinedField(
                    value = config.requestUrl,
                    onValueChange = {
                        onEvent(SettingsEvent.OnImageUploaderChanged(config.copy(requestUrl = it)))
                    },
                    label = s.uploaderRequestUrl,
                    placeholder = "https://example.com/upload",
                    enabled = config.enabled,
                    modifier = Modifier.weight(2f)
                )
                CompactOutlinedField(
                    value = config.formField,
                    onValueChange = {
                        onEvent(SettingsEvent.OnImageUploaderChanged(config.copy(formField = it)))
                    },
                    label = s.uploaderFormField,
                    placeholder = "image",
                    enabled = config.enabled,
                    modifier = Modifier.weight(1f)
                )
            }

            CompactOutlinedField(
                value = config.extraHeaders,
                onValueChange = {
                    onEvent(SettingsEvent.OnImageUploaderChanged(config.copy(extraHeaders = it)))
                },
                label = s.uploaderExtraHeaders,
                placeholder = s.uploaderExtraHeadersHint,
                enabled = config.enabled,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactOutlinedField(
                    value = config.linkFormat,
                    onValueChange = {
                        onEvent(SettingsEvent.OnImageUploaderChanged(config.copy(linkFormat = it)))
                    },
                    label = s.uploaderImageLink,
                    placeholder = "{url}",
                    enabled = config.enabled,
                    modifier = Modifier.weight(1f)
                )
                CompactOutlinedField(
                    value = config.deletionLinkFormat,
                    onValueChange = {
                        onEvent(SettingsEvent.OnImageUploaderChanged(config.copy(deletionLinkFormat = it)))
                    },
                    label = s.uploaderDeletionLink,
                    enabled = config.enabled,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    enabled = config.requestUrl.isNotBlank() && !checking,
                    onClick = {
                        checking = true
                        statusText = null
                        scope.launch {
                            val status = uploaderClient.checkStatus(config)
                            statusOk = status.reachable
                            statusText = if (status.reachable) {
                                s.uploaderStatusOk
                                    .replace("{0}", status.httpCode?.toString() ?: "?")
                                    .replace("{1}", status.latencyMs?.toString() ?: "?")
                            } else {
                                s.uploaderStatusFail.replace("{0}", status.error ?: "?")
                            }
                            checking = false
                        }
                    }
                ) {
                    if (checking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(s.uploaderStatusChecking)
                    } else {
                        Text(s.uploaderCheckStatus)
                    }
                }
                statusText?.let { text ->
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (statusOk) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
        }
    }
}
