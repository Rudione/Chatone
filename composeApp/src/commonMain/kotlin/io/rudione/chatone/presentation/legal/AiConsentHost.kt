package io.rudione.chatone.presentation.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.*
import io.rudione.chatone.domain.ai.AiConsentStore
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import io.rudione.chatone.data.remote.AiProviders
import io.rudione.chatone.presentation.nativecontext.SuspendNativeMessageMenus

@Composable
fun AiConsentHost(store: AiConsentStore = koinInject()) {
    val pending by store.pending.collectAsState()
    val request = pending ?: return
    SuspendNativeMessageMenus()
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = { store.respond(request, false) },
        title = { Text(stringResource(Res.string.ai_consent_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val provider = AiProviders.providerFor(request.recipient)?.name
                    ?: AiProviders.label(request.recipient)
                Text(stringResource(Res.string.ai_consent_body, "$provider\n${request.recipient}"))
                TextButton(onClick = { uriHandler.openUri(AppLinks.Privacy) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.legal_privacy))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { store.respond(request, true) }) { Text(stringResource(Res.string.ai_consent_accept)) }
        },
        dismissButton = {
            TextButton(onClick = { store.respond(request, false) }) { Text(stringResource(Res.string.ai_consent_decline)) }
        }
    )
}
