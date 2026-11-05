package io.rudione.chatone.presentation.account

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.accounts_active
import chatone.composeapp.generated.resources.accounts_add
import chatone.composeapp.generated.resources.accounts_custom_settings
import chatone.composeapp.generated.resources.accounts_list_title
import chatone.composeapp.generated.resources.accounts_none_added
import chatone.composeapp.generated.resources.accounts_proxy_auth_optional
import chatone.composeapp.generated.resources.accounts_proxy_desc
import chatone.composeapp.generated.resources.accounts_proxy_enabled
import chatone.composeapp.generated.resources.accounts_proxy_host
import chatone.composeapp.generated.resources.accounts_proxy_invalid
import chatone.composeapp.generated.resources.accounts_proxy_password
import chatone.composeapp.generated.resources.accounts_proxy_port
import chatone.composeapp.generated.resources.accounts_proxy_saved
import chatone.composeapp.generated.resources.accounts_proxy_title
import chatone.composeapp.generated.resources.accounts_proxy_type
import chatone.composeapp.generated.resources.accounts_proxy_type_http
import chatone.composeapp.generated.resources.accounts_proxy_type_socks5
import chatone.composeapp.generated.resources.accounts_proxy_username
import chatone.composeapp.generated.resources.accounts_remove
import chatone.composeapp.generated.resources.accounts_remove_confirm
import chatone.composeapp.generated.resources.accounts_set_primary
import chatone.composeapp.generated.resources.accounts_uses_custom
import chatone.composeapp.generated.resources.accounts_uses_global
import chatone.composeapp.generated.resources.cancel
import chatone.composeapp.generated.resources.save
import org.jetbrains.compose.resources.stringResource
import io.rudione.chatone.data.repository.AccountManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.outlined.Delete
import io.rudione.chatone.icons.material.outlined.Settings
import io.rudione.chatone.icons.material.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import io.rudione.chatone.domain.model.AccountProxyConfig
import io.rudione.chatone.domain.model.ProxyType
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneSwitch
import io.rudione.chatone.presentation.components.ChatoneTextField

@Composable
fun AccountsSettingsSection(
    accounts: List<TwitchAccount>,
    accountManager: AccountManager,
    onAddAccount: () -> Unit,
    onRemoveAccount: (TwitchAccount) -> Unit,
    onSetPrimary: (TwitchAccount) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeId by accountManager.activeAccountId.collectAsState()
    var expandedAccount by remember { mutableStateOf<String?>(null) }
    var confirmRemove by remember { mutableStateOf<TwitchAccount?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(Res.string.accounts_list_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onAddAccount) {
                Text(stringResource(Res.string.accounts_add))
            }
        }

        if (accounts.isEmpty()) {
            Text(
                stringResource(Res.string.accounts_none_added),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            accounts.forEach { account ->
                val isPrimary = account.userId == activeId
                val isExpanded = expandedAccount == account.userId
                AccountManagementCard(
                    account = account,
                    isPrimary = isPrimary,
                    isExpanded = isExpanded,
                    accountManager = accountManager,
                    onToggleExpand = {
                        expandedAccount = if (isExpanded) null else account.userId
                    },
                    onSetPrimary = { onSetPrimary(account) },
                    onRemove = { confirmRemove = account }
                )
            }
        }
    }

    confirmRemove?.let { acc ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text(stringResource(Res.string.accounts_remove)) },
            text = { Text(stringResource(Res.string.accounts_remove_confirm) + "\n@${acc.login}") },
            confirmButton = {
                TextButton(onClick = {
                    accountManager.deleteAllPerAccountData(acc.userId)
                    onRemoveAccount(acc)
                    confirmRemove = null
                }) { Text(stringResource(Res.string.accounts_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = null }) { Text(stringResource(Res.string.cancel)) }
            }
        )
    }
}

@Composable
private fun AccountManagementCard(
    account: TwitchAccount,
    isPrimary: Boolean,
    isExpanded: Boolean,
    accountManager: AccountManager,
    onToggleExpand: () -> Unit,
    onSetPrimary: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = account.displayName.takeIf { it.isNotBlank() } ?: account.login,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isPrimary) {
                            Spacer(Modifier.width(6.dp))
                            FilterChip(
                                selected = true,
                                onClick = {},
                                label = { Text(stringResource(Res.string.accounts_active), style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Text(
                        text = "@${account.login}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ChatoneIconButton(onClick = onToggleExpand) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isExpanded) {
                Spacer(Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(Modifier.height(10.dp))

                if (!isPrimary) {
                    TextButton(
                        onClick = onSetPrimary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Outlined.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.accounts_set_primary))
                    }
                }

                OverrideSettingsRow(
                    userId = account.userId,
                    accountManager = accountManager
                )

                Spacer(Modifier.height(10.dp))
                ProxyConfigSection(
                    userId = account.userId,
                    accountManager = accountManager
                )

                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = onRemove,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.accounts_remove), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun OverrideSettingsRow(
    userId: String,
    accountManager: AccountManager
) {
    var enabled by remember(userId) { mutableStateOf(accountManager.isOverrideEnabled(userId)) }
    val perAccountSettings = remember(accountManager) { PerAccountSettingsLoader(accountManager) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(Res.string.accounts_custom_settings), style = MaterialTheme.typography.bodyMedium)
            Text(
                if (enabled) stringResource(Res.string.accounts_uses_custom) else stringResource(Res.string.accounts_uses_global),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ChatoneSwitch(
            checked = enabled,
            onCheckedChange = {
                enabled = it
                perAccountSettings.setOverrideEnabled(userId, it)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProxyConfigSection(
    userId: String,
    accountManager: AccountManager
) {
    val accountsProxyInvalidText = stringResource(Res.string.accounts_proxy_invalid)
    val accountsProxySavedText = stringResource(Res.string.accounts_proxy_saved)
    val initial = remember(userId) { accountManager.getProxy(userId) }
    var enabled by remember(userId) { mutableStateOf(initial?.enabled ?: false) }
    var type by remember(userId) { mutableStateOf(initial?.type ?: ProxyType.HTTP) }
    var host by remember(userId) { mutableStateOf(initial?.host ?: "") }
    var port by remember(userId) { mutableStateOf((initial?.port ?: 0).toString().takeIf { it != "0" } ?: "") }
    var username by remember(userId) { mutableStateOf(initial?.username ?: "") }
    var password by remember(userId) { mutableStateOf(initial?.password ?: "") }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var saveHint by remember(userId) { mutableStateOf<String?>(null) }

    fun save() {
        val portInt = port.toIntOrNull()
        if (host.isBlank() || portInt == null || portInt !in 1..65535) {
            saveHint = accountsProxyInvalidText
            return
        }
        val cfg = AccountProxyConfig(
            type = type,
            host = host.trim(),
            port = portInt,
            username = username.takeIf { it.isNotBlank() },
            password = password.takeIf { it.isNotBlank() },
            enabled = enabled
        )
        accountManager.saveProxy(userId, cfg)
        saveHint = accountsProxySavedText
    }

    Column {
        Text(stringResource(Res.string.accounts_proxy_title), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(Res.string.accounts_proxy_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.accounts_proxy_enabled), modifier = Modifier.weight(1f))
            ChatoneSwitch(checked = enabled, onCheckedChange = {
                enabled = it
                if (!it) accountManager.saveProxy(
                    userId,
                    accountManager.getProxy(userId)?.copy(enabled = false)
                )
            })
        }

        if (enabled) {
            Spacer(Modifier.height(6.dp))
            ExposedDropdownMenuBox(
                expanded = typeMenuExpanded,
                onExpandedChange = { typeMenuExpanded = !typeMenuExpanded }
            ) {
                ChatoneTextField(
                    value = when (type) {
                        ProxyType.HTTP -> stringResource(Res.string.accounts_proxy_type_http)
                        ProxyType.SOCKS5 -> stringResource(Res.string.accounts_proxy_type_socks5)
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = stringResource(Res.string.accounts_proxy_type),
                    trailing = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = typeMenuExpanded,
                    onDismissRequest = { typeMenuExpanded = false }
                ) {
                    ProxyType.entries.forEach { t ->
                        DropdownMenuItem(
                            text = {
                                Text(when (t) {
                                    ProxyType.HTTP -> stringResource(Res.string.accounts_proxy_type_http)
                                    ProxyType.SOCKS5 -> stringResource(Res.string.accounts_proxy_type_socks5)
                                })
                            },
                            onClick = {
                                type = t
                                typeMenuExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row {
                ChatoneTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = stringResource(Res.string.accounts_proxy_host),
                    modifier = Modifier.weight(2f),
                    singleLine = true
                )
                Spacer(Modifier.width(6.dp))
                ChatoneTextField(
                    value = port,
                    onValueChange = { port = it.filter { ch -> ch.isDigit() }.take(5) },
                    label = stringResource(Res.string.accounts_proxy_port),
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(Res.string.accounts_proxy_auth_optional),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row {
                ChatoneTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = stringResource(Res.string.accounts_proxy_username),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(6.dp))
                io.rudione.chatone.presentation.components.ChatoneSecretField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(Res.string.accounts_proxy_password),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row {
                Button(onClick = ::save, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.save))
                }
            }
            saveHint?.let { hint ->
                Text(
                    hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hint == stringResource(Res.string.accounts_proxy_invalid))
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
