package io.rudione.chatone.presentation.chat

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.data.remote.gif.GiphyApiClient
import io.rudione.chatone.data.repository.GifRepository
import io.rudione.chatone.domain.model.GifSearchItem
import io.rudione.chatone.presentation.chat.rendering.LocalScrollActivity
import io.rudione.chatone.presentation.chat.rendering.rememberScrollActivity
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.Result
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

private const val SEARCH_DEBOUNCE_MS = 350L
private const val LOAD_MORE_THRESHOLD = 8
private const val GIF_PREVIEW_MAX_DIMENSION = 192

@Composable
internal fun GifPickerTab(
    onGifSelected: (GifSearchItem) -> Unit,
    canSendGifs: Boolean,
    sendError: String?,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val repository: GifRepository = koinInject()
    val uriHandler = LocalUriHandler.current

    val favorites by repository.favorites.collectAsState()
    var query by remember { mutableStateOf("") }
    var showFavorites by remember { mutableStateOf(false) }
    var apiKeyPresent by remember { mutableStateOf(repository.hasApiKey()) }

    val debouncedQuery by produceState(initialValue = query, query) {
        if (query.isEmpty()) {
            value = ""
        } else {
            delay(SEARCH_DEBOUNCE_MS)
            value = query
        }
    }

    val results = remember { mutableStateListOf<GifSearchItem>() }
    var nextOffset by remember { mutableStateOf<Int?>(0) }
    var isLoading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var loadMoreTick by remember { mutableIntStateOf(0) }

    val gridState = rememberLazyGridState()
    val scrollActivity = rememberScrollActivity(gridState)

    suspend fun fetchNextPage() {
        val offset = nextOffset ?: return
        if (isLoading) return
        isLoading = true
        when (val page = repository.load(debouncedQuery, offset)) {
            is Result.Success -> {
                val known = results.mapTo(HashSet()) { it.id }
                results.addAll(page.data.items.filter { known.add(it.id) })
                nextOffset = page.data.nextOffset
                loadError = null
            }

            else -> {
                nextOffset = null
                loadError = s.gifLoadFailed
            }
        }
        isLoading = false
    }

    LaunchedEffect(debouncedQuery, apiKeyPresent) {
        results.clear()
        nextOffset = 0
        loadError = null
        loadMoreTick = 0
        isLoading = false
        if (gridState.firstVisibleItemIndex > 0) gridState.scrollToItem(0)
        if (apiKeyPresent) fetchNextPage()
    }

    LaunchedEffect(loadMoreTick) {
        if (loadMoreTick > 0 && apiKeyPresent) fetchNextPage()
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            results.isNotEmpty() && lastVisible >= results.size - LOAD_MORE_THRESHOLD
        }
    }

    LaunchedEffect(shouldLoadMore, isLoading, nextOffset) {
        if (showFavorites || !shouldLoadMore || isLoading || nextOffset == null) return@LaunchedEffect
        loadMoreTick++
    }

    val shown = if (showFavorites) favorites else results

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
            !apiKeyPresent && !showFavorites -> GifApiKeyPrompt(
                onOpenSignup = { runCatching { uriHandler.openUri(GiphyApiClient.SIGNUP_URL) } },
                onRecheck = { apiKeyPresent = repository.hasApiKey() }
            )

            shown.isEmpty() && isLoading -> GifCenteredBox {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }

            shown.isEmpty() -> GifCenteredBox {
                Text(
                    loadError ?: s.gifNoResults,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
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
                            isFavorite = favorites.any { it.id == gif.id },
                            onClick = { onGifSelected(gif) },
                            onToggleFavorite = { repository.toggleFavorite(gif) }
                        )
                    }
                    if (isLoading) {
                        item(span = { GridItemSpan(maxLineSpan) }, contentType = "loader") {
                            LoadMoreIndicator(isLoading = true)
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }, contentType = "attribution") {
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
        Icon(
            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
            contentDescription = LocalStrings.current.gifFavorites,
            tint = if (isFavorite) Color(0xFFFFD700) else Color.White.copy(alpha = 0.75f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(3.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(onClick = onToggleFavorite)
                .padding(2.dp)
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
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}
