package io.rudione.chatone.tv.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.media_retry
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.RefreshCw
import io.rudione.chatone.presentation.components.expressive.MorphingLoadingIndicator
import org.jetbrains.compose.resources.stringResource

@Composable
fun TvLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().heightIn(min = 160.dp), contentAlignment = Alignment.Center) {
        MorphingLoadingIndicator(size = 56.dp)
    }
}

@Composable
fun TvMessage(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    onRetry: (() -> Unit)? = null,
    actionLabel: String? = null,
    claimsFocus: Boolean = true
) {
    val focus = remember { FocusRequester() }
    Column(
        modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        if (body != null) {
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 520.dp)
            )
        }
        if (onRetry != null) {
            TvPillButton(
                actionLabel ?: stringResource(Res.string.media_retry),
                onClick = onRetry,
                icon = if (actionLabel == null) Lucide.RefreshCw else null,
                modifier = Modifier.focusRequester(focus)
            )
            if (claimsFocus) LaunchedEffect(Unit) { focus.requestFocus() }
        }
    }
}
