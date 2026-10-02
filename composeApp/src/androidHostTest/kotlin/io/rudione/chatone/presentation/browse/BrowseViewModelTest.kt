package io.rudione.chatone.presentation.browse

import androidx.paging.testing.asSnapshot
import io.rudione.chatone.domain.browse.BrowseCategory
import io.rudione.chatone.domain.browse.BrowsePage
import io.rudione.chatone.domain.browse.BrowseSearchResult
import io.rudione.chatone.domain.browse.BrowseStream
import io.rudione.chatone.domain.browse.FollowedAccount
import io.rudione.chatone.domain.browse.FollowedAccountProvider
import io.rudione.chatone.domain.browse.StreamDirectory
import io.rudione.chatone.domain.browse.StreamFilter
import io.rudione.chatone.domain.browse.StreamSort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val directory = FakeDirectory()
    private val accountProvider = FakeFollowedAccountProvider()

    private fun browseViewModel() = BrowseViewModel(directory, accountProvider)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun category(id: String) = BrowseCategory(id, "Category $id", "", viewers = 100, broadcasters = 5)

    private fun stream(login: String, vararg tags: String) = BrowseStream(
        id = login, login = login, displayName = login, avatarUrl = "", title = "", viewers = 10,
        startedAtMs = null, previewUrl = "", language = "RU", tags = tags.toList()
    )

    @Test
    fun categoriesKeepLoadingWhileScrolling() = runTest(dispatcher) {
        directory.categoryPages += BrowsePage((1..30).map { category("$it") }, nextCursor = "c1")
        directory.categoryPages += BrowsePage((31..60).map { category("$it") }, nextCursor = null)
        val viewModel = browseViewModel()

        val loaded = viewModel.categories.asSnapshot { scrollTo(index = 45) }

        assertEquals((1..60).map { "$it" }, loaded.map { it.id })
        assertEquals(listOf(null, "c1"), directory.categoryCursors)
    }

    @Test
    fun searchIsDebouncedToTheLastQuery() = runTest(dispatcher) {
        val viewModel = browseViewModel()

        viewModel.sendEvent(BrowseEvent.QueryChanged("sh"))
        runCurrent()
        advanceTimeBy(BrowseViewModel.SEARCH_DEBOUNCE_MS / 2)
        viewModel.sendEvent(BrowseEvent.QueryChanged("shroud"))
        advanceUntilIdle()

        assertEquals(listOf("shroud"), directory.searchQueries)
        assertEquals("shroud", viewModel.state.value.query)
    }

    @Test
    fun failedSearchCanBeRetried() = runTest(dispatcher) {
        directory.searchResults += null
        val viewModel = browseViewModel()

        viewModel.sendEvent(BrowseEvent.QueryChanged("abc"))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.searchFailed)

        viewModel.sendEvent(BrowseEvent.RetrySearch)
        advanceUntilIdle()
        assertEquals(false, viewModel.state.value.searchFailed)
        assertEquals(listOf("abc", "abc"), directory.searchQueries)
    }

    @Test
    fun clearingTheQueryDropsResults() = runTest(dispatcher) {
        val viewModel = browseViewModel()

        viewModel.sendEvent(BrowseEvent.QueryChanged("abc"))
        advanceUntilIdle()
        viewModel.sendEvent(BrowseEvent.QueryChanged(""))
        advanceUntilIdle()

        assertNull(viewModel.state.value.searchResult)
    }

    @Test
    fun openingACategoryPagesItsStreams() = runTest(dispatcher) {
        directory.streamPages += BrowsePage(listOf(stream("a"), stream("b")), nextCursor = null)
        val viewModel = browseViewModel()

        viewModel.sendEvent(BrowseEvent.OpenCategory(category("509658")))
        advanceUntilIdle()

        assertEquals(listOf("a", "b"), viewModel.streams.asSnapshot().map { it.login })
        assertEquals("509658", directory.streamRequests.single().first)
    }

    @Test
    fun applyingAFilterReloadsWithIt() = runTest(dispatcher) {
        directory.streamPages += BrowsePage(listOf(stream("a")), nextCursor = null)
        directory.streamPages += BrowsePage(listOf(stream("z")), nextCursor = null)
        val viewModel = browseViewModel()
        val filter = StreamFilter(StreamSort.RECENT, language = "RU")

        viewModel.sendEvent(BrowseEvent.OpenCategory(category("1")))
        advanceUntilIdle()
        viewModel.streams.asSnapshot()
        viewModel.sendEvent(BrowseEvent.ApplyFilter(filter))
        advanceUntilIdle()

        assertEquals(listOf("z"), viewModel.streams.asSnapshot().map { it.login })
        assertEquals(filter, directory.streamRequests.last().second)
    }

    @Test
    fun anotherCategoryKeepsFiltersButDropsTheTag() = runTest(dispatcher) {
        val viewModel = browseViewModel()

        viewModel.sendEvent(BrowseEvent.OpenCategory(category("1")))
        viewModel.sendEvent(BrowseEvent.ApplyFilter(StreamFilter(language = "EN", tag = "Chill", hideMature = true)))
        viewModel.sendEvent(BrowseEvent.CloseCategory)
        viewModel.sendEvent(BrowseEvent.OpenCategory(category("2")))
        advanceUntilIdle()

        assertEquals(StreamFilter(language = "EN", hideMature = true), viewModel.state.value.filter)
    }

    @Test
    fun resetRestoresDefaultFilters() = runTest(dispatcher) {
        val viewModel = browseViewModel()

        viewModel.sendEvent(BrowseEvent.ApplyFilter(StreamFilter(minViewers = 100)))
        viewModel.sendEvent(BrowseEvent.ResetFilters)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.filter.isDefault)
    }

    @Test
    fun closingACategoryReturnsToTheDirectory() = runTest(dispatcher) {
        val viewModel = browseViewModel()

        viewModel.sendEvent(BrowseEvent.OpenCategory(category("1")))
        advanceUntilIdle()
        viewModel.sendEvent(BrowseEvent.CloseCategory)
        advanceUntilIdle()

        assertNull(viewModel.state.value.category)
    }

    @Test
    fun tagSuggestionsAreRankedByFrequency() {
        val streams = listOf(stream("a", "Русский", "IRL"), stream("b", "Русский"), stream("c", "Chill", "IRL"), stream("d", "Русский"))

        assertEquals(listOf("Русский", "IRL", "Chill"), BrowseViewModel.tagSuggestionsFor(streams, activeTag = null))
        assertEquals(listOf("Chill", "Русский", "IRL"), BrowseViewModel.tagSuggestionsFor(streams, activeTag = "Chill"))
    }

    private class FakeDirectory : StreamDirectory {
        val categoryPages = ArrayDeque<BrowsePage<BrowseCategory>?>()
        val streamPages = ArrayDeque<BrowsePage<BrowseStream>?>()
        val searchResults = ArrayDeque<BrowseSearchResult?>()
        val categoryCursors = mutableListOf<String?>()
        val searchQueries = mutableListOf<String>()
        val streamRequests = mutableListOf<Pair<String, StreamFilter>>()

        override suspend fun topCategories(cursor: String?): BrowsePage<BrowseCategory>? {
            categoryCursors += cursor
            return if (categoryPages.isEmpty()) BrowsePage(emptyList(), null) else categoryPages.removeFirst()
        }

        override suspend fun categoryStreams(categoryId: String, filter: StreamFilter, cursor: String?): BrowsePage<BrowseStream>? {
            streamRequests += categoryId to filter
            return if (streamPages.isEmpty()) BrowsePage(emptyList(), null) else streamPages.removeFirst()
        }

        val topStreamPages = ArrayDeque<BrowsePage<BrowseStream>?>()

        override suspend fun topStreams(filter: StreamFilter, cursor: String?): BrowsePage<BrowseStream>? =
            if (topStreamPages.isEmpty()) BrowsePage(emptyList(), null) else topStreamPages.removeFirst()

        override suspend fun followedStreams(userId: String, accessToken: String, cursor: String?): BrowsePage<BrowseStream>? =
            BrowsePage(emptyList(), null)

        override suspend fun search(query: String): BrowseSearchResult? {
            searchQueries += query
            return if (searchResults.isEmpty()) BrowseSearchResult(emptyList(), emptyList()) else searchResults.removeFirst()
        }
    }

    private class FakeFollowedAccountProvider : FollowedAccountProvider {
        override val activeAccountId: StateFlow<String> = MutableStateFlow("")
        override suspend fun account(activeId: String): FollowedAccount? = null
    }
}
