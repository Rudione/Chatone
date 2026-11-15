package io.rudione.chatone.presentation.chat.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.chat_hide_pin
import chatone.composeapp.generated.resources.chat_unpin_message
import chatone.composeapp.generated.resources.pin_collapsed
import chatone.composeapp.generated.resources.pin_expanded
import chatone.composeapp.generated.resources.pin_options
import chatone.composeapp.generated.resources.pin_pinned_by
import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.domain.model.ChatNoticeUser
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.icons.lucide.ChevronDown
import io.rudione.chatone.icons.lucide.EllipsisVertical
import io.rudione.chatone.icons.lucide.EyeOff
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Pin
import io.rudione.chatone.presentation.chat.ChatSpillRequest
import io.rudione.chatone.presentation.chat.ChatNickname
import io.rudione.chatone.presentation.chat.LocalChatMediaSettings
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu
import io.rudione.chatone.presentation.components.LinkConfirmDialog
import io.rudione.chatone.util.link.openChatUrl
import org.jetbrains.compose.resources.stringResource

private val PinShape = RoundedCornerShape(topStart = 14.dp, topEnd = 5.dp, bottomEnd = 14.dp, bottomStart = 5.dp)

@Composable
internal fun PinnedMessageBar(
    message: DisplayMessage.PrivMsg,
    canUnpin: Boolean,
    pinnedByName: String? = null,
    pinnedByBadges: List<Badge> = emptyList(),
    onUnpin: () -> Unit,
    onHide: () -> Unit = {},
    pinnedByUser: ChatNoticeUser? = null
) {
    var expanded by remember(message.id) { mutableStateOf(false) }
    var menuOpen by remember(message.id) { mutableStateOf(false) }
    var pendingLink by remember(message.id) { mutableStateOf<String?>(null) }
    val interaction = remember { MutableInteractionSource() }
    val description = stringResource(if (expanded) Res.string.pin_expanded else Res.string.pin_collapsed)
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, tween(180), label = "pinChevron")
    val media = LocalChatMediaSettings.current
    val foreground = MaterialTheme.colorScheme.onSurfaceVariant
    val pinner = pinnedByName?.takeIf { it.isNotBlank() }
    ChatSpillRequest(key = "pinned", active = expanded && LocalEventBannerSelected.current)

    Column(
        Modifier.fillMaxWidth().clip(PinShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 1f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), PinShape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button) { expanded = !expanded }
            .animateContentSize(tween(180))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .semantics { stateDescription = description }
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = if (expanded) Alignment.Top else Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(Modifier.size(20.dp, 28.dp), contentAlignment = Alignment.Center) {
                Icon(Lucide.Pin, null, Modifier.size(13.dp), tint = foreground)
            }
            PinnedMessageText(
                message = message,
                expanded = expanded,
                onOpenLink = { pendingLink = it },
                modifier = Modifier.weight(1f).heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState(), enabled = expanded).padding(vertical = 5.dp)
            )
            PinActionButton(
                icon = Lucide.ChevronDown,
                label = description,
                tint = foreground,
                onClick = { expanded = !expanded },
                iconModifier = Modifier.graphicsLayer { rotationZ = chevron }
            )
            Box {
                PinActionButton(Lucide.EllipsisVertical, stringResource(Res.string.pin_options), foreground, { menuOpen = true })
                ChatoneDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.chat_hide_pin)) },
                        leadingIcon = { Icon(Lucide.EyeOff, null, tint = foreground) },
                        onClick = { menuOpen = false; onHide() }
                    )
                    if (canUnpin) DropdownMenuItem(
                        text = { Text(stringResource(Res.string.chat_unpin_message)) },
                        onClick = { menuOpen = false; onUnpin() }
                    )
                }
            }
        }
        if (expanded && pinner != null) {
            Row(
                Modifier.fillMaxWidth().padding(start = 25.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    stringResource(Res.string.pin_pinned_by, "").trim(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                ChatNickname(
                    user = pinnedByUser ?: ChatNoticeUser("", pinner, pinner),
                    modifier = Modifier.weight(1f),
                    fontSizeSp = 11f,
                    badges = pinnedByBadges,
                    showBadges = true
                )
            }
        }
    }
    pendingLink?.let { url ->
        LinkConfirmDialog(url, onOpen = { openChatUrl(url, media.linkOpenMode) }, onDismiss = { pendingLink = null })
    }
}

@Composable
private fun PinActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    iconModifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier.size(28.dp).clip(RoundedCornerShape(7.dp))
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, iconModifier.size(14.dp), tint = tint) }
}

