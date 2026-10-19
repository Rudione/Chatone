package io.rudione.chatone.presentation.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.repository.AiAssistantController
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Sparkles
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.theme.i18n.format
import org.koin.compose.koinInject

@Composable
internal fun AiHighlightCard(
    proposal: AiHighlightProposal,
    controller: AiAssistantController = koinInject()
) {
    val strings = LocalStrings.current
    val s = strings.aiAssist
    val watch by controller.highlightWatch.collectAsState()
    val active = watch?.criteria == proposal.criteria
    val label = proposal.label.ifBlank { proposal.criteria }
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        color = accent.copy(alpha = if (active) 0.12f else 0.06f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = if (active) 0.4f else 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Lucide.Sparkles,
                contentDescription = null,
                tint = if (active) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                if (active) strings.format(s.highlightActive, label) else "$label · ${s.highlightStopped}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (active) {
                TextButton(onClick = controller::stopHighlight) { Text(s.highlightStop) }
            } else {
                TextButton(onClick = {
                    controller.startHighlight(controller.snapshot.value.activeChannel, proposal.criteria, proposal.label)
                }) { Text(s.highlightResume) }
            }
        }
    }
}
