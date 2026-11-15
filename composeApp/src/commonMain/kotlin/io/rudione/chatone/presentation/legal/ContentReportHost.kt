package io.rudione.chatone.presentation.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.*
import androidx.compose.ui.platform.LocalClipboard
import io.rudione.chatone.util.platform.setText
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import io.rudione.chatone.presentation.nativecontext.SuspendNativeMessageMenus

@Composable
fun ContentReportHost() {
    val report by ContentReports.pending.collectAsState()
    val target = report ?: return
    var reason by remember(target) { mutableStateOf("") }
    var details by remember(target) { mutableStateOf("") }
    var openFailed by remember(target) { mutableStateOf(false) }
    SuspendNativeMessageMenus()
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { ContentReports.dismiss(target) },
        title = { Text(stringResource(Res.string.report_content)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(Res.string.report_explanation, AppLinks.ReportEmail))
                Text("${target.login} · #${target.channel.ifBlank { target.login }}", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(reason, { reason = it.take(150) }, label = { Text(stringResource(Res.string.report_reason)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(details, { details = it.take(2000) }, label = { Text(stringResource(Res.string.report_details)) }, modifier = Modifier.fillMaxWidth(), maxLines = 4)
                Text(stringResource(Res.string.report_twitch_hint), style = MaterialTheme.typography.bodySmall)
                if (openFailed) Text(stringResource(Res.string.report_mail_unavailable), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { scope.launch { clipboard.setText(target.body(reason, details)) } }) {
                    Text(stringResource(Res.string.report_copy))
                }
            }
        },
        confirmButton = {
            TextButton(enabled = reason.isNotBlank(), onClick = {
                openFailed = runCatching { uriHandler.openUri(target.mailto(reason, details)) }.isFailure
            }) { Text(stringResource(Res.string.report_prepare_email)) }
        },
        dismissButton = {
            TextButton(onClick = { ContentReports.dismiss(target) }) { Text(stringResource(Res.string.close)) }
        }
    )
}
