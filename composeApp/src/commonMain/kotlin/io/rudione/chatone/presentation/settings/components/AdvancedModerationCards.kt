package io.rudione.chatone.presentation.settings.components

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.cancel
import chatone.composeapp.generated.resources.ext_rights_approve_as
import chatone.composeapp.generated.resources.ext_rights_code_label
import chatone.composeapp.generated.resources.ext_rights_connect
import chatone.composeapp.generated.resources.ext_rights_connected
import chatone.composeapp.generated.resources.ext_rights_disconnect
import chatone.composeapp.generated.resources.ext_rights_for_account
import chatone.composeapp.generated.resources.ext_rights_manual
import chatone.composeapp.generated.resources.ext_rights_manual_hide
import chatone.composeapp.generated.resources.ext_rights_manual_show
import chatone.composeapp.generated.resources.ext_rights_mismatch
import chatone.composeapp.generated.resources.ext_rights_not_connected
import chatone.composeapp.generated.resources.ext_rights_open_twitch
import chatone.composeapp.generated.resources.ext_rights_reconnect
import chatone.composeapp.generated.resources.ext_rights_title
import chatone.composeapp.generated.resources.ext_rights_unlocks
import chatone.composeapp.generated.resources.ext_rights_waiting
import chatone.composeapp.generated.resources.ext_rights_what
import chatone.composeapp.generated.resources.streamer_hide_mod
import chatone.composeapp.generated.resources.streamer_hide_thumbs
import chatone.composeapp.generated.resources.streamer_mask_channels
import chatone.composeapp.generated.resources.streamer_mask_tokens
import chatone.composeapp.generated.resources.streamer_mode_hide_data
import chatone.composeapp.generated.resources.streamer_mode_obs
import chatone.composeapp.generated.resources.streamer_mode_title
import chatone.composeapp.generated.resources.streamer_stays_on
import chatone.composeapp.generated.resources.streamer_suppress_notif
import chatone.composeapp.generated.resources.token_card_label
import chatone.composeapp.generated.resources.token_card_rejected
import chatone.composeapp.generated.resources.token_card_validate
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.repository.AccountManager
import io.rudione.chatone.data.repository.DeviceAuthState
import io.rudione.chatone.data.repository.FirstPartyDeviceAuthController
import io.rudione.chatone.data.repository.ModerationAuthStore
import io.rudione.chatone.data.repository.StreamerModeController
import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.filled.CheckCircle
import io.rudione.chatone.icons.material.outlined.Info
import io.rudione.chatone.presentation.account.AccountListLoader
import io.rudione.chatone.presentation.account.rememberAccountListState
import io.rudione.chatone.presentation.components.ChatoneActionRow
import io.rudione.chatone.presentation.components.ChatoneButtonText
import io.rudione.chatone.presentation.components.ChatoneSecretField
import io.rudione.chatone.presentation.components.ChatoneSwitch
import io.rudione.chatone.presentation.components.SecretRevealButton
import io.rudione.chatone.presentation.components.SettingsCard
import io.rudione.chatone.presentation.components.expressive.HelpHint
import io.rudione.chatone.presentation.components.rememberSecretVisibility
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.util.link.openUrl
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun FirstPartyTokenCard(
    store: ModerationAuthStore = koinInject(),
    deviceController: FirstPartyDeviceAuthController = koinInject(),
    accountManager: AccountManager = koinInject(),
    accountLoader: AccountListLoader = koinInject()
) {
    val tokenCardRejectedText = stringResource(Res.string.token_card_rejected)
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

    SettingsCard(title = stringResource(Res.string.ext_rights_title)) {
        ExtRightsStatusRow(connected = connected, label = identity?.let {
            it.displayName.ifBlank { it.login }
        })

        if (activeLabel.isNotBlank()) {
            Text(
                stringResource(Res.string.ext_rights_for_account, activeLabel),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            stringResource(Res.string.ext_rights_what),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!connected) {
            Text(
                stringResource(Res.string.ext_rights_unlocks),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        boundElsewhere?.let { mismatch ->
            ExtRightsBanner(
                text = stringResource(
                    Res.string.ext_rights_mismatch,
                    mismatch.displayName,
                    activeLabel
                ),
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
                            text = stringResource(Res.string.ext_rights_approve_as, activeLabel),
                            tone = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Text(
                        stringResource(Res.string.ext_rights_code_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val codeVisibility = rememberSecretVisibility()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            if (codeVisibility.revealed) ds.userCode
                            else "•".repeat(ds.userCode.length.coerceIn(4, 10)),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        SecretRevealButton(codeVisibility)
                    }
                    ChatoneActionRow {
                        Button(onClick = {
                            openUrl(ds.verificationUri, SettingsState.LinkOpenMode.DEFAULT)
                        }) {
                            ChatoneButtonText(stringResource(Res.string.ext_rights_open_twitch))
                        }
                        OutlinedButton(onClick = { deviceController.cancel() }) {
                            ChatoneButtonText(stringResource(Res.string.cancel))
                        }
                    }
                }
            }

            DeviceAuthState.Validating, DeviceAuthState.ConfirmingOnLoginPage -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(16.dp).height(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        stringResource(Res.string.ext_rights_waiting),
                        style = MaterialTheme.typography.bodySmall
                    )
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
                ChatoneButtonText(
                    if (connected) stringResource(Res.string.ext_rights_reconnect) else stringResource(
                        Res.string.ext_rights_connect
                    )
                )
            }
            if (connected) {
                OutlinedButton(onClick = { store.clear(); input = ""; error = null }) {
                    ChatoneButtonText(stringResource(Res.string.ext_rights_disconnect))
                }
            }
            OutlinedButton(onClick = { showManual = !showManual }) {
                ChatoneButtonText(
                    if (showManual) stringResource(Res.string.ext_rights_manual_hide) else stringResource(
                        Res.string.ext_rights_manual_show
                    )
                )
            }
        }

        if (showManual) {
            Text(
                stringResource(Res.string.ext_rights_manual),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
            ChatoneSecretField(
                value = input,
                onValueChange = { input = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(Res.string.token_card_label),
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
                        if (id == null) error = tokenCardRejectedText else input = ""
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
                    ChatoneButtonText(stringResource(Res.string.token_card_validate))
                }
            }
        }
    }
}

@Composable
private fun ExtRightsStatusRow(connected: Boolean, label: String?) {
    val tone =
        if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
            if (connected) stringResource(Res.string.ext_rights_connected) else stringResource(Res.string.ext_rights_not_connected),
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
    val state by controller.state.collectAsState()

    SettingsCard(title = stringResource(Res.string.streamer_mode_title)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.streamer_mode_hide_data),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f, fill = false)
                )
                HelpHint(
                    title = stringResource(Res.string.streamer_mode_hide_data),
                    text = "${stringResource(Res.string.streamer_mode_obs)} ${stringResource(Res.string.streamer_stays_on)}",
                    size = 28.dp
                )
            }
            ChatoneSwitch(
                checked = state.enabled,
                onCheckedChange = { controller.setEnabled(it) }
            )
        }

        if (state.enabled) {
            Spacer(Modifier.height(4.dp))
            StreamerToggle(
                stringResource(Res.string.streamer_mask_tokens),
                state.options.hideTokens
            ) { v ->
                controller.setOptions { it.copy(hideTokens = v) }
            }
            StreamerToggle(
                stringResource(Res.string.streamer_hide_mod),
                state.options.hideModActions
            ) { v ->
                controller.setOptions { it.copy(hideModActions = v) }
            }
            StreamerToggle(
                stringResource(Res.string.streamer_mask_channels),
                state.options.hideChannelNames
            ) { v ->
                controller.setOptions { it.copy(hideChannelNames = v) }
            }
            StreamerToggle(
                stringResource(Res.string.streamer_hide_thumbs),
                state.options.hideThumbnails
            ) { v ->
                controller.setOptions { it.copy(hideThumbnails = v) }
            }
            StreamerToggle(
                stringResource(Res.string.streamer_suppress_notif),
                state.options.suppressNotifications
            ) { v ->
                controller.setOptions { it.copy(suppressNotifications = v) }
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
        ChatoneSwitch(
            checked = checked,
            onCheckedChange = onChange,
            modifier = Modifier.height(26.dp)
        )
    }
}
