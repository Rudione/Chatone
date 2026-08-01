package io.rudione.chatone.presentation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.util.chat.SlashCommand

@Composable
fun SlashCommandSuggestionsRow(
    commands: List<SlashCommand.CommandInfo>,
    selectedIndex: Int,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (commands.isEmpty()) return
    val listState = rememberLazyListState()
    LaunchedEffect(selectedIndex) {
        if (selectedIndex in commands.indices) {
            listState.animateScrollToItem(selectedIndex)
        }
    }
    LiquidGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp),
        backgroundAlphaHigh = 0.55f,
        backgroundAlphaLow = 0.40f,
        borderAlphaHigh = 0.25f,
        borderAlphaLow = 0.12f
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .padding(vertical = 4.dp)
        ) {
            items(commands, key = { it.name }) { cmd ->
                val isSelected = commands.getOrNull(selectedIndex)?.name == cmd.name
                val selectedBg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(selectedBg)
                        .clickable { onPick(cmd.name) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        cmd.usage,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.widthIn(min = 110.dp, max = 240.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        cmd.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
