package io.rudione.chatone.presentation.settings.components

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.ai_active_hours
import chatone.composeapp.generated.resources.ai_auto_mod_title
import chatone.composeapp.generated.resources.ai_config_title
import chatone.composeapp.generated.resources.ai_enable
import chatone.composeapp.generated.resources.ai_enable_desc
import chatone.composeapp.generated.resources.ai_extra_instructions
import chatone.composeapp.generated.resources.ai_monitor_desc
import chatone.composeapp.generated.resources.ai_monitor_title
import chatone.composeapp.generated.resources.ai_sens_high
import chatone.composeapp.generated.resources.ai_sens_low
import chatone.composeapp.generated.resources.ai_sens_med
import chatone.composeapp.generated.resources.ai_sensitivity
import org.jetbrains.compose.resources.stringResource
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
import io.rudione.chatone.presentation.ai.AiSetupContent
import io.rudione.chatone.presentation.components.ChatoneSwitch
import io.rudione.chatone.presentation.components.SettingsCard
import org.koin.compose.koinInject
import io.rudione.chatone.presentation.components.ChatoneTextField

@Composable
fun AiAssistantConfigCard(
    controller: AiAssistantController = koinInject(),
    client: AiAssistantClient = koinInject()
) {
    val enabled by controller.enabled.collectAsState()

    SettingsCard(title = stringResource(Res.string.ai_config_title)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.ai_enable), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    stringResource(Res.string.ai_enable_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ChatoneSwitch(checked = enabled, onCheckedChange = { controller.setEnabled(it) })
        }

        AiSetupContent(
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
    val autoMod by controller.autoMod.collectAsState()

    SettingsCard(title = stringResource(Res.string.ai_auto_mod_title)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.ai_monitor_title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    stringResource(Res.string.ai_monitor_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ChatoneSwitch(checked = autoMod.enabled, onCheckedChange = { v -> controller.updateAutoMod { it.copy(enabled = v) } })
        }

        if (autoMod.enabled) {
            Text(stringResource(Res.string.ai_sensitivity), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1 to stringResource(Res.string.ai_sens_low), 2 to stringResource(Res.string.ai_sens_med), 3 to stringResource(Res.string.ai_sens_high)).forEach { (level, label) ->
                    FilterChip(
                        selected = autoMod.sensitivity == level,
                        onClick = { controller.updateAutoMod { it.copy(sensitivity = level) } },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Text(
                stringResource(Res.string.ai_active_hours),
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
                label = stringResource(Res.string.ai_extra_instructions)
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
