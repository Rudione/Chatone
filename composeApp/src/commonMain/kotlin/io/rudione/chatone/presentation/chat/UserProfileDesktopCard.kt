package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.repository.ThirdPartyBadgeRepository
import io.rudione.chatone.data.repository.UserNoteRepository
import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.HighlightRule
import io.rudione.chatone.domain.model.SevenTvCosmetics
import io.rudione.chatone.presentation.chat.roles.UserRolesTab
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import androidx.compose.foundation.layout.fillMaxSize
import org.koin.compose.koinInject

internal data class ProfileCardData(
    val userId: String,
    val username: String,
    val displayName: String,
    val color: String?,
    val avatarUrl: String,
    val badges: List<Badge>,
    val sevenTvBadge: SevenTvCosmetics.Badge?,
    val isBroadcaster: Boolean,
    val isModerator: Boolean,
    val isVip: Boolean,
    val isSubscriber: Boolean
)

internal data class ProfileCardCallbacks(
    val onBlock: () -> Unit,
    val onUnblock: () -> Unit,
    val onTimeout: (Int) -> Unit,
    val onBan: () -> Unit,
    val onUnban: () -> Unit,
    val onMod: () -> Unit,
    val onUnmod: () -> Unit,
    val onVip: () -> Unit,
    val onUnvip: () -> Unit,
    val onWhisper: () -> Unit,
    val onOpenChannel: (String) -> Unit,
    val onDetach: (() -> Unit)?,
    val onDismiss: () -> Unit
)

@Composable
internal fun UserProfileDesktopCard(
    data: ProfileCardData,
    profileState: UserProfileState,
    sessionMessages: List<DisplayMessage.PrivMsg>,
    highlightRules: List<HighlightRule>,
    chatFontSizeSp: Float,
    canModerate: Boolean,
    currentUserIsBroadcaster: Boolean,
    isBlocked: Boolean,
    mentionsMuted: Boolean,
    onToggleMentionMute: () -> Unit,
    noteRepository: UserNoteRepository,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    callbacks: ProfileCardCallbacks
) {
    val s = LocalStrings.current
    var selectedTab by remember(data.userId) { mutableStateOf(ProfileCardTab.Messages) }
    val showHistory = canModerate &&
            (profileState.isModerationHistoryLoading || profileState.moderationHistory.isNotEmpty())
    val activeTab = if (selectedTab == ProfileCardTab.ModHistory && !showHistory) ProfileCardTab.Messages
    else selectedTab
    var notesOpen by remember(data.userId) { mutableStateOf(false) }
    var archiveSearchOpen by remember(data.userId) { mutableStateOf(false) }
    val archive = profileState.archive
    var noteText by remember(data.userId) { mutableStateOf(noteRepository.getNote(data.userId) ?: "") }

    val sevenTv = rememberSevenTvCosmetic(data.userId)

    val badgeRepository: ThirdPartyBadgeRepository = koinInject()
    val ffzBadges by badgeRepository.ffzByLogin.collectAsState()
    val bttvBadges by badgeRepository.bttvByUserId.collectAsState()

    val badgeItems = remember(
        data.userId, data.badges, profileState.gqlBadges, data.sevenTvBadge,
        ffzBadges, bttvBadges
    ) {
        buildProfileBadges(
            chatBadges = data.badges,
            gqlBadges = profileState.gqlBadges,
            sevenTvBadge = data.sevenTvBadge ?: sevenTv?.badge,
            thirdPartyBadges = buildList {
                ffzBadges[data.username.lowercase()]?.let { addAll(it) }
                bttvBadges[data.userId]?.let { add(it) }
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxWidth < PROFILE_CARD_COMPACT_WIDTH
        val inlineSearch = maxWidth >= PROFILE_CARD_INLINE_SEARCH_WIDTH
        val feedFontSize = if (compact) (chatFontSizeSp * 0.85f).coerceAtLeast(10f)
        else chatFontSizeSp

        Column(modifier = Modifier.fillMaxSize()) {
            ProfileCardHeader(
                data = data,
                dossier = profileState.dossier,
                followedAt = profileState.followedAt,
                badges = badgeItems,
                compact = compact,
                sevenTv = sevenTv,
                previousNames = profileState.previousNames,
                isPinned = isPinned,
                onTogglePin = onTogglePin,
                onDetach = callbacks.onDetach,
                onDismiss = callbacks.onDismiss
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            ProfileQuickActions(
                canModerate = canModerate,
                currentUserIsBroadcaster = currentUserIsBroadcaster,
                targetIsBroadcaster = data.isBroadcaster,
                targetIsModerator = data.isModerator,
                targetIsVip = data.isVip,
                isBlocked = isBlocked,
                mentionsMuted = mentionsMuted,
                noteFilled = noteText.isNotBlank(),
                notesOpen = notesOpen,
                compact = compact,
                onToggleNotes = { notesOpen = !notesOpen },
                onToggleMentionMute = onToggleMentionMute,
                onBlockToggle = { if (isBlocked) callbacks.onUnblock() else callbacks.onBlock() },
                onWhisper = callbacks.onWhisper,
                onMod = callbacks.onMod,
                onUnmod = callbacks.onUnmod,
                onVip = callbacks.onVip,
                onUnvip = callbacks.onUnvip
            )

            if (notesOpen) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    CompactModField(
                        value = noteText,
                        onValueChange = { updated ->
                            noteText = updated
                            if (updated.isNotBlank()) noteRepository.saveNote(data.userId, updated)
                            else noteRepository.deleteNote(data.userId)
                        },
                        placeholder = s.profileNotePlaceholder,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (canModerate && !data.isBroadcaster) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
                ProfileModerationStrip(
                    canModerate = true,
                    targetIsBroadcaster = data.isBroadcaster,
                    compact = compact,
                    onTimeout = callbacks.onTimeout,
                    onBan = callbacks.onBan,
                    onUnban = callbacks.onUnban
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            ProfileCardTabRow(
                selected = activeTab,
                onSelect = { selectedTab = it },
                messagesCount = sessionMessages.size + profileState.localHistory.size +
                        profileState.remoteHistory.size,
                historyCount = profileState.moderationHistory.size,
                showHistory = showHistory,
                compact = compact,
                trailing = {
                    if (activeTab == ProfileCardTab.Messages) {
                        ProfileArchiveControls(
                            archive = archive,
                            inlineSearch = inlineSearch,
                            searchExpanded = archiveSearchOpen,
                            onToggleSearch = {
                                if (archiveSearchOpen && archive.isSearching) archive.updateQuery("")
                                archiveSearchOpen = !archiveSearchOpen
                            }
                        )
                    }
                }
            )

            if (!inlineSearch) {
                ProfileArchiveSearchBar(
                    archive = archive,
                    visible = activeTab == ProfileCardTab.Messages && (archiveSearchOpen || archive.isSearching)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
            ) {
                when (activeTab) {
                    ProfileCardTab.Messages -> if (profileState.archive.isActive) {
                        ProfileArchiveFeed(
                            archive = profileState.archive,
                            displayName = data.displayName,
                            nameColor = rememberNickColors().of(data.color, data.username),
                            modifier = Modifier.fillMaxSize()
                        )
                    } else ProfileMessageFeed(
                        sessionMessages = sessionMessages,
                        localHistory = profileState.localHistory,
                        remoteHistory = profileState.remoteHistory,
                        displayName = data.displayName,
                        userColor = data.color,
                        login = data.username,
                        isHistoryLoading = profileState.isHistoryLoading,
                        hasMoreHistory = profileState.hasMoreHistory,
                        onLoadHistory = { profileState.loadMoreHistory() }
                            .takeIf { profileState.canLoadHistory },
                        highlightRules = highlightRules,
                        chatFontSizeSp = feedFontSize,
                        modifier = Modifier.fillMaxSize()
                    )

                    ProfileCardTab.Dossier -> UserDossierTab(
                        dossier = profileState.dossier,
                        isLoading = profileState.isDossierLoading,
                        subAge = profileState.subAge,
                        followedAtFallback = profileState.followedAt,
                        sevenTv = sevenTv,
                        presence = profileState.presence,
                        onOpenChannel = callbacks.onOpenChannel,
                        modifier = Modifier.fillMaxSize()
                    )

                    ProfileCardTab.Roles -> UserRolesTab(
                        state = profileState.roles,
                        onOpenChannel = callbacks.onOpenChannel,
                        modifier = Modifier.fillMaxSize()
                    )

                    ProfileCardTab.ModHistory -> ModerationHistoryTab(
                        entries = profileState.moderationHistory,
                        isLoading = profileState.isModerationHistoryLoading
                    )
                }
            }
        }
    }
}
