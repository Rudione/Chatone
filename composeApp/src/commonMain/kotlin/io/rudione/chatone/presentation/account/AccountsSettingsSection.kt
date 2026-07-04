package io.rudione.chatone.presentation.account

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import io.rudione.chatone.domain.model.AccountProxyConfig
import io.rudione.chatone.domain.model.ProxyType
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.presentation.theme.i18n.LocalStrings

@Composable
fun AccountsSettingsSection(
    accounts: List<TwitchAccount>,
    accountManager: AccountManager,
    onAddAccount: () -> Unit,
    onRemoveAccount: (TwitchAccount) -> Unit,
    onSetPrimary: (TwitchAccount) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val activeId by accountManager.activeAccountId.collectAsState()
    var expandedAccount by remember { mutableStateOf<String?>(null) }
    var confirmRemove by remember { mutableStateOf<TwitchAccount?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                strings.accountsListTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onAddAccount) {
                Text(strings.accountsAdd)
            }
        }

        if (accounts.isEmpty()) {
            Text(
                strings.accountsNoneAdded,
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
            title = { Text(strings.accountsRemove) },
            text = { Text(strings.accountsRemoveConfirm + "\n@${acc.login}") },
            confirmButton = {
                TextButton(onClick = {
                    accountManager.deleteAllPerAccountData(acc.userId)
                    onRemoveAccount(acc)
                    confirmRemove = null
                }) { Text(strings.accountsRemove) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = null }) { Text(strings.cancel) }
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
    val strings = LocalStrings.current
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
                                label = { Text(strings.accountsActive, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Text(
                        text = "@${account.login}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onToggleExpand) {
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
                        Text(strings.accountsSetPrimary)
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
                    Text(strings.accountsRemove, color = MaterialTheme.colorScheme.error)
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
    val strings = LocalStrings.current
    var enabled by remember(userId) { mutableStateOf(accountManager.isOverrideEnabled(userId)) }
    val perAccountSettings = remember(accountManager) { PerAccountSettingsLoader(accountManager) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(strings.accountsCustomSettings, style = MaterialTheme.typography.bodyMedium)
            Text(
                if (enabled) strings.accountsUsesCustom else strings.accountsUsesGlobal,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        io.rudione.chatone.presentation.components.ChatoneSwitch(
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
    val strings = LocalStrings.current
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
            saveHint = strings.accountsProxyInvalid
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
        saveHint = strings.accountsProxySaved
    }

    Column {
        Text(strings.accountsProxyTitle, style = MaterialTheme.typography.titleSmall)
        Text(
            strings.accountsProxyDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(strings.accountsProxyEnabled, modifier = Modifier.weight(1f))
            io.rudione.chatone.presentation.components.ChatoneSwitch(checked = enabled, onCheckedChange = {
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
                OutlinedTextField(
                    value = when (type) {
                        ProxyType.HTTP -> strings.accountsProxyTypeHttp
                        ProxyType.SOCKS5 -> strings.accountsProxyTypeSocks5
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(strings.accountsProxyType) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
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
                                    ProxyType.HTTP -> strings.accountsProxyTypeHttp
                                    ProxyType.SOCKS5 -> strings.accountsProxyTypeSocks5
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
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text(strings.accountsProxyHost) },
                    modifier = Modifier.weight(2f),
                    singleLine = true
                )
                Spacer(Modifier.width(6.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { ch -> ch.isDigit() }.take(5) },
                    label = { Text(strings.accountsProxyPort) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                strings.accountsProxyAuthOptional,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(strings.accountsProxyUsername) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(6.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(strings.accountsProxyPassword) },
                    modifier = Modifier.weight(1f),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            }
            Spacer(Modifier.height(8.dp))
            Row {
                Button(onClick = ::save, modifier = Modifier.weight(1f)) {
                    Text(strings.save)
                }
            }
            saveHint?.let { hint ->
                Text(
                    hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hint == strings.accountsProxyInvalid)
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
