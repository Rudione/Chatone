package io.rudione.chatone.presentation.chat.components

import chatone.composeapp.generated.resources.apply
import chatone.composeapp.generated.resources.cancel
import chatone.composeapp.generated.resources.points_bits_no_rewards
import chatone.composeapp.generated.resources.points_bits_twitch_only_rewards
import chatone.composeapp.generated.resources.points_bits_unavailable_rewards
import chatone.composeapp.generated.resources.points_reward_animated_message
import chatone.composeapp.generated.resources.points_reward_bypass_sub_mode
import chatone.composeapp.generated.resources.points_reward_celebration
import chatone.composeapp.generated.resources.points_reward_chosen_sub_emote
import chatone.composeapp.generated.resources.points_reward_gigantified_emote
import chatone.composeapp.generated.resources.points_reward_highlight_message
import chatone.composeapp.generated.resources.points_reward_modify_sub_emote
import chatone.composeapp.generated.resources.points_reward_random_sub_emote
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.points_balance_description
import chatone.composeapp.generated.resources.points_close
import chatone.composeapp.generated.resources.points_get_bits
import coil3.compose.AsyncImage
import io.rudione.chatone.data.remote.GqlChannelPointReward
import io.rudione.chatone.data.remote.TwitchAutomaticRewardType
import io.rudione.chatone.domain.points.WatchStreak
import io.rudione.chatone.icons.lucide.Gift
import io.rudione.chatone.icons.lucide.Lock
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.components.ChatoneTextField
import io.rudione.chatone.presentation.components.FitText
import io.rudione.chatone.presentation.components.expressive.SwipeDismissSheet
import io.rudione.chatone.presentation.points.WatchStreakCard
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.util.format.groupedNumber
import io.rudione.chatone.util.icons.TwitchBitsIcon
import io.rudione.chatone.util.icons.TwitchPointsIcon
import io.rudione.chatone.util.link.openUrl
import org.jetbrains.compose.resources.stringResource

private val TileMinWidth = 100.dp
private val TileShape = RoundedCornerShape(14.dp)
private val FallbackTile = Color(0xFF6441A5)

@Composable
private fun rewardTitle(reward: GqlChannelPointReward): String {
    return when (reward.automaticType) {
        null -> reward.title
        TwitchAutomaticRewardType.SEND_HIGHLIGHTED_MESSAGE -> stringResource(Res.string.points_reward_highlight_message)
        TwitchAutomaticRewardType.RANDOM_SUB_EMOTE_UNLOCK -> stringResource(Res.string.points_reward_random_sub_emote)
        TwitchAutomaticRewardType.CHOSEN_SUB_EMOTE_UNLOCK -> stringResource(Res.string.points_reward_chosen_sub_emote)
        TwitchAutomaticRewardType.CHOSEN_MODIFIED_SUB_EMOTE_UNLOCK -> stringResource(Res.string.points_reward_modify_sub_emote)
        TwitchAutomaticRewardType.SINGLE_MESSAGE_BYPASS_SUB_MODE -> stringResource(Res.string.points_reward_bypass_sub_mode)
        TwitchAutomaticRewardType.SEND_ANIMATED_MESSAGE -> stringResource(Res.string.points_reward_animated_message)
        TwitchAutomaticRewardType.SEND_GIGANTIFIED_EMOTE -> stringResource(Res.string.points_reward_gigantified_emote)
        TwitchAutomaticRewardType.CELEBRATION -> stringResource(Res.string.points_reward_celebration)
    }
}

@Composable
internal fun CurrencyIcon(pricingType: String, pointsIconUrl: String?, size: Int, tint: Color) {
    when {
        pricingType == "BITS" -> Icon(TwitchBitsIcon, null, tint = tint, modifier = Modifier.size(size.dp))
        !pointsIconUrl.isNullOrBlank() -> AsyncImage(model = pointsIconUrl, contentDescription = null, modifier = Modifier.size(size.dp))
        else -> Icon(TwitchPointsIcon, null, tint = tint, modifier = Modifier.size(size.dp))
    }
}

@Composable
private fun RewardTile(
    reward: GqlChannelPointReward,
    balance: Long,
    pointsIconUrl: String?,
    onRedeem: (String) -> Unit
) {
    var showTextInputPrompt by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    val title = rewardTitle(reward)
    val redeemable = reward.isEnabled && reward.isInStock && reward.isRedeemableInApp
    val affordable = redeemable && (reward.pricingType == "BITS" || balance >= reward.cost)
    val tile = reward.backgroundColor?.let { Color(it) } ?: FallbackTile
    val onTile = if (tile.luminance() > 0.6f) Color(0xFF18181B) else Color.White

    Column(
        modifier = Modifier
            .clip(TileShape)
            .clickable(enabled = affordable, role = Role.Button) {
                if (reward.isUserInputRequired) showTextInputPrompt = true else onRedeem("")
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(TileShape)
                .alpha(if (redeemable) 1f else 0.55f)
                .background(tile)
        ) {
            AsyncImage(
                model = reward.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 22.dp)
                    .size(if (reward.hasCustomImage) 46.dp else 36.dp)
            )
            if (!redeemable || !affordable) {
                Icon(
                    Lucide.Lock,
                    contentDescription = null,
                    tint = onTile.copy(alpha = 0.8f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(7.dp).size(13.dp)
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 6.dp, vertical = 7.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.32f))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CurrencyIcon(reward.pricingType, pointsIconUrl, size = 13, tint = Color.White)
                FitText(
                    groupedNumber(reward.cost),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    minFontSize = 8.sp
                )
            }
        }
        FitText(
            title,
            style = MaterialTheme.typography.labelMedium.copy(lineHeight = 14.sp),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minFontSize = 9.sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
        )
    }

    if (showTextInputPrompt) {
        AlertDialog(
            onDismissRequest = { showTextInputPrompt = false },
            title = { Text(title) },
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
                }) { Text(stringResource(Res.string.apply), style = MaterialTheme.typography.titleMedium) }
            },
            dismissButton = {
                TextButton(onClick = { showTextInputPrompt = false }) {
                    Text(stringResource(Res.string.cancel), style = MaterialTheme.typography.titleMedium)
                }
            }
        )
    }
}

private data class RewardGroups(
    val available: List<GqlChannelPointReward>,
    val twitchOnly: List<GqlChannelPointReward>,
    val unavailable: List<GqlChannelPointReward>
)

private fun groupRewards(rewards: List<GqlChannelPointReward>): RewardGroups {
    val (blocked, enabled) = rewards.partition { !it.isEnabled || !it.isInStock }
    val (available, twitchOnly) = enabled.partition { it.isRedeemableInApp }
    return RewardGroups(available.sortedBy { it.cost }, twitchOnly.sortedBy { it.cost }, blocked.sortedBy { it.cost })
}

private fun LazyGridScope.rewardGroup(
    rewards: List<GqlChannelPointReward>,
    title: String,
    balance: Long,
    pointsIconUrl: String?,
    onRedeem: (GqlChannelPointReward, String) -> Unit
) {
    if (rewards.isEmpty()) return
    item(span = { GridItemSpan(maxLineSpan) }, key = "header_$title", contentType = "header") {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
        )
    }
    items(rewards, key = { it.id }, contentType = { "reward" }) { reward ->
        RewardTile(reward = reward, balance = balance, pointsIconUrl = pointsIconUrl) { text -> onRedeem(reward, text) }
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
    streak: WatchStreak? = null,
    docked: Boolean = false
) {
    if (docked) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            PointsSheetBody(balance, pointsIconUrl, bitsCount, rewards, isLoading, error, onRedeem, onClose, streak, docked = true)
        }
        return
    }
    val sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    SwipeDismissSheet(
        onDismissRequest = onClose,
        modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
        surface = Modifier
            .shadow(24.dp, sheetShape, spotColor = Color.Black.copy(alpha = 0.4f))
            .clip(sheetShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.03f))),
                sheetShape
            ),
        scrimAlpha = 0.35f,
        handleColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    ) { sheet ->
        PointsSheetBody(balance, pointsIconUrl, bitsCount, rewards, isLoading, error, onRedeem, { sheet.close() }, streak, docked = false)
    }
}

@Composable
private fun ColumnScope.PointsSheetBody(
    balance: Long,
    pointsIconUrl: String?,
    bitsCount: Long,
    rewards: List<GqlChannelPointReward>,
    isLoading: Boolean,
    error: String?,
    onRedeem: (GqlChannelPointReward, String) -> Unit,
    onClose: () -> Unit,
    streak: WatchStreak?,
    docked: Boolean
) {
    val pointsBitsTwitchOnlyRewardsText = stringResource(Res.string.points_bits_twitch_only_rewards)
    val pointsBitsUnavailableRewardsText = stringResource(Res.string.points_bits_unavailable_rewards)
    SheetHeader(balance, pointsIconUrl, bitsCount, onClose)
    if (streak != null) {
        WatchStreakCard(streak, Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
    }
    when {
        isLoading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
        }
        error != null -> Text(
            error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(24.dp)
        )
        rewards.isEmpty() -> Row(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Lucide.Gift, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(Res.string.points_bits_no_rewards), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> {
            val groups = remember(rewards) { groupRewards(rewards) }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(TileMinWidth),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().then(if (docked) Modifier.weight(1f) else Modifier.heightIn(max = 420.dp))
            ) {
                items(groups.available, key = { it.id }, contentType = { "reward" }) { reward ->
                    RewardTile(reward = reward, balance = balance, pointsIconUrl = pointsIconUrl) { text -> onRedeem(reward, text) }
                }
                rewardGroup(groups.twitchOnly, pointsBitsTwitchOnlyRewardsText, balance, pointsIconUrl, onRedeem)
                rewardGroup(groups.unavailable, pointsBitsUnavailableRewardsText, balance, pointsIconUrl, onRedeem)
            }
        }
    }
}

@Composable
private fun SheetHeader(balance: Long, pointsIconUrl: String?, bitsCount: Long, onClose: () -> Unit) {
    val formattedBalance = groupedNumber(balance)
    val balanceDescription = stringResource(Res.string.points_balance_description, formattedBalance)
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            Modifier.semantics(mergeDescendants = true) { contentDescription = balanceDescription },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CurrencyIcon("POINTS", pointsIconUrl, size = 22, tint = MaterialTheme.colorScheme.primary)
            Text(formattedBalance, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        val bitsLabel = stringResource(Res.string.points_get_bits)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(CircleShape)
                .clickable(role = Role.Button) {
                    runCatching { openUrl("https://www.twitch.tv/bits", SettingsState.LinkOpenMode.DEFAULT) }
                }
                .semantics { contentDescription = bitsLabel }
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Icon(TwitchBitsIcon, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
            Text(groupedNumber(bitsCount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable(role = Role.Button, onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Lucide.X,
                contentDescription = stringResource(Res.string.points_close),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
