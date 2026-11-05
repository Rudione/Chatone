package io.rudione.chatone.presentation.settings.components

import org.jetbrains.compose.resources.getString
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.settings_hub_uploader_add_field
import chatone.composeapp.generated.resources.settings_hub_uploader_behaviour_group
import chatone.composeapp.generated.resources.settings_hub_uploader_field_name
import chatone.composeapp.generated.resources.settings_hub_uploader_field_value
import chatone.composeapp.generated.resources.settings_hub_uploader_fields_help
import chatone.composeapp.generated.resources.settings_hub_uploader_fields_title
import chatone.composeapp.generated.resources.settings_hub_uploader_link_help
import chatone.composeapp.generated.resources.settings_hub_uploader_remove_field
import chatone.composeapp.generated.resources.settings_hub_uploader_service_group
import chatone.composeapp.generated.resources.uploader_ask_on_upload
import chatone.composeapp.generated.resources.uploader_check_status
import chatone.composeapp.generated.resources.uploader_deletion_link
import chatone.composeapp.generated.resources.uploader_desc
import chatone.composeapp.generated.resources.uploader_enable
import chatone.composeapp.generated.resources.uploader_extra_headers
import chatone.composeapp.generated.resources.uploader_extra_headers_hint
import chatone.composeapp.generated.resources.uploader_form_field
import chatone.composeapp.generated.resources.uploader_image_link
import chatone.composeapp.generated.resources.uploader_request_url
import chatone.composeapp.generated.resources.uploader_status_checking
import chatone.composeapp.generated.resources.uploader_status_fail
import chatone.composeapp.generated.resources.uploader_status_ok
import org.jetbrains.compose.resources.stringResource
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
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
internal fun UploadsContent(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    val config = state.imageUploader
    val update: (ImageUploaderConfig) -> Unit = { onEvent(SettingsEvent.OnImageUploaderChanged(it)) }
    SettingsGroup(stringResource(Res.string.settings_hub_uploader_behaviour_group)) {
        SwitchRow(stringResource(Res.string.uploader_enable), stringResource(Res.string.uploader_desc), config.enabled) { update(config.copy(enabled = it)) }
        RowDivider()
        SwitchRow(stringResource(Res.string.uploader_ask_on_upload), null, config.askOnUpload) { update(config.copy(askOnUpload = it)) }
    }
    UploaderServiceGroup(config, update)
}

@Composable
private fun UploaderServiceGroup(
    config: ImageUploaderConfig,
    update: (ImageUploaderConfig) -> Unit,
    uploaderClient: ImageUploaderClient = koinInject()
) {
    val scope = rememberCoroutineScope()
    var statusText by remember { mutableStateOf<String?>(null) }
    var statusOk by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    SettingsGroup(stringResource(Res.string.settings_hub_uploader_service_group)) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val requestUrlSecret = rememberSecretVisibility()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactOutlinedField(
                    value = config.requestUrl,
                    onValueChange = { update(config.copy(requestUrl = it)) },
                    label = stringResource(Res.string.uploader_request_url),
                    placeholder = ImageUploaderConfig.DEFAULT_REQUEST_URL,
                    modifier = Modifier.weight(2f).settingsAnchor(stringResource(Res.string.uploader_request_url)),
                    visualTransformation = requestUrlSecret.transformation,
                    trailing = if (config.requestUrl.isNotEmpty()) {
                        { SecretRevealButton(requestUrlSecret, Modifier.size(22.dp)) }
                    } else null
                )
                CompactOutlinedField(
                    value = config.formField,
                    onValueChange = { update(config.copy(formField = it)) },
                    label = stringResource(Res.string.uploader_form_field),
                    placeholder = ImageUploaderConfig.DEFAULT_FORM_FIELD,
                    modifier = Modifier.weight(1f).settingsAnchor(stringResource(Res.string.uploader_form_field))
                )
            }
            val headersSecret = rememberSecretVisibility()
            CompactOutlinedField(
                value = config.extraHeaders,
                onValueChange = { update(config.copy(extraHeaders = it)) },
                label = stringResource(Res.string.uploader_extra_headers),
                placeholder = stringResource(Res.string.uploader_extra_headers_hint),
                modifier = Modifier.fillMaxWidth().settingsAnchor(stringResource(Res.string.uploader_extra_headers)),
                visualTransformation = headersSecret.transformation,
                trailing = if (config.extraHeaders.isNotEmpty()) {
                    { SecretRevealButton(headersSecret, Modifier.size(22.dp)) }
                } else null
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactOutlinedField(
                    value = config.linkFormat,
                    onValueChange = { update(config.copy(linkFormat = it)) },
                    label = stringResource(Res.string.uploader_image_link),
                    placeholder = ImageUploaderConfig.DEFAULT_LINK_FORMAT,
                    modifier = Modifier.weight(1f).settingsAnchor(stringResource(Res.string.uploader_image_link))
                )
                CompactOutlinedField(
                    value = config.deletionLinkFormat,
                    onValueChange = { update(config.copy(deletionLinkFormat = it)) },
                    label = stringResource(Res.string.uploader_deletion_link),
                    modifier = Modifier.weight(1f).settingsAnchor(stringResource(Res.string.uploader_deletion_link))
                )
            }
            Text(
                stringResource(Res.string.settings_hub_uploader_link_help),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            UploaderFormFieldsEditor(config, update)
            Row(
                modifier = Modifier.settingsAnchor(stringResource(Res.string.uploader_check_status)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChatoneButton(
                    text = if (checking) stringResource(Res.string.uploader_status_checking) else stringResource(Res.string.uploader_check_status),
                    onClick = {
                        checking = true
                        statusText = null
                        scope.launch {
                            val status = uploaderClient.checkStatus(config)
                            statusOk = status.reachable
                            statusText = if (status.reachable) {
                                getString(
                                    Res.string.uploader_status_ok,
                                    status.httpCode?.toString() ?: "?",
                                    status.latencyMs?.toString() ?: "?"
                                )
                            } else {
                                getString(Res.string.uploader_status_fail, status.error ?: "?")
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
    val fields = config.formFields
    fun replace(index: Int, field: UploaderFormField) {
        update(config.copy(formFields = fields.toMutableList().also { it[index] = field }))
    }
    Column(
        modifier = Modifier.settingsAnchor(stringResource(Res.string.settings_hub_uploader_fields_title)),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChatoneFieldLabel(stringResource(Res.string.settings_hub_uploader_fields_title), modifier = Modifier.weight(1f, fill = false))
            HelpHint(title = stringResource(Res.string.settings_hub_uploader_fields_title), text = stringResource(Res.string.settings_hub_uploader_fields_help), size = 24.dp)
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
                    label = stringResource(Res.string.settings_hub_uploader_field_name),
                    placeholder = "key",
                    modifier = Modifier.weight(1f)
                )
                CompactOutlinedField(
                    value = field.value,
                    onValueChange = { replace(index, field.copy(value = it)) },
                    label = stringResource(Res.string.settings_hub_uploader_field_value),
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
                        contentDescription = stringResource(Res.string.settings_hub_uploader_remove_field),
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        if (fields.size < MAX_FORM_FIELDS) {
            ChatoneButton(
                text = stringResource(Res.string.settings_hub_uploader_add_field),
                onClick = { update(config.copy(formFields = fields + UploaderFormField())) },
                tone = ChatoneButtonTone.Neutral,
                icon = Lucide.Plus
            )
        }
    }
}

private const val MAX_FORM_FIELDS = 8
