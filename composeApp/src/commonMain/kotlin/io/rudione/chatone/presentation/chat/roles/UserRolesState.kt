package io.rudione.chatone.presentation.chat.roles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.rudione.chatone.data.remote.ChannelRole
import io.rudione.chatone.data.remote.ChannelRoleCounts
import io.rudione.chatone.data.remote.RoleChannel
import io.rudione.chatone.data.remote.RolesTvClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Stable
class RoleChannelList internal constructor() {
    var channels by mutableStateOf<List<RoleChannel>>(emptyList())
        internal set
    var total by mutableStateOf(0)
        internal set
    var isLoading by mutableStateOf(false)
        internal set
    var failed by mutableStateOf(false)
        internal set
    internal var cursor: String? = null
    internal var started = false

    val hasMore: Boolean get() = !started || cursor != null
}

@Stable
class UserRolesState internal constructor(
    private val scope: CoroutineScope,
    private val client: RolesTvClient,
    private val userId: String,
    private val login: String
) {
    enum class Status { Idle, Loading, Ready, Failed }

    var status by mutableStateOf(Status.Idle)
        private set
    var counts by mutableStateOf(ChannelRoleCounts.NONE)
        private set
    var selected by mutableStateOf<ChannelRole?>(null)
        private set

    private val lists = ChannelRole.entries.associateWith { RoleChannelList() }

    val isSupported: Boolean get() = userId.isNotBlank() || login.isNotBlank()
    val profileLogin: String get() = login

    fun ensureLoaded() {
        if (!isSupported || status == Status.Loading || status == Status.Ready) return
        status = Status.Loading
        scope.launch {
            val loaded = client.roleCounts(userId, login)
            if (loaded == null) {
                status = Status.Failed
                return@launch
            }
            counts = loaded
            status = Status.Ready
            loaded.held.firstOrNull()?.let(::select)
        }
    }

    fun select(role: ChannelRole) {
        selected = role
        if (!channelsOf(role).started) loadMore(role)
    }

    fun channelsOf(role: ChannelRole): RoleChannelList = lists.getValue(role)

    fun loadMore(role: ChannelRole) {
        val list = channelsOf(role)
        if (list.isLoading || !list.hasMore) return
        list.isLoading = true
        list.failed = false
        scope.launch {
            val page = client.roleChannels(role, userId, login, list.cursor)
            list.isLoading = false
            if (page == null) {
                list.failed = true
                return@launch
            }
            list.started = true
            list.cursor = page.nextCursor
            list.total = page.total
            val known = list.channels.mapTo(HashSet()) { it.id }
            list.channels = list.channels + page.channels.filterNot { it.id in known }
        }
    }
}

@Composable
internal fun rememberUserRoles(userId: String, login: String): UserRolesState {
    val scope = rememberCoroutineScope()
    val client: RolesTvClient = koinInject()
    return remember(userId, login) { UserRolesState(scope, client, userId, login) }
}
