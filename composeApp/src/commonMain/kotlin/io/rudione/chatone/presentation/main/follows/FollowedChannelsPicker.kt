package io.rudione.chatone.presentation.main.follows

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.follow_picker_already_open
import chatone.composeapp.generated.resources.follow_picker_clear
import chatone.composeapp.generated.resources.follow_picker_empty
import chatone.composeapp.generated.resources.follow_picker_failed
import chatone.composeapp.generated.resources.follow_picker_nothing_found
import chatone.composeapp.generated.resources.follow_picker_offline
import chatone.composeapp.generated.resources.follow_picker_pick_live
import chatone.composeapp.generated.resources.follow_picker_retry
import chatone.composeapp.generated.resources.follow_picker_search_placeholder
import chatone.composeapp.generated.resources.follow_picker_signed_out
import chatone.composeapp.generated.resources.follow_picker_viewers
import org.jetbrains.compose.resources.stringResource
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.follow.FollowedChannel
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Search
import io.rudione.chatone.presentation.chat.compactCount
import io.rudione.chatone.presentation.components.ChatoneTextField
import io.rudione.chatone.presentation.components.ExpressiveCheckbox
import io.rudione.chatone.presentation.components.expressive.pressSquish
import io.rudione.chatone.presentation.theme.ChatoneTheme

@Composable
fun FollowedChannelsPicker(state: FollowedPickerState, modifier: Modifier = Modifier) {
    Column(modifier.padding(start = 10.dp, end = 10.dp, top = 2.dp, bottom = 8.dp)) {
        ChatoneTextField(
            value = state.query,
            onValueChange = state::updateQuery,
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(Res.string.follow_picker_search_placeholder),
            leading = {
                Icon(
                    Lucide.Search,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true
        )
        AnimatedVisibility(visible = state.status == FollowedPickerStatus.Ready && (state.hasLiveToPick || state.selectedCount > 0)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (state.hasLiveToPick) PickerChip(stringResource(Res.string.follow_picker_pick_live), onClick = state::pickLive)
                if (state.selectedCount > 0) PickerChip(stringResource(Res.string.follow_picker_clear), onClick = state::clear)
            }
        }
        Spacer(Modifier.height(6.dp))
        AnimatedContent(
            targetState = state.status,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "followedPicker"
        ) { status ->
            when (status) {
                FollowedPickerStatus.Loading -> Box(
                    Modifier.fillMaxWidth().height(96.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) }

                FollowedPickerStatus.SignedOut -> PickerMessage(stringResource(Res.string.follow_picker_signed_out), null, null)
                FollowedPickerStatus.Failed -> PickerMessage(stringResource(Res.string.follow_picker_failed), stringResource(Res.string.follow_picker_retry)) { state.load(force = true) }
                FollowedPickerStatus.Ready -> FollowedList(state)
            }
        }
    }
}

@Composable
private fun FollowedList(state: FollowedPickerState) {
    val channels = state.visible
    if (channels.isEmpty()) {
        PickerMessage(if (state.channels.isEmpty()) stringResource(Res.string.follow_picker_empty) else stringResource(Res.string.follow_picker_nothing_found), null, null)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(channels, key = { it.login }, contentType = { "followed" }) { channel ->
            FollowedRow(
                channel = channel,
                selected = state.isSelected(channel.login),
                open = state.isOpen(channel.login),
                onToggle = { state.toggle(channel.login) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun FollowedRow(
    channel: FollowedChannel,
    selected: Boolean,
    open: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val tint by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
        tween(200),
        label = "followedRowTint"
    )
    val dim by animateFloatAsState(if (open) 0.5f else 1f, tween(200), label = "followedRowDim")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressSquish(interaction)
            .graphicsLayer { alpha = dim }
            .clip(RoundedCornerShape(12.dp))
            .background(tint)
            .clickable(interactionSource = interaction, indication = null, enabled = !open, onClick = onToggle)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExpressiveCheckbox(checked = selected || open, onCheckedChange = null, boxSize = 18)
        Spacer(Modifier.width(10.dp))
        FollowedAvatar(channel)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                channel.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                when {
                    open -> stringResource(Res.string.follow_picker_already_open)
                    channel.isLive -> listOfNotNull(
                        channel.liveCategory,
                        stringResource(Res.string.follow_picker_viewers, compactCount(channel.liveViewers ?: 0))
                    ).joinToString(" · ")
                    else -> stringResource(Res.string.follow_picker_offline)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FollowedAvatar(channel: FollowedChannel) {
    val live = ChatoneTheme.extraColors.live
    Box(contentAlignment = Alignment.BottomEnd) {
        val ring = if (channel.isLive) Modifier.border(2.dp, live, CircleShape).padding(3.dp) else Modifier
        Box(Modifier.size(36.dp).then(ring).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
            if (channel.avatarUrl.isNotEmpty()) {
                AsyncImage(model = channel.avatarUrl, contentDescription = null, modifier = Modifier.size(36.dp))
            }
        }
        AnimatedVisibility(visible = channel.isLive, enter = scaleIn(), exit = scaleOut()) {
            Box(
                Modifier.size(11.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(live)
            )
        }
    }
}

@Composable
private fun PickerChip(text: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .pressSquish(interaction)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PickerMessage(text: String, action: String?, onAction: (() -> Unit)?) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}
