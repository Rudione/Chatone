package io.rudione.chatone.tv.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.browse_categories_header
import chatone.composeapp.generated.resources.tv_load_failed
import chatone.composeapp.generated.resources.tv_nav_search
import chatone.composeapp.generated.resources.tv_search_channels
import chatone.composeapp.generated.resources.tv_search_empty
import chatone.composeapp.generated.resources.tv_search_hint
import chatone.composeapp.generated.resources.tv_search_streams
import io.rudione.chatone.domain.browse.BrowseChannel
import io.rudione.chatone.domain.browse.BrowseSearchResult
import io.rudione.chatone.presentation.browse.BrowseEvent
import io.rudione.chatone.presentation.browse.BrowseViewModel
import io.rudione.chatone.tv.components.TvCategoryCard
import io.rudione.chatone.tv.components.TvLoading
import io.rudione.chatone.tv.components.TvMediaCard
import io.rudione.chatone.tv.components.TvMessage
import io.rudione.chatone.tv.components.TvStreamCard
import io.rudione.chatone.tv.theme.TvDimens
import org.jetbrains.compose.resources.stringResource

@Composable
fun TvSearchScreen(
    viewModel: BrowseViewModel,
    actions: TvBrowseActions,
    onOpenChannel: (BrowseChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    Column(modifier.fillMaxSize().padding(top = TvDimens.ScreenVertical)) {
        TvScreenTitle(stringResource(Res.string.tv_nav_search))
        TvSearchField(
            initialQuery = state.query,
            onQueryChange = { viewModel.sendEvent(BrowseEvent.QueryChanged(it)) },
            modifier = Modifier.padding(horizontal = TvDimens.ScreenHorizontal, vertical = 16.dp)
        )
        val result = state.searchResult
        when {
            state.searchLoading -> TvLoading()
            state.searchFailed -> TvMessage(
                stringResource(Res.string.tv_load_failed),
                onRetry = { viewModel.sendEvent(BrowseEvent.RetrySearch) },
                claimsFocus = false
            )

            result != null && result.isEmpty -> TvMessage(stringResource(Res.string.tv_search_empty))
            result != null -> TvSearchResults(result, actions, onOpenChannel)
        }
    }
}

@Composable
private fun TvSearchField(initialQuery: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    val surfaceFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val scheme = MaterialTheme.colorScheme
    val query = rememberTextFieldState(initialQuery)
    val latestOnQueryChange by rememberUpdatedState(onQueryChange)
    LaunchedEffect(Unit) { runCatching { surfaceFocus.requestFocus() } }
    LaunchedEffect(query) {
        snapshotFlow { query.text.toString() }
            .drop(1)
            .distinctUntilChanged()
            .collect { latestOnQueryChange(it) }
    }
    Surface(
        onClick = { focusRequester.requestFocus() },
        modifier = modifier.fillMaxWidth().focusRequester(surfaceFocus),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.01f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, scheme.onSurface), shape = CircleShape)
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = scheme.surfaceVariant,
            focusedContainerColor = scheme.surfaceVariant
        )
    ) {
        BasicTextField(
            state = query,
            lineLimits = TextFieldLineLimits.SingleLine,
            textStyle = MaterialTheme.typography.titleMedium.copy(color = scheme.onSurface),
            cursorBrush = SolidColor(scheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            onKeyboardAction = { focusManager.moveFocus(FocusDirection.Down) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    when {
                        event.type != KeyEventType.KeyDown -> false
                        event.key == Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Down)
                        event.key == Key.DirectionUp || event.key == Key.Escape -> {
                            surfaceFocus.requestFocus()
                            true
                        }

                        else -> false
                    }
                },
            decorator = { field ->
                if (query.text.isEmpty()) {
                    Text(
                        stringResource(Res.string.tv_search_hint),
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
                field()
            }
        )
    }
}

@Composable
private fun TvSearchResults(
    result: BrowseSearchResult,
    actions: TvBrowseActions,
    onOpenChannel: (BrowseChannel) -> Unit
) {
    val rowPadding = PaddingValues(horizontal = TvDimens.ScreenHorizontal, vertical = 8.dp)
    LazyColumn(
        contentPadding = PaddingValues(bottom = TvDimens.ScreenVertical),
        verticalArrangement = Arrangement.spacedBy(TvDimens.RowGap)
    ) {
        if (result.streams.isNotEmpty()) {
            item(key = "streams") {
                TvRowTitle(stringResource(Res.string.tv_search_streams))
                LazyRow(
                    modifier = Modifier.focusRestorer(),
                    contentPadding = rowPadding,
                    horizontalArrangement = Arrangement.spacedBy(TvDimens.CardGap)
                ) {
                    items(result.streams, key = { it.id }) { stream ->
                        TvStreamCard(stream = stream, onClick = { actions.onOpenStream(stream) })
                    }
                }
            }
        }
        if (result.channels.isNotEmpty()) {
            item(key = "channels") {
                TvRowTitle(stringResource(Res.string.tv_search_channels))
                LazyRow(
                    modifier = Modifier.focusRestorer(),
                    contentPadding = rowPadding,
                    horizontalArrangement = Arrangement.spacedBy(TvDimens.CardGap)
                ) {
                    items(result.channels, key = { it.login }) { channel ->
                        TvMediaCard(
                            title = channel.displayName,
                            subtitle = channel.liveCategory.orEmpty(),
                            thumbnailUrl = channel.avatarUrl,
                            onClick = { onOpenChannel(channel) },
                            width = 160.dp
                        )
                    }
                }
            }
        }
        if (result.categories.isNotEmpty()) {
            item(key = "categories") {
                TvRowTitle(stringResource(Res.string.browse_categories_header))
                LazyRow(
                    modifier = Modifier.focusRestorer(),
                    contentPadding = rowPadding,
                    horizontalArrangement = Arrangement.spacedBy(TvDimens.CardGap)
                ) {
                    items(result.categories, key = { it.id }) { category ->
                        TvCategoryCard(category, onClick = { actions.onOpenCategory(category) })
                    }
                }
            }
        }
    }
}
