package io.rudione.chatone.presentation.stream.channel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import io.rudione.chatone.base.BaseViewModel
import io.rudione.chatone.base.UIEffect
import io.rudione.chatone.base.UiEvent
import io.rudione.chatone.base.UiState
import io.rudione.chatone.domain.channel.ChannelSnapshot
import io.rudione.chatone.domain.channel.ChannelSnapshotSource
import io.rudione.chatone.domain.follow.ChannelFollowGateway
import io.rudione.chatone.domain.follow.FollowFailure
import io.rudione.chatone.domain.follow.FollowResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class FollowStatus { UNKNOWN, FOLLOWING, NOT_FOLLOWING, SIGNED_OUT }

@Immutable
data class StreamChannelState(
    val channel: ChannelSnapshot = ChannelSnapshot(login = ""),
    val follow: FollowStatus = FollowStatus.UNKNOWN,
    val followBusy: Boolean = false,
    val failure: FollowFailure? = null
) : UiState {
    val canToggleFollow: Boolean
        get() = channel.channelId.isNotBlank() && follow != FollowStatus.SIGNED_OUT
}

sealed interface StreamChannelEvent : UiEvent {
    data class Bind(val login: String, val hint: ChannelSnapshot? = null) : StreamChannelEvent
    data object ToggleFollow : StreamChannelEvent
    data object DismissFailure : StreamChannelEvent
}

sealed interface StreamChannelEffect : UIEffect

class StreamChannelViewModel(
    private val follows: ChannelFollowGateway,
    private val snapshots: ChannelSnapshotSource
) : BaseViewModel<StreamChannelState, StreamChannelEvent, StreamChannelEffect>(StreamChannelState()) {

    private val known = LinkedHashMap<String, ChannelSnapshot>()
    private val lookups = HashSet<String>()
    private var followLoadedFor = ""
    private var followJob: Job? = null
    private var failureJob: Job? = null

    init {
        subscribeToEvents()
    }

    override suspend fun onEvent(event: StreamChannelEvent) {
        when (event) {
            is StreamChannelEvent.Bind -> bind(event.login, event.hint)
            StreamChannelEvent.ToggleFollow -> toggleFollow()
            StreamChannelEvent.DismissFailure -> update { it.copy(failure = null) }
        }
    }

    private fun bind(login: String, hint: ChannelSnapshot?) {
        val key = login.trim().lowercase()
        if (key.isEmpty()) return
        val snapshot = remember(key, hint)
        if (key != state.value.channel.login) {
            followJob?.cancel()
            failureJob?.cancel()
            followLoadedFor = ""
            update { StreamChannelState(channel = snapshot) }
        } else {
            update { it.copy(channel = snapshot) }
        }
        if (!snapshot.complete && lookups.add(key)) lookUp(key)
        loadFollow(snapshot.channelId)
    }

    private fun remember(key: String, hint: ChannelSnapshot?): ChannelSnapshot {
        val base = known.remove(key) ?: ChannelSnapshot(login = key)
        val merged = if (hint != null) base.mergedWith(hint.copy(login = key)) else base
        known[key] = merged
        if (known.size > MAX_KNOWN_CHANNELS) known.remove(known.keys.first())
        return merged
    }

    private fun lookUp(key: String) {
        viewModelScope.launch {
            val fetched = snapshots.snapshot(key)
            if (fetched == null) {
                lookups.remove(key)
                return@launch
            }
            val merged = remember(key, fetched)
            if (state.value.channel.login != key) return@launch
            update { it.copy(channel = merged) }
            loadFollow(merged.channelId)
        }
    }

    private fun loadFollow(channelId: String) {
        if (channelId.isBlank() || channelId == followLoadedFor) return
        followLoadedFor = channelId
        followJob?.cancel()
        followJob = viewModelScope.launch {
            val status = when (val result = follows.isFollowing(channelId)) {
                is FollowResult.Done -> if (result.value) FollowStatus.FOLLOWING else FollowStatus.NOT_FOLLOWING
                is FollowResult.Failed ->
                    if (result.reason == FollowFailure.SIGNED_OUT) FollowStatus.SIGNED_OUT else FollowStatus.UNKNOWN
            }
            update { if (it.channel.channelId == channelId) it.copy(follow = status) else it }
        }
    }

    private fun toggleFollow() {
        val current = state.value
        if (current.followBusy || !current.canToggleFollow) return
        val channelId = current.channel.channelId
        val follow = current.follow != FollowStatus.FOLLOWING
        failureJob?.cancel()
        update { it.copy(followBusy = true, failure = null) }
        viewModelScope.launch {
            val result = follows.setFollowing(channelId, follow)
            update { st ->
                when {
                    st.channel.channelId != channelId -> st
                    result is FollowResult.Failed -> st.copy(followBusy = false, failure = result.reason)
                    else -> st.copy(
                        followBusy = false,
                        follow = if (follow) FollowStatus.FOLLOWING else FollowStatus.NOT_FOLLOWING
                    )
                }
            }
            if (result is FollowResult.Failed) dismissFailureLater()
        }
    }

    private fun dismissFailureLater() {
        failureJob?.cancel()
        failureJob = viewModelScope.launch {
            delay(FAILURE_VISIBLE_MS)
            update { it.copy(failure = null) }
        }
    }

    private companion object {
        const val FAILURE_VISIBLE_MS = 5_000L
        const val MAX_KNOWN_CHANNELS = 24
    }
}
