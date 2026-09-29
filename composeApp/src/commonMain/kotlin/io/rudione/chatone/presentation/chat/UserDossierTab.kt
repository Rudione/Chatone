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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import io.rudione.chatone.data.remote.SubAgeInfo
import io.rudione.chatone.domain.model.SevenTvUserCosmetic
import io.rudione.chatone.presentation.chat.presence.PresenceMapSection
import io.rudione.chatone.presentation.chat.presence.UserPresenceState
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.theme.i18n.format
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant
import io.rudione.chatone.icons.lucide.BadgeCheck
import io.rudione.chatone.icons.lucide.CalendarDays
import io.rudione.chatone.icons.lucide.Globe
import io.rudione.chatone.icons.lucide.Heart
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Play
import io.rudione.chatone.icons.lucide.Star
import io.rudione.chatone.icons.lucide.Users
import io.rudione.chatone.icons.lucide.Video

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun UserDossierTab(
    dossier: GqlUserDossier?,
    isLoading: Boolean,
    subAge: SubAgeInfo?,
    followedAtFallback: String?,
    onOpenChannel: (String) -> Unit,
    modifier: Modifier = Modifier,
    sevenTv: SevenTvUserCosmetic? = null,
    presence: UserPresenceState? = null
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
            presence?.let { PresenceMapSection(it) }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        sevenTv?.let { SevenTvDossierSection(cosmetic = it) }

        if (dossier.isLive) {
            LiveRow(dossier = dossier, onOpenChannel = onOpenChannel)
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            dossier.createdAtEpochMs?.let { created ->
                DossierChip(
                    icon = Lucide.CalendarDays,
                    tint = MaterialTheme.colorScheme.primary,
                    caption = s.profileDossierAccountAge,
                    value = "${relativeAge(created, s.insights.ageUnits)} · ${formatDate(created)}"
                )
            }
            dossier.followerCount?.let { count ->
                DossierChip(
                    icon = Lucide.Users,
                    tint = MaterialTheme.colorScheme.tertiary,
                    caption = s.profileDossierFollowers,
                    value = compactCount(count)
                )
            }
            DossierChip(
                icon = Lucide.Heart,
                tint = ChatoneTheme.extraColors.live,
                caption = s.profileDossierWithUs,
                value = (dossier.followedChannelAtEpochMs?.let { relativeAge(it, s.insights.ageUnits) })
                    ?: followedAtFallback
                    ?: s.profileDossierNotFollowing
            )
            if (dossier.isPartner) {
                DossierChip(Lucide.BadgeCheck, Color(0xFF9146FF), value = s.profileDossierPartner)
            }
            if (dossier.isAffiliate) {
                DossierChip(Lucide.BadgeCheck, MaterialTheme.colorScheme.tertiary, value = s.profileDossierAffiliate)
            }
            if (dossier.isStaff) {
                DossierChip(Lucide.BadgeCheck, MaterialTheme.colorScheme.error, value = "Twitch Staff")
            }
            dossier.subscriptionTier?.let { tier ->
                DossierChip(
                    Lucide.Star,
                    MaterialTheme.colorScheme.primary,
                    value = s.format(s.profileDossierSubTier, tierLabel(tier))
                )
            }
            subAge?.takeIf { !it.hidden && it.cumulativeMonths > 0 }?.let { sa ->
                DossierChip(
                    Lucide.Star,
                    MaterialTheme.colorScheme.primary,
                    value = s.profileSubAgeMonths.replace("{0}", sa.cumulativeMonths.toString())
                )
            }
            dossier.teamName?.let { team ->
                DossierChip(Lucide.Users, MaterialTheme.colorScheme.secondary, value = team)
            }
            dossier.language?.let { lang ->
                DossierChip(Lucide.Globe, MaterialTheme.colorScheme.onSurfaceVariant, value = lang.uppercase())
            }
            if (dossier.isStreamer) {
                dossier.lastBroadcastEpochMs?.let { last ->
                    DossierChip(
                        Lucide.Video,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        value = s.format(
                            s.profileDossierLastStream,
                            relativeAge(last, s.insights.ageUnits),
                            dossier.lastBroadcastGame ?: "—"
                        )
                    )
                }
                DossierChip(
                    Lucide.Play,
                    MaterialTheme.colorScheme.primary,
                    value = s.profileDossierOpenChannel,
                    onClick = { onOpenChannel(dossier.login) }
                )
            }
        }

        dossier.bio?.let { bio ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    s.profileDossierBio,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }
        }

        presence?.let { PresenceMapSection(it) }
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
                relativeAge(since, s.insights.ageUnits),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun DossierChip(
    icon: ImageVector,
    tint: Color,
    caption: String? = null,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(tint.copy(alpha = 0.11f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(11.dp), tint = tint)
        if (caption != null) {
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                maxLines = 1
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

internal fun compactCount(value: Int): String = when {
    value >= 1_000_000 -> "${value / 100_000 / 10.0}M".replace(".0M", "M")
    value >= 1_000 -> "${value / 100 / 10.0}K".replace(".0K", "K")
    else -> value.toString()
}

internal fun relativeAge(epochMs: Long, units: List<String>): String {
    val nowMs = Clock.System.now().toEpochMilliseconds()
    val diff = (nowMs - epochMs).coerceAtLeast(0L)
    val minutes = diff / 60_000
    val hours = minutes / 60
    val days = hours / 24
    val months = days / 30
    val years = days / 365
    return when {
        years >= 1 -> "$years${units[0]}"
        months >= 1 -> "$months${units[1]}"
        days >= 1 -> "$days${units[2]}"
        hours >= 1 -> "$hours${units[3]}"
        else -> "$minutes${units[4]}"
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
