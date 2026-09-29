package io.rudione.chatone.presentation.browse

import androidx.compose.runtime.Immutable
import io.rudione.chatone.base.UIEffect
import io.rudione.chatone.base.UiEvent
import io.rudione.chatone.base.UiState
import io.rudione.chatone.domain.browse.BrowseCategory
import io.rudione.chatone.domain.browse.BrowseSearchResult
import io.rudione.chatone.domain.browse.StreamFilter

enum class BrowseTab { FOLLOWING, POPULAR, CATEGORIES }

@Immutable
data class BrowseState(
    val query: String = "",
    val searchLoading: Boolean = false,
    val searchFailed: Boolean = false,
    val searchResult: BrowseSearchResult? = null,
    val tab: BrowseTab = BrowseTab.FOLLOWING,
    val signedIn: Boolean = false,
    val category: BrowseCategory? = null,
    val filter: StreamFilter = StreamFilter()
) : UiState {
    val isSearching: Boolean get() = query.isNotBlank()
}

sealed interface BrowseEvent : UiEvent {
    data class QueryChanged(val query: String) : BrowseEvent
    data object RetrySearch : BrowseEvent
    data class SelectTab(val tab: BrowseTab) : BrowseEvent
    data class OpenCategory(val category: BrowseCategory) : BrowseEvent
    data object CloseCategory : BrowseEvent
    data class ApplyFilter(val filter: StreamFilter) : BrowseEvent
    data object ResetFilters : BrowseEvent
}

sealed interface BrowseEffect : UIEffect
