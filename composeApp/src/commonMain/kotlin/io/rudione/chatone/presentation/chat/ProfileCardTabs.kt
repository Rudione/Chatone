package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.components.expressive.ExpressiveTabSize
import io.rudione.chatone.presentation.components.expressive.ExpressiveTabs
import io.rudione.chatone.presentation.theme.i18n.LocalStrings

internal enum class ProfileCardTab { Messages, Dossier, Roles, ModHistory }

@Composable
internal fun ProfileCardTabRow(
    selected: ProfileCardTab,
    onSelect: (ProfileCardTab) -> Unit,
    messagesCount: Int,
    historyCount: Int,
    showHistory: Boolean,
    compact: Boolean,
    trailing: @Composable () -> Unit = {}
) {
    val s = LocalStrings.current
    val tabs = if (showHistory) ProfileCardTabsWithHistory else ProfileCardTabsWithoutHistory
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = if (compact) 2.dp else 4.dp, end = if (compact) 3.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExpressiveTabs(
            tabs = tabs,
            selected = selected,
            onSelect = onSelect,
            label = { tab ->
                when (tab) {
                    ProfileCardTab.Messages -> s.profileTabMessages
                    ProfileCardTab.Dossier -> s.profileTabDossier
                    ProfileCardTab.Roles -> s.insights.tabRoles
                    ProfileCardTab.ModHistory -> s.profileTabHistory
                }
            },
            badge = { tab ->
                when (tab) {
                    ProfileCardTab.Messages -> messagesCount
                    ProfileCardTab.ModHistory -> historyCount
                    else -> 0
                }
            },
            fill = false,
            size = ExpressiveTabSize.Compact,
            divider = false,
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}

private val ProfileCardTabsWithHistory = ProfileCardTab.entries.toList()
private val ProfileCardTabsWithoutHistory = ProfileCardTab.entries.filter { it != ProfileCardTab.ModHistory }
