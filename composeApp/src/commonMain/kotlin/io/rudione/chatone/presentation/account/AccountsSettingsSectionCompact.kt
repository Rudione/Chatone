package io.rudione.chatone.presentation.account

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.accounts_active
import chatone.composeapp.generated.resources.accounts_add
import chatone.composeapp.generated.resources.accounts_custom_settings
import chatone.composeapp.generated.resources.accounts_none_added
import chatone.composeapp.generated.resources.accounts_proxy_desc
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
import chatone.composeapp.generated.resources.ext_rights_connected
import chatone.composeapp.generated.resources.save
import org.jetbrains.compose.resources.stringResource
import io.rudione.chatone.data.repository.AccountManager
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
import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.outlined.Delete
import io.rudione.chatone.icons.material.outlined.KeyboardArrowDown
import io.rudione.chatone.icons.material.outlined.KeyboardArrowUp
import io.rudione.chatone.icons.material.outlined.Settings
import io.rudione.chatone.icons.material.outlined.Star
import io.rudione.chatone.icons.material.outlined.StarOutline
import io.rudione.chatone.icons.material.outlined.VerifiedUser
import io.rudione.chatone.icons.material.outlined.VpnLock
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import io.rudione.chatone.domain.model.AccountProxyConfig
import io.rudione.chatone.domain.model.ProxyType
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.data.repository.ModerationAuthStore
import io.rudione.chatone.presentation.components.ChatoneSecretField
import io.rudione.chatone.presentation.components.ChatoneSwitch
import io.rudione.chatone.presentation.components.ChatoneTextField
import org.koin.compose.koinInject

@Composable
fun AccountsSettingsSectionCompact(
    accounts: List<TwitchAccount>,
    accountManager: AccountManager,
    onAddAccount: () -> Unit,
    onRemoveAccount: (TwitchAccount) -> Unit,
    onSetPrimary: (TwitchAccount) -> Unit,
    modifier: Modifier = Modifier
) {
    val moderationAuthStore: ModerationAuthStore = koinInject()
    val activeId by accountManager.activeAccountId.collectAsState()
    var expandedAccount by remember { mutableStateOf<String?>(null) }
    var confirmRemove by remember { mutableStateOf<TwitchAccount?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (accounts.isEmpty()) {
            Text(
                stringResource(Res.string.accounts_none_added),
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
                    hasExtendedRights = moderationAuthStore.hasTokenFor(account.userId),
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
            onClick = onAddAccount,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(Res.string.accounts_add))
        }
    }

    confirmRemove?.let { acc ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text(stringResource(Res.string.accounts_remove)) },
            text = { Text("${stringResource(Res.string.accounts_remove_confirm)}\n@${acc.login}") },
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
private fun AccountRowCompact(
    account: TwitchAccount,
    isPrimary: Boolean,
    isExpanded: Boolean,
    hasExtendedRights: Boolean,
    accountManager: AccountManager,
    onToggleExpand: () -> Unit,
    onSetPrimary: () -> Unit,
    onRemove: () -> Unit
) {
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
                            contentDescription = stringResource(Res.string.accounts_active),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (hasExtendedRights) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Outlined.VerifiedUser,
                            contentDescription = stringResource(Res.string.ext_rights_connected),
                            tint = MaterialTheme.colorScheme.tertiary,
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
                            contentDescription = stringResource(Res.string.accounts_proxy_title),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    if (hasCustomSettings) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = stringResource(Res.string.accounts_uses_custom),
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
                        label = stringResource(Res.string.accounts_set_primary),
                        onClick = onSetPrimary
                    )
                }
                ToggleRow(
                    label = stringResource(Res.string.accounts_custom_settings),
                    sublabel = if (hasCustomSettings) stringResource(Res.string.accounts_uses_custom) else stringResource(Res.string.accounts_uses_global),
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
                    label = stringResource(Res.string.accounts_remove),
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
        ChatoneSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ProxyExpandableRow(
    userId: String,
    accountManager: AccountManager
) {
    val accountsProxyInvalidText = stringResource(Res.string.accounts_proxy_invalid)
    val accountsProxySavedText = stringResource(Res.string.accounts_proxy_saved)
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
                Text(stringResource(Res.string.accounts_proxy_title), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(Res.string.accounts_proxy_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ChatoneSwitch(checked = enabled, onCheckedChange = {
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
                    ProxyType.HTTP -> stringResource(Res.string.accounts_proxy_type_http)
                    ProxyType.SOCKS5 -> stringResource(Res.string.accounts_proxy_type_socks5)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.accounts_proxy_type) + ":", modifier = Modifier.padding(end = 6.dp))
                    TextButton(onClick = {
                        type = if (type == ProxyType.HTTP) ProxyType.SOCKS5 else ProxyType.HTTP
                    }) { Text(typeLabel) }
                }
                Row {
                    ChatoneTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = stringResource(Res.string.accounts_proxy_host),
                        modifier = Modifier.weight(2f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(4.dp))
                    ChatoneTextField(
                        value = port,
                        onValueChange = { port = it.filter { ch -> ch.isDigit() }.take(5) },
                        label = stringResource(Res.string.accounts_proxy_port),
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
                        label = stringResource(Res.string.accounts_proxy_username),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(4.dp))
                    ChatoneSecretField(
                        value = password,
                        onValueChange = { password = it },
                        label = stringResource(Res.string.accounts_proxy_password),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = {
                        val portInt = port.toIntOrNull()
                        if (host.isBlank() || portInt == null || portInt !in 1..65535) {
                            saveHint = accountsProxyInvalidText
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
                        saveHint = accountsProxySavedText
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(Res.string.save)) }
                saveHint?.let { hint ->
                    Text(
                        hint,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hint == stringResource(Res.string.accounts_proxy_invalid))
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
