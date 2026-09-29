package io.rudione.chatone.presentation.main.components.sidebar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.rudione.chatone.domain.model.ChannelFolder
import io.rudione.chatone.presentation.components.LiquidGlassDropdownItem
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.stream.StreamOpenQuality
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.icons.lucide.Bell
import io.rudione.chatone.icons.lucide.BellRing
import io.rudione.chatone.icons.lucide.Folder
import io.rudione.chatone.icons.lucide.FolderPlus
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Play
import io.rudione.chatone.icons.lucide.X

internal class ChannelMenuActions(
    val onMoveToFolder: (String?) -> Unit,
    val onCreateFolder: () -> Unit,
    val onToggleLiveNotify: () -> Unit,
    val onOpenStream: (StreamOpenQuality) -> Unit,
    val onClose: () -> Unit
)

@Composable
internal fun ChannelContextMenu(
    folders: List<ChannelFolder>,
    currentFolderId: String?,
    liveNotifyEnabled: Boolean,
    opensStreamInApp: Boolean,
    actions: ChannelMenuActions,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current
    val anchorOffset = with(LocalDensity.current) { IntOffset(0, 24.dp.roundToPx()) }
    val appearance = remember { MutableTransitionState(false).apply { targetState = true } }
    var showQualities by remember { mutableStateOf(false) }
    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

    fun run(action: () -> Unit) {
        onDismiss()
        action()
    }

    Popup(
        alignment = Alignment.TopEnd,
        offset = anchorOffset,
        properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true),
        onDismissRequest = onDismiss
    ) {
        AnimatedVisibility(
            visibleState = appearance,
            enter = fadeIn(tween(110)) + scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                initialScale = 0.82f,
                transformOrigin = TransformOrigin(1f, 0f)
            )
        ) {
            LiquidGlassSurface(
                modifier = Modifier
                    .width(216.dp)
                    .shadow(16.dp, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(4.dp),
                backgroundAlphaHigh = 0.99f
            ) {
                Column {
                    if (folders.isEmpty()) {
                        LiquidGlassDropdownItem(
                            text = s.mainCreateFolderTitle,
                            icon = Lucide.FolderPlus,
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = { run(actions.onCreateFolder) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = dividerColor)
                    } else {
                        if (currentFolderId != null) {
                            LiquidGlassDropdownItem(
                                text = s.mainRemoveFromFolder,
                                icon = Lucide.X,
                                onClick = { run { actions.onMoveToFolder(null) } }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = dividerColor)
                        }
                        folders.filter { it.id != currentFolderId }.forEach { folder ->
                            LiquidGlassDropdownItem(
                                text = folder.name,
                                icon = Lucide.Folder,
                                onClick = { run { actions.onMoveToFolder(folder.id) } }
                            )
                        }
                        LiquidGlassDropdownItem(
                            text = s.mainNewFolder,
                            icon = Lucide.FolderPlus,
                            onClick = { run(actions.onCreateFolder) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = dividerColor)
                    }
                    LiquidGlassDropdownItem(
                        text = if (liveNotifyEnabled) s.mainLiveNotifyOff else s.mainLiveNotifyOn,
                        icon = if (liveNotifyEnabled) Lucide.BellRing else Lucide.Bell,
                        onClick = { run(actions.onToggleLiveNotify) }
                    )
                    LiquidGlassDropdownItem(
                        text = if (opensStreamInApp) s.mainWatchInPlayer else s.mainOpenInPlayer,
                        icon = Lucide.Play,
                        onClick = { showQualities = !showQualities }
                    )
                    AnimatedVisibility(
                        visible = showQualities,
                        enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn()
                    ) {
                        Column(Modifier.padding(start = 12.dp)) {
                            StreamOpenQuality.entries.forEach { quality ->
                                LiquidGlassDropdownItem(
                                    text = quality.label,
                                    icon = Lucide.Play,
                                    onClick = { run { actions.onOpenStream(quality) } }
                                )
                            }
                        }
                    }
                    LiquidGlassDropdownItem(
                        text = s.mainCloseChannel,
                        icon = Lucide.X,
                        onClick = { run(actions.onClose) }
                    )
                }
            }
        }
    }
}
