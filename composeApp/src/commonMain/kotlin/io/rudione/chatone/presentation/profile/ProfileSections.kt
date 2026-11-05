package io.rudione.chatone.presentation.profile

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.channel_page_about_empty
import chatone.composeapp.generated.resources.channel_page_account_title
import chatone.composeapp.generated.resources.channel_page_by_category
import chatone.composeapp.generated.resources.channel_page_followers
import chatone.composeapp.generated.resources.channel_page_joined
import chatone.composeapp.generated.resources.channel_page_language
import chatone.composeapp.generated.resources.channel_page_links
import chatone.composeapp.generated.resources.channel_page_live
import chatone.composeapp.generated.resources.channel_page_live_now
import chatone.composeapp.generated.resources.channel_page_popular_clips
import chatone.composeapp.generated.resources.channel_page_recent_streams
import chatone.composeapp.generated.resources.channel_page_show_all
import chatone.composeapp.generated.resources.channel_page_team
import chatone.composeapp.generated.resources.channel_page_video_count
import chatone.composeapp.generated.resources.insights_aka_seen
import chatone.composeapp.generated.resources.insights_aka_title
import chatone.composeapp.generated.resources.profile_dossier_account_age
import chatone.composeapp.generated.resources.profile_dossier_affiliate
import chatone.composeapp.generated.resources.profile_dossier_partner
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import io.rudione.chatone.presentation.profile.channelLiveLine
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.model.SevenTvUserCosmetic
import io.rudione.chatone.domain.profile.ChannelClip
import io.rudione.chatone.presentation.chat.SevenTvDossierSection
import io.rudione.chatone.presentation.chat.compactCount
import io.rudione.chatone.presentation.chat.formatDate
import io.rudione.chatone.presentation.chat.relativeAge
import io.rudione.chatone.presentation.chat.roles.UserRolesState
import io.rudione.chatone.presentation.chat.roles.UserRolesTab
import io.rudione.chatone.presentation.theme.ChatoneTheme

@Immutable
internal class ProfileContentActions(
    val onOpenUrl: (String) -> Unit,
    val onOpenClip: (String) -> Unit,
    val onOpenVideo: (String) -> Unit,
    val onWatch: () -> Unit,
    val onSelectTab: (ProfileTab) -> Unit,
    val onOpenChannel: (String) -> Unit
)

internal val GridGutter = 12.dp

internal fun LazyGridScope.span(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }, contentType = key) {
        Box(Modifier.followsProfileTabSwipe(LocalProfileTabSwipe.current)) { content() }
    }
}

internal fun LazyGridScope.headerSpan(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }, contentType = key) { content() }
}

internal fun Modifier.bleed(horizontal: Dp): Modifier = layout { measurable, constraints ->
    val extra = horizontal.roundToPx() * 2
    val placeable = measurable.measure(
        constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra)
    )
    layout(placeable.width - extra, placeable.height) { placeable.place(-extra / 2, 0) }
}

internal fun LazyGridScope.homeSection(state: ChannelProfileState, actions: ProfileContentActions) {
    val profile = state.profile ?: return
    profile.live?.let { live ->
        span("home_live") {
            Column(Modifier.padding(horizontal = 4.dp)) {
                SectionTitle(stringResource(Res.string.channel_page_live_now))
                Box(
                    Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable(onClick = actions.onWatch)
                ) {
                    if (live.previewUrl.isNotEmpty()) {
                        AsyncImage(model = live.previewUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.7f))))
                    Text(
                        stringResource(Res.string.channel_page_live),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(6.dp))
                            .background(ChatoneTheme.extraColors.live).padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                        Text(live.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            channelLiveLine(live.category, compactCount(live.viewers)),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
    if (state.recentVideos.isNotEmpty()) {
        span("home_recent") {
            MediaRow(stringResource(Res.string.channel_page_recent_streams), null, null, state.recentVideos, { it.id }) { video ->
                VideoCard(video, onClick = { actions.onOpenVideo(video.id) }, width = 240.dp)
            }
        }
    }
    if (state.topClips.isNotEmpty()) {
        span("home_clips") {
            MediaRow(stringResource(Res.string.channel_page_popular_clips), stringResource(Res.string.channel_page_show_all), { actions.onSelectTab(ProfileTab.CLIPS) }, state.topClips, { it.slug }) { clip: ChannelClip ->
                ClipCard(clip, onClick = { actions.onOpenClip(clip.slug) }, width = 220.dp)
            }
        }
    }
    val categories = state.calendar?.categories.orEmpty()
    if (categories.isNotEmpty()) {
        span("home_categories") {
            Column(Modifier.padding(horizontal = 4.dp)) {
                SectionTitle(stringResource(Res.string.channel_page_by_category))
                categories.take(9).chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                        row.forEach { CategoryTile(it, Modifier.weight(1f)) }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
    if (profile.live == null && state.recentVideos.isEmpty() && state.topClips.isEmpty()) {
        span("home_empty") { EmptyNote(stringResource(Res.string.channel_page_about_empty)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
internal fun LazyGridScope.aboutSection(state: ChannelProfileState, actions: ProfileContentActions) {
    val profile = state.profile ?: return
    span("about_info") {
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoCard {
                profile.joinedLabel()?.let { InfoLine(stringResource(Res.string.channel_page_joined, it)) }
                profile.teamName?.let { InfoLine("${stringResource(Res.string.channel_page_team)}: $it") }
                profile.language?.let { InfoLine("${stringResource(Res.string.channel_page_language)}: ${it.uppercase()}") }
            }
            if (profile.links.isNotEmpty()) {
                InfoCard {
                    Text(stringResource(Res.string.channel_page_links), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    profile.links.forEach { link ->
                        Text(
                            link.title.ifBlank { link.url },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { actions.onOpenUrl(link.url) }.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
    profile.panels.forEachIndexed { index, panel ->
        span("about_panel_$index") {
            Column(
                Modifier.padding(horizontal = 4.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .then(if (panel.linkUrl.isNotEmpty()) Modifier.clickable { actions.onOpenUrl(panel.linkUrl) } else Modifier)
            ) {
                if (panel.imageUrl.isNotEmpty()) {
                    AsyncImage(model = panel.imageUrl, contentDescription = panel.title, contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth())
                }
                if (panel.title.isNotEmpty() || panel.description.isNotEmpty()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (panel.title.isNotEmpty()) Text(panel.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (panel.description.isNotEmpty()) {
                            Text(cleanPanelText(panel.description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
                        }
                    }
                }
            }
        }
    }
    if (profile.panels.isEmpty() && profile.links.isEmpty() && profile.description.isBlank()) {
        span("about_empty") { EmptyNote(stringResource(Res.string.channel_page_about_empty)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
internal fun LazyGridScope.dossierSection(
    state: ChannelProfileState,
    sevenTv: SevenTvUserCosmetic?,
    actions: ProfileContentActions
) {
    val profile = state.profile ?: return
    span("dossier_account") {
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoCard {
                Text(stringResource(Res.string.channel_page_account_title), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    profile.createdAtMs?.let { FactChip("${stringResource(Res.string.profile_dossier_account_age)} ${relativeAge(it)} · ${formatDate(it)}") }
                    profile.followers?.let { FactChip(pluralStringResource(Res.plurals.channel_page_followers, it, compactCount(it))) }
                    if (profile.isPartner) FactChip(stringResource(Res.string.profile_dossier_partner))
                    if (profile.isAffiliate) FactChip(stringResource(Res.string.profile_dossier_affiliate))
                    if (profile.isStaff) FactChip("Twitch Staff")
                    profile.teamName?.let { FactChip(it) }
                    profile.language?.let { FactChip(it.uppercase()) }
                    if (profile.pastBroadcastCount > 0) FactChip(pluralStringResource(Res.plurals.channel_page_video_count, profile.pastBroadcastCount, profile.pastBroadcastCount))
                    FactChip("ID ${profile.id}")
                }
            }
            sevenTv?.let { SevenTvDossierSection(cosmetic = it) }
            if (state.accountBadges.isNotEmpty()) {
                InfoCard {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.accountBadges.forEach { badge ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                AsyncImage(model = badge.imageUrl, contentDescription = badge.title, modifier = Modifier.size(18.dp))
                                Text(badge.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            if (state.previousNames.isNotEmpty()) {
                InfoCard {
                    Text(stringResource(Res.string.insights_aka_title), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.previousNames.forEach { entry ->
                            val range = listOfNotNull(entry.firstSeenMs?.let(::formatDate), entry.lastSeenMs?.let(::formatDate))
                            FactChip(if (range.size == 2) "${entry.login} · ${stringResource(Res.string.insights_aka_seen, range[0], range[1])}" else entry.login)
                        }
                    }
                }
            }
            state.calendar?.let { calendar -> StreamCalendarCard(calendar, onOpenVideo = { actions.onOpenVideo(it.id) }) }
        }
    }
}

internal fun LazyGridScope.rolesSection(roles: UserRolesState, actions: ProfileContentActions) {
    span("roles") {
        UserRolesTab(
            state = roles,
            onOpenChannel = actions.onOpenChannel,
            modifier = Modifier.fillMaxWidth().height(560.dp)
        )
    }
}

@Composable
private fun <T> MediaRow(
    title: String,
    action: String?,
    onAction: (() -> Unit)?,
    items: List<T>,
    key: (T) -> String,
    card: @Composable (T) -> Unit
) {
    Column {
        Row(Modifier.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { SectionTitle(title) }
            if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) }
        }
        LazyRow(
            modifier = Modifier.bleed(GridGutter),
            contentPadding = PaddingValues(horizontal = GridGutter + 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = key) { card(it) }
        }
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 10.dp))
}

@Composable
internal fun EmptyNote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 4.dp)
    )
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) { content() }
}

@Composable
private fun InfoLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun FactChip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

private val MarkdownLink = Regex("\\[([^\\]]+)]\\(([^)]+)\\)")

internal fun cleanPanelText(raw: String): String =
    raw.replace(MarkdownLink) { it.groupValues[1] }
        .lines()
        .joinToString("\n") { it.trimStart('>', ' ', '#') }
        .replace("**", "")
        .trim()
