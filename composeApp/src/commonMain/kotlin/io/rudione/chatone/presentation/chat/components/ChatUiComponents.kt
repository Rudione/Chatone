package io.rudione.chatone.presentation.chat.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.GenericEmote
import coil3.compose.AsyncImage
import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.presentation.chat.AnimatedEmoteImage
import io.rudione.chatone.presentation.chat.parseColor
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.components.chatoneGlassPanel
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.chat.MessageToken
import kotlinx.coroutines.delay
import kotlin.time.Clock
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneWindowSize
import io.rudione.chatone.presentation.components.LocalWindowSize
import io.rudione.chatone.util.media.ChannelAvatarCache
import org.koin.compose.koinInject

private fun formatRemaining(totalSeconds: Long): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

@Composable
internal fun PinnedMessageBar(
    message: DisplayMessage.PrivMsg,
    canUnpin: Boolean,
    endsAtMs: Long? = null,
    pinnedByName: String? = null,
    pinnedByBadges: List<Badge> = emptyList(),
    onUnpin: () -> Unit,
    onHide: () -> Unit = {}
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val compact = LocalWindowSize.current == ChatoneWindowSize.Compact
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    val sidePadding = if (compact) null else pinSidePadding(maxWidth)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = sidePadding ?: 8.dp,
                end = sidePadding ?: 18.dp,
                top = 4.dp,
                bottom = 4.dp
            )
            .chatoneGlassPanel(RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawWithContent {
                    drawContent()
                    drawRect(
                        color = primaryColor.copy(alpha = 0.9f),
                        size = Size(3.dp.toPx(), size.height)
                    )
                }
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (canUnpin) {
                    LiquidGlassTooltipBox(tooltip = LocalStrings.current.chatUnpinMessage) {
                        ChatoneIconButton(onClick = onUnpin, modifier = Modifier.size(24.dp)) {
                            Icon(
                                Icons.Filled.PushPin,
                                contentDescription = LocalStrings.current.chatUnpinMessage,
                                modifier = Modifier.size(16.dp),
                                tint = primaryColor
                            )
                        }
                    }
                } else {
                    Icon(
                        Icons.Filled.PushPin,
                        contentDescription = "Pinned",
                        modifier = Modifier.size(16.dp),
                        tint = primaryColor
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            message.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = parseColor(message.color) ?: primaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (!pinnedByName.isNullOrBlank()) {
                            Spacer(Modifier.width(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                pinnedByBadges.forEach { badge ->
                                    if (badge.imageUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = badge.imageUrl,
                                            contentDescription = badge.tooltip.ifBlank { badge.id },
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                Text(
                                    pinnedByName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Text(
                        message.tokens.joinToString("") { token ->
                            when (token) {
                                is MessageToken.Text -> token.text; is MessageToken.TwitchEmoteToken -> token.name
                                is MessageToken.ThirdPartyEmoteToken -> token.emote.code; is MessageToken.Link -> token.displayText
                                is MessageToken.Mention -> token.username
                                is MessageToken.Cheer -> "${token.prefix}${token.amount}"
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
                if (endsAtMs != null) {
                    var remainingSec by remember(endsAtMs) {
                        mutableStateOf((endsAtMs - Clock.System.now().toEpochMilliseconds()) / 1000)
                    }
                    LaunchedEffect(endsAtMs) {
                        while (remainingSec > 0) {
                            delay(1000)
                            remainingSec = (endsAtMs - Clock.System.now().toEpochMilliseconds()) / 1000
                        }
                    }
                    Text(
                        if (remainingSec > 0) formatRemaining(remainingSec) else "0:00",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                LiquidGlassTooltipBox(tooltip = LocalStrings.current.chatHidePin) {
                    ChatoneIconButton(onClick = onHide, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Outlined.VisibilityOff,
                            contentDescription = LocalStrings.current.chatHidePin,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    }
}

private fun pinSidePadding(width: Dp): Dp = when {
    width >= 1800.dp -> 148.dp
    width >= 1550.dp -> 128.dp
    width >= 1300.dp -> 92.dp
    width >= 1050.dp -> 72.dp
    width >= 850.dp -> 24.dp
    else -> 16.dp
}

@Composable
internal fun HiddenEventsRestoreButton(count: Int, onClick: () -> Unit) {
    val s = LocalStrings.current
    LiquidGlassTooltipBox(tooltip = s.chatShowHiddenEvents) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .chatoneGlassPanel(CircleShape, elevation = 8.dp)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.PushPin,
                contentDescription = s.chatShowHiddenEvents,
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            if (count > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        count.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
internal fun ReplyBar(displayName: String, messagePreview: String, onCancel: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(3.dp).height(28.dp).clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    LocalStrings.current.chatReplyingTo.replace("{0}", displayName),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    messagePreview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ChatoneIconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Cancel reply",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun RaidBanner(
    targetLogin: String,
    startedAtMs: Long,
    onCancel: () -> Unit,
    onRaidNow: () -> Unit,
    accessToken: String = ""
) {
    val totalSeconds = 90
    val lockSeconds = 10
    var remaining by remember(startedAtMs) {
        val e = ((Clock.System.now().toEpochMilliseconds() - startedAtMs) / 1000L).toInt()
        mutableStateOf((totalSeconds - e).coerceIn(0, totalSeconds))
    }
    var elapsedSec by remember(startedAtMs) {
        val e = ((Clock.System.now().toEpochMilliseconds() - startedAtMs) / 1000L).toInt()
        mutableStateOf(e.coerceAtLeast(0))
    }
    LaunchedEffect(startedAtMs) {
        while (remaining > 0) {
            delay(200)
            val e = (Clock.System.now().toEpochMilliseconds() - startedAtMs) / 1000.0
            elapsedSec = e.toInt().coerceAtLeast(0)
            remaining = (totalSeconds - e.toInt()).coerceIn(0, totalSeconds)
        }
        onCancel()
    }
    val raidNowEnabled = elapsedSec >= lockSeconds
    val accent = MaterialTheme.colorScheme.tertiary

    var avatarUrl by remember(targetLogin) {
        mutableStateOf(ChannelAvatarCache.cached(targetLogin))
    }
    if (avatarUrl == null && accessToken.isNotBlank()) {
        val apiClient: TwitchApiClient = koinInject()
        LaunchedEffect(targetLogin, accessToken) {
            avatarUrl = ChannelAvatarCache.fetch(apiClient, accessToken, targetLogin)
        }
    }

    val progressFraction by animateFloatAsState(
        targetValue = (remaining.toFloat() / totalSeconds).coerceIn(0f, 1f),
        label = "raidProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .shadow(
                10.dp, RoundedCornerShape(12.dp),
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.99f),
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f))
                ),
                RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(30.dp).clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = targetLogin,
                        modifier = Modifier.size(30.dp).clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier.size(30.dp).clip(CircleShape)
                            .background(accent.copy(alpha = 0.25f))
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    LocalStrings.current.raidStarted.replace("{0}", "@$targetLogin"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    when {
                        remaining <= 0 -> LocalStrings.current.raidSuccess
                        !raidNowEnabled -> "${lockSeconds - elapsedSec}s · ${remaining}s"
                        else -> "${remaining}s"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = onRaidNow,
                enabled = raidNowEnabled,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Text(
                    LocalStrings.current.raidNow,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (raidNowEnabled) accent else accent.copy(alpha = 0.38f)
                )
            }
            TextButton(
                onClick = onCancel,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Text(
                    LocalStrings.current.cancel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = if (raidNowEnabled) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            trackColor = Color.Transparent,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
internal fun rememberStaggeredMessages(
    source: List<DisplayMessage>,
    enabled: Boolean,
    stepMs: Long
): List<DisplayMessage> {
    if (!enabled) return source
    var visible by remember { mutableStateOf(source) }
    LaunchedEffect(source) {
        val visibleIds = visible.map { it.id }.toHashSet()
        val sourceIds = source.map { it.id }.toHashSet()
        val trimmed = visible.filter { it.id in sourceIds }
            .map { cur -> source.firstOrNull { it.id == cur.id } ?: cur }
        val newOnes = source.filter { it.id !in visibleIds }
        val isBulk = visible.isEmpty() || newOnes.size > 3 || newOnes.size > source.size / 2
        if (newOnes.isEmpty() || isBulk) {
            visible = source; return@LaunchedEffect
        }
        val tailStart = source.size - newOnes.size
        if (tailStart < 0 || source.subList(tailStart, source.size)
                .map { it.id } != newOnes.map { it.id }
        ) {
            visible = source; return@LaunchedEffect
        }
        var working = trimmed
        var lastUser: String? = (working.lastOrNull() as? DisplayMessage.PrivMsg)?.userId
        for (msg in newOnes) {
            val user = (msg as? DisplayMessage.PrivMsg)?.userId
            if (lastUser != null && user != null && user != lastUser) delay(stepMs)
            working = working + msg; visible = working; lastUser = user ?: lastUser
        }
    }
    return visible
}

@Composable
internal fun EmoteAutocompleteRow(
    emotes: List<GenericEmote>,
    selectedIndex: Int = -1,
    onSelect: (GenericEmote) -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) listState.animateScrollToItem(
            selectedIndex
        )
    }
    Box(
        modifier = Modifier.fillMaxSize().zIndex(10f)
            .pointerInput(Unit) { detectTapGestures(onPress = { onDismiss() }) }) {
        LiquidGlassSurface(
            modifier = Modifier.align(Alignment.BottomStart)
                .padding(start = 8.dp, end = 8.dp, bottom = 64.dp).heightIn(max = 120.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            backgroundAlphaHigh = 0.80f,
            backgroundAlphaLow = 0.65f,
            borderAlphaHigh = 0f,
            borderAlphaLow = 0f
        ) {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(emotes, key = { _, e -> e.id }) { idx, emote ->
                    val isSelected = idx == selectedIndex
                    Surface(
                        onClick = { onSelect(emote); onDismiss() },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).then(
                            if (isSelected) Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            ) else Modifier
                        ).height(32.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AnimatedEmoteImage(
                                url = emote.url2x.ifEmpty { emote.url1x },
                                contentDescription = emote.code,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = emote.code,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MentionAutocompleteRow(
    usernames: List<String>,
    selectedIndex: Int = -1,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) listState.animateScrollToItem(
            selectedIndex
        )
    }
    Box(
        modifier = Modifier.fillMaxSize().zIndex(10f)
            .pointerInput(Unit) { detectTapGestures(onPress = { onDismiss() }) }) {
        LiquidGlassSurface(
            modifier = Modifier.align(Alignment.BottomStart)
                .padding(start = 8.dp, end = 8.dp, bottom = 64.dp).heightIn(max = 120.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            backgroundAlphaHigh = 0.80f,
            backgroundAlphaLow = 0.65f,
            borderAlphaHigh = 0f,
            borderAlphaLow = 0f
        ) {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(usernames, key = { _, u -> u }) { idx, username ->
                    val isSelected = idx == selectedIndex
                    Surface(
                        onClick = { onSelect(username); onDismiss() },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).then(
                            if (isSelected) Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            ) else Modifier
                        ).height(32.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(
                                    alpha = 0.6f
                                )
                            )
                            Text(
                                text = "@$username",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
