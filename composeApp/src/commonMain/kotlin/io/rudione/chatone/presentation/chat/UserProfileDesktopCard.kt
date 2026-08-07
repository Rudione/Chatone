package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.rudione.chatone.data.remote.GqlUserDossier
import io.rudione.chatone.data.repository.ThirdPartyBadgeRepository
import io.rudione.chatone.data.repository.UserNoteRepository
import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.HighlightRule
import io.rudione.chatone.domain.model.SevenTvCosmetics
import io.rudione.chatone.domain.model.SevenTvUserCosmetic
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.copy_check
import chatone.composeapp.generated.resources.ic_copy
import chatone.composeapp.generated.resources.ic_twitch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
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
    var notesOpen by remember(data.userId) { mutableStateOf(false) }
    var noteText by remember(data.userId) { mutableStateOf(noteRepository.getNote(data.userId) ?: "") }

    val sevenTv = rememberSevenTvCosmetic(data.userId)

    val badgeRepository: ThirdPartyBadgeRepository = koinInject()
    val ffzBadges by badgeRepository.ffzByLogin.collectAsState()
    val bttvBadges by badgeRepository.bttvByUserId.collectAsState()
    val chatoneBadges by badgeRepository.chatoneByUserId.collectAsState()

    val badgeItems = remember(
        data.userId, data.badges, profileState.gqlBadges, data.sevenTvBadge,
        ffzBadges, bttvBadges, chatoneBadges
    ) {
        buildProfileBadges(
            chatBadges = data.badges,
            gqlBadges = profileState.gqlBadges,
            sevenTvBadge = data.sevenTvBadge ?: sevenTv?.badge,
            thirdPartyBadges = buildList {
                chatoneBadges[data.userId]?.let { addAll(it) }
                ffzBadges[data.username.lowercase()]?.let { addAll(it) }
                bttvBadges[data.userId]?.let { add(it) }
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxWidth < PROFILE_CARD_COMPACT_WIDTH
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
                selected = selectedTab,
                onSelect = { selectedTab = it },
                messagesCount = sessionMessages.size + profileState.localHistory.size +
                        profileState.remoteHistory.size,
                historyCount = profileState.moderationHistory.size,
                showHistory = canModerate,
                compact = compact
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
            ) {
                when (selectedTab) {
                    ProfileCardTab.Messages -> ProfileMessageFeed(
                        sessionMessages = sessionMessages,
                        localHistory = profileState.localHistory,
                        remoteHistory = profileState.remoteHistory,
                        displayName = data.displayName,
                        userColor = data.color,
                        isHistoryLoading = profileState.isHistoryLoading,
                        hasMoreHistory = profileState.hasMoreHistory,
                        historyLoadFailed = profileState.historyError != null,
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
                        onOpenChannel = callbacks.onOpenChannel,
                        modifier = Modifier.fillMaxSize()
                    )

                    ProfileCardTab.ModHistory ->
                        ModerationHistoryTab(entries = profileState.moderationHistory)
                }
            }
        }
    }
}

internal enum class ProfileCardTab { Messages, Dossier, ModHistory }

@Composable
private fun ProfileCardTabRow(
    selected: ProfileCardTab,
    onSelect: (ProfileCardTab) -> Unit,
    messagesCount: Int,
    historyCount: Int,
    showHistory: Boolean,
    compact: Boolean
) {
    val s = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth()
            .then(if (compact) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
            .padding(horizontal = if (compact) 5.dp else 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 1.dp else 3.dp)
    ) {
        ProfileCardTabChip(
            label = s.profileTabMessages,
            badge = messagesCount,
            selected = selected == ProfileCardTab.Messages,
            compact = compact,
            onClick = { onSelect(ProfileCardTab.Messages) }
        )
        ProfileCardTabChip(
            label = s.profileTabDossier,
            badge = 0,
            selected = selected == ProfileCardTab.Dossier,
            compact = compact,
            onClick = { onSelect(ProfileCardTab.Dossier) }
        )
        if (showHistory) {
            ProfileCardTabChip(
                label = s.profileTabHistory,
                badge = historyCount,
                selected = selected == ProfileCardTab.ModHistory,
                compact = compact,
                onClick = { onSelect(ProfileCardTab.ModHistory) }
            )
        }
    }
}

@Composable
private fun ProfileCardTabChip(
    label: String,
    badge: Int,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 5.dp else 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            label,
            style = if (compact) MaterialTheme.typography.labelSmall
            else MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (badge > 0) {
            Text(
                if (badge > 999) "999+" else "$badge",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileCardHeader(
    data: ProfileCardData,
    dossier: GqlUserDossier?,
    followedAt: String?,
    badges: List<ProfileBadgeItem>,
    compact: Boolean,
    sevenTv: SevenTvUserCosmetic?,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    onDetach: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    var copiedName by remember { mutableStateOf(false) }
    var copiedId by remember { mutableStateOf(false) }

    LaunchedEffect(copiedName) {
        if (copiedName) { kotlinx.coroutines.delay(1400); copiedName = false }
    }
    LaunchedEffect(copiedId) {
        if (copiedId) { kotlinx.coroutines.delay(1400); copiedId = false }
    }

    val avatarSize = if (compact) 38.dp else 46.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .profileWindowDrag()
            .padding(
                start = if (compact) 8.dp else 12.dp,
                end = 4.dp,
                top = if (compact) 6.dp else 9.dp,
                bottom = if (compact) 5.dp else 7.dp
            ),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (data.avatarUrl.isNotEmpty()) {
                AsyncImage(
                    model = data.avatarUrl,
                    contentDescription = data.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    data.displayName.take(2).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.width(if (compact) 8.dp else 11.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileDisplayName(
                        displayName = data.displayName,
                        color = data.color,
                        paint = sevenTv?.paint,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(3.dp))
                    ChatoneIconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(data.username))
                            copiedName = true
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            painterResource(if (copiedName) Res.drawable.copy_check else Res.drawable.ic_copy),
                            contentDescription = s.chatCopyUsername,
                            modifier = Modifier.size(12.dp),
                            tint = if (copiedName) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                    }
                    ChatoneIconButton(
                        onClick = { uriHandler.openUri("https://www.twitch.tv/${data.username.lowercase()}") },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            painterResource(Res.drawable.ic_twitch),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFF9146FF)
                        )
                    }
                    if (sevenTv?.profile != null) {
                        SevenTvLinkButton(profile = sevenTv.profile)
                    }
                }
                ProfileCardHeaderActions(
                    isPinned = isPinned,
                    onTogglePin = onTogglePin,
                    onDetach = onDetach,
                    onDismiss = onDismiss
                )
            }

            Spacer(Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    "ID: ${data.userId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                ChatoneIconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(data.userId))
                        copiedId = true
                    },
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(
                        painterResource(if (copiedId) Res.drawable.copy_check else Res.drawable.ic_copy),
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = if (copiedId) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    )
                }
            }

            Spacer(Modifier.height(2.dp))

            val followText =
                dossier?.followedChannelAtEpochMs?.let { formatDate(it) } ?: followedAt
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                dossier?.followerCount?.let { count ->
                    ProfileIdentityLine(
                        icon = Icons.Outlined.Group,
                        text = compactCount(count)
                    )
                }
                dossier?.createdAtEpochMs?.let { created ->
                    ProfileIdentityLine(
                        icon = ProfileIconJoined,
                        text = formatDate(created)
                    )
                }
                followText?.let { date ->
                    ProfileIdentityLine(
                        icon = ProfileIconFollow,
                        text = date,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                    )
                }
            }

            if (badges.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                ProfileBadgeStrip(
                    badges = badges,
                    badgeSize = if (compact) 16.dp else 18.dp
                )
            }
        }
    }
}
