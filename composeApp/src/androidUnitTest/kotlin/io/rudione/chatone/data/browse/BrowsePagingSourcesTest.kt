package io.rudione.chatone.data.browse

import androidx.paging.PagingConfig
import androidx.paging.PagingSource.LoadResult
import androidx.paging.testing.TestPager
import io.rudione.chatone.domain.browse.BrowseCategory
import io.rudione.chatone.domain.browse.BrowsePage
import io.rudione.chatone.domain.browse.BrowseSearchResult
import io.rudione.chatone.domain.browse.BrowseStream
import io.rudione.chatone.domain.browse.StreamDirectory
import io.rudione.chatone.domain.browse.StreamFilter
import io.rudione.chatone.domain.browse.StreamSort
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BrowsePagingSourcesTest {

    private val config = PagingConfig(pageSize = 24, enablePlaceholders = false)
    private val directory = ScriptedDirectory()

    private fun category(id: String) = BrowseCategory(id, "Category $id", "", viewers = 100, broadcasters = 5)

    private fun stream(login: String, viewers: Int = 10, mature: Boolean = false) = BrowseStream(
        id = login, login = login, displayName = login, avatarUrl = "", title = "", viewers = viewers,
        startedAtMs = null, previewUrl = "", language = "RU", tags = emptyList(), isMature = mature
    )

    @Test
    fun categoriesFollowTheCursorAndDropDuplicates() = runTest {
        directory.categoryPages += BrowsePage(listOf(category("1"), category("2")), nextCursor = "c1")
        directory.categoryPages += BrowsePage(listOf(category("2"), category("3")), nextCursor = null)
        val pager = TestPager(config, TopCategoriesPagingSource(directory))

        val first = assertIs<LoadResult.Page<String, BrowseCategory>>(pager.refresh())
        val second = assertIs<LoadResult.Page<String, BrowseCategory>>(pager.append())

        assertEquals("c1", first.nextKey)
        assertEquals(listOf("3"), second.data.map { it.id })
        assertNull(second.nextKey)
        assertEquals(listOf(null, "c1"), directory.categoryCursors)
    }

    @Test
    fun unavailableDirectoryBecomesARetryableError() = runTest {
        directory.categoryPages += null
        val pager = TestPager(config, TopCategoriesPagingSource(directory))

        val result = assertIs<LoadResult.Error<String, BrowseCategory>>(pager.refresh())

        assertIs<DirectoryUnavailableException>(result.throwable)
    }

    @Test
    fun streamsAreDedupedAcrossPages() = runTest {
        directory.streamPages += BrowsePage(listOf(stream("a"), stream("b")), nextCursor = "s1")
        directory.streamPages += BrowsePage(listOf(stream("b"), stream("c")), nextCursor = null)
        val pager = TestPager(config, CategoryStreamsPagingSource(directory, "509658", StreamFilter()))

        pager.refresh()
        pager.append()

        assertEquals(listOf("a", "b", "c"), pager.getPages().flatMap { page -> page.data.map { it.login } })
        assertEquals(listOf(null, "s1"), directory.streamCursors)
    }

    @Test
    fun localFiltersDropMatureAndSmallStreams() = runTest {
        directory.streamPages += BrowsePage(
            listOf(stream("big", viewers = 500), stream("mature", viewers = 900, mature = true), stream("tiny", viewers = 3)),
            nextCursor = null
        )
        val filter = StreamFilter(sort = StreamSort.RECENT, minViewers = 100, hideMature = true)
        val pager = TestPager(config, CategoryStreamsPagingSource(directory, "1", filter))

        val page = assertIs<LoadResult.Page<String, BrowseStream>>(pager.refresh())

        assertEquals(listOf("big"), page.data.map { it.login })
    }

    @Test
    fun viewerSortedStreamsToleratesStaleOrdering() = runTest {
        directory.streamPages += BrowsePage(listOf(stream("a", viewers = 300), stream("b", viewers = 40)), nextCursor = "s1")
        val pager = TestPager(config, CategoryStreamsPagingSource(directory, "1", StreamFilter(minViewers = 100)))

        val page = assertIs<LoadResult.Page<String, BrowseStream>>(pager.refresh())

        assertEquals(listOf("a"), page.data.map { it.login })
        assertEquals("s1", page.nextKey)
    }

    @Test
    fun viewerSortedStreamsStopOnceAWholePageIsBelowTheMinimum() = runTest {
        directory.streamPages += BrowsePage(listOf(stream("a", viewers = 300)), nextCursor = "s1")
        directory.streamPages += BrowsePage(listOf(stream("b", viewers = 60), stream("c", viewers = 40)), nextCursor = "s2")
        val pager = TestPager(config, CategoryStreamsPagingSource(directory, "1", StreamFilter(minViewers = 100)))

        pager.refresh()
        val last = assertIs<LoadResult.Page<String, BrowseStream>>(pager.append())

        assertTrue(last.data.isEmpty())
        assertNull(last.nextKey)
        assertEquals(listOf(null, "s1"), directory.streamCursors)
    }

    @Test
    fun filteredOutPagesAreSkippedWithinOneLoad() = runTest {
        directory.streamPages += BrowsePage(listOf(stream("m1", mature = true)), nextCursor = "s1")
        directory.streamPages += BrowsePage(listOf(stream("ok")), nextCursor = "s2")
        val pager = TestPager(config, CategoryStreamsPagingSource(directory, "1", StreamFilter(hideMature = true)))

        val page = assertIs<LoadResult.Page<String, BrowseStream>>(pager.refresh())

        assertEquals(listOf("ok"), page.data.map { it.login })
        assertEquals("s2", page.nextKey)
        assertEquals(listOf(null, "s1"), directory.streamCursors)
    }

    @Test
    fun endlessFilteredOutPagesEndThePagination() = runTest {
        repeat(10) { index ->
            directory.streamPages += BrowsePage(listOf(stream("m$index", mature = true)), nextCursor = "s$index")
        }
        val pager = TestPager(config, CategoryStreamsPagingSource(directory, "1", StreamFilter(hideMature = true)))

        val page = assertIs<LoadResult.Page<String, BrowseStream>>(pager.refresh())

        assertTrue(page.data.isEmpty())
        assertNull(page.nextKey)
        assertEquals(5, directory.streamCursors.size)
    }

    @Test
    fun activeFilterCountIgnoresDefaults() {
        assertEquals(0, StreamFilter().activeCount)
        assertEquals(3, StreamFilter(language = "RU", minViewers = 10, hideMature = true).activeCount)
        assertTrue(StreamFilter().isDefault)
    }

    private class ScriptedDirectory : StreamDirectory {
        val categoryPages = ArrayDeque<BrowsePage<BrowseCategory>?>()
        val streamPages = ArrayDeque<BrowsePage<BrowseStream>?>()
        val topStreamPages = ArrayDeque<BrowsePage<BrowseStream>?>()
        val categoryCursors = mutableListOf<String?>()
        val streamCursors = mutableListOf<String?>()
        val topStreamCursors = mutableListOf<String?>()

        override suspend fun topCategories(cursor: String?): BrowsePage<BrowseCategory>? {
            categoryCursors += cursor
            return if (categoryPages.isEmpty()) BrowsePage(emptyList(), null) else categoryPages.removeFirst()
        }

        override suspend fun categoryStreams(categoryId: String, filter: StreamFilter, cursor: String?): BrowsePage<BrowseStream>? {
            streamCursors += cursor
            return if (streamPages.isEmpty()) BrowsePage(emptyList(), null) else streamPages.removeFirst()
        }

        override suspend fun topStreams(filter: StreamFilter, cursor: String?): BrowsePage<BrowseStream>? {
            topStreamCursors += cursor
            return if (topStreamPages.isEmpty()) BrowsePage(emptyList(), null) else topStreamPages.removeFirst()
        }

        val followedPages = ArrayDeque<BrowsePage<BrowseStream>?>()

        override suspend fun followedStreams(userId: String, accessToken: String, cursor: String?): BrowsePage<BrowseStream>? =
            if (followedPages.isEmpty()) BrowsePage(emptyList(), null) else followedPages.removeFirst()

        override suspend fun search(query: String): BrowseSearchResult? = BrowseSearchResult(emptyList(), emptyList())
    }
}
