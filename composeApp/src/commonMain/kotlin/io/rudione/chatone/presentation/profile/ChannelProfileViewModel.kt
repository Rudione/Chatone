package io.rudione.chatone.presentation.profile

import androidx.lifecycle.viewModelScope
import io.rudione.chatone.base.BaseViewModel
import io.rudione.chatone.data.remote.BadgeHqClient
import io.rudione.chatone.data.remote.BestLogsClient
import io.rudione.chatone.data.repository.ViewerSessionProvider
import io.rudione.chatone.domain.follow.ChannelFollowGateway
import io.rudione.chatone.domain.follow.FollowFailure
import io.rudione.chatone.domain.follow.FollowResult
import io.rudione.chatone.domain.profile.ChannelProfileSource
import io.rudione.chatone.domain.profile.ChannelVideo
import io.rudione.chatone.domain.profile.ClipQuery
import io.rudione.chatone.domain.profile.StreamCalendar
import io.rudione.chatone.domain.profile.VideoKind
import io.rudione.chatone.domain.profile.VideoQuery
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class ChannelProfileViewModel(
    private val login: String,
    private val source: ChannelProfileSource,
    private val follows: ChannelFollowGateway,
    private val sessions: ViewerSessionProvider,
    private val bestLogs: BestLogsClient,
    private val badgeHq: BadgeHqClient,
    private val clock: Clock = Clock.System
) : BaseViewModel<ChannelProfileState, ChannelProfileEvent, ChannelProfileEffect>(
    ChannelProfileState(login = login.lowercase())
) {
    private var clipsJob: Job? = null
    private var videosJob: Job? = null

    init {
        subscribeToEvents()
        load()
    }

    override suspend fun onEvent(event: ChannelProfileEvent) {
        when (event) {
            ChannelProfileEvent.Retry -> load()
            is ChannelProfileEvent.SelectTab -> selectTab(event.tab)
            ChannelProfileEvent.LoadMoreClips -> loadClips(reset = false)
            ChannelProfileEvent.LoadMoreVideos -> loadVideos(reset = false)
            is ChannelProfileEvent.ApplyClipQuery -> {
                if (event.query == state.value.clipQuery && state.value.clips.loaded) return
                update { it.copy(clipQuery = event.query) }
                loadClips(reset = true)
            }
            is ChannelProfileEvent.ApplyVideoQuery -> {
                if (event.query == state.value.videoQuery && state.value.videos.loaded) return
                update { it.copy(videoQuery = event.query) }
                loadVideos(reset = true)
            }
            is ChannelProfileEvent.SetFollowing -> setFollowing(event.follow)
        }
    }

    private fun load() {
        update { it.copy(load = ProfileLoad.LOADING) }
        viewModelScope.launch {
            val profileCall = async { source.profile(login) }
            val archivesCall = async { collectArchives() }
            val clipsCall = async { source.clips(login, ClipQuery(), null) }
            val session = sessions.current()
            val profile = profileCall.await()
            if (profile == null) {
                val reachable = archivesCall.await() != null
                update { it.copy(load = if (reachable) ProfileLoad.NOT_FOUND else ProfileLoad.FAILED) }
                return@launch
            }
            val archives = archivesCall.await().orEmpty()
            val clips = clipsCall.await()?.items.orEmpty()
            val zone = TimeZone.currentSystemDefault()
            update {
                it.copy(
                    load = ProfileLoad.READY,
                    profile = profile,
                    isOwn = session?.userId == profile.id,
                    recentVideos = archives.take(HOME_ROW),
                    topClips = clips.take(HOME_ROW),
                    calendar = StreamCalendar.build(archives, clock.todayIn(zone), zone),
                    follow = when {
                        session == null || session.userId == profile.id -> FollowStatus.UNAVAILABLE
                        else -> it.follow
                    }
                )
            }
            if (session != null && session.userId != profile.id) refreshFollow(profile.id)
            launch {
                val names = bestLogs.nameHistory(profile.id).orEmpty()
                    .filterNot { entry -> entry.login.equals(profile.login, ignoreCase = true) }
                update { it.copy(previousNames = names) }
            }
            launch {
                val badges = badgeHq.accountBadges(profile.login).orEmpty()
                update { it.copy(accountBadges = badges) }
            }
            when (state.value.tab) {
                ProfileTab.CLIPS -> loadClips(reset = true)
                ProfileTab.VIDEOS -> loadVideos(reset = true)
                else -> Unit
            }
        }
    }

    private suspend fun collectArchives(): List<ChannelVideo>? {
        val query = VideoQuery(kind = VideoKind.ARCHIVE)
        var page = source.videos(login, query, null) ?: return null
        val all = page.items.toMutableList()
        var pages = 1
        while (pages < ARCHIVE_PAGES) {
            val cursor = page.nextCursor ?: break
            page = source.videos(login, query, cursor) ?: break
            all += page.items
            pages++
        }
        return all
    }

    private fun selectTab(tab: ProfileTab) {
        update { it.copy(tab = tab) }
        if (state.value.load != ProfileLoad.READY) return
        when (tab) {
            ProfileTab.CLIPS -> if (!state.value.clips.loaded) loadClips(reset = true)
            ProfileTab.VIDEOS -> if (!state.value.videos.loaded) loadVideos(reset = true)
            else -> Unit
        }
    }

    private fun loadClips(reset: Boolean) {
        val current = state.value.clips
        if (!reset && !current.canLoadMore) return
        if (reset) clipsJob?.cancel() else if (clipsJob?.isActive == true) return
        val query = state.value.clipQuery
        val cursor = if (reset) null else current.cursor
        update { it.copy(clips = (if (reset) MediaFeed() else it.clips).copy(loading = true, failed = false)) }
        clipsJob = viewModelScope.launch {
            val page = source.clips(login, query, cursor)
            update { s ->
                if (s.clipQuery != query) return@update s
                s.copy(
                    clips = if (page == null) s.clips.copy(loading = false, failed = true)
                    else MediaFeed(
                        items = (if (reset) emptyList() else s.clips.items) + page.items,
                        cursor = page.nextCursor,
                        loaded = true
                    )
                )
            }
        }
    }

    private fun loadVideos(reset: Boolean) {
        val current = state.value.videos
        if (!reset && !current.canLoadMore) return
        if (reset) videosJob?.cancel() else if (videosJob?.isActive == true) return
        val query = state.value.videoQuery
        val cursor = if (reset) null else current.cursor
        update { it.copy(videos = (if (reset) MediaFeed() else it.videos).copy(loading = true, failed = false)) }
        videosJob = viewModelScope.launch {
            val page = source.videos(login, query, cursor)
            update { s ->
                if (s.videoQuery != query) return@update s
                s.copy(
                    videos = if (page == null) s.videos.copy(loading = false, failed = true)
                    else MediaFeed(
                        items = ((if (reset) emptyList() else s.videos.items) + page.items).distinctBy { it.id },
                        cursor = page.nextCursor,
                        loaded = true
                    )
                )
            }
        }
    }

    private suspend fun refreshFollow(channelId: String) {
        val status = when (val result = follows.isFollowing(channelId)) {
            is FollowResult.Done -> if (result.value) FollowStatus.FOLLOWING else FollowStatus.NOT_FOLLOWING
            is FollowResult.Failed ->
                if (result.reason == FollowFailure.SIGNED_OUT) FollowStatus.UNAVAILABLE else FollowStatus.NOT_FOLLOWING
        }
        update { it.copy(follow = status) }
    }

    private fun setFollowing(follow: Boolean) {
        val channelId = state.value.profile?.id ?: return
        val previous = state.value.follow
        if (previous == FollowStatus.BUSY || previous == FollowStatus.UNAVAILABLE) return
        update { it.copy(follow = FollowStatus.BUSY) }
        viewModelScope.launch {
            when (val result = follows.setFollowing(channelId, follow)) {
                is FollowResult.Done -> update {
                    it.copy(
                        follow = if (follow) FollowStatus.FOLLOWING else FollowStatus.NOT_FOLLOWING,
                        profile = it.profile?.let { p -> p.copy(followers = p.followers?.plus(if (follow) 1 else -1)) }
                    )
                }
                is FollowResult.Failed -> {
                    update { it.copy(follow = previous) }
                    sendEffect(
                        if (result.reason == FollowFailure.BLOCKED) ChannelProfileEffect.FollowBlocked
                        else ChannelProfileEffect.FollowFailed
                    )
                }
            }
        }
    }

    private companion object {
        const val HOME_ROW = 10
        const val ARCHIVE_PAGES = 3
    }
}
