package io.rudione.chatone.presentation.chat.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.emoji_icon
import io.rudione.chatone.presentation.chat.pauseHotkeyMatches
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.bottomBarBackgroundColor
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.theme.panelBlur
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onSendKeepText: () -> Unit = {},
    onHistoryUp: () -> Unit = {},
    onHistoryDown: () -> Unit = {},
    onEmotePickerClick: () -> Unit = {},
    onTogglePause: () -> Unit = {},
    pauseHotkey: String = "",
    enabled: Boolean,
    isBanned: Boolean = false,
    banReason: String? = null,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    showEmoteCompletions: Boolean = false,
    showMentionCompletions: Boolean = false,
    emoteCount: Int = 0,
    mentionCount: Int = 0,
    emoteTabIndex: Int = -1,
    mentionTabIndex: Int = -1,
    onTabEmote: (Int) -> Unit = {},
    onTabMention: (Int) -> Unit = {},
    onConfirmEmoteTab: () -> Unit = {},
    onConfirmMentionTab: () -> Unit = {},
    hidePlaceholder: Boolean = false,
    hideEmojiButton: Boolean = false,
    placeholderText: String? = null,
    showSlashCompletions: Boolean = false,
    slashCount: Int = 0,
    slashTabIndex: Int = -1,
    onTabSlash: (Int) -> Unit = {},
    onConfirmSlashTab: () -> Unit = {},
    glowIntensity: Float = 0f,
    glowTriggerTs: Long = 0L
) {
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

    LaunchedEffect(value) {
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

    val canSend =
        enabled &&
                !isBanned &&
                value.isNotBlank() &&
                !isOverLimit

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val errorRed = Color(0xFFCF6679)
    val warnAmber = Color(0xFFD4A017)
    val primaryCol = MaterialTheme.colorScheme.primary

    val borderColor by animateColorAsState(
        when {
            isBanned -> errorRed
            isOverLimit -> errorRed
            isNearLimit -> warnAmber
            isFocused -> primaryCol.copy(alpha = 0.92f)
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.82f)
        },
        tween(220)
    )

    val borderWidth by animateDpAsState(
        when {
            isBanned || isOverLimit -> 1.8.dp
            isFocused -> 1.55.dp
            else -> 1.15.dp
        },
        tween(220)
    )

    val containerColor by animateColorAsState(
        when {
            isBanned -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.14f)
            isFocused -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.96f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
        },
        tween(220)
    )

    val glowColor by animateColorAsState(
        when {
            isFocused -> primaryCol.copy(alpha = 0.14f)
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
        },
        tween(220)
    )

    val sendContainerColor by animateColorAsState(
        when {
            canSend && isFocused -> primaryCol
            canSend -> primaryCol.copy(alpha = 0.9f)
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        },
        tween(180)
    )

    val sendScale by animateFloatAsState(
        if (canSend && isFocused) 1f else 0.97f,
        tween(180)
    )

    val emojiContainerColor by animateColorAsState(
        when {
            isFocused -> primaryCol.copy(alpha = 0.1f)
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.035f)
        },
        tween(180)
    )

    val bottomWallpaper = LocalWallpaperController.current.state

    val bottomBarBlur =
        bottomWallpaper.panelColorConfig.bottomBarBlurRadius

    val bottomBarColor = bottomBarBackgroundColor(
        bottomWallpaper,
        ChatoneTheme.extraColors.chatInputSurface
    )

    val s = LocalStrings.current

    Column(
        modifier = modifier.fillMaxWidth()
    ) {

        val glowAnim = remember { Animatable(0f) }
        LaunchedEffect(glowTriggerTs, glowIntensity) {
            if (glowIntensity > 0f) {
                glowAnim.snapTo(glowIntensity)
                glowAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 5000)
                )
            }
        }
        val baseDividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        val glowColor = MaterialTheme.colorScheme.primary
        val glowAlpha = glowAnim.value.coerceIn(0f, 1f)
        val glowStripHeight = (1f + glowAlpha * 4f).dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(glowStripHeight)
                .background(
                    if (glowAlpha > 0.01f) {
                        glowColor.copy(alpha = glowAlpha)
                    } else {
                        baseDividerColor
                    }
                )
        )

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {

            if (bottomBarBlur > 0f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(bottomBarColor)
                        .panelBlur(bottomBarBlur)
                )
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(bottomBarColor)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = if (isFocused) 8.dp else 1.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = glowColor,
                            spotColor = glowColor
                        )
                        .border(
                            width = borderWidth,
                            color = borderColor,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(containerColor)
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(
                                if (isFocused) 1.2.dp else 0.8.dp
                            )
                            .background(
                                if (isFocused) {
                                    primaryCol.copy(alpha = 0.22f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.025f)
                                }
                            )
                            .align(Alignment.TopCenter)
                    )

                    if (isBanned) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 14.dp,
                                    vertical = 10.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            Icon(
                                Icons.Filled.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = errorRed
                            )

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    s.chatYouAreBanned,
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

                    } else {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            if (!hideEmojiButton) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(emojiContainerColor)
                                        .border(
                                            width = if (isFocused) 0.7.dp else 0.dp,
                                            color = if (isFocused) {
                                                primaryCol.copy(alpha = 0.16f)
                                            } else {
                                                Color.Transparent
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable(
                                            interactionSource = remember {
                                                MutableInteractionSource()
                                            },
                                            indication = null,
                                            enabled = enabled,
                                            onClick = onEmotePickerClick
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {

                                    Icon(
                                        painter = painterResource(Res.drawable.emoji_icon),
                                        contentDescription = "Emotes",
                                        modifier = Modifier.size(21.dp),
                                        tint = if (enabled) {
                                            if (isFocused) {
                                                primaryCol
                                            } else {
                                                primaryCol.copy(alpha = 0.68f)
                                            }
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
                                        }
                                    )
                                }

                                Spacer(
                                    modifier = Modifier.width(4.dp)
                                )
                            }

                            BasicTextField(
                                value = tfv,
                                onValueChange = { newTfv ->
                                    tfv = newTfv
                                    onValueChange(newTfv.text)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 10.dp)
                                    .focusRequester(focusRequester)
                                    .focusable(interactionSource = interactionSource)
                                    .onPreviewKeyEvent { event ->

                                        if (event.type != KeyEventType.KeyDown) {
                                            return@onPreviewKeyEvent false
                                        }

                                        val isCtrl =
                                            event.isCtrlPressed || event.isMetaPressed

                                        when {

                                            event.key == Key.Tab &&
                                                    !isCtrl &&
                                                    !event.isAltPressed -> {

                                                when {

                                                    showSlashCompletions &&
                                                            slashCount > 0 -> {

                                                        val next =
                                                            if (slashTabIndex < 0) 0
                                                            else (slashTabIndex + 1) % slashCount

                                                        onTabSlash(next)
                                                    }

                                                    showEmoteCompletions &&
                                                            emoteCount > 0 -> {

                                                        val next =
                                                            if (emoteTabIndex < 0) 0
                                                            else (emoteTabIndex + 1) % emoteCount

                                                        onTabEmote(next)
                                                    }

                                                    showMentionCompletions &&
                                                            mentionCount > 0 -> {

                                                        val next =
                                                            if (mentionTabIndex < 0) 0
                                                            else (mentionTabIndex + 1) % mentionCount

                                                        onTabMention(next)
                                                    }
                                                }

                                                true
                                            }

                                            event.key == Key.Enter &&
                                                    !isCtrl &&
                                                    !event.isShiftPressed -> {

                                                when {

                                                    showSlashCompletions &&
                                                            slashTabIndex >= 0 -> {
                                                        onConfirmSlashTab()
                                                        true
                                                    }

                                                    showEmoteCompletions &&
                                                            emoteTabIndex >= 0 -> {
                                                        onConfirmEmoteTab()
                                                        true
                                                    }

                                                    showMentionCompletions &&
                                                            mentionTabIndex >= 0 -> {
                                                        onConfirmMentionTab()
                                                        true
                                                    }

                                                    canSend -> {
                                                        onSend()
                                                        true
                                                    }

                                                    else -> true
                                                }
                                            }

                                            isCtrl && event.key == Key.Enter -> {
                                                onSendKeepText()
                                                true
                                            }

                                            event.key == Key.DirectionUp &&
                                                    !isCtrl &&
                                                    !event.isShiftPressed &&
                                                    !event.isAltPressed &&
                                                    (tfv.text.isEmpty() ||
                                                            tfv.selection.start == 0) -> {

                                                onHistoryUp()
                                                true
                                            }

                                            event.key == Key.DirectionDown &&
                                                    !isCtrl &&
                                                    !event.isShiftPressed &&
                                                    !event.isAltPressed &&
                                                    (tfv.text.isEmpty() ||
                                                            tfv.selection.end == tfv.text.length) -> {

                                                onHistoryDown()
                                                true
                                            }

                                            event.key == Key.Backspace &&
                                                    event.isShiftPressed -> {

                                                val sel = tfv.selection
                                                val text = tfv.text

                                                if (sel.collapsed && sel.start > 0) {

                                                    val end = sel.start
                                                    var start = end

                                                    while (
                                                        start > 0 &&
                                                        text[start - 1].isWhitespace()
                                                    ) {
                                                        start--
                                                    }

                                                    while (
                                                        start > 0 &&
                                                        !text[start - 1].isWhitespace()
                                                    ) {
                                                        start--
                                                    }

                                                    val newText =
                                                        text.removeRange(start, end)

                                                    tfv = TextFieldValue(
                                                        newText,
                                                        selection = TextRange(start)
                                                    )

                                                    onValueChange(newText)
                                                    true

                                                } else {
                                                    false
                                                }
                                            }

                                            event.key == Key.Delete &&
                                                    event.isShiftPressed -> {

                                                val sel = tfv.selection
                                                val text = tfv.text

                                                if (sel.collapsed && sel.start < text.length) {

                                                    val start = sel.start
                                                    var end = start

                                                    while (
                                                        end < text.length &&
                                                        text[end].isWhitespace()
                                                    ) {
                                                        end++
                                                    }

                                                    while (
                                                        end < text.length &&
                                                        !text[end].isWhitespace()
                                                    ) {
                                                        end++
                                                    }

                                                    val newText =
                                                        text.removeRange(start, end)

                                                    tfv = TextFieldValue(
                                                        newText,
                                                        selection = TextRange(start)
                                                    )

                                                    onValueChange(newText)
                                                    true

                                                } else {
                                                    false
                                                }
                                            }

                                            pauseHotkeyMatches(
                                                event,
                                                pauseHotkey
                                            ) -> {
                                                onTogglePause()
                                                true
                                            }

                                            else -> false
                                        }
                                    },
                                enabled = enabled,
                                maxLines = 4,
                                interactionSource = interactionSource,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(primaryCol),
                                decorationBox = { inner ->

                                    if (tfv.text.isEmpty() && !hidePlaceholder) {

                                        Text(
                                            placeholderText ?: s.chatPlaceholder,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme
                                                .onSurfaceVariant
                                                .copy(
                                                    alpha = if (isFocused) {
                                                        0.56f
                                                    } else {
                                                        0.42f
                                                    }
                                                )
                                        )
                                    }

                                    inner()
                                }
                            )

                            Spacer(
                                modifier = Modifier.width(0.dp)
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .graphicsLayer {
                                            scaleX = sendScale
                                            scaleY = sendScale
                                        }
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(sendContainerColor)
                                        .border(
                                            width = if (canSend) 1.dp else 0.dp,
                                            color = primaryCol.copy(alpha = 0.22f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable(
                                            interactionSource = remember {
                                                MutableInteractionSource()
                                            },
                                            indication = null,
                                            enabled = canSend,
                                            onClick = onSend
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {

                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = s.chatSend,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (canSend) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                                        }
                                    )
                                }

                                if (isNearLimit || isOverLimit) {

                                    val counterColor =
                                        if (isOverLimit) errorRed
                                        else warnAmber

                                    Text(
                                        text = "$charCount/$MAX_CHARS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp
                                        ),
                                        color = counterColor,
                                        fontWeight = if (isOverLimit) {
                                            FontWeight.SemiBold
                                        } else {
                                            FontWeight.Medium
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidGlassTooltipBox(
    tooltip: String,
    content: @Composable () -> Unit
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
           
            io.rudione.chatone.presentation.components.LiquidGlassSurface(
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                backgroundAlphaHigh = 0.88f,
                backgroundAlphaLow = 0.72f,
                borderAlphaHigh = 0.25f,
                borderAlphaLow = 0.08f
            ) {
                Text(
                    text = tooltip,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        state = rememberTooltipState()
    ) {
        content()
    }
}

private fun formatTimestampFor(
    timestamp: Long,
    format: SettingsState.TimestampFormat
): String {
    val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(timestamp)
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
