package io.rudione.chatone.presentation.chat

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.insights_tab_roles
import chatone.composeapp.generated.resources.profile_tab_dossier
import chatone.composeapp.generated.resources.profile_tab_history
import chatone.composeapp.generated.resources.profile_tab_messages
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.components.expressive.ExpressiveTabSize
import io.rudione.chatone.presentation.components.expressive.ExpressiveTabs

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
    val profileTabMessagesText = stringResource(Res.string.profile_tab_messages)
    val profileTabDossierText = stringResource(Res.string.profile_tab_dossier)
    val insightsTabRolesText = stringResource(Res.string.insights_tab_roles)
    val profileTabHistoryText = stringResource(Res.string.profile_tab_history)
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
                    ProfileCardTab.Messages -> profileTabMessagesText
                    ProfileCardTab.Dossier -> profileTabDossierText
                    ProfileCardTab.Roles -> insightsTabRolesText
                    ProfileCardTab.ModHistory -> profileTabHistoryText
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
