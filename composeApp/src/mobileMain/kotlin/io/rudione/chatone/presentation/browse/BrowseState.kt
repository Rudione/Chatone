package io.rudione.chatone.presentation.browse

import androidx.compose.runtime.Immutable
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
    val followsNeedSignIn: Boolean = false,
    val category: BrowseCategory? = null,
    val filter: StreamFilter = StreamFilter()
) : UiState {
    val isSearching: Boolean get() = query.isNotBlank()
}
