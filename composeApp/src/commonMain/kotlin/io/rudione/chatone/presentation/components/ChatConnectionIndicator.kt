package io.rudione.chatone.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.chat_connection_auth
import chatone.composeapp.generated.resources.chat_connection_connected
import chatone.composeapp.generated.resources.chat_connection_connecting
import chatone.composeapp.generated.resources.chat_connection_disconnected
import chatone.composeapp.generated.resources.chat_connection_network
import chatone.composeapp.generated.resources.chat_connection_retry
import chatone.composeapp.generated.resources.chat_connection_retrying
import chatone.composeapp.generated.resources.chat_connection_server
import chatone.composeapp.generated.resources.chat_connection_timeout
import io.rudione.chatone.data.remote.TwitchIrcClient.ConnectionState
import io.rudione.chatone.data.remote.TwitchIrcClient.FailureReason
import io.rudione.chatone.presentation.chat.components.LiquidGlassTooltipBox
import io.rudione.chatone.presentation.theme.ChatoneTheme
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal fun connectionStatusResource(state: ConnectionState): StringResource = when (state) {
    ConnectionState.Connected -> Res.string.chat_connection_connected
    ConnectionState.Connecting -> Res.string.chat_connection_connecting
    ConnectionState.Disconnected -> Res.string.chat_connection_disconnected
    ConnectionState.WaitingForNetwork -> Res.string.chat_connection_network
    is ConnectionState.Reconnecting -> failureResource(state.reason)
    is ConnectionState.Error -> failureResource(state.reason)
}

private fun failureResource(reason: FailureReason): StringResource = when (reason) {
    FailureReason.NETWORK -> Res.string.chat_connection_network
    FailureReason.TIMEOUT -> Res.string.chat_connection_timeout
    FailureReason.AUTHENTICATION -> Res.string.chat_connection_auth
    FailureReason.SERVER -> Res.string.chat_connection_server
}

@Composable
internal fun ChatConnectionIndicator(state: ConnectionState, onReconnect: () -> Unit, automaticHintEnabled: Boolean = true) {
    val connected = state is ConnectionState.Connected
    val connecting = state is ConnectionState.Connecting || state is ConnectionState.Reconnecting
    val label = stringResource(connectionStatusResource(state))
    var expanded by remember { mutableStateOf(false) }
    var automaticHint by remember { mutableStateOf(false) }
    LaunchedEffect(connected, automaticHintEnabled) {
        automaticHint = false
        expanded = false
        if (!connected && automaticHintEnabled) {
            delay(4_000)
            automaticHint = true
            delay(6_000)
            automaticHint = false
        }
    }
    val color = when {
        connected -> ChatoneTheme.extraColors.connected
        connecting -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    Box {
        LiquidGlassTooltipBox(tooltip = label) {
            Box(
                Modifier.size(24.dp).semantics { contentDescription = label }
                    .clip(CircleShape).clickable { expanded = !expanded; automaticHint = false },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(color))
            }
        }
        DropdownMenu(
            expanded = expanded || automaticHint,
            onDismissRequest = { expanded = false; automaticHint = false },
            properties = PopupProperties(focusable = false),
            shape = RoundedCornerShape(topStart = 15.dp, topEnd = 5.dp, bottomEnd = 15.dp, bottomStart = 5.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp
        ) {
            DisableSelection {
                Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
                if (connecting) Text(
                    stringResource(Res.string.chat_connection_retrying),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                )
                if (!connected) TextButton(onClick = {
                    expanded = false
                    automaticHint = false
                    onReconnect()
                }, modifier = Modifier.padding(horizontal = 4.dp)) {
                    Text(stringResource(Res.string.chat_connection_retry))
                }
            }
        }
    }
}
