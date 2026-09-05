package io.rudione.chatone.presentation.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.clip
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
import io.rudione.chatone.data.repository.AccountManager
import io.rudione.chatone.presentation.account.AccountListLoader
import io.rudione.chatone.presentation.account.rememberAccountListState
import io.rudione.chatone.presentation.components.ChatoneActionRow
import io.rudione.chatone.presentation.components.ChatoneButtonText
import io.rudione.chatone.presentation.components.ChatoneSwitch
import io.rudione.chatone.presentation.components.SettingsCard
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.theme.i18n.format
import io.rudione.chatone.util.link.openUrl
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import io.rudione.chatone.presentation.components.ChatoneTextField

@Composable
fun FirstPartyTokenCard(
    store: ModerationAuthStore = koinInject(),
    deviceController: FirstPartyDeviceAuthController = koinInject(),
    accountManager: AccountManager = koinInject(),
    accountLoader: AccountListLoader = koinInject()
) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val identity by store.identity.collectAsState()
    val deviceState by deviceController.state.collectAsState()
    val activeAccountId by accountManager.activeAccountId.collectAsState()
    val accounts = rememberAccountListState(accountLoader)

    var showManual by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var validating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val activeAccount = accounts.firstOrNull { it.userId == activeAccountId }
    val activeLabel = activeAccount?.displayName?.ifBlank { activeAccount.login }
        ?: activeAccount?.login.orEmpty()
    val connected = identity != null
    val boundElsewhere = (deviceState as? DeviceAuthState.Success)
        ?.takeIf { activeAccountId.isNotBlank() && it.userId != activeAccountId }

    SettingsCard(title = s.extRightsTitle) {
        ExtRightsStatusRow(connected = connected, label = identity?.let {
            it.displayName.ifBlank { it.login }
        })

        if (activeLabel.isNotBlank()) {
            Text(
                s.format(s.extRightsForAccount, activeLabel),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            s.extRightsWhat,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!connected) {
            Text(
                s.extRightsUnlocks,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        boundElsewhere?.let { mismatch ->
            ExtRightsBanner(
                text = s.format(s.extRightsMismatch, mismatch.displayName, activeLabel),
                tone = MaterialTheme.colorScheme.error
            )
        }

        when (val ds = deviceState) {
            is DeviceAuthState.WaitingForApproval -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                ) {
                    if (activeLabel.isNotBlank()) {
                        ExtRightsBanner(
                            text = s.format(s.extRightsApproveAs, activeLabel),
                            tone = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Text(
                        s.extRightsCodeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        ds.userCode,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    ChatoneActionRow {
                        Button(onClick = {
                            openUrl(ds.verificationUri, SettingsState.LinkOpenMode.DEFAULT)
                        }) {
                            ChatoneButtonText(s.extRightsOpenTwitch)
                        }
                        OutlinedButton(onClick = { deviceController.cancel() }) {
                            ChatoneButtonText(s.cancel)
                        }
                    }
                }
            }

            DeviceAuthState.Validating -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(16.dp).height(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(s.extRightsWaiting, style = MaterialTheme.typography.bodySmall)
                }
            }

            is DeviceAuthState.Error -> ExtRightsBanner(
                text = ds.message,
                tone = MaterialTheme.colorScheme.error
            )

            else -> Unit
        }

        val busy = deviceState is DeviceAuthState.WaitingForApproval ||
                deviceState == DeviceAuthState.Validating

        ChatoneActionRow(modifier = Modifier.padding(top = 2.dp)) {
            Button(
                onClick = { deviceController.start() },
                enabled = !busy,
                modifier = Modifier.widthIn(min = 150.dp)
            ) {
                ChatoneButtonText(if (connected) s.extRightsReconnect else s.extRightsConnect)
            }
            if (connected) {
                OutlinedButton(onClick = { store.clear(); input = ""; error = null }) {
                    ChatoneButtonText(s.extRightsDisconnect)
                }
            }
            OutlinedButton(onClick = { showManual = !showManual }) {
                ChatoneButtonText(if (showManual) s.extRightsManualHide else s.extRightsManualShow)
            }
        }

        if (showManual) {
            Text(
                s.extRightsManual,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
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
            Button(
                onClick = {
                    val token = input.trim()
                    if (token.isEmpty()) return@Button
                    validating = true
                    error = null
                    scope.launch {
                        val id = store.setAndValidate(token)
                        validating = false
                        if (id == null) error = s.tokenCardRejected else input = ""
                    }
                },
                enabled = !validating && input.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (validating) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(16.dp).height(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    ChatoneButtonText(s.tokenCardValidate)
                }
            }
        }
    }
}

@Composable
private fun ExtRightsStatusRow(connected: Boolean, label: String?) {
    val s = LocalStrings.current
    val tone = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            if (connected) Icons.Filled.CheckCircle else Icons.Outlined.Info,
            contentDescription = null,
            tint = tone,
            modifier = Modifier.width(16.dp).height(16.dp)
        )
        Text(
            if (connected) s.extRightsConnected else s.extRightsNotConnected,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = tone
        )
        if (connected && !label.isNullOrBlank()) {
            Text(
                "· $label",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExtRightsBanner(text: String, tone: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(tone.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
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
