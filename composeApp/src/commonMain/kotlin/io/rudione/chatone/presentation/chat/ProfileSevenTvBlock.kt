package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.remote.emote.SevenTvCosmeticsClient
import io.rudione.chatone.domain.model.SevenTvCosmetics
import io.rudione.chatone.domain.model.SevenTvProfile
import io.rudione.chatone.domain.model.SevenTvUserCosmetic
import org.koin.compose.koinInject
import io.rudione.chatone.icons.lucide.ExternalLink
import io.rudione.chatone.icons.lucide.Link
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Star

private val SEVEN_TV_COLOR = Color(0xFF29D8F6)

@Composable
fun resolveSevenTvPaint(
    message: io.rudione.chatone.domain.model.DisplayMessage.PrivMsg
): SevenTvCosmetics.Paint? {
    val paints = LocalSevenTvPaints.current
    val userId = message.userId
    val published by remember(paints, userId) { derivedStateOf { paints.value[userId] } }
    return message.sevenTvPaint ?: published
}

@Composable
internal fun rememberSevenTvCosmetic(userId: String): SevenTvUserCosmetic? {
    val client: SevenTvCosmeticsClient = koinInject()
    val cosmetics = LocalSevenTvCosmetics.current
    LaunchedEffect(userId) {
        if (userId.isNotBlank()) client.requestCosmetics(userId)
    }
    val cosmetic by remember(cosmetics, userId) { derivedStateOf { cosmetics.value[userId] } }
    return cosmetic
}

@Composable
internal fun ProfileDisplayName(
    displayName: String,
    color: String?,
    paint: SevenTvCosmetics.Paint?,
    style: TextStyle,
    modifier: Modifier = Modifier,
    fallbackLogin: String? = null
) {
    val density = LocalDensity.current
    val nickColors = rememberNickColors()
    val fallback = parseHexColor(color)?.let { nickColors.adjust(it) }
        ?: fallbackLogin?.takeIf { it.isNotBlank() }?.let { nickColors.adjust(stableUserColor(it)) }
        ?: MaterialTheme.colorScheme.primary
    val brush = paint?.let { rememberSevenTvPaintBrush(it, fallback) }
    val shadow = paint?.let { sevenTvPaintShadow(it, density.density) }

    if (brush != null) {
        Text(
            displayName,
            style = style.copy(
                brush = brush,
                fontWeight = FontWeight.Bold,
                shadow = shadow
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    } else {
        Text(
            displayName,
            style = style,
            color = fallback,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    }
}

@Composable
internal fun SevenTvLinkButton(profile: SevenTvProfile) {
    val uriHandler = LocalUriHandler.current
    Box(
        modifier = Modifier
            .size(width = 30.dp, height = 18.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(SEVEN_TV_COLOR.copy(alpha = 0.14f))
            .clickable { uriHandler.openUri(profile.profileUrl) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            "7TV",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = SEVEN_TV_COLOR
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SevenTvDossierSection(cosmetic: SevenTvUserCosmetic) {
    val profile = cosmetic.profile ?: return
    val uriHandler = LocalUriHandler.current
    val roles = profile.roles.filterNot { it.equals("Default", ignoreCase = true) }
    val links = profile.connections.filterNot { it.platform.equals("TWITCH", true) }

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SEVEN_TV_COLOR.copy(alpha = 0.07f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .clickable { uriHandler.openUri(profile.profileUrl) }
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                "7TV",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = SEVEN_TV_COLOR
            )
            Icon(
                Lucide.ExternalLink,
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = SEVEN_TV_COLOR.copy(alpha = 0.85f)
            )
        }

        cosmetic.paint?.let { paint ->
            ProfileDisplayName(
                displayName = paint.name.ifBlank { profile.displayName.ifBlank { profile.username } },
                color = null,
                paint = paint,
                style = MaterialTheme.typography.labelSmall
            )
        }

        cosmetic.badge?.let { badge ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                coil3.compose.AsyncImage(
                    model = badge.url2x.ifEmpty { badge.url1x },
                    contentDescription = badge.tooltip,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    badge.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        roles.forEach { role ->
            SevenTvTag(
                text = role,
                icon = if (role.equals("Subscriber", true)) Lucide.Star else null
            )
        }

        links.forEach { link ->
            ProfileIdentityLine(
                icon = Lucide.Link,
                text = link.platform.lowercase().replaceFirstChar { it.uppercase() } +
                        ": " + link.displayName.ifBlank { link.username }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SevenTvIdentityBlock(profile: SevenTvProfile, compact: Boolean) {
    val uriHandler = LocalUriHandler.current
    val roles = profile.roles.filterNot { it.equals("Default", ignoreCase = true) }

    Column(modifier = Modifier.fillMaxWidth()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (profile.isSubscriber) {
                SevenTvTag(text = "Subscriber", icon = Lucide.Star)
            }
            roles.filterNot { it.equals("Subscriber", ignoreCase = true) }.forEach { role ->
                SevenTvTag(text = role, icon = null)
            }
        }

        val links = profile.connections.filterNot { it.platform.equals("TWITCH", true) }
        if (links.isNotEmpty()) {
            Spacer(Modifier.width(2.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp)
            ) {
                links.forEach { link ->
                    ProfileIdentityLine(
                        icon = Lucide.Link,
                        text = "${link.platform.lowercase().replaceFirstChar { it.uppercase() }}: " +
                                link.displayName.ifBlank { link.username }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { uriHandler.openUri(profile.profileUrl) }
                .padding(vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                Lucide.ExternalLink,
                contentDescription = null,
                modifier = Modifier.size(11.dp),
                tint = SEVEN_TV_COLOR.copy(alpha = 0.8f)
            )
            Text(
                "7tv.app",
                style = MaterialTheme.typography.labelSmall,
                color = SEVEN_TV_COLOR.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun SevenTvTag(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector?) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(SEVEN_TV_COLOR.copy(alpha = 0.12f))
            .padding(horizontal = 5.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(9.dp),
                tint = SEVEN_TV_COLOR
            )
        }
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = SEVEN_TV_COLOR.copy(alpha = 0.9f),
            maxLines = 1
        )
    }
}
