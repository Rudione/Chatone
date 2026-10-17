package io.rudione.chatone.presentation.ai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.automirrored.filled.Send
import io.rudione.chatone.icons.material.filled.Add
import io.rudione.chatone.icons.material.filled.AutoAwesome
import io.rudione.chatone.icons.material.filled.Check
import io.rudione.chatone.icons.material.filled.Close
import io.rudione.chatone.icons.material.outlined.Delete
import io.rudione.chatone.icons.material.outlined.History
import io.rudione.chatone.icons.material.outlined.ErrorOutline
import io.rudione.chatone.icons.material.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.remote.AiAssistantClient
import io.rudione.chatone.data.remote.AiChatMessage
import io.rudione.chatone.presentation.ai.components.AiChip
import io.rudione.chatone.presentation.ai.components.AiMessageActions
import io.rudione.chatone.data.repository.AiAssistantController
import io.rudione.chatone.data.repository.AiThread
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.SystemBackHandler
import io.rudione.chatone.util.system.isDesktopPlatform
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu
import io.rudione.chatone.presentation.components.ChatoneTextField
import io.rudione.chatone.presentation.components.DockState
import io.rudione.chatone.presentation.components.SwipeDirection
import io.rudione.chatone.presentation.components.swipeToDismiss
import io.rudione.chatone.presentation.stream.FloatingStreamPlayer
import io.rudione.chatone.presentation.stream.LocalFloatingStreamPlayer
import androidx.compose.ui.unit.Dp

private data class Preset(
    val label: String,
    val emoji: String,
    val needsChat: Boolean,
    val useMentions: Boolean,
    val prompt: String
)

@Composable
fun AiAssistantOverlay(
    controller: AiAssistantController = koinInject(),
    client: AiAssistantClient = koinInject()
) {
    val open by controller.isOpen.collectAsState()
    val floating by controller.floating.collectAsState()
    if (DockState.large && !floating) return
    val session = rememberAiAssistantSession(controller, client)
    if (open) SystemBackHandler { controller.close() }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = open, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(onClick = { controller.close() })
            )
        }
        AnimatedVisibility(
            visible = open,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            AiAssistantPanel(
                controller = controller,
                client = client,
                session = session,
                bottomClearance = LocalFloatingStreamPlayer.current?.bottomEndClearance ?: 0.dp,
                modifier = Modifier.swipeToDismiss(SwipeDirection.Right) { controller.close() }
            )
        }
        if (open) FloatingStreamPlayer(onExpand = { controller.close() })
    }
}

@Composable
internal fun AiAssistantPanel(
    controller: AiAssistantController,
    client: AiAssistantClient,
    modifier: Modifier = Modifier,
    session: AiAssistantSession = rememberAiAssistantSession(controller, client),
    bottomClearance: Dp = 0.dp
) {
    val s = LocalStrings.current
    val config by controller.config.collectAsState()

    val presets = remember(s) {
        listOf(
            Preset(s.aiPresetSummary, "📋", true, false, s.aiPresetSummaryPrompt),
            Preset(s.aiPresetMood, "🎭", true, false, s.aiPresetMoodPrompt),
            Preset(s.aiPresetMentions, "🔔", true, true, s.aiPresetMentionsPrompt),
            Preset(s.aiPresetRisky, "🛡️", true, false, s.aiPresetRiskyPrompt),
            Preset(s.aiPresetReply, "✍️", true, false, s.aiPresetReplyPrompt),
            Preset(s.aiPresetIdea, "💡", false, false, s.aiPresetIdeaPrompt),
            Preset(s.aiPresetRule, "🛠️", true, false, s.aiPresetRulePrompt)
        )
    }
    val clipboard = LocalClipboardManager.current
    val messages = session.messages
    val sending = session.sending

    LaunchedEffect(session, messages.size) {
        if (messages.isNotEmpty()) session.listState.animateScrollToItem(messages.size - 1)
    }

    val pendingPrompt by controller.pendingPrompt.collectAsState()
    SideEffect(session, pendingPrompt) {
        controller.consumePendingPrompt()?.let { session.runPendingPrompt(it, localReachable = isDesktopPlatform) }
    }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .widthIn(min = 320.dp, max = 420.dp)
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.End))
                .padding(bottom = bottomClearance)
        ) {
            Row(
                Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f))
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    s.aiAssistantTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                ChatoneIconButton(onClick = { session.showSetup = !session.showSetup }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Outlined.Tune, s.aiSetupManage, modifier = Modifier.size(18.dp))
                }
                ChatoneIconButton(
                    onClick = { session.showHistory = !session.showHistory },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Outlined.History, s.aiHistory, modifier = Modifier.size(18.dp))
                }
                ChatoneIconButton(onClick = session::newThread, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Filled.Add, s.aiNewChat, modifier = Modifier.size(18.dp))
                }
                ChatoneIconButton(onClick = { controller.close() }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Filled.Close, s.aiClose, modifier = Modifier.size(18.dp))
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            if (session.showSetup) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    AiSetupContent(
                        controller = controller,
                        client = client,
                        onDone = { session.showSetup = false })
                }
            } else if (session.showHistory) {
                ThreadHistory(
                    threads = session.threads,
                    onOpen = session::openThread,
                    onDelete = session::deleteThread
                )
            } else {
                LazyColumn(
                    state = session.listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (messages.isEmpty()) {
                        item(key = "empty", contentType = "empty") { EmptyState() }
                    }
                    itemsIndexed(messages, contentType = { _, m -> m.role }) { index, m ->
                        MessageBubble(
                            m = m,
                            onCopy = { clipboard.setText(AnnotatedString(AiActions.strip(m.content))) },
                            onRegenerate = if (m.role == AiChatMessage.ASSISTANT && index == messages.lastIndex && !sending) {
                                { session.regenerate(index) }
                            } else null,
                            variantCount = session.variantCount(index),
                            variantIndex = session.variantIndex(index),
                            onPrevVariant = { session.selectVariant(index, session.variantIndex(index) - 1) },
                            onNextVariant = { session.selectVariant(index, session.variantIndex(index) + 1) }
                        )
                    }
                    if (sending) {
                        item(key = "thinking", contentType = "thinking") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    s.aiThinking,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    session.error?.let {
                        item(key = "error", contentType = "error") {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.ErrorOutline, null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            s.aiConnectErrorTitle,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    Text(
                                        s.aiConnectErrorHint.replace("{0}", config.baseUrl),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ModelPickerChip(
                        controller = controller,
                        client = client,
                        config = config,
                        enabled = !sending
                    )
                    AiChip(
                        label = s.aiAttachChat,
                        leadingEmoji = "📎",
                        selected = session.attachChat,
                        enabled = !sending,
                        onClick = { session.attachChat = !session.attachChat }
                    )
                    presets.forEach { p ->
                        AiChip(
                            label = p.label,
                            leadingEmoji = p.emoji,
                            enabled = !sending,
                            onClick = {
                                session.send(
                                    p.prompt,
                                    includeChat = p.needsChat,
                                    useMentions = p.useMentions
                                )
                            }
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp, bottom = 9.dp, top = 2.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ChatoneTextField(
                        value = session.input,
                        onValueChange = { session.input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = s.aiInputPlaceholder,
                        singleLine = false,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions.Default
                    )
                    AiSendButton(
                        enabled = session.input.isNotBlank() && !sending,
                        contentDescription = s.chatSend,
                        onClick = { session.send(session.input, includeChat = session.attachChat, useMentions = false) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AiSendButton(
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            !enabled -> 0.92f
            pressed -> 0.9f
            hovered -> 1.08f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "aiSendScale"
    )
    val container by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
            hovered || pressed -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        },
        animationSpec = tween(160),
        label = "aiSendBg"
    )
    val iconTint =
        if (enabled) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)

    Box(
        modifier = Modifier
            .padding(bottom = 1.dp)
            .size(32.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .drawBehind { drawRect(container) }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Send,
            contentDescription,
            modifier = Modifier.size(15.dp),
            tint = iconTint
        )
    }
}

private fun shortModelName(model: String): String {
    val base = model.substringAfterLast('/').removeSuffix(":latest")
    return if (base.length <= 20) base else base.take(19) + "…"
}

@Composable
private fun ModelPickerChip(
    controller: AiAssistantController,
    client: AiAssistantClient,
    config: AiAssistantController.Config,
    enabled: Boolean
) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    Box {
        AiChip(
            label = shortModelName(config.model),
            leadingEmoji = "🧠",
            selected = true,
            enabled = enabled,
            onClick = {
                menuOpen = true
                if (!loading) {
                    loading = true
                    scope.launch {
                        models = client.listModels(config.baseUrl, config.apiKey)
                        loading = false
                    }
                }
            }
        )
        ChatoneDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            Text(
                s.aiModel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            when {
                loading && models.isEmpty() -> DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                            Text("…", style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    onClick = {},
                    enabled = false
                )

                models.isEmpty() -> DropdownMenuItem(
                    text = {
                        Text(
                            s.aiServerUnreachable,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    onClick = {},
                    enabled = false
                )

                else -> models.forEach { m ->
                    val isCurrent = m == config.model || m == "${config.model}:latest"
                    DropdownMenuItem(
                        text = {
                            Text(
                                m,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        trailingIcon = if (isCurrent) {
                            {
                                Icon(
                                    Icons.Filled.Check,
                                    null,
                                    Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else null,
                        onClick = {
                            controller.updateConfig { it.copy(model = m) }
                            menuOpen = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    val s = LocalStrings.current
    Column(
        Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Filled.AutoAwesome,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        )
        Text(
            s.aiGreetingTitle,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            s.aiGreetingBody,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MessageBubble(
    m: AiChatMessage,
    onCopy: () -> Unit,
    onRegenerate: (() -> Unit)?,
    variantCount: Int,
    variantIndex: Int,
    onPrevVariant: () -> Unit,
    onNextVariant: () -> Unit
) {
    if (m.role == AiChatMessage.SYSTEM) return
    val isUser = m.role == AiChatMessage.USER
    val proposal = if (!isUser) remember(m.content) { AiActions.parse(m.content) } else null
    val text =
        if (proposal != null) remember(m.content) { AiActions.strip(m.content) } else m.content

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (text.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                    .padding(horizontal = 11.dp, vertical = 9.dp),
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    if (isUser) LocalStrings.current.aiRoleYou else "CHATONE AI",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        letterSpacing = 1.4.sp
                    ),
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)
                )
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.94f),
                    textAlign = if (isUser) TextAlign.End else TextAlign.Start
                )
            }
        }
        if (proposal != null) AiRuleActionCard(proposal)
        if (!isUser && text.isNotBlank()) {
            AiMessageActions(
                onCopy = onCopy,
                onRegenerate = onRegenerate,
                variantCount = variantCount,
                variantIndex = variantIndex,
                onPrevVariant = onPrevVariant,
                onNextVariant = onNextVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun AiRuleActionCard(proposal: AiRuleProposal) {
    val s = LocalStrings.current
    val repo = koinInject<io.rudione.chatone.data.repository.AutomodRepository>()
    val rule = remember(proposal) { AiActions.toChatRule(proposal) } ?: return
    var state by remember(proposal) { mutableStateOf(0) }
    if (state == 2) return

    Surface(
        color = ChatoneTheme.extraColors.modUnban.copy(alpha = 0.10f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ChatoneTheme.extraColors.modUnban.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                s.aiActionProposedRule,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${rule.displayLabel} · ${rule.action} · ${rule.scopeLabel}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            if (proposal.reason.isNotBlank()) {
                Text(
                    proposal.reason,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state == 1) {
                Text(
                    s.aiActionApplied,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        repo.upsertChatRule(rule); state = 1
                    }) { Text(s.aiActionApply) }
                    TextButton(onClick = { state = 2 }) { Text(s.aiActionDismiss) }
                }
            }
        }
    }
}

@Composable
private fun ThreadHistory(
    threads: List<AiThread>,
    onOpen: (AiThread) -> Unit,
    onDelete: (AiThread) -> Unit
) {
    val s = LocalStrings.current
    if (threads.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                s.aiHistoryEmpty,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(threads, key = { it.id }, contentType = { "thread" }) { t ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth().clickable { onOpen(t) }
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        t.title,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    ChatoneIconButton(onClick = { onDelete(t) }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Outlined.Delete,
                            s.delete,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
