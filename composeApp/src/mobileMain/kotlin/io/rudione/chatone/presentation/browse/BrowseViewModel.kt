package io.rudione.chatone.presentation.browse

import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import io.rudione.chatone.base.BaseViewModel
import io.rudione.chatone.data.browse.CategoryStreamsPagingSource
import io.rudione.chatone.data.browse.FollowedStreamsPagingSource
import io.rudione.chatone.data.browse.TopCategoriesPagingSource
import io.rudione.chatone.data.browse.TopStreamsPagingSource
import io.rudione.chatone.domain.browse.BrowseCategory
import io.rudione.chatone.domain.browse.BrowseStream
import io.rudione.chatone.domain.browse.FollowedAccount
import io.rudione.chatone.domain.browse.FollowedAccountProvider
import io.rudione.chatone.domain.browse.StreamDirectory
import io.rudione.chatone.domain.browse.StreamFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModel(
    private val directory: StreamDirectory,
    private val accountProvider: FollowedAccountProvider
) : BaseViewModel<BrowseState, BrowseEvent, BrowseEffect>(BrowseState()) {

    private var searchJob: Job? = null

    private val followAccount = MutableStateFlow<FollowedAccount?>(null)

    val followedStreams: Flow<PagingData<BrowseStream>> = combine(
        followAccount,
        state.map { it.filter }.distinctUntilChanged()
    ) { account, filter -> account to filter }
        .distinctUntilChanged()
        .flatMapLatest { (account, filter) ->
            if (account == null) {
                flowOf(PagingData.empty())
            } else {
                Pager(
                    config = PagingConfig(
                        pageSize = STREAM_PAGE_SIZE,
                        initialLoadSize = STREAM_PAGE_SIZE,
                        prefetchDistance = STREAM_PAGE_SIZE / 2,
                        enablePlaceholders = false
                    ),
                    pagingSourceFactory = {
                        FollowedStreamsPagingSource(directory, account.userId, account.token, filter)
                    }
                ).flow.onStart { emit(PagingData.empty(LoadingStates)) }
            }
        }
        .cachedIn(viewModelScope)

    val categories: Flow<PagingData<BrowseCategory>> = Pager(
        config = PagingConfig(
            pageSize = CATEGORY_PAGE_SIZE,
            initialLoadSize = CATEGORY_PAGE_SIZE,
            prefetchDistance = CATEGORY_PAGE_SIZE / 2,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { TopCategoriesPagingSource(directory) }
    ).flow.cachedIn(viewModelScope)

    val popularStreams: Flow<PagingData<BrowseStream>> = state
        .map { it.filter }
        .distinctUntilChanged()
        .flatMapLatest { filter ->
            Pager(
                config = PagingConfig(
                    pageSize = STREAM_PAGE_SIZE,
                    initialLoadSize = STREAM_PAGE_SIZE,
                    prefetchDistance = STREAM_PAGE_SIZE / 2,
                    enablePlaceholders = false
                ),
                pagingSourceFactory = { TopStreamsPagingSource(directory, filter) }
            ).flow.onStart { emit(PagingData.empty(LoadingStates)) }
        }
        .cachedIn(viewModelScope)

    val streams: Flow<PagingData<BrowseStream>> = state
        .mapNotNull { current -> current.category?.let { StreamQuery(it.id, current.filter) } }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            Pager(
                config = PagingConfig(
                    pageSize = STREAM_PAGE_SIZE,
                    initialLoadSize = STREAM_PAGE_SIZE,
                    prefetchDistance = STREAM_PAGE_SIZE / 2,
                    enablePlaceholders = false
                ),
                pagingSourceFactory = { CategoryStreamsPagingSource(directory, query.categoryId, query.filter) }
            ).flow.onStart { emit(PagingData.empty(LoadingStates)) }
        }
        .cachedIn(viewModelScope)

    init {
        subscribeToEvents()
        observeAccount()
    }

    private fun observeAccount() {
        viewModelScope.launch {
            accountProvider.activeAccountId.collect { id ->
                val account = accountProvider.account(id)
                followAccount.value = account
                update { current ->
                    val signedIn = account != null
                    val tab = if (!signedIn && current.tab == BrowseTab.FOLLOWING) BrowseTab.POPULAR else current.tab
                    current.copy(signedIn = signedIn, tab = tab)
                }
            }
        }
    }

    override suspend fun onEvent(event: BrowseEvent) {
        when (event) {
            is BrowseEvent.QueryChanged -> onQueryChanged(event.query)
            BrowseEvent.RetrySearch -> state.value.query.takeIf { it.isNotBlank() }?.let { runSearch(it, debounce = false) }
            is BrowseEvent.SelectTab -> update { if (it.tab == event.tab) it else it.copy(tab = event.tab) }
            is BrowseEvent.OpenCategory -> update { current ->
                if (current.category?.id == event.category.id) current
                else current.copy(category = event.category, filter = current.filter.copy(tag = null))
            }
            BrowseEvent.CloseCategory -> update { it.copy(category = null) }
            is BrowseEvent.ApplyFilter -> update { it.copy(filter = event.filter) }
            BrowseEvent.ResetFilters -> update { it.copy(filter = StreamFilter()) }
        }
    }

    private fun onQueryChanged(query: String) {
        val clipped = query.take(MAX_QUERY_LENGTH)
        update { it.copy(query = clipped, searchFailed = false) }
        searchJob?.cancel()
        if (clipped.isBlank()) {
            update { it.copy(searchResult = null, searchLoading = false) }
            return
        }
        runSearch(clipped, debounce = true)
    }

    private fun runSearch(query: String, debounce: Boolean) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounce) delay(SEARCH_DEBOUNCE_MS)
            update { it.copy(searchLoading = true, searchFailed = false) }
            val result = directory.search(query.trim())
            update { current ->
                when {
                    current.query != query -> current
                    result == null -> current.copy(searchLoading = false, searchFailed = true)
                    else -> current.copy(searchLoading = false, searchResult = result)
                }
            }
        }
    }

    private data class StreamQuery(val categoryId: String, val filter: StreamFilter)

    companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        private const val MAX_QUERY_LENGTH = 100
        private const val CATEGORY_PAGE_SIZE = 30
        private const val STREAM_PAGE_SIZE = 24
        private const val MAX_TAG_SUGGESTIONS = 14
        private val LoadingStates = LoadStates(
            refresh = LoadState.Loading,
            prepend = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.NotLoading(endOfPaginationReached = false)
        )

        fun tagSuggestionsFor(streams: List<BrowseStream>, activeTag: String?): List<String> {
            val ranked = streams.asSequence()
                .flatMap { it.tags.asSequence() }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .map { it.key }
            val suggestions = ranked.filter { !it.equals(activeTag, ignoreCase = true) }.take(MAX_TAG_SUGGESTIONS)
            return if (activeTag.isNullOrBlank()) suggestions else listOf(activeTag) + suggestions
        }
    }
}
