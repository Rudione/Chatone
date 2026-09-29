package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.rudione.chatone.data.remote.GqlUserDossier
import io.rudione.chatone.data.remote.NameHistoryEntry
import io.rudione.chatone.domain.model.SevenTvUserCosmetic
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.window.windowDragArea
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.ic_twitch
import io.rudione.chatone.icons.lucide.Copy
import io.rudione.chatone.icons.lucide.CopyCheck
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Users
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileCardHeader(
    data: ProfileCardData,
    dossier: GqlUserDossier?,
    followedAt: String?,
    badges: List<ProfileBadgeItem>,
    compact: Boolean,
    sevenTv: SevenTvUserCosmetic?,
    previousNames: List<NameHistoryEntry>,
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
            .windowDragArea()
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
                        fallbackLogin = data.username,
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
                            if (copiedName) Lucide.CopyCheck else Lucide.Copy,
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

            FlowRow(
                itemVerticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (data.userId.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                                if (copiedId) Lucide.CopyCheck else Lucide.Copy,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = if (copiedId) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
                ProfileColorChip(data.color)
                ProfileAkaChip(previousNames)
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
                        icon = Lucide.Users,
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
