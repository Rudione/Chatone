package io.rudione.chatone.presentation.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.remote.AiFailure
import io.rudione.chatone.data.remote.AiFailureKind
import io.rudione.chatone.data.remote.AiProviders
import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.outlined.ErrorOutline
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.theme.i18n.format

@Immutable
internal data class AiFailureText(val title: String, val hint: String, val detail: String?)

internal fun aiFailureText(failure: AiFailure, baseUrl: String, model: String, strings: AppStrings): AiFailureText {
    val s = strings.aiAssist
    val provider = AiProviders.label(baseUrl)
    val local = AiProviders.isLocal(baseUrl)
    fun text(title: String, hint: String, showDetail: Boolean = false) = AiFailureText(
        title = strings.format(title, provider, model),
        hint = strings.format(hint, provider, model),
        detail = failure.detail?.takeIf { showDetail }
    )
    return when (failure.kind) {
        AiFailureKind.OVERLOADED -> text(s.errorOverloadedTitle, s.errorOverloadedHint)
        AiFailureKind.RATE_LIMITED -> text(s.errorRateLimitedTitle, s.errorRateLimitedHint)
        AiFailureKind.UNAUTHORIZED -> text(s.errorKeyTitle, s.errorKeyHint)
        AiFailureKind.MODEL_NOT_FOUND ->
            text(s.errorModelTitle, if (local) s.errorModelLocalHint else s.errorModelHint)
        AiFailureKind.BAD_REQUEST -> text(s.errorRequestTitle, s.errorRequestHint, showDetail = true)
        AiFailureKind.SERVER -> AiFailureText(
            title = s.errorServerTitle,
            hint = strings.format(s.errorServerHint, provider, failure.status ?: "?"),
            detail = failure.detail
        )
        AiFailureKind.NETWORK ->
            if (local) AiFailureText(
                strings.aiConnectErrorTitle,
                strings.format(strings.aiConnectErrorHint, baseUrl),
                null
            )
            else text(s.errorNetworkTitle, s.errorNetworkHint)
        AiFailureKind.EMPTY_REPLY -> text(s.errorEmptyTitle, s.errorEmptyHint)
    }
}

@Composable
internal fun AiFailureCard(
    failure: AiFailure,
    baseUrl: String,
    model: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val text = aiFailureText(failure, baseUrl, model, strings)
    Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = if (onRetry != null) 4.dp else 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Outlined.ErrorOutline,
                    null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Text(
                text.hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(top = 4.dp)
            )
            text.detail?.let { detail ->
                Text(
                    detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                    maxLines = 3,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (onRetry != null) {
                TextButton(onClick = onRetry, modifier = Modifier.align(Alignment.End)) {
                    Text(strings.aiAssist.retry)
                }
            }
        }
    }
}
