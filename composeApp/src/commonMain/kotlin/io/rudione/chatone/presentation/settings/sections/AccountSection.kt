package io.rudione.chatone.presentation.settings.sections

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.repository.AccountManager
import io.rudione.chatone.icons.lucide.LogOut
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Trash2
import io.rudione.chatone.presentation.account.AccountActions
import io.rudione.chatone.presentation.account.AccountAddDialog
import io.rudione.chatone.presentation.account.AccountListLoader
import io.rudione.chatone.presentation.account.AccountsSettingsSectionCompact
import io.rudione.chatone.presentation.account.rememberAccountUiState
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.settings.components.BlockedUsersSection
import io.rudione.chatone.presentation.settings.components.FirstPartyTokenCard
import io.rudione.chatone.presentation.settings.components.SettingsGroup
import io.rudione.chatone.presentation.settings.components.StreamerModeCard
import io.rudione.chatone.presentation.settings.settingsAnchor
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import org.koin.compose.koinInject

@Composable
internal fun AccountContent(viewModel: SettingsViewModel) {
    val s = LocalStrings.current
    val state by viewModel.state.collectAsState()
    val accountManager = koinInject<AccountManager>()
    val accountActions = koinInject<AccountActions>()
    val accountLoader = koinInject<AccountListLoader>()
    val accountUi = rememberAccountUiState(accountLoader, accountManager)
    var showAddAccountDialog by remember { mutableStateOf(false) }

    SettingsGroup(s.accountsTitle) {
        AccountsSettingsSectionCompact(
            accounts = accountUi.accounts,
            accountManager = accountManager,
            onAddAccount = { showAddAccountDialog = true },
            onRemoveAccount = { accountActions.remove(it) },
            onSetPrimary = { accountActions.setPrimary(it) }
        )
    }
    SettingsGroup(s.settingsAccount) {
        AccountSessionBody(
            onLogout = { viewModel.sendEvent(SettingsEvent.OnLogoutClicked) },
            onClearCache = { viewModel.sendEvent(SettingsEvent.OnClearCacheClicked) }
        )
    }
    FirstPartyTokenCard()
    SideEffect(Unit) {
        if (state.blockedUsernames.isEmpty() && !state.isLoadingBlockedUsers) {
            viewModel.sendEvent(SettingsEvent.OnLoadBlockedUsers)
        }
    }
    BlockedUsersSection(
        blockedUsernames = state.blockedUsernames,
        showBlockedMode = state.showBlockedMode,
        isLoadingBlockedUsers = state.isLoadingBlockedUsers,
        loadError = state.blockedLoadError,
        onShowBlockedModeChange = { viewModel.sendEvent(SettingsEvent.OnShowBlockedModeChanged(it)) },
        onUnblockUser = { username -> viewModel.sendEvent(SettingsEvent.OnUnblockUserFromSettings(username, "")) },
        onRefresh = { viewModel.sendEvent(SettingsEvent.OnLoadBlockedUsers) }
    )
    StreamerModeCard()

    if (showAddAccountDialog) {
        AccountAddDialog(
            onDismiss = { showAddAccountDialog = false },
            onAccountAdded = { accountActions.setPrimary(it) }
        )
    }
}

@Composable
private fun AccountSessionBody(onLogout: () -> Unit, onClearCache: () -> Unit) {
    val s = LocalStrings.current
    var showClearDialog by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        DangerAction(
            anchor = s.settingsLogout,
            description = s.settingsLogoutDesc,
            label = s.settingsLogout,
            icon = Lucide.LogOut,
            onClick = onLogout
        )
        Spacer(Modifier.height(10.dp))
        DangerAction(
            anchor = s.settingsClearCache,
            description = s.settingsClearCacheDesc,
            label = s.settingsClearCache,
            icon = Lucide.Trash2,
            onClick = { showClearDialog = true }
        )
    }
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(s.settingsClearCache) },
            text = { Text(s.settingsConfirmClearCache) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearCache()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(s.settingsClearCache) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text(s.settingsCancel) }
            }
        )
    }
}

@Composable
private fun DangerAction(
    anchor: String,
    description: String,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.settingsAnchor(anchor)) {
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label)
        }
    }
}
