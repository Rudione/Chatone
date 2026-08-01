package io.rudione.chatone.presentation.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.ic_lock
import chatone.composeapp.generated.resources.ic_unlock
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.presentation.window.ChatoneDetachedWindow
import org.jetbrains.compose.resources.painterResource
import io.rudione.chatone.presentation.components.ChatoneIconButton

@Composable
actual fun DetachedProfileWindow(
    msg: DisplayMessage.PrivMsg,
    channelMessages: List<DisplayMessage>,
    accessToken: String,
    channelId: String,
    showModActions: Boolean,
    currentUserIsBroadcaster: Boolean,
    isBlocked: Boolean,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
    onTimeout: (Int) -> Unit,
    onBan: () -> Unit,
    onUnban: () -> Unit,
    onMod: () -> Unit,
    onUnmod: () -> Unit,
    onVip: () -> Unit,
    onUnvip: () -> Unit,
    onWhisper: () -> Unit,
    onClose: () -> Unit
) {
    var isPinned by remember { mutableStateOf(false) }

    ChatoneDetachedWindow(
        windowId = "profile",
        title = "${msg.displayName} — Profile",
        defaultWidth = 340.dp,
        defaultHeight = 600.dp,
        alwaysOnTop = isPinned,
        onCloseRequest = onClose
    ) {
        val pinTint by animateColorAsState(
            targetValue = if (isPinned) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(200)
        )

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = msg.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )

                        ChatoneIconButton(
                            onClick = { isPinned = !isPinned },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                painter = if (isPinned) painterResource(Res.drawable.ic_lock) else painterResource(
                                    Res.drawable.ic_unlock
                                ),
                                contentDescription = if (isPinned) "Unpin (always on top)" else "Pin (always on top)",
                                modifier = Modifier.size(16.dp),
                                tint = pinTint
                            )
                        }

                        ChatoneIconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )

                UserProfileContent(
                    msg = msg,
                    channelMessages = channelMessages,
                    accessToken = accessToken,
                    channelId = channelId,
                    showModActions = showModActions,
                    currentUserIsBroadcaster = currentUserIsBroadcaster,
                    isBlocked = isBlocked,
                    onBlock = onBlock,
                    onUnblock = onUnblock,
                    onTimeout = onTimeout,
                    onBan = onBan,
                    onUnban = onUnban,
                    onMod = onMod,
                    onUnmod = onUnmod,
                    onVip = onVip,
                    onUnvip = onUnvip,
                    onWhisper = onWhisper,
                    onDismiss = onClose
                )
            }
        }
    }
}
