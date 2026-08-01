package io.rudione.chatone.presentation.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PublicOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.VpnLock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import io.rudione.chatone.domain.model.AccountProxyConfig
import io.rudione.chatone.domain.model.ProxyType
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.components.ChatoneTextField

@Composable
fun AccountsSettingsSectionCompact(
    accounts: List<TwitchAccount>,
    accountManager: AccountManager,
    onAddAccount: () -> Unit,
    onAddAccountBrowser: () -> Unit,
    onRemoveAccount: (TwitchAccount) -> Unit,
    onSetPrimary: (TwitchAccount) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val activeId by accountManager.activeAccountId.collectAsState()
    var expandedAccount by remember { mutableStateOf<String?>(null) }
    var confirmRemove by remember { mutableStateOf<TwitchAccount?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (accounts.isEmpty()) {
            Text(
                strings.accountsNoneAdded,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            accounts.forEach { account ->
                AccountRowCompact(
                    account = account,
                    isPrimary = account.userId == activeId,
                    isExpanded = expandedAccount == account.userId,
                    accountManager = accountManager,
                    onToggleExpand = {
                        expandedAccount = if (expandedAccount == account.userId) null else account.userId
                    },
                    onSetPrimary = { onSetPrimary(account) },
                    onRemove = { confirmRemove = account }
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(strings.accountsAdd)
        }
    }

    if (showAddDialog) {
        AddAccountChoiceDialog(
            onDismiss = { showAddDialog = false },
            onBrowser = {
                showAddDialog = false
                onAddAccountBrowser()
            },
            onPasteToken = {
                showAddDialog = false
                onAddAccount()
            }
        )
    }

    confirmRemove?.let { acc ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text(strings.accountsRemove) },
            text = { Text("${strings.accountsRemoveConfirm}\n@${acc.login}") },
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
private fun AccountRowCompact(
    account: TwitchAccount,
    isPrimary: Boolean,
    isExpanded: Boolean,
    accountManager: AccountManager,
    onToggleExpand: () -> Unit,
    onSetPrimary: () -> Unit,
    onRemove: () -> Unit
) {
    val strings = LocalStrings.current
    val hasProxy = remember(account.userId) {
        accountManager.getProxy(account.userId)?.let { it.enabled && it.isValid } == true
    }
    var hasCustomSettings by remember(account.userId) {
        mutableStateOf(accountManager.isOverrideEnabled(account.userId))
    }
    val perAccountSettings = remember(accountManager) { PerAccountSettingsLoader(accountManager) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isPrimary)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccountAvatar(account = account, size = 32.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = account.displayName.takeIf { it.isNotBlank() } ?: account.login,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isPrimary) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Outlined.Star,
                            contentDescription = strings.accountsActive,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "@${account.login}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (hasProxy) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Outlined.VpnLock,
                            contentDescription = strings.accountsProxyTitle,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    if (hasCustomSettings) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = strings.accountsUsesCustom,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
            Icon(
                if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                if (!isPrimary) {
                    QuickRowButton(
                        icon = Icons.Outlined.StarOutline,
                        label = strings.accountsSetPrimary,
                        onClick = onSetPrimary
                    )
                }
                ToggleRow(
                    label = strings.accountsCustomSettings,
                    sublabel = if (hasCustomSettings) strings.accountsUsesCustom else strings.accountsUsesGlobal,
                    checked = hasCustomSettings,
                    onCheckedChange = {
                        hasCustomSettings = it
                        perAccountSettings.setOverrideEnabled(account.userId, it)
                    }
                )
                ProxyExpandableRow(
                    userId = account.userId,
                    accountManager = accountManager
                )
                Spacer(Modifier.height(4.dp))
                QuickRowButton(
                    icon = Icons.Outlined.Delete,
                    label = strings.accountsRemove,
                    onClick = onRemove,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun QuickRowButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = tint)
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = tint)
    }
}

@Composable
private fun ToggleRow(
    label: String,
    sublabel: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                sublabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        io.rudione.chatone.presentation.components.ChatoneSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ProxyExpandableRow(
    userId: String,
    accountManager: AccountManager
) {
    val strings = LocalStrings.current
    val initial = remember(userId) { accountManager.getProxy(userId) }
    var enabled by remember(userId) { mutableStateOf(initial?.enabled ?: false) }
    var formExpanded by remember(userId) { mutableStateOf(initial != null && initial.isValid) }
    var type by remember(userId) { mutableStateOf(initial?.type ?: ProxyType.HTTP) }
    var host by remember(userId) { mutableStateOf(initial?.host ?: "") }
    var port by remember(userId) {
        mutableStateOf((initial?.port ?: 0).toString().takeIf { it != "0" } ?: "")
    }
    var username by remember(userId) { mutableStateOf(initial?.username ?: "") }
    var password by remember(userId) { mutableStateOf(initial?.password ?: "") }
    var saveHint by remember(userId) { mutableStateOf<String?>(null) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(strings.accountsProxyTitle, style = MaterialTheme.typography.bodyMedium)
                Text(
                    strings.accountsProxyDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            io.rudione.chatone.presentation.components.ChatoneSwitch(checked = enabled, onCheckedChange = {
                enabled = it
                formExpanded = it
                if (!it) {
                    accountManager.getProxy(userId)?.let { existing ->
                        accountManager.saveProxy(userId, existing.copy(enabled = false))
                    }
                }
            })
        }

        AnimatedVisibility(visible = formExpanded && enabled) {
            Column(modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)) {
                val typeLabel = when (type) {
                    ProxyType.HTTP -> strings.accountsProxyTypeHttp
                    ProxyType.SOCKS5 -> strings.accountsProxyTypeSocks5
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.accountsProxyType + ":", modifier = Modifier.padding(end = 6.dp))
                    TextButton(onClick = {
                        type = if (type == ProxyType.HTTP) ProxyType.SOCKS5 else ProxyType.HTTP
                    }) { Text(typeLabel) }
                }
                Row {
                    ChatoneTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = strings.accountsProxyHost,
                        modifier = Modifier.weight(2f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(4.dp))
                    ChatoneTextField(
                        value = port,
                        onValueChange = { port = it.filter { ch -> ch.isDigit() }.take(5) },
                        label = strings.accountsProxyPort,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row {
                    ChatoneTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = strings.accountsProxyUsername,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(4.dp))
                    ChatoneTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = strings.accountsProxyPassword,
                        modifier = Modifier.weight(1f),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = {
                        val portInt = port.toIntOrNull()
                        if (host.isBlank() || portInt == null || portInt !in 1..65535) {
                            saveHint = strings.accountsProxyInvalid
                            return@Button
                        }
                        accountManager.saveProxy(
                            userId,
                            AccountProxyConfig(
                                type = type, host = host.trim(), port = portInt,
                                username = username.takeIf { it.isNotBlank() },
                                password = password.takeIf { it.isNotBlank() },
                                enabled = enabled
                            )
                        )
                        saveHint = strings.accountsProxySaved
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(strings.save) }
                saveHint?.let { hint ->
                    Text(
                        hint,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hint == strings.accountsProxyInvalid)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddAccountChoiceDialog(
    onDismiss: () -> Unit,
    onBrowser: () -> Unit,
    onPasteToken: () -> Unit
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.accountsAdd) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.accountsAdd, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Twitch OAuth",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onBrowser) { Text(strings.authBrowser) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onPasteToken) { Text(strings.authPasteToken) }
                TextButton(onClick = onDismiss) { Text(strings.cancel) }
            }
        }
    )
}
