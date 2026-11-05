package io.rudione.chatone.presentation.ai

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.ai_dl_cancel
import chatone.composeapp.generated.resources.ai_dl_downloading
import chatone.composeapp.generated.resources.ai_dl_failed
import chatone.composeapp.generated.resources.ai_dl_resume_note
import chatone.composeapp.generated.resources.ai_dl_retry
import chatone.composeapp.generated.resources.ai_dl_retrying
import chatone.composeapp.generated.resources.ai_dl_verifying
import chatone.composeapp.generated.resources.ai_local_model_generic_hint
import chatone.composeapp.generated.resources.ai_model_note_deepseek_r17b
import chatone.composeapp.generated.resources.ai_model_note_gemma22b
import chatone.composeapp.generated.resources.ai_model_note_llama321b
import chatone.composeapp.generated.resources.ai_model_note_llama323b
import chatone.composeapp.generated.resources.ai_model_note_mistral7b
import chatone.composeapp.generated.resources.ai_model_note_phi3_mini
import chatone.composeapp.generated.resources.ai_model_note_qwen253b
import chatone.composeapp.generated.resources.ai_model_note_qwen257b
import chatone.composeapp.generated.resources.ai_provider_needs_key_hint
import chatone.composeapp.generated.resources.ai_provider_no_key_hint
import chatone.composeapp.generated.resources.ai_server_reachable
import chatone.composeapp.generated.resources.ai_server_unreachable
import chatone.composeapp.generated.resources.ai_setup_active
import chatone.composeapp.generated.resources.ai_setup_api_key
import chatone.composeapp.generated.resources.ai_setup_cloud
import chatone.composeapp.generated.resources.ai_setup_get_key
import chatone.composeapp.generated.resources.ai_setup_install
import chatone.composeapp.generated.resources.ai_setup_installed
import chatone.composeapp.generated.resources.ai_setup_intro
import chatone.composeapp.generated.resources.ai_setup_local
import chatone.composeapp.generated.resources.ai_setup_local_source
import chatone.composeapp.generated.resources.ai_setup_need_ollama
import chatone.composeapp.generated.resources.ai_setup_save
import chatone.composeapp.generated.resources.ai_setup_skip
import chatone.composeapp.generated.resources.ai_setup_title
import chatone.composeapp.generated.resources.ai_setup_use
import chatone.composeapp.generated.resources.ai_temperature
import chatone.composeapp.generated.resources.ai_temperature_hint
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.outlined.Delete
import io.rudione.chatone.icons.material.outlined.Memory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.remote.AiAssistantClient
import io.rudione.chatone.data.remote.AiCloudProvider
import io.rudione.chatone.data.remote.AiLocalModel
import io.rudione.chatone.data.remote.AiProviders
import io.rudione.chatone.data.remote.OllamaInstalledModel
import io.rudione.chatone.data.remote.formatDownloadSpeed
import io.rudione.chatone.data.remote.formatEta
import io.rudione.chatone.data.remote.formatModelSize
import io.rudione.chatone.data.repository.AiAssistantController
import io.rudione.chatone.domain.model.ModelDownloadState
import io.rudione.chatone.domain.usecase.CancelModelDownloadUseCase
import io.rudione.chatone.domain.usecase.DeleteModelUseCase
import io.rudione.chatone.domain.usecase.DownloadModelUseCase
import io.rudione.chatone.domain.usecase.ListInstalledModelsUseCase
import io.rudione.chatone.domain.usecase.ObserveModelDownloadsUseCase
import io.rudione.chatone.domain.usecase.RetryModelDownloadUseCase
import io.rudione.chatone.presentation.ai.components.AiCard
import io.rudione.chatone.presentation.ai.components.AiMonoText
import io.rudione.chatone.presentation.ai.components.AiSectionLabel
import io.rudione.chatone.presentation.ai.components.AiStatusDot
import io.rudione.chatone.presentation.chat.components.LiquidGlassTooltipBox
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.util.link.openUrl
import io.rudione.chatone.util.system.isDesktopPlatform
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import io.rudione.chatone.presentation.components.ChatoneSlider
import io.rudione.chatone.presentation.components.ChatoneIconButton

@Composable
fun AiSetupContent(
    controller: AiAssistantController,
    client: AiAssistantClient,
    onDone: () -> Unit,
    scrollable: Boolean = true,
    showHeader: Boolean = true
) {
    val scope = rememberCoroutineScope()
    val config by controller.config.collectAsState()

    val observeDownloads: ObserveModelDownloadsUseCase = koinInject()
    val startDownload: DownloadModelUseCase = koinInject()
    val cancelDownload: CancelModelDownloadUseCase = koinInject()
    val retryDownload: RetryModelDownloadUseCase = koinInject()
    val listInstalled: ListInstalledModelsUseCase = koinInject()
    val deleteModel: DeleteModelUseCase = koinInject()

    val downloads by observeDownloads().collectAsState()

    var selected by remember { mutableStateOf<AiCloudProvider?>(null) }
    var apiKey by remember { mutableStateOf("") }
    var installed by remember { mutableStateOf<List<OllamaInstalledModel>>(emptyList()) }
    var localReachable by remember { mutableStateOf<Boolean?>(null) }

    suspend fun refreshLocal() {
        val status = client.probe(AiProviders.LOCAL_BASE_URL)
        val ok = status is AiAssistantClient.ServerStatus.Reachable
        localReachable = ok
        installed = if (ok) listInstalled() else emptyList()
    }

    LaunchedEffect(Unit) { if (isDesktopPlatform) refreshLocal() }
    LaunchedEffect(downloads.values.count { it.state is ModelDownloadState.Completed }) {
        if (isDesktopPlatform) refreshLocal()
    }

    val columnModifier = Modifier.widthIn(max = 400.dp).fillMaxWidth()
        .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
        .padding(
            horizontal = if (showHeader) 14.dp else 0.dp,
            vertical = if (showHeader) 12.dp else 4.dp
        )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            columnModifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showHeader) {
                Text(
                    stringResource(Res.string.ai_setup_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(Res.string.ai_setup_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AiSectionLabel(stringResource(Res.string.ai_setup_cloud))
            AiProviders.cloud.forEach { p ->
                CloudProviderRow(
                    provider = p,
                    expanded = selected == p,
                    apiKey = apiKey,
                    onApiKeyChange = { apiKey = it },
                    onSelect = { selected = if (selected == p) null else p; apiKey = "" },
                    onGetKey = {
                        if (p.keyUrl.isNotBlank()) openUrl(
                            p.keyUrl,
                            SettingsState.LinkOpenMode.DEFAULT
                        )
                    },
                    onSave = {
                        controller.updateConfig {
                            it.copy(
                                baseUrl = p.baseUrl,
                                model = p.defaultModel,
                                apiKey = apiKey.trim()
                            )
                        }
                        controller.markOnboardingSeen()
                        onDone()
                    }
                )
            }

            if (isDesktopPlatform) {
                HorizontalDivider(
                    Modifier.padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                AiSectionLabel(stringResource(Res.string.ai_setup_local))
                when (localReachable) {
                    true -> AiStatusDot(true, stringResource(Res.string.ai_server_reachable))
                    false -> {
                        AiStatusDot(false, stringResource(Res.string.ai_server_unreachable))
                        Text(
                            stringResource(Res.string.ai_setup_need_ollama),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = {
                            openUrl(
                                "https://ollama.com/download",
                                SettingsState.LinkOpenMode.DEFAULT
                            )
                        }) {
                            Text(
                                "ollama.com/download",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    null -> {}
                }
                Text(
                    "${stringResource(Res.string.ai_setup_local_source)} · ${stringResource(Res.string.ai_dl_resume_note)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val installedNames = installed.map { it.name }.toSet()
                AiProviders.local.forEach { m ->
                    LocalModelRow(
                        model = m,
                        installed = m.name in installedNames,
                        download = downloads[m.name]?.state,
                        reachable = localReachable == true,
                        onInstall = { startDownload(m.name) },
                        onCancel = { cancelDownload(m.name) },
                        onRetry = { retryDownload(m.name) },
                        onUse = {
                            controller.updateConfig {
                                it.copy(
                                    baseUrl = AiProviders.LOCAL_BASE_URL,
                                    model = m.name,
                                    apiKey = ""
                                )
                            }
                            controller.markOnboardingSeen()
                            onDone()
                        }
                    )
                }

                if (installed.isNotEmpty()) {
                    AiSectionLabel(stringResource(Res.string.ai_setup_installed))
                    installed.forEach { im ->

                        val isActive = config.baseUrl == AiProviders.LOCAL_BASE_URL &&
                                (config.model == im.name || "${config.model}:latest" == im.name)
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    im.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                                val size = formatModelSize(im.sizeBytes)
                                if (size.isNotEmpty()) Text(
                                    size,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isActive) {
                                Text(
                                    "✓ ${stringResource(Res.string.ai_setup_active)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        controller.updateConfig {
                                            it.copy(
                                                baseUrl = AiProviders.LOCAL_BASE_URL,
                                                model = im.name,
                                                apiKey = ""
                                            )
                                        }
                                        controller.markOnboardingSeen()
                                    },
                                    contentPadding = PaddingValues(
                                        horizontal = 10.dp,
                                        vertical = 2.dp
                                    ),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(
                                        stringResource(Res.string.ai_setup_use),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            ChatoneIconButton(
                                onClick = { scope.launch { deleteModel(im.name); refreshLocal() } },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    null,
                                    Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

            }

            HorizontalDivider(
                Modifier.padding(vertical = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                AiSectionLabel(stringResource(Res.string.ai_temperature), Modifier.weight(1f))
                Text(
                    ((config.temperature * 10).toInt() / 10.0).toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            ChatoneSlider(
                value = config.temperature.toFloat().coerceIn(0f, 1.5f),
                onValueChange = { v -> controller.updateConfig { it.copy(temperature = v.toDouble()) } },
                valueRange = 0f..1.5f
            )
            Text(
                stringResource(Res.string.ai_temperature_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(2.dp))
            OutlinedButton(
                onClick = {
                    controller.markOnboardingSeen()
                    onDone()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(Res.string.ai_setup_skip)) }
        }
    }
}

private fun cloudProviderAccent(name: String): Color = when {
    name.contains("Groq", ignoreCase = true) -> Color(0xFFF55036)
    name.contains("Gemini", ignoreCase = true) -> Color(0xFF1A73E8)
    name.contains("OpenAI", ignoreCase = true) -> Color(0xFF10A37F)
    name.contains("Anthropic", ignoreCase = true) -> Color(0xFFCC785C)
    name.contains("xAI", ignoreCase = true) -> Color(0xFF5B5B66)
    name.contains("DeepSeek", ignoreCase = true) -> Color(0xFF4C6FFF)
    name.contains("DashScope", ignoreCase = true) -> Color(0xFFFF6A00)
    else -> Color(0xFF6C6C6C)
}

private fun providerMonogram(name: String): String =
    name.substringBefore(" (").take(2).uppercase()

@Composable
private fun ProviderAvatar(name: String, size: androidx.compose.ui.unit.Dp = 32.dp) {
    val accent = cloudProviderAccent(name)
    val textColor = if (accent.luminance() < 0.5f) Color.White else Color.Black
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(accent),
        contentAlignment = Alignment.Center
    ) {
        Text(
            providerMonogram(name),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LocalModelAvatar(size: androidx.compose.ui.unit.Dp = 32.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.Memory,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun CloudProviderRow(
    provider: AiCloudProvider,
    expanded: Boolean,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    onSelect: () -> Unit,
    onGetKey: () -> Unit,
    onSave: () -> Unit
) {
    AiCard(selected = expanded, onClick = onSelect) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LiquidGlassTooltipBox(
                tooltip = if (provider.needsKey) stringResource(Res.string.ai_provider_needs_key_hint) else stringResource(
                    Res.string.ai_provider_no_key_hint
                )
            ) {
                ProviderAvatar(provider.name)
            }
            Text(
                provider.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                provider.defaultModel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        if (expanded) {
            if (provider.needsKey) {
                io.rudione.chatone.presentation.components.ChatoneSecretField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(Res.string.ai_setup_api_key),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (provider.keyUrl.isNotBlank()) {
                        TextButton(
                            onClick = onGetKey,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                stringResource(Res.string.ai_setup_get_key),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onSave,
                        enabled = apiKey.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            stringResource(Res.string.ai_setup_save),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(Res.string.ai_setup_save)) }
            }
        }
    }
}

@Composable
private fun localModelNote(modelName: String): String = when (modelName) {
    "llama3.2:1b" -> stringResource(Res.string.ai_model_note_llama321b)
    "llama3.2:3b" -> stringResource(Res.string.ai_model_note_llama323b)
    "qwen2.5:3b" -> stringResource(Res.string.ai_model_note_qwen253b)
    "gemma2:2b" -> stringResource(Res.string.ai_model_note_gemma22b)
    "phi3:mini" -> stringResource(Res.string.ai_model_note_phi3_mini)
    "qwen2.5:7b" -> stringResource(Res.string.ai_model_note_qwen257b)
    "deepseek-r1:7b" -> stringResource(Res.string.ai_model_note_deepseek_r17b)
    "mistral:7b" -> stringResource(Res.string.ai_model_note_mistral7b)
    else -> ""
}

@Composable
private fun LocalModelRow(
    model: AiLocalModel,
    installed: Boolean,
    download: ModelDownloadState?,
    reachable: Boolean,
    onInstall: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onUse: () -> Unit
) {
    val note = localModelNote(model.name)
    AiCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LiquidGlassTooltipBox(
                tooltip = note.ifBlank { stringResource(Res.string.ai_local_model_generic_hint) }
            ) {
                LocalModelAvatar()
            }
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        model.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        model.sizeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when {
                download is ModelDownloadState.Running || download is ModelDownloadState.Queued ->
                    TextButton(onClick = onCancel) { Text(stringResource(Res.string.ai_dl_cancel)) }

                download is ModelDownloadState.Failed ->
                    OutlinedButton(onClick = onRetry) { Text(stringResource(Res.string.ai_dl_retry)) }

                installed || download is ModelDownloadState.Completed ->
                    OutlinedButton(onClick = onUse) { Text(stringResource(Res.string.ai_setup_save)) }

                else ->
                    OutlinedButton(onClick = onInstall) { Text(stringResource(Res.string.ai_setup_install)) }
            }
        }

        when (download) {
            is ModelDownloadState.Running -> DownloadProgress(download)
            is ModelDownloadState.Queued -> LinearProgressIndicator(Modifier.fillMaxWidth())
            is ModelDownloadState.Failed ->
                Text(
                    "${stringResource(Res.string.ai_dl_failed)}: ${download.message}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )

            else -> {}
        }

        if (!reachable && !installed && download == null) {
            AiMonoText("ollama pull ${model.name}")
        }
    }
}

@Composable
private fun DownloadProgress(state: ModelDownloadState.Running) {
    val label = when (state.phase) {
        ModelDownloadState.Phase.DOWNLOADING -> stringResource(Res.string.ai_dl_downloading)
        ModelDownloadState.Phase.VERIFYING -> stringResource(Res.string.ai_dl_verifying)
        ModelDownloadState.Phase.RETRYING -> stringResource(Res.string.ai_dl_retrying)
    }
    val determinate = state.phase == ModelDownloadState.Phase.DOWNLOADING && state.totalBytes > 0
    if (determinate) {
        LinearProgressIndicator(
            progress = { state.percent / 100f },
            modifier = Modifier.fillMaxWidth()
        )
        val speed = formatDownloadSpeed(state.bytesPerSec)
        val eta = formatEta(state.etaSeconds)
        val extra = buildString {
            if (speed.isNotEmpty()) append(" · $speed")
            if (eta.isNotEmpty()) append(" · $eta")
        }
        Text(
            "$label ${state.percent}% · ${formatModelSize(state.completedBytes)} / ${
                formatModelSize(
                    state.totalBytes
                )
            }$extra",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
