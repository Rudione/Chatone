package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.remote.GqlUserDossier
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.theme.i18n.format
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun UserDossierTab(
    dossier: GqlUserDossier?,
    isLoading: Boolean,
    subAge: io.rudione.chatone.data.remote.SubAgeInfo?,
    followedAtFallback: String?,
    onOpenChannel: (String) -> Unit,
    modifier: Modifier = Modifier,
    sevenTv: io.rudione.chatone.domain.model.SevenTvUserCosmetic? = null
) {
    val s = LocalStrings.current

    if (isLoading && dossier == null && sevenTv == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        }
        return
    }

    if (dossier == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sevenTv?.let { SevenTvDossierSection(cosmetic = it) }
            Text(
                s.profileDossierUnavailable,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sevenTv?.let { SevenTvDossierSection(cosmetic = it) }

        if (dossier.isLive) {
            LiveRow(dossier = dossier, onOpenChannel = onOpenChannel)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DossierStat(
                label = s.profileDossierAccountAge,
                value = dossier.createdAtEpochMs?.let { relativeAge(it) } ?: "—",
                icon = Icons.Outlined.DateRange,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            DossierStat(
                label = s.profileDossierFollowers,
                value = dossier.followerCount?.let { compactCount(it) } ?: "—",
                icon = Icons.Outlined.Group,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
            DossierStat(
                label = s.profileDossierWithUs,
                value = (dossier.followedChannelAtEpochMs?.let { relativeAge(it) })
                    ?: followedAtFallback
                    ?: s.profileDossierNotFollowing,
                icon = Icons.Filled.Favorite,
                tint = ChatoneTheme.extraColors.live,
                modifier = Modifier.weight(1f)
            )
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (dossier.isPartner) {
                DossierTag(s.profileDossierPartner, Color(0xFF9146FF), Icons.Outlined.Verified)
            }
            if (dossier.isAffiliate) {
                DossierTag(s.profileDossierAffiliate, MaterialTheme.colorScheme.tertiary, Icons.Outlined.Verified)
            }
            if (dossier.isStaff) {
                DossierTag("Twitch Staff", MaterialTheme.colorScheme.error, Icons.Outlined.Verified)
            }
            dossier.subscriptionTier?.let { tier ->
                DossierTag(
                    s.format(s.profileDossierSubTier, tierLabel(tier)),
                    MaterialTheme.colorScheme.primary,
                    Icons.Filled.Star
                )
            }
            subAge?.takeIf { !it.hidden && it.cumulativeMonths > 0 }?.let { sa ->
                DossierTag(
                    s.profileSubAgeMonths.replace("{0}", sa.cumulativeMonths.toString()),
                    MaterialTheme.colorScheme.primary,
                    Icons.Filled.Star
                )
            }
            dossier.teamName?.let { team ->
                DossierTag(team, MaterialTheme.colorScheme.secondary, Icons.Outlined.Group)
            }
            dossier.language?.let { lang ->
                DossierTag(lang.uppercase(), MaterialTheme.colorScheme.onSurfaceVariant, Icons.Outlined.Language)
            }
        }

        if (dossier.isStreamer) {
            DossierSection(title = s.profileDossierStreamerSection) {
                dossier.lastBroadcastEpochMs?.let { last ->
                    DossierLine(
                        icon = Icons.Outlined.Videocam,
                        text = s.format(
                            s.profileDossierLastStream,
                            relativeAge(last),
                            dossier.lastBroadcastGame ?: "—"
                        )
                    )
                }
                if (dossier.followerCount != null) {
                    DossierLine(
                        icon = Icons.Outlined.Group,
                        text = s.format(
                            s.profileDossierFollowerLine,
                            compactCount(dossier.followerCount)
                        )
                    )
                }
                DossierLine(
                    icon = Icons.Outlined.PlayArrow,
                    text = s.profileDossierOpenChannel,
                    clickable = true,
                    onClick = { onOpenChannel(dossier.login) }
                )
            }
        }

        dossier.bio?.let { bio ->
            DossierSection(title = s.profileDossierBio) {
                Text(
                    bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }
        }

        dossier.createdAtEpochMs?.let { created ->
            Text(
                s.format(s.profileDossierCreatedOn, formatDate(created)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun LiveRow(dossier: GqlUserDossier, onOpenChannel: (String) -> Unit) {
    val s = LocalStrings.current
    val live = ChatoneTheme.extraColors.live
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(live.copy(alpha = 0.12f))
            .border(1.dp, live.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .clickable { onOpenChannel(dossier.login) }
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(live))
        Spacer(Modifier.width(7.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                s.format(
                    s.profileDossierLiveNow,
                    dossier.liveViewers?.let { compactCount(it) } ?: "—"
                ),
                style = MaterialTheme.typography.labelMedium,
                color = live,
                fontWeight = FontWeight.SemiBold
            )
            dossier.liveGame?.let { game ->
                Text(
                    game,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        dossier.liveSinceEpochMs?.let { since ->
            Text(
                relativeAge(since),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun DossierStat(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.09f))
            .padding(vertical = 7.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = tint)
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DossierTag(label: String, tint: Color, icon: ImageVector) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(11.dp), tint = tint)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun DossierSection(title: String, content: @Composable ColumnScopeAlias.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontWeight = FontWeight.SemiBold
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) { content() }
    }
}

private typealias ColumnScopeAlias = androidx.compose.foundation.layout.ColumnScope

@Composable
private fun DossierLine(
    icon: ImageVector,
    text: String,
    clickable: Boolean = false,
    onClick: () -> Unit = {}
) {
    val color = if (clickable) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = color)
        Text(text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

internal fun compactCount(value: Int): String = when {
    value >= 1_000_000 -> "${value / 100_000 / 10.0}M".replace(".0M", "M")
    value >= 1_000 -> "${value / 100 / 10.0}K".replace(".0K", "K")
    else -> value.toString()
}

internal fun relativeAge(epochMs: Long): String {
    val nowMs = Clock.System.now().toEpochMilliseconds()
    val diff = (nowMs - epochMs).coerceAtLeast(0L)
    val minutes = diff / 60_000
    val hours = minutes / 60
    val days = hours / 24
    val months = days / 30
    val years = days / 365
    return when {
        years >= 1 -> "${years}г"
        months >= 1 -> "${months}мес"
        days >= 1 -> "${days}д"
        hours >= 1 -> "${hours}ч"
        else -> "${minutes}м"
    }
}

internal fun formatDate(epochMs: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault())
    val day = dt.day.toString().padStart(2, '0')
    val month = (dt.month.ordinal + 1).toString().padStart(2, '0')
    return "$day.$month.${dt.year}"
}

private fun tierLabel(raw: String): String = when (raw.uppercase()) {
    "TIER_1", "1000" -> "1"
    "TIER_2", "2000" -> "2"
    "TIER_3", "3000" -> "3"
    else -> raw
}
