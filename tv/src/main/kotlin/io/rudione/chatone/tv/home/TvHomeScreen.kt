package io.rudione.chatone.tv.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.browse_tab_categories
import chatone.composeapp.generated.resources.browse_tab_following
import chatone.composeapp.generated.resources.browse_tab_popular
import chatone.composeapp.generated.resources.tv_follows_sign_in_action
import chatone.composeapp.generated.resources.tv_follows_sign_in_body
import chatone.composeapp.generated.resources.tv_follows_sign_in_title
import chatone.composeapp.generated.resources.tv_load_failed
import chatone.composeapp.generated.resources.tv_no_follows_live
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.browse.BrowseCategory
import io.rudione.chatone.domain.browse.BrowseStream
import io.rudione.chatone.presentation.browse.BrowseFormatting
import io.rudione.chatone.presentation.browse.BrowseViewModel
import io.rudione.chatone.tv.components.TvCategoryCard
import io.rudione.chatone.tv.components.TvLiveBadge
import io.rudione.chatone.tv.components.TvLoading
import io.rudione.chatone.tv.components.TvMessage
import io.rudione.chatone.tv.components.TvStreamCard
import io.rudione.chatone.tv.theme.TvDimens
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private val HeroHeight = 236.dp
private const val HERO_SETTLE_MS = 220L

@Immutable
class TvBrowseActions(
    val onOpenStream: (BrowseStream) -> Unit,
    val onOpenCategory: (BrowseCategory) -> Unit,
    val onSignIn: () -> Unit
)

@Composable
fun TvHomeScreen(
    viewModel: BrowseViewModel,
    actions: TvBrowseActions,
    contentFocus: FocusRequester,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val followed = viewModel.followedStreams.collectAsLazyPagingItems()
    val popular = viewModel.popularStreams.collectAsLazyPagingItems()
    val categories = viewModel.categories.collectAsLazyPagingItems()
    var focused by remember { mutableStateOf<BrowseStream?>(null) }
    var hero by remember { mutableStateOf<BrowseStream?>(null) }
    LaunchedEffect(focused) {
        delay(HERO_SETTLE_MS)
        hero = focused
    }
    val listState = rememberLazyListState()
    val hasContent = followed.itemCount > 0 || popular.itemCount > 0
    LaunchedEffect(hasContent) {
        if (hasContent) {
            withFrameNanos { }
            runCatching { contentFocus.requestFocus() }
        }
    }
    val fallback =
        if (followed.itemCount > 0) followed.peek(0) else if (popular.itemCount > 0) popular.peek(0) else null
    Column(modifier.fillMaxSize()) {
        TvHero(stream = hero ?: fallback, modifier = Modifier
            .fillMaxWidth()
            .height(HeroHeight))
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 12.dp, bottom = TvDimens.ScreenVertical),
            verticalArrangement = Arrangement.spacedBy(TvDimens.RowGap)
        ) {
            if (state.signedIn) {
                item(key = "followed") {
                    if (state.followsNeedSignIn) {
                        TvMessage(
                            title = stringResource(Res.string.tv_follows_sign_in_title),
                            body = stringResource(Res.string.tv_follows_sign_in_body),
                            onRetry = actions.onSignIn,
                            actionLabel = stringResource(Res.string.tv_follows_sign_in_action),
                            claimsFocus = false
                        )
                    } else {
                        TvStreamRow(
                            title = stringResource(Res.string.browse_tab_following),
                            streams = followed,
                            emptyText = stringResource(Res.string.tv_no_follows_live),
                            actions = actions,
                            onFocused = { focused = it }
                        )
                    }
                }
            }
            item(key = "popular") {
                TvStreamRow(
                    title = stringResource(Res.string.browse_tab_popular),
                    streams = popular,
                    emptyText = null,
                    actions = actions,
                    onFocused = { focused = it }
                )
            }
            item(key = "categories") { TvCategoryRow(categories, actions) }
        }
    }
}

@Composable
private fun TvHero(stream: BrowseStream?, modifier: Modifier = Modifier) {
    val background = MaterialTheme.colorScheme.background
    Box(modifier) {
        Crossfade(targetState = stream, animationSpec = tween(320), label = "tvHero") { current ->
            if (current != null) {
                AsyncImage(
                    model = current.previewUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to background,
                        0.55f to background.copy(alpha = 0.72f),
                        1f to Color.Transparent
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(0.55f to Color.Transparent, 1f to background)
                )
        )
        if (stream != null) {
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = TvDimens.ScreenHorizontal, vertical = 12.dp)
                    .widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvLiveBadge()
                    Text(
                        BrowseFormatting.viewers(stream.viewers),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    stream.displayName.ifBlank { stream.login },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    stream.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun TvRowTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(horizontal = TvDimens.ScreenHorizontal, vertical = 8.dp)
    )
}

@Composable
private fun TvStreamRow(
    title: String,
    streams: LazyPagingItems<BrowseStream>,
    emptyText: String?,
    actions: TvBrowseActions,
    onFocused: (BrowseStream) -> Unit
) {
    val refresh = streams.loadState.refresh
    Column {
        TvRowTitle(title)
        when {
            streams.itemCount > 0 -> LazyRow(
                modifier = Modifier.focusRestorer(),
                contentPadding = PaddingValues(
                    horizontal = TvDimens.ScreenHorizontal,
                    vertical = 8.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(TvDimens.CardGap)
            ) {
                items(
                    count = streams.itemCount,
                    key = streams.itemKey { it.id },
                    contentType = { "stream" }
                ) { index ->
                    streams[index]?.let { stream ->
                        TvStreamCard(
                            stream = stream,
                            onClick = { actions.onOpenStream(stream) },
                            onFocused = { onFocused(stream) }
                        )
                    }
                }
            }

            refresh is LoadState.Loading -> TvLoading()
            refresh is LoadState.Error -> TvMessage(
                stringResource(Res.string.tv_load_failed),
                onRetry = streams::retry,
                claimsFocus = false
            )

            emptyText != null -> Text(
                emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = TvDimens.ScreenHorizontal, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun TvCategoryRow(categories: LazyPagingItems<BrowseCategory>, actions: TvBrowseActions) {
    if (categories.itemCount == 0) return
    Column {
        TvRowTitle(stringResource(Res.string.browse_tab_categories))
        LazyRow(
            modifier = Modifier.focusRestorer(),
            contentPadding = PaddingValues(horizontal = TvDimens.ScreenHorizontal, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(TvDimens.CardGap)
        ) {
            items(
                count = categories.itemCount,
                key = categories.itemKey { it.id },
                contentType = { "category" }
            ) { index ->
                categories[index]?.let { category ->
                    TvCategoryCard(category, onClick = { actions.onOpenCategory(category) })
                }
            }
        }
    }
}
