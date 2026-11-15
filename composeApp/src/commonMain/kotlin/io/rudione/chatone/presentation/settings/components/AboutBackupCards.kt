package io.rudione.chatone.presentation.settings.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.app_name
import chatone.composeapp.generated.resources.automod_export
import chatone.composeapp.generated.resources.icon
import chatone.composeapp.generated.resources.legal_privacy
import chatone.composeapp.generated.resources.legal_support
import chatone.composeapp.generated.resources.legal_terms
import chatone.composeapp.generated.resources.settings_app_info
import chatone.composeapp.generated.resources.settings_backup_desc
import chatone.composeapp.generated.resources.settings_backup_export_csv
import chatone.composeapp.generated.resources.settings_backup_export_json
import chatone.composeapp.generated.resources.settings_backup_export_xlsx
import chatone.composeapp.generated.resources.settings_backup_import
import chatone.composeapp.generated.resources.settings_backup_title
import chatone.composeapp.generated.resources.settings_telegram
import chatone.composeapp.generated.resources.settings_version
import io.rudione.chatone.presentation.components.ChatoneActionRow
import io.rudione.chatone.presentation.components.ChatoneButton
import io.rudione.chatone.presentation.components.ChatoneButtonText
import io.rudione.chatone.presentation.components.ChatoneButtonTone
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu
import io.rudione.chatone.presentation.legal.AppLinks
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.settings.settingsAnchor
import io.rudione.chatone.util.BuildConfig
import io.rudione.chatone.util.automod.readAutomodText
import io.rudione.chatone.util.automod.saveAutomodText
import io.rudione.chatone.util.settings.SettingsImportExport
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AboutCard() {

    val uriHandler = LocalUriHandler.current

    SettingsGroup(stringResource(Res.string.settings_app_info)) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Image(
                    painter = painterResource(Res.drawable.icon),
                    contentDescription = stringResource(Res.string.app_name),
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                )

                Spacer(Modifier.width(14.dp))

                Column {

                    Text(
                        stringResource(Res.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "${stringResource(Res.string.settings_version)} ${BuildConfig.VERSION}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            listOf(
                Res.string.legal_privacy to AppLinks.Privacy,
                Res.string.legal_terms to AppLinks.Terms,
                Res.string.legal_support to AppLinks.Issues
            ).forEach { (label, url) ->
                ChatoneButton(
                    text = stringResource(label),
                    onClick = { uriHandler.openUri(url) },
                    tone = ChatoneButtonTone.Neutral,
                    modifier = Modifier.fillMaxWidth().settingsAnchor(stringResource(label)).padding(top = 6.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(Res.string.settings_telegram),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "https://t.me/rudionee",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        uriHandler.openUri("https://t.me/rudionee")
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
internal fun BackupCard(vm: SettingsViewModel) {
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    SettingsGroup(stringResource(Res.string.settings_backup_title)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(Res.string.settings_backup_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            ChatoneActionRow {
                Box {
                    OutlinedButton(onClick = { menuOpen = true }) {
                        ChatoneButtonText(stringResource(Res.string.automod_export))
                    }
                    ChatoneDropdownMenu(menuOpen, { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.settings_backup_export_json)) },
                            onClick = {
                                menuOpen = false
                                scope.launch {
                                    val backup =
                                        SettingsImportExport.snapshot(
                                            SettingsViewModel.settings,
                                            redactSecrets = true
                                        )
                                    val text =
                                        SettingsImportExport.toJson(backup)
                                    saveAutomodText(
                                        "chatone-settings.json",
                                        text
                                    )
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.settings_backup_export_csv)) },
                            onClick = {
                                menuOpen = false
                                scope.launch {
                                    val backup =
                                        SettingsImportExport.snapshot(
                                            SettingsViewModel.settings,
                                            redactSecrets = true
                                        )
                                    val text =
                                        SettingsImportExport.toCsv(backup)
                                    saveAutomodText(
                                        "chatone-settings.csv",
                                        text
                                    )
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.settings_backup_export_xlsx)) },
                            onClick = {
                                menuOpen = false
                                scope.launch {
                                    val backup =
                                        SettingsImportExport.snapshot(
                                            SettingsViewModel.settings,
                                            redactSecrets = true
                                        )
                                    val text =
                                        SettingsImportExport.toXlsx(backup)
                                    saveAutomodText(
                                        "chatone-settings.xlsx",
                                        text
                                    )
                                }
                            }
                        )
                    }
                }
                OutlinedButton(onClick = {
                    scope.launch {
                        val text = readAutomodText() ?: return@launch
                        vm.sendEvent(SettingsEvent.OnImportSettingsText(text))
                    }
                }) {
                    ChatoneButtonText(stringResource(Res.string.settings_backup_import))
                }
            }
        }
    }
}
