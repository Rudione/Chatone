package io.rudione.chatone.presentation.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu

@Composable
fun AccountQuickPicker(
    accounts: List<TwitchAccount>,
    activeUserId: String,
    onSwitchAccount: (TwitchAccount) -> Unit,
    onAddAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val active = accounts.firstOrNull { it.userId == activeUserId } ?: accounts.firstOrNull()

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { expanded = true }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        active?.let {
            AccountAvatar(account = it, size = 20.dp)
            Text(
                text = it.displayName.takeIf { it.isNotBlank() } ?: it.login,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Icon(
            Icons.Outlined.ArrowDropDown,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    ChatoneDropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        accounts.forEach { acc ->
            DropdownMenuItem(
                text = { Text("@${acc.login}") },
                enabled = acc.userId != activeUserId,
                onClick = {
                    expanded = false
                    onSwitchAccount(acc)
                }
            )
        }
        DropdownMenuItem(
            text = { Text("+") },
            onClick = {
                expanded = false
                onAddAccount()
            }
        )
    }
}
