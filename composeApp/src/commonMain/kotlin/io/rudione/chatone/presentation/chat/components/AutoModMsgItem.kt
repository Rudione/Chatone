package io.rudione.chatone.presentation.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import chatone.composeapp.generated.resources.automod_show_less
import chatone.composeapp.generated.resources.automod_show_more
import io.rudione.chatone.icons.lucide.ChevronDown
import io.rudione.chatone.icons.lucide.ChevronUp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.automod_allow
import chatone.composeapp.generated.resources.automod_allowed
import chatone.composeapp.generated.resources.automod_deny
import chatone.composeapp.generated.resources.automod_denied
import chatone.composeapp.generated.resources.automod_insight_account
import chatone.composeapp.generated.resources.automod_insight_bans
import chatone.composeapp.generated.resources.automod_insight_first_message
import chatone.composeapp.generated.resources.automod_insight_following
import chatone.composeapp.generated.resources.automod_insight_messages
import chatone.composeapp.generated.resources.automod_insight_new_account
import chatone.composeapp.generated.resources.automod_insight_not_following
import chatone.composeapp.generated.resources.automod_insight_sub
import chatone.composeapp.generated.resources.automod_insight_timeouts
import chatone.composeapp.generated.resources.chat_automod_panel
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.DisplayMessage.AutoModMsg.AutoModStatus
import io.rudione.chatone.domain.moderation.AutoModInsight
import io.rudione.chatone.domain.moderation.AutoModInsightKind
import io.rudione.chatone.domain.moderation.AutoModInsights
import io.rudione.chatone.domain.moderation.InsightTone
import io.rudione.chatone.icons.lucide.Ban
import io.rudione.chatone.icons.lucide.CalendarDays
import io.rudione.chatone.icons.lucide.Check
import io.rudione.chatone.icons.lucide.Heart
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.MessageSquare
import io.rudione.chatone.icons.lucide.Shield
import io.rudione.chatone.icons.lucide.Sparkles
import io.rudione.chatone.icons.lucide.Star
import io.rudione.chatone.icons.lucide.Timer
import io.rudione.chatone.icons.lucide.TriangleAlert
import io.rudione.chatone.icons.lucide.UserX
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.components.animatedBorder
import io.rudione.chatone.presentation.components.tactilePress
import io.rudione.chatone.presentation.theme.ChatoneTheme
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

private const val SHORT_TEXT = 80
private const val MEDIUM_TEXT = 200
private const val SHORT_SCALE = 1.1f
private const val MEDIUM_SCALE = 1f
private const val LONG_SCALE = 0.93f
private val CollapsedFieldHeight = 112.dp
private val FieldFade = 22.dp

internal fun autoModFieldScale(textLength: Int): Float = when {
    textLength <= SHORT_TEXT -> SHORT_SCALE
    textLength <= MEDIUM_TEXT -> MEDIUM_SCALE
    else -> LONG_SCALE
}
private val CardShape = RoundedCornerShape(12.dp)
private val FieldShape = RoundedCornerShape(8.dp)
private val ChipShape = RoundedCornerShape(6.dp)
private val DecisionShape = RoundedCornerShape(8.dp)
private val ChipText = 10.5.sp
private val ChipLine = 13.sp

@Composable
internal fun AutoModMsgItem(
    message: DisplayMessage.AutoModMsg,
    onAllow: () -> Unit,
    onDeny: () -> Unit,
    onUsernameClick: () -> Unit = {},
    preview: (@Composable (DisplayMessage.PrivMsg) -> Unit)? = null
) {
    val extra = ChatoneTheme.extraColors
    val pending = message.status == AutoModStatus.PENDING
    val accent by animateColorAsState(
        when (message.status) {
            AutoModStatus.PENDING -> extra.modTimeout
            AutoModStatus.ALLOWED -> extra.modUnban
            AutoModStatus.DENIED -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        tween(220),
        label = "automodAccent"
    )
    val previewMessage = message.preview?.takeIf { preview != null }
    var expanded by remember(message.msgId) { mutableStateOf(false) }
    var overflowing by remember(message.msgId) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(CardShape)
            .drawBehind {
                drawRect(accent.copy(alpha = if (pending) 0.08f else 0.05f))
                val inset = 6.dp.toPx()
                val width = 3.dp.toPx()
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(4.dp.toPx(), inset),
                    size = Size(width, (size.height - inset * 2).coerceAtLeast(0f)),
                    cornerRadius = CornerRadius(width / 2f, width / 2f)
                )
            }
            .animatedBorder(1.dp, CardShape) { accent.copy(alpha = 0.22f) }
            .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 5.dp)
    ) {
        AutoModHeader(message, accent)
        Spacer(Modifier.height(3.dp))
        if (previewMessage != null && preview != null) {
            ScaledField(
                scale = autoModFieldScale(message.text.length),
                expanded = expanded,
                onOverflow = { overflowing = it }
            ) { preview(previewMessage) }
        } else {
            FallbackBody(message, extra.modBan, onUsernameClick)
        }
        AutoModFooter(
            message = message,
            pending = pending,
            canExpand = overflowing,
            expanded = expanded,
            onToggleExpanded = { expanded = !expanded },
            onAllow = onAllow,
            onDeny = onDeny
        )
    }
}

@Composable
private fun AutoModHeader(message: DisplayMessage.AutoModMsg, accent: Color) {
    Row(modifier = Modifier.fillMaxWidth().height(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Lucide.Shield, contentDescription = null, modifier = Modifier.size(11.dp), tint = accent)
        Spacer(Modifier.width(4.dp))
        Text(
            stringResource(Res.string.chat_automod_panel),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accent,
            maxLines = 1
        )
        autoModCategoryLabel(message.reasonCategory)?.let { label ->
            Spacer(Modifier.width(6.dp))
            CategoryChip(label, message.reasonLevel, accent, Modifier.weight(1f, fill = false))
        }
        Spacer(Modifier.weight(1f))
        AnimatedVisibility(
            visible = message.status != AutoModStatus.PENDING,
            enter = fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.8f),
            exit = fadeOut(tween(120))
        ) {
            val allowed = message.status == AutoModStatus.ALLOWED
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (allowed) Lucide.Check else Lucide.X,
                    contentDescription = null,
                    modifier = Modifier.size(11.dp),
                    tint = accent
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    stringResource(if (allowed) Res.string.automod_allowed else Res.string.automod_denied),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, level: Int?, accent: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(ChipShape)
            .background(accent.copy(alpha = 0.15f))
            .padding(horizontal = 5.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = ChipText, lineHeight = ChipLine),
            fontWeight = FontWeight.SemiBold,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (level != null) {
            Spacer(Modifier.width(4.dp))
            repeat(4) { i ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 0.5.dp)
                        .size(3.5.dp)
                        .clip(CircleShape)
                        .background(if (i < level.coerceIn(0, 4)) accent else accent.copy(alpha = 0.25f))
                )
            }
        }
    }
}

@Composable
private fun ScaledField(
    scale: Float,
    expanded: Boolean,
    onOverflow: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val scaled = remember(density, scale) { Density(density.density * scale, density.fontScale) }
    val collapsedPx = with(density) { CollapsedFieldHeight.roundToPx() }
    var overflowing by remember { mutableStateOf(false) }
    val reportOverflow by rememberUpdatedState(onOverflow)
    LaunchedEffect(overflowing) { reportOverflow(overflowing) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(tween(220))
            .clip(FieldShape)
            .fadeBottomEdge(FieldFade) { overflowing && !expanded }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(maxHeight = Constraints.Infinity))
                val tooTall = placeable.height > collapsedPx
                if (tooTall != overflowing) overflowing = tooTall
                val height = if (expanded || !tooTall) placeable.height else collapsedPx
                layout(placeable.width, height) { placeable.place(0, 0) }
            }
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
    ) {
        CompositionLocalProvider(LocalDensity provides scaled) { content() }
    }
}

@Composable
private fun AutoModFooter(
    message: DisplayMessage.AutoModMsg,
    pending: Boolean,
    canExpand: Boolean,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onAllow: () -> Unit,
    onDeny: () -> Unit
) {
    val extra = ChatoneTheme.extraColors
    val insights = remember(message.context) {
        message.context?.let { AutoModInsights.of(it, Clock.System.now().toEpochMilliseconds()) }.orEmpty()
    }
    val flagged = remember(message.flaggedFragments, message.preview) {
        if (message.preview == null) emptyList()
        else message.flaggedFragments.filter { it.isNotBlank() }.distinctBy { it.lowercase() }
    }
    if (!pending && !canExpand && insights.isEmpty() && flagged.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val chipsScroll = rememberScrollState()
        Row(
            modifier = Modifier
                .weight(1f)
                .fadeTrailingEdge { chipsScroll.canScrollForward }
                .horizontalScroll(chipsScroll),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canExpand) ExpandChip(expanded, onToggleExpanded)
            flagged.forEach { term -> InfoChip(Lucide.TriangleAlert, term, extra.modBan) }
            insights.forEach { insight -> InsightChip(insight) }
        }
        AnimatedVisibility(
            visible = pending,
            enter = fadeIn(tween(160)) + expandHorizontally(tween(180)),
            exit = fadeOut(tween(140)) + shrinkHorizontally(tween(200))
        ) {
            Row(
                modifier = Modifier.padding(start = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                AutoModDecisionButton(stringResource(Res.string.automod_allow), Lucide.Check, extra.modUnban, onAllow)
                AutoModDecisionButton(stringResource(Res.string.automod_deny), Lucide.X, extra.modBan, onDeny)
            }
        }
    }
}

@Composable
private fun ExpandChip(expanded: Boolean, onClick: () -> Unit) {
    val color = MaterialTheme.colorScheme.primary
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .tactilePress(interaction, pressedScale = 0.94f)
            .clip(ChipShape)
            .background(color.copy(alpha = 0.14f))
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (expanded) Lucide.ChevronUp else Lucide.ChevronDown,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(10.dp)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            stringResource(if (expanded) Res.string.automod_show_less else Res.string.automod_show_more),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = ChipText, lineHeight = ChipLine),
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1
        )
    }
}

@Composable
private fun InsightChip(insight: AutoModInsight) {
    val extra = ChatoneTheme.extraColors
    val color = when (insight.tone) {
        InsightTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
        InsightTone.POSITIVE -> extra.modUnban
        InsightTone.WARNING -> extra.modTimeout
        InsightTone.DANGER -> extra.modBan
    }
    val since = insight.sinceMs?.let { relativeAge(it) }.orEmpty()
    val count = insight.count ?: 0
    val (icon, text) = when (insight.kind) {
        AutoModInsightKind.BANS -> Lucide.Ban to stringResource(Res.string.automod_insight_bans, count)
        AutoModInsightKind.TIMEOUTS -> Lucide.Timer to stringResource(Res.string.automod_insight_timeouts, count)
        AutoModInsightKind.NEW_ACCOUNT -> Lucide.Sparkles to stringResource(Res.string.automod_insight_new_account, since)
        AutoModInsightKind.ACCOUNT_AGE -> Lucide.CalendarDays to stringResource(Res.string.automod_insight_account, since)
        AutoModInsightKind.FOLLOWING -> Lucide.Heart to stringResource(Res.string.automod_insight_following, since)
        AutoModInsightKind.NOT_FOLLOWING -> Lucide.UserX to stringResource(Res.string.automod_insight_not_following)
        AutoModInsightKind.SUBSCRIBER -> Lucide.Star to stringResource(Res.string.automod_insight_sub, count)
        AutoModInsightKind.FIRST_MESSAGE -> Lucide.MessageSquare to stringResource(Res.string.automod_insight_first_message)
        AutoModInsightKind.MESSAGES -> Lucide.MessageSquare to stringResource(Res.string.automod_insight_messages, count)
    }
    InfoChip(icon, text, color)
}

@Composable
private fun InfoChip(icon: ImageVector, text: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(ChipShape)
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(10.dp))
        Spacer(Modifier.width(3.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = ChipText, lineHeight = ChipLine),
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1
        )
    }
}

@Composable
private fun FallbackBody(message: DisplayMessage.AutoModMsg, caughtColor: Color, onUsernameClick: () -> Unit) {
    val alpha = if (message.status == AutoModStatus.DENIED) 0.45f else 0.92f
    val nameColor = rememberNickColors().of(message.color, message.username)
    val body = remember(message.text, message.flaggedFragments, caughtColor) {
        highlightFlagged(message.text, message.flaggedFragments, caughtColor)
    }
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = message.displayName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = nameColor.copy(alpha = alpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onUsernameClick)
                .pointerHoverIcon(PointerIcon.Hand)
        )
        Text(
            text = ": ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.6f)
        )
        SelectionContainer(modifier = Modifier.weight(1f)) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )
        }
    }
}

@Composable
private fun AutoModDecisionButton(label: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val fill by animateFloatAsState(
        when {
            pressed -> 0.34f
            hovered -> 0.24f
            else -> 0.14f
        },
        tween(120),
        label = "automodDecisionFill"
    )
    val edge by animateFloatAsState(if (hovered || pressed) 0.65f else 0.3f, tween(140), label = "automodDecisionEdge")
    Row(
        modifier = Modifier
            .height(24.dp)
            .tactilePress(interaction, pressedScale = 0.93f)
            .clip(DecisionShape)
            .drawBehind { drawRect(accent.copy(alpha = fill)) }
            .animatedBorder(1.dp, DecisionShape) { accent.copy(alpha = edge) }
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = accent,
            maxLines = 1
        )
    }
}

private fun Modifier.fadeBottomEdge(height: androidx.compose.ui.unit.Dp, active: () -> Boolean): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        if (!active()) return@drawWithContent
        val fade = height.toPx().coerceAtMost(size.height)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Black, Color.Transparent),
                startY = size.height - fade,
                endY = size.height
            ),
            topLeft = Offset(0f, size.height - fade),
            size = Size(size.width, fade),
            blendMode = BlendMode.DstIn
        )
    }

private fun Modifier.fadeTrailingEdge(active: () -> Boolean): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        if (!active()) return@drawWithContent
        val fade = 18.dp.toPx().coerceAtMost(size.width)
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Black, Color.Transparent),
                startX = size.width - fade,
                endX = size.width
            ),
            topLeft = Offset(size.width - fade, 0f),
            size = Size(fade, size.height),
            blendMode = BlendMode.DstIn
        )
    }

internal fun autoModCategoryLabel(raw: String?): String? = when (raw?.lowercase()) {
    "blocked_term" -> "Blocked term"
    "blocked_link" -> "Blocked link"
    "aggression" -> "Aggression"
    "bullying" -> "Bullying"
    "disability" -> "Disability"
    "misogyny" -> "Misogyny"
    "race_ethnicity_or_religion", "race" -> "Discrimination"
    "sex_based_terms", "sexuality" -> "Sexual content"
    "sexuality_sex_or_gender", "gender" -> "Identity"
    "swearing" -> "Profanity"
    "smartdetection", "smart_detection" -> "Smart detection"
    null, "" -> null
    else -> raw.replaceFirstChar { it.uppercase() }.replace('_', ' ')
}

private fun highlightFlagged(text: String, flagged: List<String>, color: Color): AnnotatedString = buildAnnotatedString {
    val terms = flagged.filter { it.isNotBlank() }
    if (terms.isEmpty()) {
        append(text)
        return@buildAnnotatedString
    }
    var cursor = 0
    while (cursor < text.length) {
        val hit = terms
            .mapNotNull { term -> text.indexOf(term, cursor, ignoreCase = true).takeIf { it >= 0 }?.let { it to term } }
            .minByOrNull { it.first }
        if (hit == null) {
            append(text.substring(cursor))
            break
        }
        append(text.substring(cursor, hit.first))
        withStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold)) {
            append(text.substring(hit.first, hit.first + hit.second.length))
        }
        cursor = hit.first + hit.second.length
    }
}
