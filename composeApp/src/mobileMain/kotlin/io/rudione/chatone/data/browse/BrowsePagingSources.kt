package io.rudione.chatone.data.browse

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.rudione.chatone.domain.browse.BrowseCategory
import io.rudione.chatone.domain.browse.BrowseStream
import io.rudione.chatone.domain.browse.StreamDirectory
import io.rudione.chatone.domain.browse.StreamFilter

class DirectoryUnavailableException : Exception("Twitch directory is unavailable")

class TopCategoriesPagingSource(
    private val directory: StreamDirectory
) : PagingSource<String, BrowseCategory>() {

    private val seen = HashSet<String>()

    override fun getRefreshKey(state: PagingState<String, BrowseCategory>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, BrowseCategory> {
        val page = directory.topCategories(params.key) ?: return LoadResult.Error(DirectoryUnavailableException())
        return LoadResult.Page(
            data = page.items.filter { seen.add(it.id) },
            prevKey = null,
            nextKey = page.nextCursor
        )
    }
}

class TopStreamsPagingSource(
    private val directory: StreamDirectory,
    private val filter: StreamFilter
) : PagingSource<String, BrowseStream>() {

    private val seen = HashSet<String>()

    override fun getRefreshKey(state: PagingState<String, BrowseStream>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, BrowseStream> {
        var cursor = params.key
        var skippedPages = 0
        while (true) {
            val page = directory.topStreams(filter, cursor)
                ?: return LoadResult.Error(DirectoryUnavailableException())
            val fresh = page.items.filter { seen.add(it.login) }
            val visible = fresh.filter { filter.accepts(it) && matchesLanguage(it) && matchesTag(it) }
            val exhausted = page.items.isNotEmpty() && page.items.all(filter::isExhaustedBy)
            val next = page.nextCursor.takeUnless { exhausted }
            if (visible.isNotEmpty() || next == null) {
                return LoadResult.Page(data = visible, prevKey = null, nextKey = next)
            }
            if (skippedPages >= MAX_SKIPPED_PAGES) {
                return LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
            }
            cursor = next
            skippedPages++
        }
    }

    private fun matchesLanguage(stream: BrowseStream): Boolean {
        val language = filter.language ?: return true
        return stream.language.equals(language, ignoreCase = true)
    }

    private fun matchesTag(stream: BrowseStream): Boolean {
        val tag = filter.tag?.trim().orEmpty()
        if (tag.isEmpty()) return true
        return stream.tags.any { it.equals(tag, ignoreCase = true) }
    }

    private companion object {
        const val MAX_SKIPPED_PAGES = 4
    }
}

class FollowedStreamsPagingSource(
    private val directory: StreamDirectory,
    private val userId: String,
    private val accessToken: String,
    private val filter: StreamFilter
) : PagingSource<String, BrowseStream>() {

    private val seen = HashSet<String>()

    override fun getRefreshKey(state: PagingState<String, BrowseStream>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, BrowseStream> {
        val page = directory.followedStreams(userId, accessToken, params.key)
            ?: return LoadResult.Error(DirectoryUnavailableException())
        val visible = page.items
            .filter { seen.add(it.login) }
            .filter { filter.accepts(it) && matchesLanguage(it) && matchesTag(it) }
        return LoadResult.Page(data = visible, prevKey = null, nextKey = page.nextCursor)
    }

    private fun matchesLanguage(stream: BrowseStream): Boolean {
        val language = filter.language ?: return true
        return stream.language.equals(language, ignoreCase = true)
    }

    private fun matchesTag(stream: BrowseStream): Boolean {
        val tag = filter.tag?.trim().orEmpty()
        if (tag.isEmpty()) return true
        return stream.tags.any { it.equals(tag, ignoreCase = true) }
    }
}

class CategoryStreamsPagingSource(
    private val directory: StreamDirectory,
    private val categoryId: String,
    private val filter: StreamFilter
) : PagingSource<String, BrowseStream>() {

    private val seen = HashSet<String>()

    override fun getRefreshKey(state: PagingState<String, BrowseStream>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, BrowseStream> {
        var cursor = params.key
        var skippedPages = 0
        while (true) {
            val page = directory.categoryStreams(categoryId, filter, cursor)
                ?: return LoadResult.Error(DirectoryUnavailableException())
            val fresh = page.items.filter { seen.add(it.login) }
            val visible = fresh.filter(filter::accepts)
            val exhausted = page.items.isNotEmpty() && page.items.all(filter::isExhaustedBy)
            val next = page.nextCursor.takeUnless { exhausted }
            if (visible.isNotEmpty() || next == null) {
                return LoadResult.Page(data = visible, prevKey = null, nextKey = next)
            }
            if (skippedPages >= MAX_SKIPPED_PAGES) {
                return LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
            }
            cursor = next
            skippedPages++
        }
    }

    private companion object {
        const val MAX_SKIPPED_PAGES = 4
    }
}
