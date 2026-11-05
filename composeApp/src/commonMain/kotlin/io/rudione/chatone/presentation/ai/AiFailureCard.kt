package io.rudione.chatone.presentation.ai

import chatone.composeapp.generated.resources.ai_assist_error_empty_hint
import chatone.composeapp.generated.resources.ai_assist_error_empty_title
import chatone.composeapp.generated.resources.ai_assist_error_key_hint
import chatone.composeapp.generated.resources.ai_assist_error_key_title
import chatone.composeapp.generated.resources.ai_assist_error_model_hint
import chatone.composeapp.generated.resources.ai_assist_error_model_local_hint
import chatone.composeapp.generated.resources.ai_assist_error_model_title
import chatone.composeapp.generated.resources.ai_assist_error_network_hint
import chatone.composeapp.generated.resources.ai_assist_error_network_title
import chatone.composeapp.generated.resources.ai_assist_error_overloaded_hint
import chatone.composeapp.generated.resources.ai_assist_error_overloaded_title
import chatone.composeapp.generated.resources.ai_assist_error_rate_limited_hint
import chatone.composeapp.generated.resources.ai_assist_error_rate_limited_title
import chatone.composeapp.generated.resources.ai_assist_error_request_hint
import chatone.composeapp.generated.resources.ai_assist_error_request_title
import chatone.composeapp.generated.resources.ai_assist_error_server_hint
import chatone.composeapp.generated.resources.ai_assist_error_server_title
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.ai_assist_retry
import chatone.composeapp.generated.resources.ai_connect_error_hint
import chatone.composeapp.generated.resources.ai_connect_error_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
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

@Immutable
internal data class AiFailureText(val title: String, val hint: String, val detail: String?)

@Immutable
internal data class AiFailureMessage(
    val title: StringResource,
    val titleArgs: List<String>,
    val hint: StringResource,
    val hintArgs: List<String>,
    val detail: String?
)

internal fun aiFailureMessage(failure: AiFailure, baseUrl: String, model: String): AiFailureMessage {
    val provider = AiProviders.label(baseUrl)
    val local = AiProviders.isLocal(baseUrl)
    val args = listOf(provider, model)
    fun text(title: StringResource, hint: StringResource, showDetail: Boolean = false) =
        AiFailureMessage(title, args, hint, args, failure.detail?.takeIf { showDetail })
    return when (failure.kind) {
        AiFailureKind.OVERLOADED -> text(Res.string.ai_assist_error_overloaded_title, Res.string.ai_assist_error_overloaded_hint)
        AiFailureKind.RATE_LIMITED -> text(Res.string.ai_assist_error_rate_limited_title, Res.string.ai_assist_error_rate_limited_hint)
        AiFailureKind.UNAUTHORIZED -> text(Res.string.ai_assist_error_key_title, Res.string.ai_assist_error_key_hint)
        AiFailureKind.MODEL_NOT_FOUND ->
            text(Res.string.ai_assist_error_model_title, if (local) Res.string.ai_assist_error_model_local_hint else Res.string.ai_assist_error_model_hint)
        AiFailureKind.BAD_REQUEST -> text(Res.string.ai_assist_error_request_title, Res.string.ai_assist_error_request_hint, showDetail = true)
        AiFailureKind.SERVER -> AiFailureMessage(
            Res.string.ai_assist_error_server_title,
            emptyList(),
            Res.string.ai_assist_error_server_hint,
            listOf(provider, failure.status?.toString() ?: "?"),
            failure.detail
        )
        AiFailureKind.NETWORK ->
            if (local) AiFailureMessage(Res.string.ai_connect_error_title, emptyList(), Res.string.ai_connect_error_hint, listOf(baseUrl), null)
            else text(Res.string.ai_assist_error_network_title, Res.string.ai_assist_error_network_hint)
        AiFailureKind.EMPTY_REPLY -> text(Res.string.ai_assist_error_empty_title, Res.string.ai_assist_error_empty_hint)
    }
}

@Composable
internal fun aiFailureText(failure: AiFailure, baseUrl: String, model: String): AiFailureText {
    val message = aiFailureMessage(failure, baseUrl, model)
    return AiFailureText(
        title = stringResource(message.title, *message.titleArgs.toTypedArray()),
        hint = stringResource(message.hint, *message.hintArgs.toTypedArray()),
        detail = message.detail
    )
}

@Composable
internal fun AiFailureCard(
    failure: AiFailure,
    baseUrl: String,
    model: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val text = aiFailureText(failure, baseUrl, model)
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
                    Text(stringResource(Res.string.ai_assist_retry))
                }
            }
        }
    }
}
