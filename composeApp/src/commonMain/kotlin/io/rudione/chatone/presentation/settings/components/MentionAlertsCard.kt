package io.rudione.chatone.presentation.settings.components

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.settings_mention_alerts
import chatone.composeapp.generated.resources.settings_mention_alerts_desc
import chatone.composeapp.generated.resources.settings_notifications_blocked
import chatone.composeapp.generated.resources.settings_open_notification_settings
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.repository.NotificationPreferencesRepository
import io.rudione.chatone.presentation.components.ChatoneButton
import io.rudione.chatone.presentation.components.ChatoneButtonTone
import io.rudione.chatone.presentation.components.ChatoneSwitch
import io.rudione.chatone.util.system.NotificationAccess
import org.koin.compose.koinInject

@Composable
internal fun MentionAlertsCard(preferences: NotificationPreferencesRepository = koinInject()) {
    val enabled by preferences.mentionAlerts.collectAsState()
    val permitted by NotificationAccess.granted.collectAsState()
    SettingsSurface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.settings_mention_alerts),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(Res.string.settings_mention_alerts_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ChatoneSwitch(
                    checked = enabled,
                    onCheckedChange = { checked ->
                        preferences.setMentionAlerts(checked)
                        if (checked && !permitted) NotificationAccess.request()
                    }
                )
            }
            if (enabled && !permitted) {
                Text(
                    stringResource(Res.string.settings_notifications_blocked),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                ChatoneButton(
                    text = stringResource(Res.string.settings_open_notification_settings),
                    onClick = NotificationAccess::openSystemSettings,
                    tone = ChatoneButtonTone.Neutral
                )
            }
        }
    }
}
