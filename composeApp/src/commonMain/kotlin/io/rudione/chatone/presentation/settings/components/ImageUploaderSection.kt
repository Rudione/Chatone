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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import io.rudione.chatone.domain.model.ImageUploaderConfig
import io.rudione.chatone.domain.model.UploaderFormField
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Plus
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.components.ChatoneButton
import io.rudione.chatone.presentation.components.ChatoneButtonTone
import io.rudione.chatone.presentation.components.ChatoneFieldLabel
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.CompactOutlinedField
import io.rudione.chatone.presentation.components.SecretRevealButton
import io.rudione.chatone.presentation.components.expressive.HelpHint
import io.rudione.chatone.presentation.components.rememberSecretVisibility
import io.rudione.chatone.presentation.components.rows.RowDivider
import io.rudione.chatone.presentation.components.rows.SwitchRow
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.settingsAnchor
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
internal fun UploadsContent(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    val s = LocalStrings.current
    val hub = s.settingsHub
    val config = state.imageUploader
    val update: (ImageUploaderConfig) -> Unit = { onEvent(SettingsEvent.OnImageUploaderChanged(it)) }
    SettingsGroup(hub.uploaderBehaviourGroup) {
        SwitchRow(s.uploaderEnable, s.uploaderDesc, config.enabled) { update(config.copy(enabled = it)) }
        RowDivider()
        SwitchRow(s.uploaderAskOnUpload, null, config.askOnUpload) { update(config.copy(askOnUpload = it)) }
    }
    UploaderServiceGroup(config, update)
}

@Composable
private fun UploaderServiceGroup(
    config: ImageUploaderConfig,
    update: (ImageUploaderConfig) -> Unit,
    uploaderClient: ImageUploaderClient = koinInject()
) {
    val s = LocalStrings.current
    val hub = s.settingsHub
    val scope = rememberCoroutineScope()
    var statusText by remember { mutableStateOf<String?>(null) }
    var statusOk by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    SettingsGroup(hub.uploaderServiceGroup) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val requestUrlSecret = rememberSecretVisibility()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactOutlinedField(
                    value = config.requestUrl,
                    onValueChange = { update(config.copy(requestUrl = it)) },
                    label = s.uploaderRequestUrl,
                    placeholder = ImageUploaderConfig.DEFAULT_REQUEST_URL,
                    modifier = Modifier.weight(2f).settingsAnchor(s.uploaderRequestUrl),
                    visualTransformation = requestUrlSecret.transformation,
                    trailing = if (config.requestUrl.isNotEmpty()) {
                        { SecretRevealButton(requestUrlSecret, Modifier.size(22.dp)) }
                    } else null
                )
                CompactOutlinedField(
                    value = config.formField,
                    onValueChange = { update(config.copy(formField = it)) },
                    label = s.uploaderFormField,
                    placeholder = ImageUploaderConfig.DEFAULT_FORM_FIELD,
                    modifier = Modifier.weight(1f).settingsAnchor(s.uploaderFormField)
                )
            }
            val headersSecret = rememberSecretVisibility()
            CompactOutlinedField(
                value = config.extraHeaders,
                onValueChange = { update(config.copy(extraHeaders = it)) },
                label = s.uploaderExtraHeaders,
                placeholder = s.uploaderExtraHeadersHint,
                modifier = Modifier.fillMaxWidth().settingsAnchor(s.uploaderExtraHeaders),
                visualTransformation = headersSecret.transformation,
                trailing = if (config.extraHeaders.isNotEmpty()) {
                    { SecretRevealButton(headersSecret, Modifier.size(22.dp)) }
                } else null
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactOutlinedField(
                    value = config.linkFormat,
                    onValueChange = { update(config.copy(linkFormat = it)) },
                    label = s.uploaderImageLink,
                    placeholder = ImageUploaderConfig.DEFAULT_LINK_FORMAT,
                    modifier = Modifier.weight(1f).settingsAnchor(s.uploaderImageLink)
                )
                CompactOutlinedField(
                    value = config.deletionLinkFormat,
                    onValueChange = { update(config.copy(deletionLinkFormat = it)) },
                    label = s.uploaderDeletionLink,
                    modifier = Modifier.weight(1f).settingsAnchor(s.uploaderDeletionLink)
                )
            }
            Text(
                hub.uploaderLinkHelp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            UploaderFormFieldsEditor(config, update)
            Row(
                modifier = Modifier.settingsAnchor(s.uploaderCheckStatus),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChatoneButton(
                    text = if (checking) s.uploaderStatusChecking else s.uploaderCheckStatus,
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
                    },
                    enabled = config.requestUrl.isNotBlank() && !checking,
                    tone = ChatoneButtonTone.Neutral
                )
                if (checking) {
                    Spacer(Modifier.width(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                }
                statusText?.let { text ->
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (statusOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun UploaderFormFieldsEditor(config: ImageUploaderConfig, update: (ImageUploaderConfig) -> Unit) {
    val hub = LocalStrings.current.settingsHub
    val fields = config.formFields
    fun replace(index: Int, field: UploaderFormField) {
        update(config.copy(formFields = fields.toMutableList().also { it[index] = field }))
    }
    Column(
        modifier = Modifier.settingsAnchor(hub.uploaderFieldsTitle),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChatoneFieldLabel(hub.uploaderFieldsTitle, modifier = Modifier.weight(1f, fill = false))
            HelpHint(title = hub.uploaderFieldsTitle, text = hub.uploaderFieldsHelp, size = 24.dp)
        }
        fields.forEachIndexed { index, field ->
            val valueSecret = rememberSecretVisibility()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CompactOutlinedField(
                    value = field.name,
                    onValueChange = { replace(index, field.copy(name = it)) },
                    label = hub.uploaderFieldName,
                    placeholder = "key",
                    modifier = Modifier.weight(1f)
                )
                CompactOutlinedField(
                    value = field.value,
                    onValueChange = { replace(index, field.copy(value = it)) },
                    label = hub.uploaderFieldValue,
                    modifier = Modifier.weight(1.4f),
                    visualTransformation = valueSecret.transformation,
                    trailing = if (field.value.isNotEmpty()) {
                        { SecretRevealButton(valueSecret, Modifier.size(22.dp)) }
                    } else null
                )
                ChatoneIconButton(
                    onClick = { update(config.copy(formFields = fields.filterIndexed { i, _ -> i != index })) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Lucide.X,
                        contentDescription = hub.uploaderRemoveField,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        if (fields.size < MAX_FORM_FIELDS) {
            ChatoneButton(
                text = hub.uploaderAddField,
                onClick = { update(config.copy(formFields = fields + UploaderFormField())) },
                tone = ChatoneButtonTone.Neutral,
                icon = Lucide.Plus
            )
        }
    }
}

private const val MAX_FORM_FIELDS = 8
