package io.rudione.chatone.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.model.Channel
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.components.ChatoneTextField

enum class OpenChannelTab { CHANNEL, WHISPERS, MENTIONS, WATCHING, LIVE, AUTOMOD }

private data class TabOption(
    val tab: OpenChannelTab,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@Composable
fun OpenChannelDialog(
    query: String,
    searchResults: List<Channel>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onChannelSelected: (String, String, String) -> Unit,
    onOpenSpecialTab: (OpenChannelTab) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current
    var selected by remember { mutableStateOf(OpenChannelTab.CHANNEL) }

    val options = remember(s) {
        listOf(
            TabOption(OpenChannelTab.CHANNEL, s.openChannelChannelLabel, s.openChannelChannelDesc, Icons.Outlined.Tag),
            TabOption(OpenChannelTab.LIVE, s.openChannelLiveLabel, s.openChannelLiveDesc, Icons.Outlined.Wifi),
            TabOption(OpenChannelTab.AUTOMOD, s.openChannelAutomodLabel, s.openChannelAutomodDesc, Icons.Outlined.Shield)
        )
    }

    fun confirm() {
        if (selected == OpenChannelTab.CHANNEL) {
            if (query.isNotBlank()) onChannelSelected(query.trim().lowercase().removePrefix("#"), "", "")
        } else {
            onOpenSpecialTab(selected)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 14.dp,
            modifier = Modifier.width(340.dp)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(s.openChannelTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEach { opt ->
                        TabRow(
                            option = opt,
                            selected = selected == opt.tab,
                            onSelect = { selected = opt.tab },
                            searchSlot = if (opt.tab == OpenChannelTab.CHANNEL && selected == OpenChannelTab.CHANNEL) {
                                {
                                    ChannelSearchInline(
                                        query = query,
                                        searchResults = searchResults,
                                        isSearching = isSearching,
                                        onQueryChange = onQueryChange,
                                        onSearch = onSearch,
                                        onChannelSelected = onChannelSelected
                                    )
                                }
                            } else null
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) { Text(s.cancel) }
                    Button(
                        onClick = { confirm() },
                        enabled = selected != OpenChannelTab.CHANNEL || query.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)
                    ) { Text(s.ok) }
                }
            }
        }
    }
}

@Composable
private fun TabRow(
    option: TabOption,
    selected: Boolean,
    onSelect: () -> Unit,
    searchSlot: (@Composable () -> Unit)? = null
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    else MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.07f) else Color.Transparent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RadioButton(selected = selected, onClick = onSelect, modifier = Modifier.size(28.dp))
            Icon(
                option.icon, null,
                modifier = Modifier.size(15.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    option.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    option.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (searchSlot != null) searchSlot()
    }
}

@Composable
private fun ChannelSearchInline(
    query: String,
    searchResults: List<Channel>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onChannelSelected: (String, String, String) -> Unit
) {
    val s = LocalStrings.current
    Column(Modifier.padding(start = 10.dp, end = 10.dp, top = 2.dp, bottom = 8.dp)) {
        ChatoneTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = s.mainChannelNamePlaceholder,
            leading = {
                Text(
                    "#",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            trailing = {
                if (isSearching) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = {
                if (query.isNotBlank()) onChannelSelected(query.trim().lowercase().removePrefix("#"), "", "")
            })
        )
        if (searchResults.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 190.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(searchResults) { channel ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onChannelSelected(channel.login, channel.profileImageUrl, channel.displayName) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (channel.profileImageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = channel.profileImageUrl,
                                contentDescription = null,
                                modifier = Modifier.size(30.dp).clip(CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(channel.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            if (channel.gameName.isNotEmpty()) {
                                Text(channel.gameName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (channel.isLive) {
                            Surface(color = ChatoneTheme.extraColors.live, shape = RoundedCornerShape(4.dp)) {
                                Text("LIVE", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
