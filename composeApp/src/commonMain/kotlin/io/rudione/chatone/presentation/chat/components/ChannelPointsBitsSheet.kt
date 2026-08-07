package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.rudione.chatone.data.remote.GqlChannelPointReward
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.icons.TwitchBitsIcon
import io.rudione.chatone.util.icons.TwitchPointsIcon
import io.rudione.chatone.util.link.openUrl
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneTextField

private fun rewardTileColor(rewardId: String): Color {
    val hash = rewardId.hashCode()
    val hue = (hash.mod(360)).toFloat()
    return Color.hsv(hue, 0.55f, 0.55f)
}

@Composable
private fun RewardTile(
    reward: GqlChannelPointReward,
    balance: Long,
    onRedeem: (String) -> Unit
) {
    var showTextInputPrompt by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    val affordable = balance >= reward.cost && reward.isEnabled && reward.isInStock

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(rewardTileColor(reward.id).copy(alpha = if (affordable) 0.9f else 0.35f))
            .clickable(enabled = affordable) {
                if (reward.isUserInputRequired) showTextInputPrompt = true
                else onRedeem("")
            }
            .padding(7.dp)
            .aspectRatio(1f)
    ) {
        val hasImage = !reward.imageUrl.isNullOrBlank()
        if (hasImage) {
            AsyncImage(
                model = reward.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(28.dp).align(Alignment.TopCenter).padding(top = 4.dp)
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                reward.title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = if (hasImage) 1 else 3,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (reward.pricingType == "BITS") TwitchBitsIcon else TwitchPointsIcon,
                    contentDescription = null,
                    modifier = Modifier.size(11.dp),
                    tint = Color.White
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    reward.cost.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }

    if (showTextInputPrompt) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTextInputPrompt = false },
            title = { Text(reward.title) },
            text = {
                ChatoneTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = reward.prompt.ifBlank { "" },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showTextInputPrompt = false
                    onRedeem(textInput)
                    textInput = ""
                }) { Text(LocalStrings.current.pollCreateSubmit) }
            },
            dismissButton = {
                TextButton(onClick = { showTextInputPrompt = false }) { Text(LocalStrings.current.cancel) }
            }
        )
    }
}

@Composable
internal fun ChannelPointsBitsSheet(
    balance: Long,
    pointsIconUrl: String? = null,
    bitsCount: Long = 0L,
    rewards: List<GqlChannelPointReward>,
    isLoading: Boolean,
    error: String?,
    onRedeem: (GqlChannelPointReward, String) -> Unit,
    onClose: () -> Unit,
    docked: Boolean = false
) {
    val s = LocalStrings.current
    val backdropSource = remember { MutableInteractionSource() }
    val sheetShape =
        if (docked) RoundedCornerShape(0.dp)
        else RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    Box(modifier = Modifier.fillMaxSize()) {
        if (!docked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(indication = null, interactionSource = backdropSource) { onClose() }
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .then(if (docked) Modifier.fillMaxHeight() else Modifier.heightIn(max = 460.dp))
                .then(
                    if (docked) Modifier else Modifier.shadow(
                        24.dp, sheetShape,
                        ambientColor = Color.Black.copy(alpha = 0.3f),
                        spotColor = Color.Black.copy(alpha = 0.4f)
                    )
                )
                .clip(sheetShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.99f),
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                )
                .then(
                    if (docked) Modifier else Modifier.border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.04f))
                        ),
                        sheetShape
                    )
                )
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!pointsIconUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = pointsIconUrl,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        TwitchPointsIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    balance.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(100))
                        .clickable {
                            runCatching {
                                openUrl(
                                    "https://www.twitch.tv/bits",
                                    io.rudione.chatone.presentation.settings.SettingsState.LinkOpenMode.DEFAULT
                                )
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        TwitchBitsIcon,
                        contentDescription = s.pointsBitsGetBits,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        bitsCount.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.weight(1f))
                ChatoneIconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }

            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(modifier = Modifier.size(28.dp)) }

                error != null -> Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                rewards.isEmpty() -> Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.CardGiftcard,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            s.pointsBitsNoRewards,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    val (availableRewards, unavailableRewards) = rewards.partition { it.isEnabled && it.isInStock }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        contentPadding = PaddingValues(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)
                    ) {
                        items(availableRewards, key = { it.id }) { reward ->
                            RewardTile(reward = reward, balance = balance) { text ->
                                onRedeem(reward, text)
                            }
                        }
                        if (unavailableRewards.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                    Text(
                                        s.pointsBitsUnavailableRewards,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                    )
                                }
                            }
                            items(unavailableRewards, key = { it.id }) { reward ->
                                RewardTile(reward = reward, balance = balance) { text ->
                                    onRedeem(reward, text)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
