package io.rudione.chatone.presentation.chat.timeline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.rudione.chatone.data.remote.GqlUsercardMessage
import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.presentation.chat.UserLogArchiveState

class RemoteHistoryPager(
    val canLoad: Boolean,
    val hasMore: Boolean,
    val isLoading: Boolean,
    val loadMore: () -> Unit
)

val UserLogArchiveState.isBrowsingSeparately: Boolean
    get() = isActive && (isSearching || anchorMonth != null)

@Composable
internal fun rememberTimelineSource(
    session: List<DisplayMessage.PrivMsg>,
    local: List<ChatMessage>,
    remote: List<GqlUsercardMessage>,
    pager: RemoteHistoryPager,
    archive: UserLogArchiveState
): TimelineSource {
    val archiveLines = if (archive.isActive && !archive.isSearching) archive.lines else emptyList()
    val entries = remember(session, local, remote, archiveLines) {
        ProfileTimeline.merge(
            session,
            local,
            remote,
            archiveLines
        )
    }
    val remoteOpen = pager.canLoad && pager.hasMore
    val archiveOpen = archive.isSupported &&
            archive.availability != UserLogArchiveState.Availability.NotLogged &&
            archive.availability != UserLogArchiveState.Availability.Failed &&
            (!archive.isActive || archive.canLoadOlder)
    return TimelineSource(
        entries = entries,
        isLoadingOlder = pager.isLoading || archive.isLoading,
        canLoadOlder = remoteOpen || archiveOpen,
        onLoadOlder = {
            when {
                remoteOpen -> pager.loadMore()
                archiveOpen && !archive.isActive -> archive.open(null)
                archiveOpen -> archive.loadOlder()
            }
        }
    )
}
