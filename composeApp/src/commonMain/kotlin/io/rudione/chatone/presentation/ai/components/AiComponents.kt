package io.rudione.chatone.presentation.ai.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val AiOkGreen = Color(0xFF4CAF50)

@Composable
fun AiChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingEmoji: String? = null,
    selected: Boolean = false,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurface,
        border = if (selected) null
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (leadingEmoji != null) Text(leadingEmoji, style = MaterialTheme.typography.labelMedium)
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}

@Composable
fun AiCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val container = if (selected) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surfaceContainerHigh
    val border = BorderStroke(
        1.dp,
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    )
    val inner: @Composable () -> Unit = {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = container, border = border, tonalElevation = 3.dp, modifier = modifier.fillMaxWidth()) { inner() }
    } else {
        Surface(shape = shape, color = container, border = border, tonalElevation = 3.dp, modifier = modifier.fillMaxWidth()) { inner() }
    }
}

@Composable
fun AiSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = 2.dp)
    )
}

@Composable
fun AiStatusDot(ok: Boolean, text: String, modifier: Modifier = Modifier) {
    val color = if (ok) AiOkGreen else MaterialTheme.colorScheme.error
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(color = color, shape = CircleShape, modifier = Modifier.size(8.dp)) {}
        Text(text, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AiMonoText(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        SelectionContainer {
            Text(
                text,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}
