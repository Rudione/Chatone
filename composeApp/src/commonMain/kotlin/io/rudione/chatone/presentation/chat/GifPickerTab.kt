package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.data.remote.gif.GiphyApiClient
import io.rudione.chatone.data.repository.GifRepository
import io.rudione.chatone.presentation.chat.components.GifFavoriteStar
import io.rudione.chatone.domain.model.GifAccess
import io.rudione.chatone.domain.model.GifSearchConfig
import io.rudione.chatone.domain.model.GifSearchItem
import io.rudione.chatone.presentation.chat.rendering.LocalScrollActivity
import io.rudione.chatone.presentation.chat.rendering.rememberScrollActivity
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.Result
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.RefreshCw

private const val SEARCH_DEBOUNCE_MS = 350L
private const val LOAD_MORE_THRESHOLD = 8
private const val GIF_PREVIEW_MAX_DIMENSION = 192

@Stable
private class GifResults {
    var items by mutableStateOf<List<GifSearchItem>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set

    private var nextOffset: Int? = 0
    private val known = HashSet<String>()

    val canLoadMore: Boolean get() = nextOffset != null && !loading && !failed

    fun reset() {
        items = emptyList()
        known.clear()
        nextOffset = 0
        loading = false
        failed = false
    }

    suspend fun loadNext(repository: GifRepository, config: GifSearchConfig, query: String) {
        val offset = nextOffset ?: return
        if (loading) return
        loading = true
        try {
            when (val page = repository.load(config, query, offset)) {
                is Result.Success -> {
                    val fresh = page.data.items.filter { known.add(it.id) }
                    if (fresh.isNotEmpty()) items = items + fresh
                    nextOffset = page.data.nextOffset
                }

                else -> {
                    nextOffset = null
                    failed = true
                }
            }
        } finally {
            loading = false
        }
    }
}

@Composable
internal fun GifPickerTab(
    access: GifAccess,
    onGifSelected: (GifSearchItem) -> Unit,
    onRefreshAccess: () -> Unit,
    canSendGifs: Boolean,
    sendError: String?,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val repository: GifRepository = koinInject()
    val uriHandler = LocalUriHandler.current

    val favorites by repository.favorites.collectAsState()
    val favoriteIds = remember(favorites) { favorites.mapTo(HashSet(favorites.size)) { it.id } }
    var query by remember { mutableStateOf("") }
    var showFavorites by remember { mutableStateOf(false) }

    val debouncedQuery by produceState(initialValue = query, query) {
        if (query.isEmpty()) {
            value = ""
        } else {
            delay(SEARCH_DEBOUNCE_MS)
            value = query
        }
    }

    val results = remember { GifResults() }
    val gridState = rememberLazyGridState()
    val scrollActivity = rememberScrollActivity(gridState)
    val config = (access as? GifAccess.Ready)?.config

    LaunchedEffect(config, debouncedQuery) {
        results.reset()
        if (gridState.firstVisibleItemIndex > 0) gridState.scrollToItem(0)
        val active = config ?: return@LaunchedEffect
        results.loadNext(repository, active, debouncedQuery)
        snapshotFlow {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val loaded = results.items.size
            !showFavorites && results.canLoadMore && loaded > 0 && lastVisible >= loaded - LOAD_MORE_THRESHOLD
        }.collect { needsMore ->
            if (needsMore) results.loadNext(repository, active, debouncedQuery)
        }
    }

    val shown = if (showFavorites) favorites else results.items

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactEmoteSearchBar(
                value = query,
                onValueChange = { query = it; showFavorites = false },
                modifier = Modifier.weight(1f),
                placeholder = s.gifSearchPlaceholder
            )
            Spacer(Modifier.width(8.dp))
            GifFilterChip(
                label = if (showFavorites) s.gifFavorites else s.gifTrending,
                selected = showFavorites,
                onClick = { showFavorites = !showFavorites }
            )
        }

        if (!canSendGifs) GifNotice(text = s.gifTierHint, isError = false)
        sendError?.let { GifNotice(text = it, isError = true) }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        when {
            !showFavorites && access == GifAccess.Loading -> GifCenteredBox { GifSpinner() }

            !showFavorites && access == GifAccess.NeedsKey -> GifApiKeyPrompt(
                onOpenSignup = { runCatching { uriHandler.openUri(GiphyApiClient.SIGNUP_URL) } },
                onRecheck = onRefreshAccess
            )

            !showFavorites && access == GifAccess.Disabled -> GifCenteredBox { GifMessage(s.gifChannelDisabled) }

            shown.isEmpty() && results.loading -> GifCenteredBox { GifSpinner() }

            shown.isEmpty() -> GifCenteredBox {
                GifMessage(if (results.failed && !showFavorites) s.gifLoadFailed else s.gifNoResults)
            }

            else -> CompositionLocalProvider(LocalScrollActivity provides scrollActivity) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(96.dp),
                    state = gridState,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 280.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = shown,
                        key = { it.listKey },
                        contentType = { "gif" }
                    ) { gif ->
                        GifGridItem(
                            gif = gif,
                            isFavorite = gif.id in favoriteIds,
                            onClick = { onGifSelected(gif) },
                            onToggleFavorite = { repository.toggleFavorite(gif) }
                        )
                    }
                    if (results.loading && !showFavorites) {
                        item(key = "loader", span = { GridItemSpan(maxLineSpan) }, contentType = "loader") {
                            LoadMoreIndicator(isLoading = true)
                        }
                    }
                    item(key = "attribution", span = { GridItemSpan(maxLineSpan) }, contentType = "attribution") {
                        Text(
                            s.gifPoweredBy,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GifSpinner() {
    CircularProgressIndicator(
        modifier = Modifier.size(22.dp),
        strokeWidth = 2.dp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    )
}

@Composable
private fun GifMessage(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun GifCenteredBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun GifGridItem(
    gif: GifSearchItem,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(96.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
    ) {
        AnimatedEmoteImage(
            url = gif.previewUrl,
            contentDescription = gif.title,
            modifier = Modifier.fillMaxSize(),
            maxDimension = GIF_PREVIEW_MAX_DIMENSION
        )
        GifFavoriteStar(
            isFavorite = isFavorite,
            onToggle = onToggleFavorite,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(3.dp)
                .size(18.dp)
        )
    }
}

@Composable
private fun GifFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val background =
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f)
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(17.dp))
            .background(background)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                RoundedCornerShape(17.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}

@Composable
private fun GifNotice(text: String, isError: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = if (isError) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
    )
}

@Composable
private fun GifApiKeyPrompt(onOpenSignup: () -> Unit, onRecheck: () -> Unit) {
    val s = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            s.gifKeyMissingTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Text(
            s.gifKeyMissingBody,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onOpenSignup) {
                Text(s.gifKeyMissingAction, fontSize = 12.sp)
            }
            TextButton(onClick = onRecheck) {
                Icon(Lucide.RefreshCw, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}
