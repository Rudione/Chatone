package io.rudione.chatone.presentation.settings.sound

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.sound_add
import chatone.composeapp.generated.resources.sound_add_drop
import chatone.composeapp.generated.resources.sound_add_formats
import chatone.composeapp.generated.resources.sound_drop_release
import chatone.composeapp.generated.resources.sound_edit
import chatone.composeapp.generated.resources.sound_importing
import chatone.composeapp.generated.resources.sound_library_help
import chatone.composeapp.generated.resources.sound_library_title
import chatone.composeapp.generated.resources.sound_notice_added
import chatone.composeapp.generated.resources.sound_notice_bad_channel
import chatone.composeapp.generated.resources.sound_notice_deleted
import chatone.composeapp.generated.resources.sound_notice_failed
import chatone.composeapp.generated.resources.sound_notice_saved
import chatone.composeapp.generated.resources.sound_notice_too_large
import chatone.composeapp.generated.resources.sound_notice_unsupported
import chatone.composeapp.generated.resources.sound_play
import chatone.composeapp.generated.resources.sound_stop
import io.rudione.chatone.domain.sound.BuiltInSound
import io.rudione.chatone.icons.lucide.AudioWaveform
import io.rudione.chatone.icons.lucide.Check
import io.rudione.chatone.icons.lucide.CircleCheck
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Pencil
import io.rudione.chatone.icons.lucide.Play
import io.rudione.chatone.icons.lucide.Plus
import io.rudione.chatone.icons.lucide.Sparkles
import io.rudione.chatone.icons.lucide.Square
import io.rudione.chatone.icons.lucide.TriangleAlert
import io.rudione.chatone.icons.lucide.Upload
import io.rudione.chatone.presentation.components.expressive.MorphingLoadingIndicator
import io.rudione.chatone.presentation.components.expressive.HelpHint
import io.rudione.chatone.presentation.components.expressive.addScallop
import io.rudione.chatone.presentation.components.rows.HighlightedSettingsText
import io.rudione.chatone.presentation.settings.components.SettingsGrid
import io.rudione.chatone.presentation.settings.components.settingsCard
import io.rudione.chatone.presentation.settings.settingsAnchor
import io.rudione.chatone.presentation.theme.ChatoneIndication
import io.rudione.chatone.util.media.externalFileDropTarget
import io.rudione.chatone.util.system.isDesktopPlatform
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.stringResource

private sealed interface LibraryItem {
    data class Sound(val id: String, val custom: Boolean) : LibraryItem
    data object Add : LibraryItem
}

@Composable
internal fun SoundLibraryCard(
    state: MentionSoundUiState,
    volume: Float,
    onEvent: (MentionSoundEvent) -> Unit,
    onPick: () -> Unit
) {
    val title = stringResource(Res.string.sound_library_title)
    var dragging by remember { mutableStateOf(false) }
    val items = remember(state.library) {
        BuiltInSound.entries.map { LibraryItem.Sound(it.id, custom = false) } +
            state.library.map { LibraryItem.Sound(it.id, custom = true) } +
            LibraryItem.Add
    }
    Box(
        modifier = Modifier
            .settingsAnchor(title)
            .fillMaxWidth()
            .externalFileDropTarget(
                enabled = isDesktopPlatform,
                onFilesDropped = { onEvent(MentionSoundEvent.Import(it)) },
                onDragStateChanged = { dragging = it }
            )
            .settingsCard()
    ) {
        Column(
            modifier = Modifier.padding(CardPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CardHeader(title = title, help = stringResource(Res.string.sound_library_help))
            SettingsGrid(items = items, minCellWidth = if (isDesktopPlatform) 128.dp else 140.dp, spacing = 8.dp) { item ->
                val index = items.indexOf(item)
                when (item) {
                    LibraryItem.Add -> AddSoundTile(importing = state.importing > 0, index = index, onClick = onPick)
                    is LibraryItem.Sound -> {
                        val info = state.info(item.id)
                        SoundTile(
                            name = soundName(item.id, state),
                            durationMs = info?.durationMs ?: 0L,
                            peaks = info?.peaks.orEmpty(),
                            selected = state.config.selectedId == item.id,
                            custom = item.custom,
                            playback = state.playing,
                            id = item.id,
                            index = index,
                            onSelect = { onEvent(MentionSoundEvent.Select(item.id, volume.coerceAtLeast(0.05f))) },
                            onPreview = { onEvent(MentionSoundEvent.Preview(item.id, volume.coerceAtLeast(0.05f))) },
                            onStop = { onEvent(MentionSoundEvent.StopPreview) },
                            onEdit = { onEvent(MentionSoundEvent.OpenEditor(item.id)) }
                        )
                    }
                }
            }
            NoticePill(state.notice)
        }
        DropOverlay(active = dragging, modifier = Modifier.matchParentSize())
    }
}

@Composable
internal fun CardHeader(title: String, help: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HighlightedSettingsText(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (help != null) HelpHint(title = title, text = help, size = 24.dp)
    }
}

@Composable
private fun SoundTile(
    name: String,
    durationMs: Long,
    peaks: List<Float>,
    selected: Boolean,
    custom: Boolean,
    playback: SoundPlayback?,
    id: String,
    index: Int,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
    onStop: () -> Unit,
    onEdit: () -> Unit
) {
    val appear = rememberAppear(index)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium), label = "tilePress")
    val corner by animateDpAsState(if (selected) 26.dp else 16.dp, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow), label = "tileCorner")
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    val container by animateColorAsState(
        if (selected) primary.copy(alpha = 0.20f).compositeOver(surface) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.045f),
        tween(260),
        label = "tileContainer"
    )
    val edge by animateFloatAsState(if (selected) 1f else 0f, tween(260), label = "tileEdge")
    val progress by rememberPlaybackProgress(playback, id)
    val playing = playback?.id == id
    val shape = RoundedCornerShape(corner)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(TileHeight)
            .graphicsLayer {
                val scale = (0.9f + 0.1f * appear.value) * press
                scaleX = scale
                scaleY = scale
                alpha = appear.value
                translationY = (1f - appear.value) * 14.dp.toPx()
            }
            .clip(shape)
            .background(container)
            .border(1.5.dp, primary.copy(alpha = 0.85f * edge), shape)
            .clickable(interactionSource = interaction, indication = ChatoneIndication, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SelectionBadge(selected = selected, custom = custom)
            Spacer(Modifier.width(7.dp))
            Text(
                name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            MiniIconButton(
                if (playing) Lucide.Square else Lucide.Play,
                stringResource(if (playing) Res.string.sound_stop else Res.string.sound_play),
                if (playing) onStop else onPreview
            )
        }
        Spacer(Modifier.weight(1f))
        SoundWaveform(
            peaks = peaks,
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(22.dp),
            active = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            barCount = 22
        )
        Spacer(Modifier.height(3.dp))
        Row(modifier = Modifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (durationMs > 0) secondsLabel(durationMs) else " ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (custom) MiniIconButton(Lucide.Pencil, stringResource(Res.string.sound_edit), onEdit, size = 22.dp)
        }
    }
}

@Composable
private fun SelectionBadge(selected: Boolean, custom: Boolean) {
    val pop by animateFloatAsState(
        if (selected) 1f else 0f,
        spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow),
        label = "badgePop"
    )
    val primary = MaterialTheme.colorScheme.primary
    Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
        Icon(
            if (custom) Lucide.AudioWaveform else Lucide.Sparkles,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp).graphicsLayer {
                alpha = 1f - pop.coerceIn(0f, 1f)
                scaleX = 1f - 0.5f * pop.coerceIn(0f, 1f)
                scaleY = scaleX
            }
        )
        Box(
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = pop
                    scaleY = pop
                    rotationZ = (1f - pop) * -90f
                }
                .drawBehind {
                    val path = Path()
                    path.addScallop(size, lobes = 8, depth = 0.1f)
                    drawPath(path, primary)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Lucide.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
internal fun MiniIconButton(icon: ImageVector, label: String, onClick: () -> Unit, size: Dp = 28.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ChatoneIndication,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
private fun AddSoundTile(importing: Boolean, index: Int, onClick: () -> Unit) {
    val appear = rememberAppear(index)
    val primary = MaterialTheme.colorScheme.primary
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(TileHeight)
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 14.dp.toPx()
            }
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                drawRoundRect(
                    color = primary.copy(alpha = 0.55f),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
                    )
                )
            }
            .clickable(interactionSource = interaction, indication = ChatoneIndication, enabled = !importing, role = Role.Button, onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (importing) {
            MorphingLoadingIndicator(size = 30.dp)
        } else {
            Box(
                modifier = Modifier.size(30.dp).clip(CircleShape).background(primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Lucide.Plus, contentDescription = null, tint = primary, modifier = Modifier.size(17.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(if (importing) Res.string.sound_importing else Res.string.sound_add),
            style = MaterialTheme.typography.labelLarge,
            color = primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            if (isDesktopPlatform) stringResource(Res.string.sound_add_drop) else stringResource(Res.string.sound_add_formats),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DropOverlay(active: Boolean, modifier: Modifier) {
    val visible by animateFloatAsState(if (active) 1f else 0f, spring(stiffness = Spring.StiffnessMediumLow), label = "dropOverlay")
    if (visible <= 0.01f) return
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val march = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        val anim = Animatable(march.floatValue)
        while (isActive) {
            anim.animateTo(anim.value + 28f, tween(600, easing = LinearEasing)) { march.floatValue = value }
        }
    }
    val bounce = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        while (isActive) {
            bounce.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow))
            delay(380)
            bounce.animateTo(0f, tween(420))
        }
    }
    Box(
        modifier = modifier
            .graphicsLayer { alpha = visible }
            .clip(RoundedCornerShape(if (isDesktopPlatform) 18.dp else 26.dp))
            .background(primary.copy(alpha = 0.14f).compositeOver(surface.copy(alpha = 0.86f)))
            .drawBehind {
                val inset = 6.dp.toPx()
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2),
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14.dp.toPx(), 10.dp.toPx()), march.floatValue.dp.toPx())
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Lucide.Upload,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(34.dp).graphicsLayer { translationY = -bounce.value * 8.dp.toPx() }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(Res.string.sound_drop_release),
                style = MaterialTheme.typography.titleSmall,
                color = primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun NoticePill(notice: SoundNotice?) {
    var shown by remember { mutableStateOf(notice) }
    if (notice != null) shown = notice
    AnimatedVisibility(
        visible = notice != null,
        enter = slideInVertically(spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(),
        exit = slideOutVertically { it / 2 } + fadeOut(tween(160))
    ) {
        val current = shown ?: return@AnimatedVisibility
        val error = current in setOf(SoundNotice.Unsupported, SoundNotice.TooLarge, SoundNotice.Failed, SoundNotice.BadChannel)
        val tint = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.14f))
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(if (error) Lucide.TriangleAlert else Lucide.CircleCheck, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(8.dp))
            Text(noticeText(current), style = MaterialTheme.typography.labelMedium, color = tint)
        }
    }
}

@Composable
internal fun noticeText(notice: SoundNotice): String = stringResource(
    when (notice) {
        SoundNotice.Added -> Res.string.sound_notice_added
        SoundNotice.Saved -> Res.string.sound_notice_saved
        SoundNotice.Deleted -> Res.string.sound_notice_deleted
        SoundNotice.Unsupported -> Res.string.sound_notice_unsupported
        SoundNotice.TooLarge -> Res.string.sound_notice_too_large
        SoundNotice.Failed -> Res.string.sound_notice_failed
        SoundNotice.BadChannel -> Res.string.sound_notice_bad_channel
    }
)

@Composable
internal fun rememberAppear(index: Int): Animatable<Float, AnimationVector1D> {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index.coerceAtMost(12) * 35L)
        appear.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow))
    }
    return appear
}

private val TileHeight = if (isDesktopPlatform) 96.dp else 104.dp
