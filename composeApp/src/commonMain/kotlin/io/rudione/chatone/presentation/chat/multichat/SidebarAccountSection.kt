package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.presentation.account.AccountSwitcher


@Composable
fun SidebarAccountSection(
    accounts: List<TwitchAccount>,
    activeUserId: String,
    onSwitchAccount: (TwitchAccount) -> Unit,
    onAddAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
            .padding(8.dp)
    ) {
        AccountSwitcher(
            accounts = accounts,
            activeUserId = activeUserId,
            onSwitchAccount = onSwitchAccount,
            onAddAccount = onAddAccount,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
    }
}
