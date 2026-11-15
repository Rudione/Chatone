package io.rudione.chatone.presentation.components

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.media_cancel
import chatone.composeapp.generated.resources.media_link_leads
import chatone.composeapp.generated.resources.media_link_open
import chatone.composeapp.generated.resources.media_link_title
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.icons.lucide.ExternalLink
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.util.link.httpUrlHost

@Composable
fun LinkConfirmDialog(url: String, onOpen: () -> Unit, onDismiss: () -> Unit) {
    val host = httpUrlHost(url)?.removePrefix("www.") ?: url
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val reveal by animateFloatAsState(if (entered) 1f else 0f, tween(200), label = "linkDialogReveal")
    AlertDialog(
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 10.dp, bottomEnd = 30.dp, bottomStart = 10.dp),
        modifier = Modifier.graphicsLayer {
            alpha = reveal
            scaleX = 0.94f + 0.06f * reveal
            scaleY = scaleX
            translationY = (1f - reveal) * 12.dp.toPx()
        },
        onDismissRequest = onDismiss,
        icon = { Icon(Lucide.ExternalLink, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(Res.string.media_link_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(Res.string.media_link_leads), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        host,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(10.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onOpen()
            }) { Text(stringResource(Res.string.media_link_open)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.media_cancel)) } }
    )
}
