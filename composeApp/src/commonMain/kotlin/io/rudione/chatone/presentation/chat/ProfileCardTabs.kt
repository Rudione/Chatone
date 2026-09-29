package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color

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
    Row(
        modifier = Modifier.fillMaxWidth().padding(end = if (compact) 3.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f)
                .then(if (compact) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = if (compact) 5.dp else 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 1.dp else 3.dp)
        ) {
            ProfileCardTabChip(
                label = s.profileTabMessages,
                badge = messagesCount,
                selected = selected == ProfileCardTab.Messages,
                compact = compact,
                onClick = { onSelect(ProfileCardTab.Messages) }
            )
            ProfileCardTabChip(
                label = s.profileTabDossier,
                badge = 0,
                selected = selected == ProfileCardTab.Dossier,
                compact = compact,
                onClick = { onSelect(ProfileCardTab.Dossier) }
            )
            ProfileCardTabChip(
                label = s.insights.tabRoles,
                badge = 0,
                selected = selected == ProfileCardTab.Roles,
                compact = compact,
                onClick = { onSelect(ProfileCardTab.Roles) }
            )
            if (showHistory) {
                ProfileCardTabChip(
                    label = s.profileTabHistory,
                    badge = historyCount,
                    selected = selected == ProfileCardTab.ModHistory,
                    compact = compact,
                    onClick = { onSelect(ProfileCardTab.ModHistory) }
                )
            }
        }
        trailing()
    }
}

@Composable
private fun ProfileCardTabChip(
    label: String,
    badge: Int,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 5.dp else 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            label,
            style = if (compact) MaterialTheme.typography.labelSmall
            else MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (badge > 0) {
            Text(
                if (badge > 999) "999+" else "$badge",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
