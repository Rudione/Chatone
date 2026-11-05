package io.rudione.chatone.presentation.chat.multichat

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.multi_chat_add_panel
import chatone.composeapp.generated.resources.multi_chat_drag_hint
import chatone.composeapp.generated.resources.ok
import org.jetbrains.compose.resources.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun MultiChatGuidedTour(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(true) }
    AnimatedVisibility(visible = visible, modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f))
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = stringResource(Res.string.multi_chat_add_panel),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(Res.string.multi_chat_drag_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                TextButton(onClick = {
                    visible = false
                    onDismiss()
                }) {
                    Text(stringResource(Res.string.ok))
                }
            }
        }
    }
}
