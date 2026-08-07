package io.rudione.chatone.presentation.ai

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.link.openUrl
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import io.rudione.chatone.presentation.components.ChatoneSlider
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneTextField

@Composable
fun AiSetupContent(
    controller: AiAssistantController,
    client: AiAssistantClient,
    onDone: () -> Unit,
    scrollable: Boolean = true,
    showHeader: Boolean = true
) {
    val s = LocalStrings.current
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

    LaunchedEffect(Unit) { refreshLocal() }
    LaunchedEffect(downloads.values.count { it.state is ModelDownloadState.Completed }) { refreshLocal() }

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
                s.aiSetupTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                s.aiSetupIntro,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AiSectionLabel(s.aiSetupCloud)
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

        HorizontalDivider(
            Modifier.padding(vertical = 2.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        AiSectionLabel(s.aiSetupLocal)
        when (localReachable) {
            true -> AiStatusDot(true, s.aiServerReachable)
            false -> {
                AiStatusDot(false, s.aiServerUnreachable)
                Text(
                    s.aiSetupNeedOllama,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = {
                    openUrl(
                        "https://ollama.com/download",
                        SettingsState.LinkOpenMode.DEFAULT
                    )
                }) {
                    Text("ollama.com/download", style = MaterialTheme.typography.labelMedium)
                }
            }

            null -> {}
        }
        Text(
            "${s.aiSetupLocalSource} · ${s.aiDlResumeNote}",
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
            AiSectionLabel(s.aiSetupInstalled)
            installed.forEach { im ->

                val isActive = config.baseUrl == AiProviders.LOCAL_BASE_URL &&
                        (config.model == im.name || "${config.model}:latest" == im.name)
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(im.name, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        val size = formatModelSize(im.sizeBytes)
                        if (size.isNotEmpty()) Text(
                            size,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isActive) {
                        Text(
                            "✓ ${s.aiSetupActive}",
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
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) { Text(s.aiSetupUse, style = MaterialTheme.typography.labelMedium) }
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

        HorizontalDivider(
            Modifier.padding(vertical = 2.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            AiSectionLabel(s.aiTemperature, Modifier.weight(1f))
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
            s.aiTemperatureHint,
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
        ) { Text(s.aiSetupSkip) }
    }
    }
}

private fun cloudProviderAccent(name: String): Color = when {
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
    val s = LocalStrings.current
    AiCard(selected = expanded, onClick = onSelect) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LiquidGlassTooltipBox(
                tooltip = if (provider.needsKey) s.aiProviderNeedsKeyHint else s.aiProviderNoKeyHint
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
                ChatoneTextField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = s.aiSetupApiKey,
                    textStyle = MaterialTheme.typography.bodySmall,
                    singleLine = true
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
                            Text(s.aiSetupGetKey, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    OutlinedButton(
                        onClick = onSave,
                        enabled = apiKey.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) { Text(s.aiSetupSave, style = MaterialTheme.typography.labelMedium) }
                }
            } else {
                OutlinedButton(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(s.aiSetupSave) }
            }
        }
    }
}

private fun localModelNote(s: AppStrings, modelName: String): String = when (modelName) {
    "llama3.2:1b" -> s.aiModelNoteLlama321b
    "llama3.2:3b" -> s.aiModelNoteLlama323b
    "qwen2.5:3b" -> s.aiModelNoteQwen253b
    "gemma2:2b" -> s.aiModelNoteGemma22b
    "phi3:mini" -> s.aiModelNotePhi3Mini
    "qwen2.5:7b" -> s.aiModelNoteQwen257b
    "deepseek-r1:7b" -> s.aiModelNoteDeepseekR17b
    "mistral:7b" -> s.aiModelNoteMistral7b
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
    val s = LocalStrings.current
    val note = localModelNote(s, model.name)
    AiCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LiquidGlassTooltipBox(
                tooltip = note.ifBlank { s.aiLocalModelGenericHint }
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
                    TextButton(onClick = onCancel) { Text(s.aiDlCancel) }

                download is ModelDownloadState.Failed ->
                    OutlinedButton(onClick = onRetry) { Text(s.aiDlRetry) }

                installed || download is ModelDownloadState.Completed ->
                    OutlinedButton(onClick = onUse) { Text(s.aiSetupSave) }

                else ->
                    OutlinedButton(onClick = onInstall) { Text(s.aiSetupInstall) }
            }
        }

        when (download) {
            is ModelDownloadState.Running -> DownloadProgress(download)
            is ModelDownloadState.Queued -> LinearProgressIndicator(Modifier.fillMaxWidth())
            is ModelDownloadState.Failed ->
                Text(
                    "${s.aiDlFailed}: ${download.message}",
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
    val s = LocalStrings.current
    val label = when (state.phase) {
        ModelDownloadState.Phase.DOWNLOADING -> s.aiDlDownloading
        ModelDownloadState.Phase.VERIFYING -> s.aiDlVerifying
        ModelDownloadState.Phase.RETRYING -> s.aiDlRetrying
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
