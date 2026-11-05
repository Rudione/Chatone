package io.rudione.chatone.presentation.main

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.browse_live
import chatone.composeapp.generated.resources.cancel
import chatone.composeapp.generated.resources.follow_picker_option_desc
import chatone.composeapp.generated.resources.follow_picker_option_title
import chatone.composeapp.generated.resources.main_channel_name_placeholder
import chatone.composeapp.generated.resources.ok
import chatone.composeapp.generated.resources.open_channel_automod_desc
import chatone.composeapp.generated.resources.open_channel_automod_label
import chatone.composeapp.generated.resources.open_channel_channel_desc
import chatone.composeapp.generated.resources.open_channel_channel_label
import chatone.composeapp.generated.resources.open_channel_live_desc
import chatone.composeapp.generated.resources.open_channel_live_label
import chatone.composeapp.generated.resources.open_channel_title
import org.jetbrains.compose.resources.stringResource
import io.rudione.chatone.presentation.main.follows.followPickerConfirm
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.LaunchedEffect
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
import io.rudione.chatone.domain.follow.FollowedChannel
import io.rudione.chatone.domain.model.Channel
import io.rudione.chatone.presentation.main.follows.FollowedChannelsPicker
import io.rudione.chatone.presentation.main.follows.rememberFollowedPickerState
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.components.ChatoneTextField
import io.rudione.chatone.icons.lucide.Hash
import io.rudione.chatone.icons.lucide.Heart
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Radio
import io.rudione.chatone.icons.lucide.Shield

enum class OpenChannelTab { CHANNEL, WHISPERS, MENTIONS, WATCHING, LIVE, AUTOMOD, FOLLOWING }

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
    onDismiss: () -> Unit,
    openLogins: Set<String> = emptySet(),
    onFollowedSelected: (List<FollowedChannel>) -> Unit = {}
) {
    val openChannelChannelLabelText = stringResource(Res.string.open_channel_channel_label)
    val openChannelChannelDescText = stringResource(Res.string.open_channel_channel_desc)
    val openChannelLiveLabelText = stringResource(Res.string.open_channel_live_label)
    val openChannelLiveDescText = stringResource(Res.string.open_channel_live_desc)
    val openChannelAutomodLabelText = stringResource(Res.string.open_channel_automod_label)
    val openChannelAutomodDescText = stringResource(Res.string.open_channel_automod_desc)
    val followPickerOptionTitleText = stringResource(Res.string.follow_picker_option_title)
    val followPickerOptionDescText = stringResource(Res.string.follow_picker_option_desc)
    var selected by remember { mutableStateOf(OpenChannelTab.CHANNEL) }
    val followed = rememberFollowedPickerState(openLogins)
    LaunchedEffect(selected) { if (selected == OpenChannelTab.FOLLOWING) followed.load() }

    val options = remember(openChannelChannelLabelText, followPickerOptionTitleText) {
        listOf(
            TabOption(
                OpenChannelTab.CHANNEL,
                openChannelChannelLabelText,
                openChannelChannelDescText,
                Lucide.Hash
            ),
            TabOption(
                OpenChannelTab.LIVE,
                openChannelLiveLabelText,
                openChannelLiveDescText,
                Lucide.Radio
            ),
            TabOption(
                OpenChannelTab.AUTOMOD,
                openChannelAutomodLabelText,
                openChannelAutomodDescText,
                Lucide.Shield
            ),
            TabOption(
                OpenChannelTab.FOLLOWING,
                followPickerOptionTitleText,
                followPickerOptionDescText,
                Lucide.Heart
            )
        )
    }

    fun confirm() {
        when (selected) {
            OpenChannelTab.CHANNEL ->
                if (query.isNotBlank()) onChannelSelected(
                    query.trim().lowercase().removePrefix("#"), "", ""
                )

            OpenChannelTab.FOLLOWING -> followed.picked().takeIf { it.isNotEmpty() }
                ?.let(onFollowedSelected)

            else -> onOpenSpecialTab(selected)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 14.dp,
            modifier = Modifier.width(340.dp)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    stringResource(Res.string.open_channel_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEach { opt ->
                        TabRow(
                            option = opt,
                            selected = selected == opt.tab,
                            onSelect = { selected = opt.tab },
                            searchSlot = when {
                                opt.tab != selected -> null
                                opt.tab == OpenChannelTab.CHANNEL -> {
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
                                }

                                opt.tab == OpenChannelTab.FOLLOWING -> {
                                    { FollowedChannelsPicker(followed) }
                                }

                                else -> null
                            }
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
                    ) { Text(stringResource(Res.string.cancel)) }
                    Button(
                        onClick = { confirm() },
                        enabled = when (selected) {
                            OpenChannelTab.CHANNEL -> query.isNotBlank()
                            OpenChannelTab.FOLLOWING -> followed.selectedCount > 0
                            else -> true
                        },
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)
                    ) {
                        AnimatedContent(
                            targetState = if (selected == OpenChannelTab.FOLLOWING) {
                                followPickerConfirm(followed.selectedCount)
                            } else stringResource(Res.string.ok),
                            transitionSpec = { fadeIn(tween(140)) togetherWith fadeOut(tween(100)) },
                            label = "openChannelConfirm"
                        ) { Text(it) }
                    }
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
    val bg =
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.07f) else Color.Transparent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
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
        AnimatedVisibility(
            visible = searchSlot != null,
            enter = expandVertically(tween(220)) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(120))
        ) {
            searchSlot?.invoke()
        }
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
    Column(Modifier.padding(start = 10.dp, end = 10.dp, top = 2.dp, bottom = 8.dp)) {
        ChatoneTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(Res.string.main_channel_name_placeholder),
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
                if (query.isNotBlank()) onChannelSelected(
                    query.trim().lowercase().removePrefix("#"), "", ""
                )
            })
        )
        if (searchResults.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 190.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(searchResults, key = { it.login }, contentType = { "channel" }) { channel ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onChannelSelected(
                                    channel.login,
                                    channel.profileImageUrl,
                                    channel.displayName
                                )
                            }
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
                            Text(
                                channel.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (channel.gameName.isNotEmpty()) {
                                Text(
                                    channel.gameName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (channel.isLive) {
                            Surface(
                                color = ChatoneTheme.extraColors.live,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    stringResource(Res.string.browse_live),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
