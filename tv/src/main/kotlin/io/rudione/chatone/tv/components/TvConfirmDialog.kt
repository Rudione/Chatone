package io.rudione.chatone.tv.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.cancel
import io.rudione.chatone.presentation.components.SystemBackHandler
import org.jetbrains.compose.resources.stringResource

@Immutable
class TvConfirmRequest(
    val title: String,
    val confirmLabel: String,
    val body: String? = null,
    val onConfirm: () -> Unit
)

private val DialogShape = RoundedCornerShape(36.dp)
private val DialogWidth = 520.dp

@Composable
fun TvConfirmDialog(request: TvConfirmRequest, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val confirmFocus = remember(request) { FocusRequester() }
    SystemBackHandler(onBack = onDismiss)
    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .width(DialogWidth)
                .clip(DialogShape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(28.dp)
                .tvFocusTrap()
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(request.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            request.body?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvPillButton(
                    request.confirmLabel,
                    onClick = {
                        onDismiss()
                        request.onConfirm()
                    },
                    modifier = Modifier.focusRequester(confirmFocus)
                )
                TvPillButton(stringResource(Res.string.cancel), onClick = onDismiss, tone = TvButtonTone.Tonal)
            }
        }
    }
    LaunchedEffect(request) { confirmFocus.claim() }
}
