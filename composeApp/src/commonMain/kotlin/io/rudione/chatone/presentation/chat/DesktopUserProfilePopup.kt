package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.russhwolf.settings.Settings
import io.rudione.chatone.data.repository.MentionMuteRepository
import io.rudione.chatone.data.repository.UserNoteRepository
import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.SevenTvCosmetics
import io.rudione.chatone.presentation.components.ChatoneTextField
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.theme.i18n.LocalStrings

private const val KEY_CARD_WIDTH = "profile_card_width"
private const val KEY_CARD_HEIGHT = "profile_card_height"
private const val KEY_CARD_SIZE_MIGRATED = "profile_card_size_compact_migrated"

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun DesktopUserProfilePopup(
    userId: String,
    username: String,
    displayName: String,
    color: String?,
    initialAvatarUrl: String,
    initialCreatedAt: String,
    badges: List<Badge>,
    sevenTvBadge: SevenTvCosmetics.Badge?,
    isBroadcaster: Boolean,
    isModerator: Boolean,
    isVip: Boolean,
    isSubscriber: Boolean,
    sessionMessages: List<DisplayMessage.PrivMsg>,
    canModerate: Boolean,
    currentUserIsBroadcaster: Boolean,
    isBlocked: Boolean,
    channelId: String,
    channelLogin: String,
    accessToken: String,
    startPinned: Boolean,
    mentionMuteRepository: MentionMuteRepository?,
    noteRepository: UserNoteRepository,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
    onTimeout: (Int) -> Unit,
    onBan: () -> Unit,
    onUnban: () -> Unit,
    onMod: () -> Unit,
    onUnmod: () -> Unit,
    onVip: () -> Unit,
    onUnvip: () -> Unit,
    onWhisper: () -> Unit,
    onBanWithReason: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current
    val settings = remember { Settings() }
    val settingsState = remember { SettingsViewModel.loadInitialState() }

    remember {
        if (!settings.getBoolean(KEY_CARD_SIZE_MIGRATED, false)) {
            settings.putFloat(KEY_CARD_WIDTH, PROFILE_CARD_DEFAULT_WIDTH.value)
            settings.putFloat(KEY_CARD_HEIGHT, PROFILE_CARD_DEFAULT_HEIGHT.value)
            settings.putBoolean(KEY_CARD_SIZE_MIGRATED, true)
        }
        Unit
    }

    var cardWidth by remember {
        mutableStateOf(
            settings.getFloat(KEY_CARD_WIDTH, PROFILE_CARD_DEFAULT_WIDTH.value).dp
                .coerceIn(PROFILE_CARD_MIN_WIDTH, PROFILE_CARD_MAX_WIDTH)
        )
    }
    var cardHeight by remember {
        mutableStateOf(
            settings.getFloat(KEY_CARD_HEIGHT, PROFILE_CARD_DEFAULT_HEIGHT.value).dp
                .coerceIn(PROFILE_CARD_MIN_HEIGHT, PROFILE_CARD_MAX_HEIGHT)
        )
    }
    var isPinned by remember { mutableStateOf(startPinned) }
    var banReasonPrompt by remember { mutableStateOf(false) }
    var banReasonText by remember { mutableStateOf("") }

    var mentionsMuted by remember(username) {
        mutableStateOf(mentionMuteRepository?.isUserMuted(username) == true)
    }

    val profileState = rememberUserProfileState(
        userId = userId,
        username = username,
        channelId = channelId,
        channelLogin = channelLogin,
        accessToken = accessToken,
        initialAvatarUrl = initialAvatarUrl,
        initialCreatedAt = initialCreatedAt,
        sessionMessages = sessionMessages
    )

    UserProfileContainer(
        isPinned = isPinned,
        width = cardWidth,
        height = cardHeight,
        onResize = { newWidth, newHeight ->
            if (newWidth != cardWidth || newHeight != cardHeight) {
                cardWidth = newWidth
                cardHeight = newHeight
                settings.putFloat(KEY_CARD_WIDTH, newWidth.value)
                settings.putFloat(KEY_CARD_HEIGHT, newHeight.value)
            }
        },
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (event.key == Key.Escape) {
                    onDismiss()
                    return@onPreviewKeyEvent true
                }
                if (!event.isAltPressed) return@onPreviewKeyEvent false
                if (!canModerate || isBroadcaster) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.B -> {
                        if (event.isShiftPressed) {
                            isPinned = true
                            banReasonPrompt = true
                        } else { onBan(); onDismiss() }
                        true
                    }
                    Key.U -> { onUnban(); onDismiss(); true }
                    Key.W -> { onWhisper(); onDismiss(); true }
                    Key.K -> { if (isBlocked) onUnblock() else onBlock(); onDismiss(); true }
                    Key.M -> if (currentUserIsBroadcaster) {
                        if (isModerator) onUnmod() else onMod(); onDismiss(); true
                    } else false
                    Key.V -> if (currentUserIsBroadcaster) {
                        if (isVip) onUnvip() else onVip(); onDismiss(); true
                    } else false
                    else -> false
                }
            }
        ) {
            UserProfileDesktopCard(
                data = ProfileCardData(
                    userId = userId,
                    username = username,
                    displayName = displayName,
                    color = color,
                    avatarUrl = profileState.avatarUrl.ifEmpty { initialAvatarUrl },
                    badges = badges,
                    sevenTvBadge = sevenTvBadge,
                    isBroadcaster = isBroadcaster,
                    isModerator = isModerator,
                    isVip = isVip,
                    isSubscriber = isSubscriber
                ),
                profileState = profileState,
                sessionMessages = sessionMessages,
                highlightRules = settingsState.highlightRules,
                chatFontSizeSp = when (settingsState.fontSize) {
                    SettingsState.FontSize.SMALL -> 12f
                    SettingsState.FontSize.MEDIUM -> 15f
                    SettingsState.FontSize.LARGE -> 18f
                },
                canModerate = canModerate,
                currentUserIsBroadcaster = currentUserIsBroadcaster,
                isBlocked = isBlocked,
                mentionsMuted = mentionsMuted,
                onToggleMentionMute = {
                    val repo = mentionMuteRepository ?: return@UserProfileDesktopCard
                    if (mentionsMuted) repo.unmuteUser(username) else repo.muteUser(username)
                    mentionsMuted = !mentionsMuted
                },
                noteRepository = noteRepository,
                isPinned = isPinned,
                onTogglePin = { isPinned = !isPinned },
                callbacks = ProfileCardCallbacks(
                    onBlock = { onBlock(); if (!isPinned) onDismiss() },
                    onUnblock = { onUnblock(); if (!isPinned) onDismiss() },
                    onTimeout = { seconds -> onTimeout(seconds); if (!isPinned) onDismiss() },
                    onBan = { onBan(); if (!isPinned) onDismiss() },
                    onUnban = { onUnban(); if (!isPinned) onDismiss() },
                    onMod = onMod,
                    onUnmod = onUnmod,
                    onVip = onVip,
                    onUnvip = onUnvip,
                    onWhisper = { onWhisper(); onDismiss() },
                    onOpenChannel = { login ->
                        io.rudione.chatone.util.link.openUrl(
                            "https://www.twitch.tv/${login.lowercase()}",
                            settingsState.linkOpenMode
                        )
                    },
                    onDetach = null,
                    onDismiss = onDismiss
                )
            )
        }
    }

    if (banReasonPrompt) {
        AlertDialog(
            onDismissRequest = { banReasonPrompt = false },
            title = { Text("${s.profileBan}: $displayName") },
            text = {
                Column {
                    Text(
                        s.chatBanReasonHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    ChatoneTextField(
                        value = banReasonText,
                        onValueChange = { banReasonText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = s.chatBanReasonPlaceholder,
                        singleLine = false
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onBanWithReason(banReasonText.trim())
                    banReasonPrompt = false
                    onDismiss()
                }) { Text(s.profileBan) }
            },
            dismissButton = {
                TextButton(onClick = { banReasonPrompt = false }) { Text(s.cancel) }
            }
        )
    }
}
