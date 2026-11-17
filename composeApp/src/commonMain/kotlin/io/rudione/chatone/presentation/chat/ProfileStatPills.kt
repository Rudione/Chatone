package io.rudione.chatone.presentation.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.insights_age_months
import chatone.composeapp.generated.resources.profile_following_since
import chatone.composeapp.generated.resources.profile_joined
import chatone.composeapp.generated.resources.profile_sub_age_hidden
import chatone.composeapp.generated.resources.profile_sub_age_months
import chatone.composeapp.generated.resources.profile_sub_age_streak
import chatone.composeapp.generated.resources.profile_sub_hidden_short
import io.rudione.chatone.data.remote.SubAgeInfo
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.StarOff
import io.rudione.chatone.presentation.components.PortalPopup
import io.rudione.chatone.presentation.components.expressive.ClosingTap
import io.rudione.chatone.presentation.components.expressive.expressiveGlass
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.jetbrains.compose.resources.stringResource

internal enum class ProfileStatKind { JOINED, FOLLOWING, SUB, SUB_HIDDEN }

@Immutable
internal data class ProfileStat(val kind: ProfileStatKind, val value: String, val detail: String)

@Composable
internal fun rememberProfileStats(createdAt: String, followedAt: String?, subAge: SubAgeInfo?): List<ProfileStat> {
    val joinedMs = remember(createdAt) { isoDateEpochMs(createdAt) }
    val followedMs = remember(followedAt) { followedAt?.let(::isoDateEpochMs) }
    return buildList {
        joinedMs?.let { add(ProfileStat(ProfileStatKind.JOINED, relativeAge(it), stringResource(Res.string.profile_joined, formatDate(it)))) }
        followedMs?.let { add(ProfileStat(ProfileStatKind.FOLLOWING, relativeAge(it), stringResource(Res.string.profile_following_since, formatDate(it)))) }
        subAge?.let { sub -> subStat(sub)?.let(::add) }
    }
}

@Composable
private fun subStat(sub: SubAgeInfo): ProfileStat? = when {
    sub.hidden -> ProfileStat(
        ProfileStatKind.SUB_HIDDEN,
        stringResource(Res.string.profile_sub_hidden_short),
        stringResource(Res.string.profile_sub_age_hidden)
    )
    sub.cumulativeMonths > 0 -> {
        val tier = sub.tier?.takeIf { it.isNotBlank() && it != "1" }
        val months = stringResource(Res.string.insights_age_months, sub.cumulativeMonths)
        val streak = if (sub.streakMonths > 1) stringResource(Res.string.profile_sub_age_streak, sub.streakMonths) else null
        val detail = buildString {
            append(stringResource(Res.string.profile_sub_age_months, sub.cumulativeMonths))
            sub.tier?.let { append(" · Tier $it") }
            streak?.let { append(" · ").append(it) }
        }
        ProfileStat(ProfileStatKind.SUB, if (tier != null) "$months · T$tier" else months, detail)
    }
    else -> null
}

@Composable
internal fun ProfileStatPills(stats: List<ProfileStat>, modifier: Modifier = Modifier) {
    if (stats.isEmpty()) return
    Layout(
        content = { stats.forEach { ProfileStatPill(it) } },
        modifier = modifier
    ) { measurables, constraints ->
        val gap = PILL_GAP.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(loose) }
        var used = 0
        val fitting = placeables.takeWhile { placeable ->
            val next = used + (if (used > 0) gap else 0) + placeable.width
            (next <= constraints.maxWidth).also { fits -> if (fits) used = next }
        }
        val height = fitting.maxOfOrNull { it.height } ?: 0
        layout(used, height) {
            var x = 0
            fitting.forEach { placeable ->
                placeable.place(x, (height - placeable.height) / 2)
                x += placeable.width + gap
            }
        }
    }
}

@Composable
private fun ProfileStatPill(stat: ProfileStat) {
    var open by remember { mutableStateOf(false) }
    val closing = remember { ClosingTap() }
    val interaction = remember { MutableInteractionSource() }
    val tint = stat.kind.tint()
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = if (open) 0.2f else 0.1f))
            .pointerInput(closing) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    closing.pressed()
                }
            }
            .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                open = !closing.consume() && !open
            }
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 8.dp, top = 3.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(stat.kind.icon(), contentDescription = stat.detail, modifier = Modifier.size(11.dp), tint = tint)
            Text(
                stat.value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
        if (open) {
            val inset = with(LocalDensity.current) { BUBBLE_GAP.roundToPx() }
            PortalPopup(
                popupPositionProvider = remember(inset) { StatBubblePosition(inset) },
                onDismissRequest = {
                    open = false
                    closing.closed()
                },
                properties = PopupProperties(focusable = false, dismissOnClickOutside = true)
            ) {
                StatBubble(stat.detail)
            }
            LaunchedEffect(stat) {
                delay(BUBBLE_VISIBLE_MS)
                open = false
            }
        }
    }
}

@Composable
private fun StatBubble(text: String) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium)) }
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .graphicsLayer {
                alpha = appear.value.coerceIn(0f, 1f)
                val scale = 0.9f + 0.1f * appear.value
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0.5f, 0f)
            }
            .widthIn(max = BUBBLE_MAX_WIDTH)
            .expressiveGlass(RoundedCornerShape(14.dp), elevation = 10.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

private class StatBubblePosition(private val gapPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val x = (anchorBounds.center.x - popupContentSize.width / 2).coerceIn(0, maxX)
        val below = anchorBounds.bottom + gapPx
        val y = if (below + popupContentSize.height <= windowSize.height) below
        else (anchorBounds.top - gapPx - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}

@Composable
private fun ProfileStatKind.tint(): Color = when (this) {
    ProfileStatKind.JOINED -> MaterialTheme.colorScheme.onSurfaceVariant
    ProfileStatKind.FOLLOWING -> MaterialTheme.colorScheme.primary
    ProfileStatKind.SUB -> MaterialTheme.colorScheme.tertiary
    ProfileStatKind.SUB_HIDDEN -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun ProfileStatKind.icon(): ImageVector = when (this) {
    ProfileStatKind.JOINED -> ProfileIconJoined
    ProfileStatKind.FOLLOWING -> ProfileIconFollow
    ProfileStatKind.SUB -> ProfileIconSub
    ProfileStatKind.SUB_HIDDEN -> Lucide.StarOff
}

private fun isoDateEpochMs(value: String): Long? = runCatching {
    LocalDate.parse(value.take(10)).atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}.getOrNull()

private val PILL_GAP = 4.dp
private val BUBBLE_GAP = 6.dp
private val BUBBLE_MAX_WIDTH = 240.dp
private const val BUBBLE_VISIBLE_MS = 3_500L
