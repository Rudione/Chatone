package io.rudione.chatone.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.ChatHighlightColors

data class HighlightColorEntry(
    val label: String,
    val description: String,
    val getValue: (ChatHighlightColors) -> Long,
    val setValue: (ChatHighlightColors, Long) -> ChatHighlightColors
)

private val ENTRIES = listOf(
    HighlightColorEntry("Упоминание (фон)", "@ник подсвечивается этим цветом",
        { it.mentionBg }, { c, v -> c.copy(mentionBg = v) }),
    HighlightColorEntry("Упоминание (полоса)", "Левая полоска у @упоминаний",
        { it.mentionAccent }, { c, v -> c.copy(mentionAccent = v) }),
    HighlightColorEntry("Подсветка (ключевые слова)", "Фон для сработавших правил выделения",
        { it.highlightBg }, { c, v -> c.copy(highlightBg = v) }),
    HighlightColorEntry("Первое сообщение", "Цвет бейджа «FM» и фон первого сообщения",
        { it.firstMessageColor }, { c, v -> c.copy(firstMessageColor = v) }),
    HighlightColorEntry("Поиск (совпадение)", "Фон найденного сообщения при Ctrl+F",
        { it.searchMatchBg }, { c, v -> c.copy(searchMatchBg = v) }),
)

@Composable
fun ChatHighlightColorsCard(
    colors: ChatHighlightColors,
    onChange: (ChatHighlightColors) -> Unit
) {
    var pickerEntry by remember { mutableStateOf<HighlightColorEntry?>(null) }
    var pickerCurrentHex by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        ENTRIES.forEach { entry ->
            val colorLong = entry.getValue(colors)
            val color = Color(colorLong)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        pickerEntry = entry
                        pickerCurrentHex = colorLong.toHex()
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(entry.description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    colorLong.toHexString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }

    pickerEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { pickerEntry = null },
            title = { Text(entry.label) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(entry.description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                    val previewColor = runCatching { Color(pickerCurrentHex.fromHex()) }.getOrElse { Color(entry.getValue(colors)) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(previewColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Предпросмотр", style = MaterialTheme.typography.labelMedium,
                            color = if (previewColor.luminance() > 0.4f) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.8f))
                    }

                    OutlinedTextField(
                        value = pickerCurrentHex,
                        onValueChange = { v ->
                            if (v.length <= 8) pickerCurrentHex = v.filter { it.isLetterOrDigit() }.uppercase()
                        },
                        label = { Text("AARRGGBB (hex)") },
                        placeholder = { Text("FFFF6B6B") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val palette = listOf(
                        0xFFFF6B6B, 0xFFFFD700, 0xFF4FC3F7, 0xFF9B59B6,
                        0xFF2ECC71, 0xFFFF8C00, 0xFF7B2FBE, 0xFF00BCD4,
                        0xFFE91E63, 0xFF607D8B, 0xFF8BC34A, 0xFFFF5722
                    )
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(palette.size) { i ->
                            val c = palette[i]
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .border(
                                        if (pickerCurrentHex == c.toHex()) 2.dp else 0.dp,
                                        MaterialTheme.colorScheme.onSurface,
                                        CircleShape
                                    )
                                    .clickable { pickerCurrentHex = c.toHex() }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newLong = runCatching { pickerCurrentHex.fromHex() }.getOrNull()
                    if (newLong != null) onChange(entry.setValue(colors, newLong))
                    pickerEntry = null
                }) { Text("Применить") }
            },
            dismissButton = {
                TextButton(onClick = { pickerEntry = null }) { Text("Отмена") }
            }
        )
    }
}

private fun Long.toHex(): String = toString(16).uppercase().padStart(8, '0')
private fun Long.toHexString(): String = "#${toString(16).uppercase().padStart(6, '0').takeLast(6)}"
private fun String.fromHex(): Long = trimStart('#').toLong(16).let {
    if (length <= 6) it or 0xFF000000L else it
}
private fun Color.luminance(): Float = (red * 0.299f + green * 0.587f + blue * 0.114f)
