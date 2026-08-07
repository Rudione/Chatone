package io.rudione.chatone.presentation.window

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.system.AutoUpdater
import kotlinx.coroutines.launch

@Composable
fun UpdateAvailableOverlay() {
    val available by AutoUpdater.available.collectAsState()
    val stage by AutoUpdater.stage.collectAsState()
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    val entry = available ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(16.dp)
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.NewReleases,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    s.updateAvailableTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                "${AutoUpdater.currentVersion()}  →  ${entry.version}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            if (entry.notes.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        .padding(10.dp)
                ) {
                    Text(
                        entry.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
            }

            when (val current = stage) {
                is AutoUpdater.Stage.Downloading -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { current.percent / 100f },
                            modifier = Modifier.fillMaxWidth().height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Text(
                            "${current.percent}%  ·  ${formatMb(current.downloadedBytes)} / ${formatMb(current.totalBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AutoUpdater.Stage.Verifying -> Text(
                    s.updateVerifying,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AutoUpdater.Stage.Installing -> Text(
                    s.updateInstalling,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                is AutoUpdater.Stage.Failed -> Text(
                    "${s.updateFailed}: ${current.reason}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )

                AutoUpdater.Stage.Idle -> Unit
            }

            val busy = stage is AutoUpdater.Stage.Downloading ||
                    stage == AutoUpdater.Stage.Verifying ||
                    stage == AutoUpdater.Stage.Installing

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Spacer(Modifier.weight(1f))
                if (!busy) {
                    TextButton(onClick = { AutoUpdater.skipVersion(entry.version) }) {
                        Text(s.updateSkipVersion)
                    }
                    TextButton(onClick = { AutoUpdater.dismiss() }) {
                        Text(s.updateLater)
                    }
                }
                Button(
                    enabled = !busy,
                    onClick = { scope.launch { AutoUpdater.downloadAndUpdate(entry.release) } }
                ) {
                    Text(s.updateNow)
                }
            }
        }
    }
}

private fun formatMb(bytes: Long): String =
    if (bytes <= 0) "—" else "${(bytes / 1024 / 1024.0 * 10).toInt() / 10.0} MB"
