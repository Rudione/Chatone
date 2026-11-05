package io.rudione.chatone.presentation.main.follows

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.rudione.chatone.domain.follow.FollowedChannel
import io.rudione.chatone.domain.follow.FollowedChannelOrder
import io.rudione.chatone.domain.follow.FollowedChannelsResult
import io.rudione.chatone.domain.follow.FollowedChannelsSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

enum class FollowedPickerStatus { Loading, Ready, SignedOut, Failed }

@Stable
class FollowedPickerState internal constructor(
    private val source: FollowedChannelsSource,
    private val scope: CoroutineScope,
    private val unavailable: Set<String>
) {
    var status by mutableStateOf(FollowedPickerStatus.Loading)
        private set
    var channels by mutableStateOf<List<FollowedChannel>>(emptyList())
        private set
    var query by mutableStateOf("")
        private set

    private val selection = mutableStateListOf<String>()
    private var job: Job? = null

    val visible: List<FollowedChannel> by derivedStateOf { FollowedChannelOrder.filter(channels, query) }
    val selectedCount: Int get() = selection.size
    val hasLiveToPick: Boolean by derivedStateOf {
        channels.any { it.isLive && it.login !in unavailable && it.login !in selection }
    }

    fun isOpen(login: String): Boolean = login in unavailable

    fun isSelected(login: String): Boolean = login in selection

    fun load(force: Boolean = false) {
        if (job?.isActive == true) return
        if (channels.isEmpty() || force) status = FollowedPickerStatus.Loading
        job = scope.launch {
            when (val result = source.followedChannels(forceRefresh = force)) {
                is FollowedChannelsResult.Loaded -> {
                    channels = result.channels
                    selection.retainAll { login -> result.channels.any { it.login == login } }
                    status = FollowedPickerStatus.Ready
                }
                FollowedChannelsResult.SignedOut -> status = FollowedPickerStatus.SignedOut
                FollowedChannelsResult.Failed -> status = FollowedPickerStatus.Failed
            }
        }
    }

    fun updateQuery(value: String) {
        query = value.take(MAX_QUERY)
    }

    fun toggle(login: String) {
        if (login in unavailable) return
        if (!selection.remove(login)) selection += login
    }

    fun pickLive() {
        channels.filter { it.isLive && it.login !in unavailable && it.login !in selection }
            .forEach { selection += it.login }
    }

    fun clear() = selection.clear()

    fun picked(): List<FollowedChannel> = selection.mapNotNull { login -> channels.firstOrNull { it.login == login } }

    private companion object {
        const val MAX_QUERY = 50
    }
}

@Composable
fun rememberFollowedPickerState(openLogins: Set<String>): FollowedPickerState {
    val source: FollowedChannelsSource = koinInject()
    val scope = rememberCoroutineScope()
    return remember(source) { FollowedPickerState(source, scope, openLogins) }
}
