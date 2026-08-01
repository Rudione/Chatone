package io.rudione.chatone.presentation.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.remote.AiAssistantClient
import io.rudione.chatone.data.repository.AiAssistantController
import io.rudione.chatone.presentation.components.ChatoneSwitch
import io.rudione.chatone.presentation.components.SettingsCard
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import org.koin.compose.koinInject
import io.rudione.chatone.presentation.components.ChatoneTextField

@Composable
fun AiAssistantConfigCard(
    controller: AiAssistantController = koinInject(),
    client: AiAssistantClient = koinInject()
) {
    val s = LocalStrings.current
    val enabled by controller.enabled.collectAsState()

    SettingsCard(title = s.aiConfigTitle) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.aiEnable, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    s.aiEnableDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ChatoneSwitch(checked = enabled, onCheckedChange = { controller.setEnabled(it) })
        }

        io.rudione.chatone.presentation.ai.AiSetupContent(
            controller = controller,
            client = client,
            onDone = {},
            scrollable = false,
            showHeader = false
        )
    }
}

@Composable
fun AiAutoModCard(controller: AiAssistantController = koinInject()) {
    val s = LocalStrings.current
    val autoMod by controller.autoMod.collectAsState()

    SettingsCard(title = s.aiAutoModTitle) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.aiMonitorTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    s.aiMonitorDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ChatoneSwitch(checked = autoMod.enabled, onCheckedChange = { v -> controller.updateAutoMod { it.copy(enabled = v) } })
        }

        if (autoMod.enabled) {
            Text(s.aiSensitivity, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1 to s.aiSensLow, 2 to s.aiSensMed, 3 to s.aiSensHigh).forEach { (level, label) ->
                    FilterChip(
                        selected = autoMod.sensitivity == level,
                        onClick = { controller.updateAutoMod { it.copy(sensitivity = level) } },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Text(
                s.aiActiveHours,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
            HourGrid(autoMod.activeHours) { hour ->
                controller.updateAutoMod {
                    val next = it.activeHours.toMutableSet()
                    if (!next.add(hour)) next.remove(hour)
                    it.copy(activeHours = next)
                }
            }

            ChatoneTextField(
                value = autoMod.extraInstructions,
                onValueChange = { v -> controller.updateAutoMod { it.copy(extraInstructions = v) } },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                label = s.aiExtraInstructions
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HourGrid(active: Set<Int>, onToggle: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        (0..23).forEach { h ->
            val on = h in active
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable { onToggle(h) },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = CircleShape,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            h.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(2.dp))
}
