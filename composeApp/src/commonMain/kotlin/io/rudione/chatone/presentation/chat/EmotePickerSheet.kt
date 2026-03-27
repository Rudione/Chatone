package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.EmoteProvider
import io.rudione.chatone.domain.model.GenericEmote
import io.rudione.chatone.presentation.theme.ChatoneTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotePickerSheet(
    emotes: List<GenericEmote>,
    onEmoteSelected: (GenericEmote) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    data class ProviderTab(val label: String, val provider: EmoteProvider?, val count: Int)

    val tabs = remember(emotes) {
        val all = emotes.size
        val s7 = emotes.count { it.provider == EmoteProvider.SEVEN_TV }
        val bt = emotes.count { it.provider == EmoteProvider.BTTV }
        val fz = emotes.count { it.provider == EmoteProvider.FFZ }
        val tw = emotes.count { it.provider == EmoteProvider.TWITCH }
        buildList {
            add(ProviderTab("All ($all)", null, all))
            if (tw > 0) add(ProviderTab("Twitch", EmoteProvider.TWITCH, tw))
            if (s7 > 0) add(ProviderTab("7TV", EmoteProvider.SEVEN_TV, s7))
            if (bt > 0) add(ProviderTab("BTTV", EmoteProvider.BTTV, bt))
            if (fz > 0) add(ProviderTab("FFZ", EmoteProvider.FFZ, fz))
        }
    }

    val filteredEmotes = remember(emotes, searchQuery, selectedTab, tabs) {
        var filtered = emotes
        val tab = tabs.getOrNull(selectedTab)
        if (tab?.provider != null) {
            filtered = filtered.filter { it.provider == tab.provider }
        }
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter { it.code.contains(searchQuery, ignoreCase = true) }
        }
        filtered
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
        ) {
            // ── Search bar ──────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = {
                    Text(
                        "Search emotes...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                textStyle = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Provider tabs ───────────────────────────────────
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 12.dp,
                divider = {}
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // ── Emote grid ──────────────────────────────────────
            if (filteredEmotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No emotes match \"$searchQuery\""
                            else "No emotes loaded yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        if (searchQuery.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            TextButton(onClick = { searchQuery = "" }) {
                                Text("Clear search")
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(56.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 120.dp, max = 320.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filteredEmotes, key = { "${it.provider}_${it.id}" }) { emote ->
                        EmoteGridItem(
                            emote = emote,
                            onClick = { onEmoteSelected(emote) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmoteGridItem(
    emote: GenericEmote,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedEmoteImage(
            url = emote.url2x,
            contentDescription = emote.code,
            modifier = Modifier.size(36.dp)
        )
        Text(
            text = emote.code,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
