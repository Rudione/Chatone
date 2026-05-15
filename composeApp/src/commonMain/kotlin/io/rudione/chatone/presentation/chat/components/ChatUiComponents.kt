package io.rudione.chatone.presentation.chat

import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.GenericEmote
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.theme.FirstMessageColor
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.MessageToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@Composable
internal fun PinnedMessageBar(
    message: DisplayMessage.PrivMsg,
    canUnpin: Boolean,
    onUnpin: () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val primaryColor = MaterialTheme.colorScheme.primary
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.Transparent, tonalElevation = 2.dp) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            primaryColor.copy(alpha = 0.08f),
                            surfaceColor.copy(alpha = 0.92f),
                            primaryColor.copy(alpha = 0.05f)
                        )
                    )
                )
                .border(
                    0.5.dp,
                    Brush.horizontalGradient(
                        listOf(
                            primaryColor.copy(alpha = 0.3f),
                            primaryColor.copy(alpha = 0.1f),
                            primaryColor.copy(alpha = 0.2f)
                        )
                    ),
                    RoundedCornerShape(0.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Place,
                    contentDescription = "Pinned",
                    modifier = Modifier.size(16.dp),
                    tint = primaryColor.copy(alpha = 0.8f)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        message.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = parseColor(message.color) ?: primaryColor
                    )
                    Text(
                        message.tokens.joinToString("") { token ->
                            when (token) {
                                is MessageToken.Text -> token.text; is MessageToken.TwitchEmoteToken -> token.name
                                is MessageToken.ThirdPartyEmoteToken -> token.emote.code; is MessageToken.Link -> token.displayText
                                is MessageToken.Mention -> token.username
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
                if (canUnpin) IconButton(onClick = onUnpin, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Unpin",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
            IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
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
    onRaidNow: () -> Unit
) {
    val totalSeconds = 90;
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
            delay(1000)
            val e = ((Clock.System.now().toEpochMilliseconds() - startedAtMs) / 1000L).toInt()
            elapsedSec = e.coerceAtLeast(0)
            remaining = (totalSeconds - e).coerceIn(0, totalSeconds)
        }
        onCancel()
    }
    val raidNowEnabled = elapsedSec >= lockSeconds
    val accent = MaterialTheme.colorScheme.tertiary
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accent.copy(alpha = 0.18f),
                        accent.copy(alpha = 0.08f)
                    )
                )
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Filled.Star, null, modifier = Modifier.size(18.dp), tint = accent)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                LocalStrings.current.raidStarted.replace("{0}", "@$targetLogin"),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                when {
                    remaining <= 0 -> "Raid sent!"
                    !raidNowEnabled -> "Preparing… ${lockSeconds - elapsedSec}s (${remaining}s total)"
                    else -> "Sending viewers in ${remaining}s"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onRaidNow, enabled = raidNowEnabled) {
            Text(
                LocalStrings.current.raidNow,
                style = MaterialTheme.typography.labelMedium,
                color = if (raidNowEnabled) accent else accent.copy(alpha = 0.38f)
            )
        }
        TextButton(onClick = onCancel) {
            Text(
                LocalStrings.current.cancel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}


@Composable
internal fun HighlightScrollbar(
    listState: LazyListState,
    messages: List<DisplayMessage>,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val thumbFraction by remember {
        derivedStateOf {
            val info = listState.layoutInfo;
            val total = info.totalItemsCount
            if (total == 0) return@derivedStateOf 0f to 0.1f
            val viewportHeight = info.viewportSize.height.toFloat()
            val visibleItems = info.visibleItemsInfo
            val avgHeight = if (visibleItems.isNotEmpty()) visibleItems.map { it.size }.average()
                .toFloat() else 50f
            val totalContentHeight = avgHeight * total
            val firstVisible = visibleItems.firstOrNull() ?: return@derivedStateOf 0f to 0.1f
            val scrollOffset = firstVisible.index * avgHeight + firstVisible.offset.toFloat()
            val thumbSize = (viewportHeight / totalContentHeight).coerceIn(0.05f, 1f)
            val maxScroll = (totalContentHeight - viewportHeight).coerceAtLeast(0f)
            val thumbOffset =
                if (maxScroll > 0f) (scrollOffset / maxScroll * (1f - thumbSize)).coerceIn(
                    0f,
                    1f - thumbSize
                ) else 0f
            thumbOffset to thumbSize
        }
    }
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = offset.y / size.height
                    val target =
                        (fraction * (messages.size - 1)).toInt().coerceIn(0, messages.lastIndex)
                    coroutineScope.launch { listState.scrollToItem(target) }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {}, onDragEnd = {}, onDragCancel = {},
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val layoutInfo = listState.layoutInfo
                        val viewportHeight = layoutInfo.viewportSize.height.toFloat()
                        val avgHeight =
                            if (layoutInfo.visibleItemsInfo.isNotEmpty()) layoutInfo.visibleItemsInfo.map { it.size }
                                .average().toFloat() else 50f
                        val totalContentHeight = avgHeight * layoutInfo.totalItemsCount
                        val scrollRange = (totalContentHeight - viewportHeight).coerceAtLeast(0f)
                        val scrollDelta =
                            if (viewportHeight > 0f && scrollRange > 0f) dragAmount.y * (scrollRange / viewportHeight) else 0f
                        coroutineScope.launch { listState.scrollBy(scrollDelta) }
                    })
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Scroll) {
                            val scrollAmount = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                            if (scrollAmount != 0f) {
                                coroutineScope.launch { listState.scrollBy(scrollAmount) }; event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
            }
    ) {
        drawRect(color = Color.White.copy(alpha = 0.06f))
        val (thumbOffset, thumbSize) = thumbFraction
        val thumbTop = thumbOffset * size.height
        val thumbHeightPx = (thumbSize * size.height).coerceAtLeast(24.dp.toPx())
        drawRoundRect(
            color = Color.White.copy(alpha = 0.35f),
            topLeft = Offset(1.dp.toPx(), thumbTop),
            size = androidx.compose.ui.geometry.Size(size.width - 2.dp.toPx(), thumbHeightPx),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
        val total = messages.size.takeIf { it > 0 } ?: return@Canvas
        messages.forEachIndexed { index, msg ->
            if (msg !is DisplayMessage.PrivMsg) return@forEachIndexed
            val tickColor = when {
                msg.isMention -> if (msg.highlightColor != null) Color(msg.highlightColor) else Color(
                    0xFFFF6B6B
                )

                msg.isHighlighted -> Color(0xFFFFD700)
                msg.isFirstMessage -> FirstMessageColor.copy(alpha = 0.8f)
                else -> return@forEachIndexed
            }
            val y = (index.toFloat() / total) * size.height
            drawRect(
                color = tickColor.copy(alpha = 0.85f),
                topLeft = Offset(0f, y - 1.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx())
            )
        }
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