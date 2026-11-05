package io.rudione.chatone.presentation.chat

import io.rudione.chatone.presentation.chat.timeline.rememberTimelineSource
import io.rudione.chatone.presentation.chat.timeline.profileAuthor
import io.rudione.chatone.presentation.chat.timeline.isBrowsingSeparately
import io.rudione.chatone.presentation.chat.timeline.RemoteHistoryPager
import io.rudione.chatone.presentation.chat.timeline.ProfileTimelineFeed
import chatone.composeapp.generated.resources.ui_copy_username
import chatone.composeapp.generated.resources.close
import chatone.composeapp.generated.resources.profile_detach_card
import androidx.compose.ui.text.style.TextOverflow
import chatone.composeapp.generated.resources.ui_open_twitch_profile
import chatone.composeapp.generated.resources.ic_twitch
import chatone.composeapp.generated.resources.cancel
import chatone.composeapp.generated.resources.chat_ban_reason_hint
import chatone.composeapp.generated.resources.chat_ban_reason_placeholder
import chatone.composeapp.generated.resources.delete
import chatone.composeapp.generated.resources.profile_ban
import chatone.composeapp.generated.resources.profile_delete_note
import chatone.composeapp.generated.resources.profile_delete_note_confirm
import chatone.composeapp.generated.resources.profile_editor_edit
import org.jetbrains.compose.resources.stringResource
import io.rudione.chatone.icons.lucide.Pencil
import io.rudione.chatone.data.repository.AccountManager
import io.rudione.chatone.presentation.profile.PageNavigator
import io.rudione.chatone.presentation.chat.roles.rememberUserRoles
import io.rudione.chatone.presentation.chat.roles.UserRolesTab
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import io.rudione.chatone.util.platform.setText
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import chatone.composeapp.generated.resources.Res
import coil3.compose.AsyncImage
import io.rudione.chatone.data.remote.AccountBadge
import io.rudione.chatone.data.remote.BadgeHqClient
import io.rudione.chatone.data.remote.GqlDisplayBadge
import io.rudione.chatone.data.remote.GqlUsercardMessage
import io.rudione.chatone.data.remote.IvrApiClient
import io.rudione.chatone.data.remote.SubAgeInfo
import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.data.remote.TwitchGqlClient
import io.rudione.chatone.data.repository.ChatRepository
import io.rudione.chatone.data.repository.MentionMuteRepository
import io.rudione.chatone.data.repository.ModerationHistory
import io.rudione.chatone.data.repository.ModerationHistoryRepository
import io.rudione.chatone.data.repository.UserNoteRepository
import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.util.Result
import io.rudione.chatone.domain.model.SevenTvCosmetics
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneTextField
import io.rudione.chatone.presentation.window.FloatingPanel
import io.rudione.chatone.presentation.window.FloatingPanelGrip
import io.rudione.chatone.presentation.window.rememberFloatingPlacement
import io.rudione.chatone.presentation.window.rememberFloatingPopupPositionProvider
import io.rudione.chatone.util.system.isDesktopPlatform
import io.rudione.chatone.icons.lucide.Copy
import io.rudione.chatone.icons.lucide.CopyCheck
import io.rudione.chatone.icons.lucide.ExternalLink
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.components.PortalPopup

private const val MOBILE_PROFILE_POSITION_KEY = "mobile.profile_popup"

@OptIn(ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Composable
fun UserProfilePopup(
    userId: String,
    username: String,
    displayName: String,
    color: String?,
    accessToken: String = "",
    channelId: String = "",
    profileImageUrl: String = "",
    createdAt: String = "",
    isModerator: Boolean = false,
    isSubscriber: Boolean = false,
    isVip: Boolean = false,
    isBroadcaster: Boolean = false,
    badges: List<Badge> = emptyList(),
    sevenTvBadge: SevenTvCosmetics.Badge? = null,
    channelMessages: List<DisplayMessage> = emptyList(),
    showModActions: Boolean = false,

    canManageRoles: Boolean = false,
    isBlocked: Boolean = false,
    onBlock: () -> Unit = {},
    onUnblock: () -> Unit = {},
    onTimeout: (Int) -> Unit = {},
    onBan: () -> Unit = {},
    onBanWithReason: (String) -> Unit = { onBan() },
    onUnban: () -> Unit = {},
    onMod: () -> Unit = {},
    onUnmod: () -> Unit = {},
    onVip: () -> Unit = {},
    onUnvip: () -> Unit = {},
    onWhisper: () -> Unit = {},
    onDetach: (() -> Unit)? = null,
    channelLogin: String = "",
    startPinned: Boolean = false,
    mentionMuteRepository: MentionMuteRepository? = null,
    onPinnedChange: (Boolean) -> Unit = {},
    onDismiss: () -> Unit
) {
    val noteRepository: UserNoteRepository = koinInject()
    val profiles: PageNavigator = koinInject()
    val activeAccountId by koinInject<AccountManager>().activeAccountId.collectAsState()

    if (isDesktopPlatform) {
        DesktopUserProfilePopup(
            userId = userId,
            username = username,
            displayName = displayName,
            color = color,
            initialAvatarUrl = profileImageUrl,
            initialCreatedAt = createdAt,
            badges = badges,
            sevenTvBadge = sevenTvBadge,
            isBroadcaster = isBroadcaster,
            isModerator = isModerator,
            isVip = isVip,
            isSubscriber = isSubscriber,
            sessionMessages = remember(channelMessages, userId) {
                channelMessages.filterIsInstance<DisplayMessage.PrivMsg>()
                    .filter { it.userId == userId }
                    .takeLast(1000)
            },
            canModerate = showModActions,
            canManageRoles = canManageRoles,
            isBlocked = isBlocked,
            channelId = channelId,
            channelLogin = channelLogin,
            accessToken = accessToken,
            startPinned = startPinned,
            onPinnedChange = onPinnedChange,
            mentionMuteRepository = mentionMuteRepository,
            noteRepository = noteRepository,
            onBlock = onBlock,
            onUnblock = onUnblock,
            onTimeout = onTimeout,
            onBan = onBan,
            onUnban = onUnban,
            onMod = onMod,
            onUnmod = onUnmod,
            onVip = onVip,
            onUnvip = onUnvip,
            onWhisper = onWhisper,
            onBanWithReason = onBanWithReason,
            onDismiss = onDismiss
        )
        return
    }

    val twitchApiClient: TwitchApiClient = koinInject()
    val twitchGqlClient: TwitchGqlClient = koinInject()
    val chatRepository: ChatRepository = koinInject()
    val moderationHistoryRepository: ModerationHistoryRepository = koinInject()
    var noteText by remember { mutableStateOf("") }
    var isNoteLoaded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val clipboard = LocalClipboard.current

    var moderationHistory by remember(userId) { mutableStateOf(ModerationHistory.Empty) }
    var isModerationHistoryLoading by remember(userId) { mutableStateOf(false) }
    val archive = rememberUserLogArchive(channelId, userId)
    val roles = rememberUserRoles(userId, username)
    val uriHandler = LocalUriHandler.current

    var localHistory by remember(userId) { mutableStateOf<List<ChatMessage>>(emptyList()) }

    var historyMessages by remember(userId) { mutableStateOf<List<GqlUsercardMessage>>(emptyList()) }
    var historyCursor by remember(userId) { mutableStateOf<String?>(null) }
    var hasMoreHistory by remember(userId) { mutableStateOf(false) }
    var isHistoryLoading by remember(userId) { mutableStateOf(false) }
    var localHistoryLoaded by remember(userId) { mutableStateOf(false) }
    val historyScope = rememberCoroutineScope()
    val canLoadHistory = showModActions && accessToken.isNotEmpty() && channelId.isNotEmpty() && userId.isNotEmpty()
    val loadHistory: () -> Unit = {
        if (!isHistoryLoading && canLoadHistory) {
            isHistoryLoading = true
            historyScope.launch {
                val page = twitchGqlClient.getUsercardMessagesBySender(
                    channelId, userId, historyCursor, accessToken
                )
                if (page != null) {
                    historyMessages = historyMessages + page.messages
                    historyCursor = page.nextCursor
                    hasMoreHistory = page.hasNextPage
                } else {
                    hasMoreHistory = false
                }
                isHistoryLoading = false
            }
        }
    }

    var fetchedAvatarUrl by remember(userId) { mutableStateOf(profileImageUrl) }
    var fetchedCreatedAt by remember(userId) { mutableStateOf(createdAt) }
    var gqlBadges by remember(userId) {
        mutableStateOf<List<GqlDisplayBadge>>(emptyList())
    }
    var accountBadges by remember(userId) { mutableStateOf<List<AccountBadge>>(emptyList()) }
    val badgeHqClient: BadgeHqClient = koinInject()
    var followedAt by remember(userId) { mutableStateOf<String?>(null) }
    var subAge by remember(userId) { mutableStateOf<SubAgeInfo?>(null) }
    val ivrApiClient: IvrApiClient = koinInject()


    var selectedTab by remember { mutableIntStateOf(0) }
    var archiveSearchOpen by remember(userId) { mutableStateOf(false) }
    var banReasonPrompt by remember { mutableStateOf(false) }
    var banReasonText by remember { mutableStateOf("") }

    val userMessages = remember(channelMessages, userId) {
        channelMessages
            .filterIsInstance<DisplayMessage.PrivMsg>()
            .filter { it.userId == userId }
            .takeLast(1000)
    }

    LaunchedEffect(userId, localHistoryLoaded, isHistoryLoading) {
        val silent = userMessages.isEmpty() && localHistory.isEmpty() && historyMessages.isEmpty()
        if (localHistoryLoaded && !isHistoryLoading && silent) archive.openIfLogged()
    }

    LaunchedEffect(userId) {
        fetchedAvatarUrl = profileImageUrl
        fetchedCreatedAt = createdAt
        followedAt = null
        subAge = null
        gqlBadges = emptyList()
        accountBadges = emptyList()
        if (username.isNotEmpty() && channelLogin.isNotEmpty()) {
            launch { subAge = ivrApiClient.getSubAge(username, channelLogin) }
        }
        if (username.isNotEmpty()) {
            launch {
                gqlBadges = runCatching {
                    twitchGqlClient.getUserDisplayBadges(username, channelLogin, accessToken)
                }.getOrDefault(emptyList())
            }
            launch { accountBadges = badgeHqClient.accountBadges(username).orEmpty() }
        }

        if (channelId.isNotEmpty() && userId.isNotEmpty()) {
            launch {
                val sessionIds = userMessages.map { it.id }.toSet()
                localHistory = chatRepository.getLocalHistoryForUser(channelId, userId)
                    .filterNot { it.id in sessionIds }
                localHistoryLoaded = true
            }
            if (showModActions) {
                launch {
                    isModerationHistoryLoading = true
                    moderationHistory =
                        moderationHistoryRepository.loadHistoryForUser(channelId, userId, accessToken)
                    isModerationHistoryLoading = false
                }
            }
            loadHistory()
        }

        val existing = noteRepository.getNote(userId)
        noteText = existing ?: ""
        isNoteLoaded = true

        if (accessToken.isNotEmpty() && userId.isNotEmpty()) {
            val result = twitchApiClient.getUsers(accessToken, ids = listOf(userId))
            if (result is Result.Success) {
                result.data.data.firstOrNull()?.let { userData ->
                    fetchedAvatarUrl = userData.profileImageUrl
                    fetchedCreatedAt = userData.createdAt.take(10)
                }
            }
            if (channelId.isNotEmpty()) {
                try {
                    val followResult =
                        twitchApiClient.getChannelFollower(accessToken, channelId, userId)
                    if (followResult is Result.Success) {
                        followResult.data.data.firstOrNull()?.let { follower ->
                            followedAt = follower.followedAt.take(10)
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(Res.string.profile_delete_note)) },
            text = { Text(stringResource(Res.string.profile_delete_note_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        noteRepository.deleteNote(userId); noteText = ""; showDeleteConfirmation =
                        false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(Res.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                }) { Text(stringResource(Res.string.cancel)) }
            }
        )
    }


    val placement = rememberFloatingPlacement(MOBILE_PROFILE_POSITION_KEY)
    val card: @Composable (dragHandle: Modifier?) -> Unit = { dragHandle ->
        Card(
            modifier = Modifier.width(320.dp).padding(8.dp)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || !event.isAltPressed) return@onPreviewKeyEvent false
                    if (!showModActions || isBroadcaster) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.B -> {
                            if (event.isShiftPressed) { banReasonPrompt = true }
                            else { onBan(); onDismiss() }
                            true
                        }
                        Key.U -> { onUnban(); onDismiss(); true }
                        Key.W -> { onWhisper(); onDismiss(); true }
                        Key.K -> { if (isBlocked) onUnblock() else onBlock(); onDismiss(); true }
                        Key.M -> if (canManageRoles) {
                            if (isModerator) onUnmod() else onMod(); true
                        } else false
                        Key.V -> if (canManageRoles) {
                            if (isVip) onUnvip() else onVip(); true
                        } else false
                        else -> false
                    }
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            if (dragHandle != null) FloatingPanelGrip(dragHandle)
            Column {
                UserProfileHeader(
                    modifier = dragHandle ?: Modifier,
                    avatarUrl = fetchedAvatarUrl,
                    displayName = displayName,
                    username = username,
                    userId = userId,
                    color = color,
                    badges = badges,
                    gqlBadges = gqlBadges,
                    accountBadges = accountBadges,
                    sevenTvBadge = sevenTvBadge,
                    onDetach = onDetach,
                    onDismiss = onDismiss,
                    onOpenProfile = {
                        onDismiss()
                        profiles.openChannel(username)
                    }.takeIf { profiles.isSupported && username.isNotBlank() },
                    onEdit = {
                        onDismiss()
                        profiles.openEditor(channelLogin.ifBlank { null })
                    }.takeIf { profiles.isSupported && userId.isNotBlank() && userId == activeAccountId }
                )
                val showHistoryTab = showModActions &&
                        (isModerationHistoryLoading || moderationHistory.totalActions > 0)
                val activeTab = if (selectedTab == 2 && !showHistoryTab) 1 else selectedTab
                CompactProfileTabs(
                    selectedTab = activeTab,
                    onSelect = { selectedTab = it },
                    messagesCount = userMessages.size,
                    showHistoryTab = showHistoryTab,
                    historyCount = moderationHistory.totalActions,
                    showRolesTab = roles.isSupported,
                    trailing = {
                        if (activeTab == 1) {
                            ProfileArchiveControls(
                                archive = archive,
                                inlineSearch = false,
                                searchExpanded = archiveSearchOpen,
                                onToggleSearch = {
                                    if (archiveSearchOpen && archive.isSearching) archive.updateQuery("")
                                    archiveSearchOpen = !archiveSearchOpen
                                }
                            )
                        }
                    }
                )
                ProfileArchiveSearchBar(
                    archive = archive,
                    visible = activeTab == 1 && (archiveSearchOpen || archive.isSearching)
                )

                when (activeTab) {
                    0 -> UsercardTab(
                        userId = userId,
                        fetchedCreatedAt = fetchedCreatedAt,
                        subAge = subAge,
                        followedAt = followedAt,
                        noteText = noteText,
                        isNoteLoaded = isNoteLoaded,
                        noteRepository = noteRepository,
                        clipboard = clipboard,
                        showModActions = showModActions,
                        isBroadcaster = isBroadcaster,
                        isModerator = isModerator,
                        isVip = isVip,
                        isSubscriber = isSubscriber,
                        canManageRoles = canManageRoles,
                        isBlocked = isBlocked,
                        onBlock = onBlock,
                        onUnblock = onUnblock,
                        onNoteChange = { noteText = it },
                        onShowDeleteConfirmation = { showDeleteConfirmation = true },
                        onWhisper = onWhisper,
                        onTimeout = onTimeout,
                        onBan = onBan,
                        onUnban = onUnban,
                        onMod = onMod,
                        onUnmod = onUnmod,
                        onVip = onVip,
                        onUnvip = onUnvip,
                        onDismiss = onDismiss,
                        channelLogin = channelLogin,
                        mentionMuteRepository = mentionMuteRepository,
                        username = username
                    )

                    1 -> if (archive.isBrowsingSeparately) {
                        ProfileArchiveFeed(
                            archive = archive,
                            displayName = displayName,
                            nameColor = rememberNickColors().of(color, username.ifBlank { displayName }),
                            modifier = Modifier.fillMaxWidth().height(380.dp)
                        )
                    } else ProfileTimelineFeed(
                        source = rememberTimelineSource(
                            session = userMessages,
                            local = localHistory,
                            remote = historyMessages,
                            pager = RemoteHistoryPager(canLoadHistory, hasMoreHistory, isHistoryLoading, loadHistory),
                            archive = archive
                        ),
                        author = remember(userMessages, userId) {
                            profileAuthor(userMessages, userId, username, displayName, color, channelLogin)
                        },
                        nameColor = rememberNickColors().of(color, username.ifBlank { displayName }),
                        modifier = Modifier.fillMaxWidth().height(380.dp)
                    )

                    2 -> ModerationHistoryTab(
                        history = moderationHistory,
                        isLoading = isModerationHistoryLoading
                    )

                    PROFILE_ROLES_TAB -> UserRolesTab(
                        state = roles,
                        onOpenChannel = { login -> uriHandler.openUri("https://www.twitch.tv/${login.lowercase()}") },
                        modifier = Modifier.fillMaxWidth().height(320.dp)
                    )
                }
            }
        }
    }
    if (startPinned) {
        FloatingPanel(
            placement = placement,
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.navigationBars.union(WindowInsets.ime).only(WindowInsetsSides.Bottom)
            )
        ) { dragHandle -> card(dragHandle) }
    } else {
        PortalPopup(
            popupPositionProvider = rememberFloatingPopupPositionProvider(placement),
            onDismissRequest = onDismiss,
            properties = PopupProperties(focusable = true)
        ) { card(null) }
    }

    if (banReasonPrompt) {
        AlertDialog(
            onDismissRequest = { banReasonPrompt = false },
            title = { Text("${stringResource(Res.string.profile_ban)}: $displayName") },
            text = {
                Column {
                    Text(
                        stringResource(Res.string.chat_ban_reason_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    ChatoneTextField(
                        value = banReasonText,
                        onValueChange = { banReasonText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = stringResource(Res.string.chat_ban_reason_placeholder),
                        singleLine = false
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onBanWithReason(banReasonText.trim())
                    banReasonPrompt = false
                    onDismiss()
                }) { Text(stringResource(Res.string.profile_ban)) }
            },
            dismissButton = {
                TextButton(onClick = { banReasonPrompt = false }) { Text(stringResource(Res.string.cancel)) }
            }
        )
    }
}

@Composable
fun UserProfileHeader(
    avatarUrl: String,
    displayName: String,
    username: String,
    color: String?,
    badges: List<Badge>,
    sevenTvBadge: SevenTvCosmetics.Badge?,
    modifier: Modifier = Modifier,
    userId: String = "",
    gqlBadges: List<GqlDisplayBadge> = emptyList(),
    accountBadges: List<AccountBadge> = emptyList(),
    onDetach: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    showIconDetached: Boolean = true,
    onOpenProfile: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var showCopied by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val sevenTv = rememberSevenTvCosmetic(userId)
    val thirdPartyMaps = LocalThirdPartyBadges.current
    val badgeItems = remember(userId, badges, gqlBadges, sevenTvBadge, thirdPartyMaps, accountBadges) {
        buildProfileBadges(
            chatBadges = badges,
            gqlBadges = gqlBadges,
            sevenTvBadge = sevenTvBadge,
            thirdPartyBadges = buildList {
                thirdPartyMaps.ffzByLogin[username.lowercase()]?.let { addAll(it) }
                thirdPartyMaps.bttvByUserId[userId]?.let { add(it) }
            },
            accountBadges = accountBadges
        )
    }

    LaunchedEffect(showCopied) {
        if (showCopied) {
            kotlinx.coroutines.delay(1500)
            showCopied = false
        }
    }

    Column(modifier = modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            val avatarModifier = Modifier.size(48.dp).clip(CircleShape)
                .then(if (onOpenProfile != null) Modifier.clickable(onClick = onOpenProfile) else Modifier)
            if (avatarUrl.isNotEmpty()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = displayName,
                    modifier = avatarModifier
                )
            } else {
                Box(
                    modifier = avatarModifier
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        displayName.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                ProfileDisplayName(
                    displayName = displayName,
                    color = color,
                    fallbackLogin = username,
                    paint = sevenTv?.paint,
                    style = MaterialTheme.typography.titleMedium
                )
                if (username.lowercase() != displayName.lowercase()) {
                    Text(
                        "@$username",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ProfileHeaderAction(
                        onClick = {
                            scope.launch { clipboard.setText(username) }
                            showCopied = true
                        }
                    ) {
                        Icon(
                            if (showCopied) Lucide.CopyCheck else Lucide.Copy,
                            contentDescription = stringResource(Res.string.ui_copy_username),
                            modifier = Modifier.size(16.dp),
                            tint = if (showCopied) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ProfileHeaderAction(
                        onClick = { uriHandler.openUri("https://www.twitch.tv/${username.lowercase()}") }
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_twitch),
                            contentDescription = stringResource(Res.string.ui_open_twitch_profile),
                            modifier = Modifier.size(16.dp),
                            tint = TwitchPurple
                        )
                    }
                    sevenTv?.profile?.let { profile ->
                        Box(Modifier.padding(horizontal = 4.dp)) { SevenTvLinkButton(profile = profile) }
                    }
                    if (onEdit != null) {
                        ProfileHeaderAction(onClick = onEdit) {
                            Icon(
                                Lucide.Pencil,
                                contentDescription = stringResource(Res.string.profile_editor_edit),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (onDetach != null) {
                        ProfileHeaderAction(onClick = onDetach) {
                            Icon(
                                Lucide.ExternalLink,
                                contentDescription = stringResource(Res.string.profile_detach_card),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (onDismiss != null && showIconDetached) {
                ChatoneIconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Lucide.X,
                        contentDescription = stringResource(Res.string.close),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (badgeItems.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            ProfileBadgeStrip(badges = badgeItems, badgeSize = 20.dp, spacing = 4.dp)
        }
    }
}

@Composable
private fun ProfileHeaderAction(onClick: () -> Unit, content: @Composable () -> Unit) {
    ChatoneIconButton(onClick = onClick, modifier = Modifier.size(30.dp), content = content)
}

private val TwitchPurple = Color(0xFF9146FF)
