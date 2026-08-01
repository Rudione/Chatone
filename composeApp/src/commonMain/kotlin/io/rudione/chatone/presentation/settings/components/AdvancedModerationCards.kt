package io.rudione.chatone.presentation.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.repository.DeviceAuthState
import io.rudione.chatone.data.repository.FirstPartyDeviceAuthController
import io.rudione.chatone.data.repository.ModerationAuthStore
import io.rudione.chatone.data.repository.StreamerModeController
import io.rudione.chatone.presentation.components.ChatoneSwitch
import io.rudione.chatone.presentation.components.SettingsCard
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.link.openUrl
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import io.rudione.chatone.presentation.components.ChatoneTextField

@Composable
fun FirstPartyTokenCard(
    store: ModerationAuthStore = koinInject(),
    deviceController: FirstPartyDeviceAuthController = koinInject()
) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val identity by store.identity.collectAsState()
    val deviceState by deviceController.state.collectAsState()
    var input by remember { mutableStateOf("") }
    var validating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    SettingsCard(title = s.tokenCardTitle) {
        Text(
            s.tokenCardDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val current = identity
        if (current != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    Icons.Filled.CheckCircle, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(16.dp).height(16.dp)
                )
                Text(
                    "${s.tokenCardLinkedAs} ${current.displayName.ifBlank { current.login }}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        when (val ds = deviceState) {
            is DeviceAuthState.WaitingForApproval -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Text(
                        s.tokenCardDeviceCodeHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        ds.userCode,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { openUrl(ds.verificationUri, SettingsState.LinkOpenMode.DEFAULT) }) {
                            Text(s.tokenCardOpenTwitch)
                        }
                        OutlinedButton(onClick = { deviceController.cancel() }) {
                            Text(s.cancel)
                        }
                    }
                }
            }

            DeviceAuthState.Validating -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.width(16.dp).height(16.dp), strokeWidth = 2.dp)
                    Text(s.tokenCardValidating, style = MaterialTheme.typography.bodySmall)
                }
            }

            is DeviceAuthState.Error -> {
                Text(
                    ds.message,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            else -> {}
        }

        Button(
            onClick = { deviceController.start() },
            enabled = deviceState !is DeviceAuthState.WaitingForApproval && deviceState != DeviceAuthState.Validating,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) {
            Text(s.tokenCardConnectAutomatically)
        }

        Text(
            s.tokenCardManualHint,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        ChatoneTextField(
            value = input,
            onValueChange = { input = it; error = null },
            modifier = Modifier.fillMaxWidth(),
            label = s.tokenCardLabel,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default,
            isError = error != null,
            hint = error
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val token = input.trim()
                    if (token.isEmpty()) return@Button
                    validating = true
                    error = null
                    scope.launch {
                        val id = store.setAndValidate(token)
                        validating = false
                        if (id == null) error = s.tokenCardRejected
                        else input = ""
                    }
                },
                enabled = !validating && input.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                if (validating) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(16.dp).height(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(s.tokenCardValidate)
                }
            }
            if (current != null) {
                OutlinedButton(
                    onClick = { store.clear(); input = ""; error = null },
                    modifier = Modifier.weight(1f)
                ) { Text(s.tokenCardRemove) }
            }
        }
    }
}

@Composable
fun StreamerModeCard(controller: StreamerModeController = koinInject()) {
    val s = LocalStrings.current
    val state by controller.state.collectAsState()

    SettingsCard(title = s.streamerModeTitle) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    s.streamerModeHideData,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    s.streamerModeObs,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ChatoneSwitch(
                checked = state.enabled,
                onCheckedChange = { controller.setEnabled(it) }
            )
        }

        if (state.enabled) {
            Spacer(Modifier.height(4.dp))
            StreamerToggle(s.streamerMaskTokens, state.options.hideTokens) { v ->
                controller.setOptions { it.copy(hideTokens = v) }
            }
            StreamerToggle(s.streamerHideMod, state.options.hideModActions) { v ->
                controller.setOptions { it.copy(hideModActions = v) }
            }
            StreamerToggle(s.streamerMaskChannels, state.options.hideChannelNames) { v ->
                controller.setOptions { it.copy(hideChannelNames = v) }
            }
            StreamerToggle(s.streamerHideThumbs, state.options.hideThumbnails) { v ->
                controller.setOptions { it.copy(hideThumbnails = v) }
            }
            StreamerToggle(s.streamerSuppressNotif, state.options.suppressNotifications) { v ->
                controller.setOptions { it.copy(suppressNotifications = v) }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    Icons.Outlined.Info, null,
                    modifier = Modifier.width(14.dp).height(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    s.streamerStaysOn,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StreamerToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        ChatoneSwitch(checked = checked, onCheckedChange = onChange, modifier = Modifier.height(26.dp))
    }
}
