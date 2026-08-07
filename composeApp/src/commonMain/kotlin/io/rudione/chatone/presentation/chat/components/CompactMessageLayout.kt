package io.rudione.chatone.presentation.chat

import io.github.aakira.napier.Napier
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.icons.outlined.CopyAll
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.emoji_icon
import chatone.composeapp.generated.resources.ic_sword
import coil3.compose.AsyncImage
import io.rudione.chatone.data.repository.EmoteRepository
import io.rudione.chatone.data.repository.MentionMuteRepository
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.EmoteProvider
import io.rudione.chatone.domain.model.GenericEmote
import io.rudione.chatone.domain.model.Macro
import io.rudione.chatone.domain.model.MentionEntry
import io.rudione.chatone.domain.model.ModActionButton
import io.rudione.chatone.presentation.chat.components.BadgeVisual
import io.rudione.chatone.presentation.chat.components.ChannelHeaderBlock
import io.rudione.chatone.presentation.chat.components.ChatTopBar
import io.rudione.chatone.presentation.chat.components.collectBadgeVisuals
import io.rudione.chatone.presentation.chat.components.roomModeLabels
import io.rudione.chatone.presentation.chat.rendering.MessageTranslationLine
import io.rudione.chatone.presentation.chat.rendering.rawTokenText
import io.rudione.chatone.presentation.chat.components.LinkHoverPopup
import io.rudione.chatone.presentation.chat.components.ModActionConfirmDialog
import io.rudione.chatone.presentation.chat.components.PendingModAction
import io.rudione.chatone.presentation.chat.components.ChatSearchBar
import io.rudione.chatone.presentation.chat.components.LiquidGlassRichTooltipBox
import io.rudione.chatone.presentation.chat.components.ReplyParentHoverTooltip
import io.rudione.chatone.presentation.chat.components.LiquidGlassTooltipBox
import io.rudione.chatone.presentation.chat.components.MessageInput
import io.rudione.chatone.presentation.chat.components.SlashCommandSuggestionsRow
import io.rudione.chatone.presentation.components.GlowSurface
import io.rudione.chatone.presentation.components.LiquidGlassDropdownItem
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.settings.InlineImageMode
import io.rudione.chatone.presentation.settings.PauseHotkeyMode
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.theme.ChatBackgroundLayer
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.WallpaperState
import io.rudione.chatone.presentation.theme.chatPaneBackgroundColor
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.presentation.theme.luminance
import io.rudione.chatone.presentation.theme.panelBlur
import io.rudione.chatone.presentation.theme.topBarBackgroundColor
import io.rudione.chatone.util.EmoteAnimationCache
import io.rudione.chatone.util.chat.EmoteImageWithTooltip
import io.rudione.chatone.util.system.GlobalKeyDispatcher
import io.rudione.chatone.util.chat.MessageToken
import io.rudione.chatone.util.media.NotificationSoundPlayer
import io.rudione.chatone.util.media.externalFileDropTarget
import io.rudione.chatone.util.system.handleHover
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.chat.SlashCommand
import io.rudione.chatone.util.link.openChatUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun CompactMessageLayout(
    message: DisplayMessage.PrivMsg,
    showModActions: Boolean,
    showTimestamp: Boolean,
    timestampFormat: SettingsState.TimestampFormat,
    showBadges: Boolean,
    isMod: Boolean,
    isOwnMessage: Boolean,
    currentUserId: String,
    emoteSize: SettingsState.EmoteSize,
    customModButtons: List<ModActionButton>,
    allModButtons: List<ModActionButton>,
    modButtonsVersion: Int = 0,
    showCustomModButtons: Boolean,
    showDefaultDeleteButton: Boolean,
    showDefaultTimeoutButton: Boolean,
    showDefaultBanButton: Boolean,
    chatFontSizeSp: Float,
    onUsernameClick: () -> Unit,
    onRightClickUsername: (String) -> Unit,
    onMentionClick: (String) -> Unit,
    onReply: () -> Unit,
    onCopyText: () -> Unit,
    onPin: () -> Unit,
    onTimeout: () -> Unit,
    onCustomTimeout: (Int) -> Unit,
    onBan: () -> Unit,
    onDelete: () -> Unit,
    actorCanModerate: Boolean,
    actorIsBroadcaster: Boolean,
    extraColors: io.rudione.chatone.presentation.theme.ChatoneExtraColors,
    mentionColor: Color,
    backgroundColor: Color,
    hasAccentBar: Boolean,
    s: AppStrings,
    userColorByLogin: Map<String, Color> = emptyMap()
) {
    var prefixWidthPx by remember { mutableStateOf(0f) }
    val dynamicTopPadding = (chatFontSizeSp * 0.067f).dp
    val translationStore: TranslationStore = koinInject()

    Column(modifier = Modifier.fillMaxWidth()) {
        val replyParentName = message.rawMessage?.replyParentDisplayName
        val replyParentBody = message.rawMessage?.replyParentMsgBody
        val replyParentLogin = message.rawMessage?.replyParentUserLogin
        if (replyParentName != null && replyParentBody != null && replyParentLogin != null) {
            val parentColor = userColorByLogin[replyParentLogin.lowercase()]
                ?: stableUserColor(replyParentLogin)
            DisableSelection {
                ReplyParentHoverTooltip(
                    parentName = replyParentName,
                    parentBody = replyParentBody,
                    parentColor = parentColor
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 2.dp, end = 4.dp, top = 1.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = buildAnnotatedString {
                                pushStringAnnotation("mention", "@$replyParentLogin")
                                withStyle(
                                    SpanStyle(
                                        color = parentColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                ) { append("@$replyParentName") }
                                pop()
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.6f
                                        )
                                    )
                                ) { append(": $replyParentBody") }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 9.sp,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onMentionClick("@$replyParentLogin") }
                        )
                    }
                }
            }
        }

        val badgeVisuals = if (showBadges) {
            collectBadgeVisuals(message.username, message.userId, message.badges, message.sevenTvBadge)
        } else emptyList()

        Box(modifier = Modifier.fillMaxWidth()) {
            CompactMessageContentWithIndent(
                message = message,
                emoteSize = emoteSize,
                chatFontSizeSp = chatFontSizeSp,
                isMod = isMod,
                showTimestamp = showTimestamp,
                timestampFormat = timestampFormat,
                badgeVisuals = badgeVisuals,
                onUsernameClick = onUsernameClick,
                onRightClickUsername = onRightClickUsername,
                onMentionClick = onMentionClick,
                onReply = onReply,
                onCopyText = onCopyText,
                onPin = onPin,
                mentionColor = mentionColor,
                backgroundColor = backgroundColor,
                hasAccentBar = hasAccentBar,
                s = s,
                prefixWidthPx = prefixWidthPx,
                userColorByLogin = userColorByLogin
            )
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .onSizeChanged { prefixWidthPx = it.width.toFloat() }
                    .padding(top = dynamicTopPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showModActions) {
                    val canAct = canActOnUser(
                        actorIsBroadcaster = actorIsBroadcaster,
                        actorIsMod = actorCanModerate,
                        targetIsBroadcaster = message.isBroadcaster,
                        targetIsMod = message.isModerator,
                        targetIsVip = message.isVip,
                        targetIsSubscriber = message.isSubscriber,
                        actorIsGrandMod = false,
                        targetIsGrandMod = message.isGrandMod
                    )
                    val compactModOrderKey = "$modButtonsVersion:" + if (allModButtons.isNotEmpty())
                        allModButtons.joinToString("|") { "${it.id}:${it.sortOrder}:${it.enabled}" }
                    else
                        customModButtons.joinToString("|") { it.id }
                    key(compactModOrderKey) {
                        Row(
                            modifier = Modifier.padding(end = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val orderedButtons =
                                if (allModButtons.isNotEmpty()) allModButtons.sortedBy { it.sortOrder } else ModActionButton.defaultOrderedList() + customModButtons
                            orderedButtons.forEach { btn ->
                                if (!btn.enabled) return@forEach
                                key(btn.id) {
                                    when (btn.id) {
                                        "default_delete" -> if (showDefaultDeleteButton && (canAct || isOwnMessage)) ModActionIconBtn(
                                            icon = Icons.Outlined.Delete,
                                            label = "Del",
                                            tint = extraColors.modDelete,
                                            onClick = onDelete
                                        )

                                        "default_timeout" -> if (showDefaultTimeoutButton && canAct && !isOwnMessage) ModActionIconBtn(
                                            icon = Icons.Outlined.Refresh,
                                            label = "10m",
                                            tint = extraColors.modTimeout,
                                            onClick = onTimeout,
                                            visible = false
                                        )

                                        "default_ban" -> if (showDefaultBanButton && canAct && !isOwnMessage) ModActionIconBtn(
                                            icon = Icons.Filled.Close,
                                            label = s.chatBanUser,
                                            tint = extraColors.modBan,
                                            onClick = onBan
                                        )

                                        else -> if (showCustomModButtons && canAct && !isOwnMessage) ModActionIconBtn(
                                            icon = Icons.Outlined.Refresh,
                                            label = btn.displayLabel,
                                            tint = extraColors.modTimeout,
                                            onClick = { onCustomTimeout(btn.durationSeconds) },
                                            visible = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        MessageTranslationLine(translationStore.states[message.id]) { lang ->
            translationStore.translateTo(message.id, message.rawTokenText(), lang)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CompactMessageContentWithIndent(
    message: DisplayMessage.PrivMsg,
    emoteSize: SettingsState.EmoteSize,
    chatFontSizeSp: Float,
    isMod: Boolean,
    showTimestamp: Boolean,
    timestampFormat: SettingsState.TimestampFormat,
    badgeVisuals: List<BadgeVisual>,
    onUsernameClick: () -> Unit,
    onRightClickUsername: (String) -> Unit,
    onMentionClick: (String) -> Unit,
    onReply: () -> Unit,
    onCopyText: () -> Unit,
    onPin: () -> Unit,
    mentionColor: Color,
    backgroundColor: Color,
    hasAccentBar: Boolean,
    s: AppStrings,
    prefixWidthPx: Float,
    userColorByLogin: Map<String, Color> = emptyMap()
) {
    val lineHeightSp = when (chatFontSizeSp) {
        13f -> 20f; 15f -> 22f; 17f -> 24f; else -> chatFontSizeSp + 6f
    }
    val emoteSizeSp = when (emoteSize) {
        SettingsState.EmoteSize.SMALL -> lineHeightSp * 1.1f
        SettingsState.EmoteSize.MEDIUM -> lineHeightSp * 1.3f
        SettingsState.EmoteSize.LARGE -> lineHeightSp * 1.5f
    }.sp
    val userColor = parseColor(message.color)
        ?: stableUserColor(message.username)
    val translationStore: TranslationStore = koinInject()
    val messageRawText = remember(message.tokens) {
        message.tokens.joinToString("") { token ->
            when (token) {
                is MessageToken.Text -> token.text
                is MessageToken.TwitchEmoteToken -> token.name
                is MessageToken.ThirdPartyEmoteToken -> token.emote.code
                is MessageToken.Link -> token.displayText
                is MessageToken.Mention -> token.username
                is MessageToken.Cheer -> "${token.prefix}${token.amount}"
            }
        }
    }
    val shownName = LocalNicknames.current[message.userId] ?: message.displayName
    val nickFontSize = (chatFontSizeSp.coerceIn(11f, 16f) + 2f).sp
    val inlineContent = mutableMapOf<String, InlineTextContent>()
    val emoteKeyMap = mutableMapOf<String, GenericEmote>()
    var emoteCounter = 0
    if (!message.isDeleted) {
        resolveSevenTvPaint(message)?.takeIf { it.hasRenderableGradient() }?.let { paint ->
            registerPaintedNick(
                inlineContent = inlineContent,
                key = "nick_paint",
                name = shownName,
                paint = paint,
                fontSizeSp = chatFontSizeSp.coerceIn(11f, 16f) + 1f,
                isAction = message.isAction,
                userColor = userColor,
                onClick = onUsernameClick,
                onRightClick = { onRightClickUsername(message.displayName) }
            )
        }
    }
    val annotatedString = buildAnnotatedString {
        if (showTimestamp) {
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontSize = (chatFontSizeSp.coerceIn(11f, 16f) * 0.85f).sp,
                    fontFeatureSettings = "tnum"
                )
            ) {
                append(formatTimestamp(message.timestamp, timestampFormat))
                append(" ")
            }
        }
        if (badgeVisuals.isNotEmpty()) {
            val badgeBlockWidthSp = badgeVisuals.size * 18f + (badgeVisuals.size - 1) * 4f + 6f
            appendInlineContent("badges_cluster", " ")
            inlineContent["badges_cluster"] = InlineTextContent(
                Placeholder(badgeBlockWidthSp.sp, 18.sp, PlaceholderVerticalAlign.TextCenter)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        badgeVisuals.forEach { bv ->
                            LiquidGlassTooltipBox(tooltip = bv.tooltip) {
                                AsyncImage(
                                    model = bv.imageUrl,
                                    contentDescription = bv.tooltip,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        if (message.isDeleted) {
            pushStringAnnotation("username", message.userId)
            withStyle(
                SpanStyle(
                    color = userColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = nickFontSize
                )
            ) { append(shownName) }
            pop()
            append(": ")
            val originalText = message.tokens.joinToString("") {
                when (it) {
                    is MessageToken.Text -> it.text
                    is MessageToken.TwitchEmoteToken -> it.name
                    is MessageToken.ThirdPartyEmoteToken -> it.emote.code
                    is MessageToken.Link -> it.displayText
                    is MessageToken.Mention -> it.username
                    is MessageToken.Cheer -> "${it.prefix}${it.amount}"
                }
            }
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f),
                    textDecoration = TextDecoration.LineThrough
                )
            ) {
                append(originalText.ifEmpty { "message deleted" })
            }
        } else {
            pushStringAnnotation("username", message.userId)
            val nickPaint = resolveSevenTvPaint(message)?.takeIf { it.hasRenderableGradient() }
            if (nickPaint != null) {
                appendInlineContent("nick_paint", shownName)
                if (message.isAction) append(" ") else append(": ")
            } else if (message.isAction) {
                withStyle(
                    SpanStyle(
                        color = userColor,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = nickFontSize
                    )
                ) { append(shownName); append(" ") }
            } else {
                withStyle(SpanStyle(color = userColor, fontWeight = FontWeight.Bold, fontSize = nickFontSize)) {
                    append(
                        shownName
                    )
                }
                append(": ")
            }
            pop()
            val messageColor = if (message.isAction) userColor else Color.Unspecified
            message.tokens.forEach { token ->
                when (token) {
                    is MessageToken.Text -> {
                        if (message.isAction) withStyle(
                            SpanStyle(
                                color = messageColor,
                                fontStyle = FontStyle.Italic
                            )
                        ) { append(token.text) } else append(token.text)
                    }

                    is MessageToken.TwitchEmoteToken -> {
                        val key = "emote_${emoteCounter++}"
                        appendInlineContent(key, token.name)
                        inlineContent[key] = InlineTextContent(
                            Placeholder(
                                emoteSizeSp,
                                emoteSizeSp,
                                PlaceholderVerticalAlign.TextCenter
                            )
                        ) {
                            AnimatedEmoteImage(
                                url = token.url,
                                contentDescription = token.name,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    is MessageToken.ThirdPartyEmoteToken -> {
                        val key = "emote_${emoteCounter++}"
                        appendInlineContent(key, token.emote.code)
                        val (emoteW, emoteH) = computeEmoteDisplaySize(
                            token.emote.width,
                            token.emote.height,
                            emoteSizeSp
                        )
                        emoteKeyMap[key] = token.emote
                        inlineContent[key] = InlineTextContent(
                            Placeholder(
                                emoteW,
                                emoteH,
                                PlaceholderVerticalAlign.TextCenter
                            )
                        ) {
                            Box {
                                EmoteImageWithTooltip(
                                    emote = token.emote,
                                    modifier = Modifier.fillMaxSize(),
                                    onShowContextMenu = null
                                )
                                token.overlays.forEach { overlay ->
                                    EmoteImageWithTooltip(
                                        emote = overlay,
                                        modifier = Modifier.fillMaxSize(),
                                        onShowContextMenu = {})
                                }
                            }
                        }
                    }

                    is MessageToken.Link -> {
                        pushStringAnnotation("url", token.url)
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            )
                        ) { append(token.displayText) }
                        pop()
                    }

                    is MessageToken.Cheer -> {
                        val key = "cheer_${emoteCounter++}"
                        appendInlineContent(key, "${token.prefix}${token.amount}")
                        inlineContent[key] = InlineTextContent(
                            Placeholder(
                                (chatFontSizeSp * (1.6f + 0.62f * token.amount.toString().length)).sp,
                                (chatFontSizeSp * 1.45f).sp,
                                PlaceholderVerticalAlign.TextCenter
                            )
                        ) {
                            CheerToken(amount = token.amount, fontSizeSp = chatFontSizeSp)
                        }
                    }

                    is MessageToken.Mention -> {
                        pushStringAnnotation("mention", token.username)
                        val mentionedLogin = token.username.removePrefix("@").lowercase()
                        val mentionColorReal =
                            userColorByLogin[mentionedLogin]
                                ?: stableUserColor(mentionedLogin)
                        withStyle(
                            SpanStyle(
                                color = mentionColorReal,
                                fontWeight = FontWeight.Bold
                            )
                        ) { append(token.username) }
                        pop()
                    }
                }
            }
        }
    }
    var showContextMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    var contextMenuOffset by remember { mutableStateOf(IntOffset.Zero) }
    val uriHandler = LocalUriHandler.current
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val inlineSettings = remember { SettingsViewModel.loadInitialState() }
    var hoveredUrl by remember { mutableStateOf<String?>(null) }
    var hoverOffset by remember { mutableStateOf(IntOffset.Zero) }

    Box(modifier = Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val prefixIndent = with(density) { prefixWidthPx.toSp() }
        SelectionContainer {
            Text(
                text = annotatedString,
                inlineContent = inlineContent,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = when (chatFontSizeSp) {
                        13f -> 14.sp; 15f -> 16.sp; 17f -> 18.sp; else -> chatFontSizeSp.sp
                    },
                    lineHeight = when (chatFontSizeSp) {
                        13f -> 20.sp; 15f -> 22.sp; 17f -> 24.sp; else -> (chatFontSizeSp + 6).sp
                    },
                    textIndent = androidx.compose.ui.text.style.TextIndent(
                        firstLine = prefixIndent,
                        restLine = 0.sp
                    )
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
                    .pointerInput(annotatedString) {
                        detectTapGestures(
                            onTap = { offset ->
                                textLayoutResult?.let { layoutResult ->
                                    val charOffset = layoutResult.getOffsetForPosition(offset)
                                    annotatedString.getStringAnnotations(
                                        "url",
                                        charOffset,
                                        charOffset
                                    ).firstOrNull()?.let {
                                        try {
                                            openChatUrl(it.item, inlineSettings.linkOpenMode)
                                        } catch (_: Exception) {
                                        }; return@detectTapGestures
                                    }
                                    annotatedString.getStringAnnotations(
                                        "username",
                                        charOffset,
                                        charOffset
                                    ).firstOrNull()
                                        ?.let { onUsernameClick(); return@detectTapGestures }
                                    annotatedString.getStringAnnotations(
                                        "mention",
                                        charOffset,
                                        charOffset
                                    ).firstOrNull()
                                        ?.let { onMentionClick(it.item); return@detectTapGestures }
                                }
                            },
                            onLongPress = { offset ->
                                contextMenuOffset =
                                    IntOffset(offset.x.toInt(), offset.y.toInt()); showContextMenu =
                                true
                            }
                        )
                    }
                    .pointerInput(message.id, annotatedString) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val pos = event.changes.firstOrNull()?.position ?: continue
                                when (event.type) {
                                    PointerEventType.Move, PointerEventType.Enter -> {
                                        textLayoutResult?.let { layout ->
                                            val charOffset = layout.getOffsetForPosition(pos)
                                            val urlAnn = annotatedString.getStringAnnotations(
                                                "url",
                                                charOffset,
                                                charOffset
                                            ).firstOrNull()
                                            hoveredUrl = urlAnn?.item
                                            hoverOffset = IntOffset(pos.x.toInt(), pos.y.toInt())
                                        }
                                    }

                                    PointerEventType.Exit -> hoveredUrl = null
                                    else -> {}
                                }
                                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed &&
                                    event.changes.none { it.isConsumed }
                                ) {
                                    textLayoutResult?.let { layout ->
                                        val charOffset = layout.getOffsetForPosition(pos)
                                        val onUsername = annotatedString.getStringAnnotations(
                                            "username",
                                            charOffset,
                                            charOffset
                                        ).isNotEmpty()
                                        if (onUsername) {
                                            onRightClickUsername(message.displayName)
                                            event.changes.forEach { it.consume() }
                                        } else {
                                            contextMenuOffset =
                                                IntOffset(pos.x.toInt(), pos.y.toInt())
                                            showContextMenu = true
                                            event.changes.forEach { it.consume() }
                                        }
                                    } ?: run {
                                        contextMenuOffset = IntOffset(pos.x.toInt(), pos.y.toInt())
                                        showContextMenu = true
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        }
                    },
                onTextLayout = { textLayoutResult = it }
            )
        }
        hoveredUrl?.let { url ->
            Popup(
                popupPositionProvider = object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize
                    ): IntOffset {
                        val x = anchorBounds.left + hoverOffset.x + 16
                        val yBelow = anchorBounds.top + hoverOffset.y + 24
                        val yAbove = anchorBounds.top + hoverOffset.y - popupContentSize.height - 8
                        val finalY =
                            if (yBelow + popupContentSize.height > windowSize.height) yAbove else yBelow
                        val finalX =
                            if (x + popupContentSize.width > windowSize.width) windowSize.width - popupContentSize.width - 8 else x
                        return IntOffset(finalX, finalY)
                    }
                },
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    clippingEnabled = false
                )
            ) { LinkHoverPopup(url = url) }
        }
        if (showContextMenu) {
            Popup(
                alignment = Alignment.TopStart,
                offset = contextMenuOffset + IntOffset(8, 8),
                properties = PopupProperties(
                    focusable = true,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                ),
                onDismissRequest = { showContextMenu = false }
            ) {
                LiquidGlassSurface(
                    modifier = Modifier.widthIn(min = 160.dp, max = 240.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    backgroundAlphaHigh = 0.94f,
                    backgroundAlphaLow = 0.85f,
                    borderAlphaHigh = 0f,
                    borderAlphaLow = 0f
                ) {
                    Column {
                        if (isMod) {
                            LiquidGlassDropdownItem(
                                text = "Pin",
                                icon = Icons.Filled.Place,
                                iconTint = MaterialTheme.colorScheme.primary,
                                onClick = { showContextMenu = false; onPin() })
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        }
                        LiquidGlassDropdownItem(
                            text = s.chatReplyTo,
                            icon = Icons.AutoMirrored.Filled.Send,
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = { showContextMenu = false; onReply() })
                        LiquidGlassDropdownItem(
                            text = s.chatCopyMessage,
                            icon = Icons.Outlined.CopyAll,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = {
                                showContextMenu = false;
                                val rawText = message.tokens.joinToString("") {
                                    when (it) {
                                        is MessageToken.Text -> it.text; is MessageToken.TwitchEmoteToken -> it.name; is MessageToken.ThirdPartyEmoteToken -> it.emote.code; is MessageToken.Link -> it.displayText; is MessageToken.Mention -> it.username; is MessageToken.Cheer -> "${it.prefix}${it.amount}"
                                    }
                                }; clipboardManager.setText(AnnotatedString(rawText))
                            })
                        LiquidGlassDropdownItem(
                            text = if (translationStore.states.containsKey(message.id)) "${s.chatTranslate} ✓" else s.chatTranslate,
                            icon = Icons.Outlined.Translate,
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = {
                                showContextMenu = false
                                translationStore.toggle(message.id, messageRawText)
                            })
                    }
                }
            }
        }
    }
}
