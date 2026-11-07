package io.rudione.chatone.presentation.chat.components

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.cancel
import chatone.composeapp.generated.resources.chat_banned_word_message
import chatone.composeapp.generated.resources.chat_banned_word_send_anyway
import chatone.composeapp.generated.resources.chat_banned_word_title
import chatone.composeapp.generated.resources.chat_placeholder
import chatone.composeapp.generated.resources.chat_you_are_banned
import chatone.composeapp.generated.resources.copy
import chatone.composeapp.generated.resources.cut
import chatone.composeapp.generated.resources.input_auto_translate
import chatone.composeapp.generated.resources.input_translate
import chatone.composeapp.generated.resources.paste
import org.jetbrains.compose.resources.stringResource
import io.rudione.chatone.presentation.components.layoutHeight
import io.rudione.chatone.presentation.components.layoutWidth
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.text.TextStyle
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import io.rudione.chatone.util.platform.readText
import io.rudione.chatone.util.platform.setText
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import io.rudione.chatone.data.remote.TranslationClient
import io.rudione.chatone.presentation.chat.pauseHotkeyMatches
import io.rudione.chatone.util.chat.BannedWordsGuard
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.bottomBarBackgroundColor
import io.rudione.chatone.util.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import org.koin.compose.koinInject
import io.rudione.chatone.icons.lucide.ClipboardPaste
import io.rudione.chatone.icons.lucide.Copy
import io.rudione.chatone.icons.lucide.Languages
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Scissors
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.components.ChatoneSwitch
import io.rudione.chatone.presentation.components.LiquidGlassDropdownItem
import io.rudione.chatone.presentation.components.HoverTooltipBox
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.components.PortalPopup

private object NoOpTextContextMenuProvider :
    androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider {
    override suspend fun showTextContextMenu(
        dataProvider: androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
    ) {
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    actions: MessageInputActions,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    chrome: MessageInputChrome = MessageInputChrome(),
    completions: InputCompletionState = InputCompletionState(),
    completionCallbacks: InputCompletionCallbacks = InputCompletionCallbacks(),
    upload: MessageInputUploadState = MessageInputUploadState(),
    translation: MessageInputTranslation = MessageInputTranslation(),
    glass: ChatInputGlass? = null,
    metrics: MessageInputMetrics = MessageInputMetrics.current()
) {
    val isBanned = chrome.isBanned
    val banReason = chrome.banReason
    val hidePlaceholder = chrome.hidePlaceholder
    val hideEmojiButton = chrome.hideEmojiButton
    val placeholderText = chrome.placeholderText
    val glowIntensity = chrome.glowIntensity
    val glowTriggerTs = chrome.glowTriggerTs
    val slowModeSeconds = chrome.slowModeSeconds
    val lastMessageSentAtMs = chrome.lastMessageSentAtMs

    val onSend = actions.onSend
    val onSendKeepText = actions.onSendKeepText
    val onEmotePickerClick = actions.onEmotePickerClick
    val onFocusChanged = actions.onFocusChanged

    val uploadProgress = upload.progress
    val uploadedLink = upload.link
    val onCopyUploadedLink = upload.onCopyLink

    val translationTargetLang = translation.targetLang
    val autoTranslateEnabled = translation.autoEnabled
    val onToggleAutoTranslate = translation.onToggleAuto

    val MAX_CHARS = 500
    val WARN_CHARS = 480

    var tfv by remember {
        mutableStateOf(
            TextFieldValue(
                value,
                selection = TextRange(value.length)
            )
        )
    }

    SideEffect(value) {
        if (tfv.text != value) {
            tfv = TextFieldValue(
                value,
                selection = TextRange(value.length)
            )
        }
    }

    val charCount = tfv.text.length
    val isOverLimit = charCount > MAX_CHARS
    val isNearLimit = charCount >= WARN_CHARS && !isOverLimit
    val bannedWordHit = remember(tfv.text) {
        BannedWordsGuard.containsBannedWord(tfv.text)
    }

    val canSend =
        enabled &&
                !isBanned &&
                value.isNotBlank() &&
                !isOverLimit

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    SideEffect(isFocused) { onFocusChanged(isFocused) }

    var slowmodeRemainingMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(slowModeSeconds, lastMessageSentAtMs) {
        if (slowModeSeconds <= 0 || lastMessageSentAtMs <= 0L) {
            slowmodeRemainingMs = 0L
            return@LaunchedEffect
        }
        val totalMs = slowModeSeconds * 1000L
        while (true) {
            val elapsed = Clock.System.now().toEpochMilliseconds() - lastMessageSentAtMs
            val remaining = (totalMs - elapsed).coerceAtLeast(0L)
            slowmodeRemainingMs = remaining
            if (remaining <= 0L) break
            delay(100)
        }
    }
    val slowmodeFraction = remember { Animatable(0f) }
    LaunchedEffect(slowModeSeconds) {
        snapshotFlow {
            if (slowModeSeconds > 0) (1f - slowmodeRemainingMs.toFloat() / (slowModeSeconds * 1000f)).coerceIn(
                0f,
                1f
            ) else 0f
        }.collectLatest { slowmodeFraction.animateTo(it, tween(150)) }
    }
    val showSlowmodeRing by remember { derivedStateOf { slowmodeFraction.value > 0f } }
    val slowmodeRemainingSeconds by remember {
        derivedStateOf { ((slowmodeRemainingMs + 999) / 1000).toInt().coerceAtLeast(0) }
    }
    val slowmodeColor = ChatoneTheme.extraColors.modTimeout

    val errorRed = Color(0xFFCF6679)
    val warnAmber = Color(0xFFD4A017)
    val primaryCol = MaterialTheme.colorScheme.primary

    val borderColor by animateColorAsState(
        when {
            isBanned || bannedWordHit != null || isOverLimit -> errorRed
            isNearLimit -> warnAmber
            isFocused -> MaterialTheme.colorScheme.outline
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        tween(220)
    )

    val borderWidth by animateDpAsState(
        if (isBanned || isOverLimit || bannedWordHit != null) 1.5.dp else 1.dp,
        tween(220)
    )

    val containerColor by animateColorAsState(
        when {
            isBanned -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.14f)
            isFocused -> MaterialTheme.colorScheme.surfaceContainerHighest
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        tween(220)
    )
    val shadowColor = ChatoneTheme.extraColors.shadowColor

    val bottomWallpaper = LocalWallpaperController.current.state

    val bottomBarBlur =
        bottomWallpaper.panelColorConfig.bottomBarBlurRadius

    val bottomBarColor = bottomBarBackgroundColor(
        bottomWallpaper,
        ChatoneTheme.extraColors.chatInputSurface
    )


    val clipboard = LocalClipboard.current
    val translationClient: TranslationClient = koinInject()
    val inputScope = rememberCoroutineScope()
    var isTranslatingInput by remember { mutableStateOf(false) }
    var showInputContextMenu by remember { mutableStateOf(false) }
    var inputContextMenuOffset by remember { mutableStateOf(IntOffset.Zero) }

    fun translateInputInPlace() {
        val text = tfv.text
        if (text.isBlank() || isTranslatingInput) return
        isTranslatingInput = true
        inputScope.launch {
            val result = translationClient.translate(text, translationTargetLang)
            if (result is Result.Success && result.data.text.isNotBlank()) {
                onValueChange(result.data.text)
            }
            isTranslatingInput = false
        }
    }

    var showBannedWordConfirm by remember { mutableStateOf(false) }
    var pendingKeepTextForConfirm by remember { mutableStateOf(false) }

    fun proceedSend(keepText: Boolean) {
        if (autoTranslateEnabled && tfv.text.isNotBlank()) {
            inputScope.launch {
                val result = translationClient.translate(tfv.text, translationTargetLang)
                if (result is Result.Success && result.data.text.isNotBlank()) {
                    onValueChange(result.data.text)
                }
                if (keepText) onSendKeepText() else onSend()
            }
        } else {
            if (keepText) onSendKeepText() else onSend()
        }
    }

    fun performSend(keepText: Boolean) {
        if (bannedWordHit != null) {
            pendingKeepTextForConfirm = keepText
            showBannedWordConfirm = true
            return
        }
        proceedSend(keepText)
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {

        val glowAnim = remember { Animatable(0f) }
        LaunchedEffect(glowTriggerTs, glowIntensity) {
            if (glowIntensity > 0f) {
                glowAnim.snapTo(glowIntensity)
                glowAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 5000)
                )
            }
        }
        val baseDividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        val glowColor = MaterialTheme.colorScheme.primary
        val glowAlpha: () -> Float = { glowAnim.value.coerceIn(0f, 1f) }
        if (glass == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .layoutHeight { (1f + glowAlpha() * 4f).dp }
                    .drawBehind {
                        val alpha = glowAlpha()
                        drawRect(if (alpha > 0.01f) glowColor.copy(alpha = alpha) else baseDividerColor)
                    }
            )
        }

        val surface = InputSurface(
            resting = { containerColor.compositeOver(bottomBarColor) },
            border = { androidx.compose.ui.graphics.lerp(borderColor, glowColor, glowAlpha()) },
            borderWidth = { borderWidth },
            focused = isFocused,
            shadow = shadowColor
        )
        val onTfvChange: (TextFieldValue) -> Unit = { updated ->
            tfv = updated
            onValueChange(updated.text)
        }
        val fieldContent: @Composable RowScope.() -> Unit = {
            MessageInputTextField(
                tfv = tfv,
                onTfvChange = onTfvChange,
                enabled = enabled,
                isFocused = isFocused,
                primaryCol = primaryCol,
                textStyle = metrics.textStyle(),
                focusRequester = focusRequester,
                interactionSource = interactionSource,
                hidePlaceholder = hidePlaceholder,
                placeholderText = placeholderText,
                completions = completions,
                completionCallbacks = completionCallbacks,
                canSend = canSend,
                pauseHotkey = chrome.pauseHotkey,
                onSend = { keepText -> performSend(keepText) },
                onHistoryUp = actions.onHistoryUp,
                onHistoryDown = actions.onHistoryDown,
                onTogglePause = actions.onTogglePause,
                onContextMenu = { position ->
                    inputContextMenuOffset = position
                    showInputContextMenu = true
                }
            )
            if (showInputContextMenu) {
                MessageInputContextMenu(
                    offset = inputContextMenuOffset,
                    tfv = tfv,
                    autoTranslateEnabled = autoTranslateEnabled,
                    onToggleAutoTranslate = onToggleAutoTranslate,
                    onTfvChange = onTfvChange,
                    onTranslate = { translateInputInPlace() },
                    onDismiss = { showInputContextMenu = false },
                    clipboard = clipboard,
                    scope = inputScope
                )
            }
            if (autoTranslateEnabled) {
                MessageInputTranslationChip(
                    targetLang = translationTargetLang,
                    enabled = enabled,
                    onSelectLanguage = translation.onTargetLangChange,
                    onTurnOff = { onToggleAutoTranslate(false) }
                )
            }
        }
        val slowmodeRing: @Composable BoxScope.() -> Unit = {
            if (showSlowmodeRing) {
                MessageInputSlowmodeRing(fraction = { slowmodeFraction.value }, color = slowmodeColor)
            }
        }
        val limits: @Composable () -> Unit = {
            MessageInputLimits(
                charCount = charCount,
                maxChars = MAX_CHARS,
                isNearLimit = isNearLimit,
                isOverLimit = isOverLimit,
                slowmodeRemainingSeconds = slowmodeRemainingSeconds,
                slowmodeColor = slowmodeColor
            )
        }

        if (glass != null && !metrics.touch) {
            SegmentedMessageBar(
                glass = glass,
                metrics = metrics,
                surface = surface,
                upload = upload,
                emoji = if (hideEmojiButton || isBanned) null else {
                    { MessageInputEmojiButton(enabled = enabled, metrics = metrics, onClick = onEmotePickerClick) }
                },
                send = { size ->
                    MessageInputSendButton(canSend = canSend, metrics = metrics, onClick = { performSend(false) }, diameter = size)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = glass.style.horizontalMargin,
                        end = glass.style.horizontalMargin,
                        top = glass.style.topMargin,
                        bottom = glass.style.bottomMargin
                    )
            ) { split ->
                slowmodeRing()
                if (isBanned) {
                    MessageInputBannedBanner(banReason = banReason, errorRed = errorRed)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = metrics.rowMinHeight)
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val startInset = if (hideEmojiButton) FieldInsetSplit else FieldInsetMerged
                        Spacer(Modifier.layoutWidth { androidx.compose.ui.unit.lerp(startInset, FieldInsetSplit, split()) })
                        fieldContent()
                        limits()
                        Spacer(Modifier.layoutWidth { androidx.compose.ui.unit.lerp(FieldInsetMerged, FieldInsetSplit, split()) })
                    }
                }
            }
        } else {
            MessageInputPillBar(
                glass = glass,
                metrics = metrics,
                surface = surface,
                bottomBarColor = bottomBarColor,
                bottomBarBlur = bottomBarBlur,
                showSend = !isBanned && value.isNotBlank(),
                pill = {
                    slowmodeRing()
                    if (isBanned) {
                        MessageInputBannedBanner(banReason = banReason, errorRed = errorRed)
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = metrics.rowMinHeight)
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (hideEmojiButton) {
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                MessageInputEmojiButton(enabled = enabled, metrics = metrics, onClick = onEmotePickerClick)
                            }
                            fieldContent()
                            if (upload.progress != null || upload.link != null) {
                                MessageInputUploadButton(upload = upload, size = metrics.emojiTarget)
                            }
                            if (!metrics.touch) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(end = 2.dp)
                                ) {
                                    MessageInputSendButton(canSend = canSend, metrics = metrics, onClick = { performSend(false) })
                                    limits()
                                }
                            }
                        }
                    }
                },
                send = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MessageInputSendButton(canSend = canSend, metrics = metrics, onClick = { performSend(false) })
                        limits()
                    }
                }
            )
        }

        if (showBannedWordConfirm) {
            MessageInputBannedWordDialog(
                errorRed = errorRed,
                onConfirm = {
                    showBannedWordConfirm = false
                    proceedSend(pendingKeepTextForConfirm)
                },
                onDismiss = { showBannedWordConfirm = false }
            )
        }
    }
}

internal data class InputCompletionState(
    val showEmote: Boolean = false,
    val emoteCount: Int = 0,
    val emoteTabIndex: Int = -1,
    val showMention: Boolean = false,
    val mentionCount: Int = 0,
    val mentionTabIndex: Int = -1,
    val showSlash: Boolean = false,
    val slashCount: Int = 0,
    val slashTabIndex: Int = -1
)

internal data class InputCompletionCallbacks(
    val onTabEmote: (Int) -> Unit = {},
    val onConfirmEmote: () -> Unit = {},
    val onTabMention: (Int) -> Unit = {},
    val onConfirmMention: () -> Unit = {},
    val onTabSlash: (Int) -> Unit = {},
    val onConfirmSlash: () -> Unit = {}
)

internal data class MessageInputChrome(
    val isBanned: Boolean = false,
    val banReason: String? = null,
    val hidePlaceholder: Boolean = false,
    val hideEmojiButton: Boolean = false,
    val placeholderText: String? = null,
    val pauseHotkey: String = "",
    val glowIntensity: Float = 0f,
    val glowTriggerTs: Long = 0L,
    val slowModeSeconds: Int = 0,
    val lastMessageSentAtMs: Long = 0L
)

internal data class MessageInputActions(
    val onSend: () -> Unit,
    val onSendKeepText: () -> Unit = {},
    val onHistoryUp: () -> Unit = {},
    val onHistoryDown: () -> Unit = {},
    val onEmotePickerClick: () -> Unit = {},
    val onTogglePause: () -> Unit = {},
    val onFocusChanged: (Boolean) -> Unit = {}
)

internal data class MessageInputUploadState(
    val progress: Float? = null,
    val link: String? = null,
    val onCopyLink: () -> Unit = {}
)

internal data class MessageInputTranslation(
    val targetLang: String = "en",
    val autoEnabled: Boolean = false,
    val onToggleAuto: (Boolean) -> Unit = {},
    val onTargetLangChange: (String) -> Unit = {}
)

private fun handleCompletionTab(
    completions: InputCompletionState,
    callbacks: InputCompletionCallbacks
) {
    when {
        completions.showSlash && completions.slashCount > 0 -> callbacks.onTabSlash(
            if (completions.slashTabIndex < 0) 0
            else (completions.slashTabIndex + 1) % completions.slashCount
        )

        completions.showEmote && completions.emoteCount > 0 -> callbacks.onTabEmote(
            if (completions.emoteTabIndex < 0) 0
            else (completions.emoteTabIndex + 1) % completions.emoteCount
        )

        completions.showMention && completions.mentionCount > 0 -> callbacks.onTabMention(
            if (completions.mentionTabIndex < 0) 0
            else (completions.mentionTabIndex + 1) % completions.mentionCount
        )
    }
}

private fun deleteWord(tfv: TextFieldValue, forward: Boolean): TextFieldValue? {
    val sel = tfv.selection
    val text = tfv.text
    if (!sel.collapsed) return null
    if (forward) {
        if (sel.start >= text.length) return null
        val start = sel.start
        var end = start
        while (end < text.length && text[end].isWhitespace()) end++
        while (end < text.length && !text[end].isWhitespace()) end++
        return TextFieldValue(text.removeRange(start, end), selection = TextRange(start))
    }
    if (sel.start <= 0) return null
    val end = sel.start
    var start = end
    while (start > 0 && text[start - 1].isWhitespace()) start--
    while (start > 0 && !text[start - 1].isWhitespace()) start--
    return TextFieldValue(text.removeRange(start, end), selection = TextRange(start))
}

@Composable
private fun RowScope.MessageInputTextField(
    tfv: TextFieldValue,
    onTfvChange: (TextFieldValue) -> Unit,
    enabled: Boolean,
    isFocused: Boolean,
    primaryCol: Color,
    textStyle: TextStyle,
    focusRequester: FocusRequester,
    interactionSource: MutableInteractionSource,
    hidePlaceholder: Boolean,
    placeholderText: String?,
    completions: InputCompletionState,
    completionCallbacks: InputCompletionCallbacks,
    canSend: Boolean,
    pauseHotkey: String,
    onSend: (Boolean) -> Unit,
    onHistoryUp: () -> Unit,
    onHistoryDown: () -> Unit,
    onTogglePause: () -> Unit,
    onContextMenu: (IntOffset) -> Unit
) {
    CompositionLocalProvider(
        androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuDropdownProvider provides NoOpTextContextMenuProvider
    ) {
        BasicTextField(
            value = tfv,
            onValueChange = onTfvChange,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                .focusRequester(focusRequester)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press &&
                                event.buttons.isSecondaryPressed
                            ) {
                                val pos = event.changes.first().position
                                onContextMenu(IntOffset(pos.x.toInt(), pos.y.toInt()))
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) {
                        return@onPreviewKeyEvent false
                    }
                    val isCtrl = event.isCtrlPressed || event.isMetaPressed
                    when {
                        event.key == Key.Tab && !isCtrl && !event.isAltPressed -> {
                            handleCompletionTab(completions, completionCallbacks)
                            true
                        }

                        event.key == Key.Enter && !isCtrl && !event.isShiftPressed -> when {
                            completions.showSlash && completions.slashTabIndex >= 0 -> {
                                completionCallbacks.onConfirmSlash()
                                true
                            }

                            completions.showEmote && completions.emoteTabIndex >= 0 -> {
                                completionCallbacks.onConfirmEmote()
                                true
                            }

                            completions.showMention && completions.mentionTabIndex >= 0 -> {
                                completionCallbacks.onConfirmMention()
                                true
                            }

                            canSend -> {
                                onSend(false)
                                true
                            }

                            else -> true
                        }

                        isCtrl && event.key == Key.Enter -> {
                            onSend(true)
                            true
                        }

                        event.key == Key.DirectionUp && !isCtrl && !event.isShiftPressed &&
                                !event.isAltPressed &&
                                (tfv.text.isEmpty() || tfv.selection.start == 0) -> {
                            onHistoryUp()
                            true
                        }

                        event.key == Key.DirectionDown && !isCtrl && !event.isShiftPressed &&
                                !event.isAltPressed &&
                                (tfv.text.isEmpty() ||
                                        tfv.selection.end == tfv.text.length) -> {
                            onHistoryDown()
                            true
                        }

                        event.key == Key.Backspace && event.isShiftPressed -> {
                            val updated = deleteWord(tfv, forward = false)
                            if (updated != null) {
                                onTfvChange(updated)
                                true
                            } else {
                                false
                            }
                        }

                        event.key == Key.Delete && event.isShiftPressed -> {
                            val updated = deleteWord(tfv, forward = true)
                            if (updated != null) {
                                onTfvChange(updated)
                                true
                            } else {
                                false
                            }
                        }

                        pauseHotkeyMatches(event, pauseHotkey) -> {
                            onTogglePause()
                            true
                        }

                        else -> false
                    }
                },
            enabled = enabled,
            maxLines = 4,
            interactionSource = interactionSource,
            textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(primaryCol),
            decorationBox = { inner ->
                if (tfv.text.isEmpty() && !hidePlaceholder) {
                    Text(
                        placeholderText ?: stringResource(Res.string.chat_placeholder),
                        style = textStyle,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                inner()
            }
        )
    }
}

@Composable
private fun BoxScope.MessageInputSlowmodeRing(fraction: () -> Float, color: Color) {
    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { 24.dp.toPx() }
    val strokePx = with(density) { 2.5.dp.toPx() }
    Spacer(
        modifier = Modifier.matchParentSize().drawWithCache {
            val inset = strokePx / 2f
            val radius = (cornerRadiusPx - inset).coerceAtLeast(0f)
            val fullPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = inset,
                        top = inset,
                        right = size.width - inset,
                        bottom = size.height - inset,
                        cornerRadius = CornerRadius(radius, radius)
                    )
                )
            }
            val measure = PathMeasure().apply { setPath(fullPath, false) }
            val segment = Path()
            val stroke = Stroke(width = strokePx, cap = StrokeCap.Round)
            onDrawBehind {
                segment.rewind()
                measure.getSegment(0f, measure.length * fraction(), segment, true)
                drawPath(path = segment, color = color, style = stroke)
            }
        }
    )
}

@Composable
private fun MessageInputBannedBanner(banReason: String?, errorRed: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Lucide.X,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = errorRed
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(Res.string.chat_you_are_banned),
                style = MaterialTheme.typography.labelMedium,
                color = errorRed,
                fontWeight = FontWeight.SemiBold
            )
            if (!banReason.isNullOrBlank()) {
                Text(
                    banReason,
                    style = MaterialTheme.typography.labelSmall,
                    color = errorRed.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MessageInputContextMenu(
    offset: IntOffset,
    tfv: TextFieldValue,
    autoTranslateEnabled: Boolean,
    onToggleAutoTranslate: (Boolean) -> Unit,
    onTfvChange: (TextFieldValue) -> Unit,
    onTranslate: () -> Unit,
    onDismiss: () -> Unit,
    clipboard: Clipboard,
    scope: CoroutineScope
) {
    PortalPopup(
        alignment = Alignment.TopStart,
        offset = offset + IntOffset(8, 8),
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        onDismissRequest = onDismiss
    ) {
        LiquidGlassSurface(
            modifier = Modifier.widthIn(min = 180.dp, max = 260.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            backgroundAlphaHigh = 0.94f,
            backgroundAlphaLow = 0.85f,
            borderAlphaHigh = 0f,
            borderAlphaLow = 0f
        ) {
            Column {
                LiquidGlassDropdownItem(
                    text = stringResource(Res.string.cut),
                    icon = Lucide.Scissors,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = {
                        onDismiss()
                        val selection = tfv.selection
                        if (!selection.collapsed) {
                            scope.launch {
                                clipboard.setText(tfv.text.substring(selection.min, selection.max))
                            }
                            val newText = tfv.text.removeRange(selection.min, selection.max)
                            onTfvChange(TextFieldValue(newText, TextRange(selection.min)))
                        } else if (tfv.text.isNotEmpty()) {
                            scope.launch { clipboard.setText(tfv.text) }
                            onTfvChange(TextFieldValue("", TextRange(0)))
                        }
                    }
                )
                LiquidGlassDropdownItem(
                    text = stringResource(Res.string.copy),
                    icon = Lucide.Copy,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = {
                        onDismiss()
                        val selection = tfv.selection
                        val textToCopy = if (!selection.collapsed) {
                            tfv.text.substring(selection.min, selection.max)
                        } else {
                            tfv.text
                        }
                        if (textToCopy.isNotEmpty()) {
                            scope.launch { clipboard.setText(textToCopy) }
                        }
                    }
                )
                LiquidGlassDropdownItem(
                    text = stringResource(Res.string.paste),
                    icon = Lucide.ClipboardPaste,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = {
                        onDismiss()
                        scope.launch {
                            val clip = clipboard.readText()
                            if (!clip.isNullOrEmpty()) {
                                val selection = tfv.selection
                                val newText =
                                    tfv.text.replaceRange(selection.min, selection.max, clip)
                                onTfvChange(
                                    TextFieldValue(newText, TextRange(selection.min + clip.length))
                                )
                            }
                        }
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
                LiquidGlassDropdownItem(
                    text = stringResource(Res.string.input_translate),
                    icon = Lucide.Languages,
                    iconTint = MaterialTheme.colorScheme.primary,
                    onClick = {
                        onDismiss()
                        onTranslate()
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(Res.string.input_auto_translate),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    ChatoneSwitch(
                        checked = autoTranslateEnabled,
                        onCheckedChange = onToggleAutoTranslate
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageInputBannedWordDialog(
    errorRed: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.chat_banned_word_title)) },
        text = { Text(stringResource(Res.string.chat_banned_word_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = errorRed)
            ) { Text(stringResource(Res.string.chat_banned_word_send_anyway)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        }
    )
}


@Composable
fun LiquidGlassTooltipBox(
    tooltip: String,
    content: @Composable () -> Unit
) {
    LiquidGlassTooltipBox(
        tooltipContent = {
            Text(
                text = tooltip,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        content = content
    )
}

@Composable
fun LiquidGlassTooltipBox(
    tooltipContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    HoverTooltipBox(
        tooltip = {
            DisableSelection {
                LiquidGlassSurface(
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    backgroundAlphaHigh = 0.88f,
                    backgroundAlphaLow = 0.72f,
                    borderAlphaHigh = 0.25f,
                    borderAlphaLow = 0.08f
                ) {
                    tooltipContent()
                }
            }
        },
        content = content
    )
}

@Composable
fun LiquidGlassRichTooltipBox(
    tooltipContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    HoverTooltipBox(
        tooltip = {
            DisableSelection {
                LiquidGlassSurface(
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(10.dp),
                    backgroundAlphaHigh = 0.95f,
                    backgroundAlphaLow = 0.85f,
                    borderAlphaHigh = 0.25f,
                    borderAlphaLow = 0.08f
                ) {
                    tooltipContent()
                }
            }
        },
        content = content
    )
}

@Composable
fun ReplyParentHoverTooltip(
    parentName: String,
    parentBody: String,
    parentColor: Color,
    content: @Composable () -> Unit
) {
    LiquidGlassRichTooltipBox(
        tooltipContent = {
            Column(
                modifier = Modifier.widthIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "@$parentName",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = parentColor
                )
                Text(
                    text = parentBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        content = content
    )
}

private fun formatTimestampFor(
    timestamp: Long,
    format: SettingsState.TimestampFormat
): String {
    val instant = kotlin.time.Instant.fromEpochMilliseconds(timestamp)
    val dt = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    return when (format) {
        SettingsState.TimestampFormat.H24 ->
            "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"

        SettingsState.TimestampFormat.H12 -> {
            val h = if (dt.hour == 0) 12 else if (dt.hour > 12) dt.hour - 12 else dt.hour
            "$h:${dt.minute.toString().padStart(2, '0')} ${if (dt.hour < 12) "AM" else "PM"}"
        }

        SettingsState.TimestampFormat.OFF -> ""
    }
}

private val FieldInsetMerged = 2.dp
private val FieldInsetSplit = 12.dp
