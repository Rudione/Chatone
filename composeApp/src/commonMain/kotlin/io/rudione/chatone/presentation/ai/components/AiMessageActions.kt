package io.rudione.chatone.presentation.ai.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.components.ChatoneIconButton

@Composable
fun AiMessageActions(
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    onRegenerate: (() -> Unit)? = null,
    variantCount: Int = 1,
    variantIndex: Int = 0,
    onPrevVariant: (() -> Unit)? = null,
    onNextVariant: (() -> Unit)? = null
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        ActionIcon(Icons.Outlined.ContentCopy, onClick = onCopy)
        if (onRegenerate != null) {
            ActionIcon(Icons.Outlined.Refresh, onClick = onRegenerate)
        }
        if (variantCount > 1) {
            ActionIcon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                enabled = variantIndex > 0,
                onClick = { onPrevVariant?.invoke() }
            )
            Text(
                "${variantIndex + 1}/$variantCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ActionIcon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                enabled = variantIndex < variantCount - 1,
                onClick = { onNextVariant?.invoke() }
            )
        }
    }
}

@Composable
private fun ActionIcon(icon: ImageVector, onClick: () -> Unit, enabled: Boolean = true) {
    ChatoneIconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(30.dp)) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        )
    }
}
