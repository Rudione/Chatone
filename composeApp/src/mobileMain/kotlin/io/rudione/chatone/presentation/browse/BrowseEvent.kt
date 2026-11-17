package io.rudione.chatone.presentation.browse

import io.rudione.chatone.base.UiEvent
import io.rudione.chatone.domain.browse.BrowseCategory
import io.rudione.chatone.domain.browse.StreamFilter

sealed interface BrowseEvent : UiEvent {
    data class QueryChanged(val query: String) : BrowseEvent
    data object RetrySearch : BrowseEvent
    data class SelectTab(val tab: BrowseTab) : BrowseEvent
    data class OpenCategory(val category: BrowseCategory) : BrowseEvent
    data object CloseCategory : BrowseEvent
    data class ApplyFilter(val filter: StreamFilter) : BrowseEvent
    data object ResetFilters : BrowseEvent
}
