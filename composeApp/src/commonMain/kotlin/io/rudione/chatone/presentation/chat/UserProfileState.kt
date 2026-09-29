package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.rudione.chatone.data.remote.BestLogsClient
import io.rudione.chatone.data.remote.GqlDisplayBadge
import io.rudione.chatone.data.remote.GqlUserDossier
import io.rudione.chatone.data.remote.GqlUsercardMessage
import io.rudione.chatone.data.remote.IvrApiClient
import io.rudione.chatone.data.remote.NameHistoryEntry
import io.rudione.chatone.data.remote.RolesTvClient
import io.rudione.chatone.presentation.chat.roles.UserRolesState
import io.rudione.chatone.data.remote.SubAgeInfo
import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.data.remote.TwitchGqlClient
import io.rudione.chatone.data.repository.ChatRepository
import io.rudione.chatone.data.repository.ModerationAuthStore
import io.rudione.chatone.data.repository.ModerationHistoryEntry
import io.rudione.chatone.data.repository.ModerationHistoryRepository
import io.rudione.chatone.presentation.chat.presence.UserPresenceState
import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.util.Result
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val EMPTY_PAGE_SKIPS = 2

class UserProfileState internal constructor(
    private val scope: CoroutineScope,
    private val gqlClient: TwitchGqlClient,
    private val apiClient: TwitchApiClient,
    private val chatRepository: ChatRepository,
    private val moderationHistoryRepository: ModerationHistoryRepository,
    private val moderationAuthStore: ModerationAuthStore,
    private val ivrApiClient: IvrApiClient,
    private val bestLogsClient: BestLogsClient,
    private val userId: String,
    private val username: String,
    private val channelId: String,
    private val channelLogin: String,
    private val accessToken: String,
    private val canModerate: Boolean,
    val archive: UserLogArchiveState,
    val presence: UserPresenceState,
    val roles: UserRolesState
) {
    var avatarUrl by mutableStateOf("")
        internal set
    var createdAt by mutableStateOf("")
        internal set
    var followedAt by mutableStateOf<String?>(null)
        internal set
    var subAge by mutableStateOf<SubAgeInfo?>(null)
        internal set
    var dossier by mutableStateOf<GqlUserDossier?>(null)
        internal set
    var isDossierLoading by mutableStateOf(false)
        internal set
    var gqlBadges by mutableStateOf<List<GqlDisplayBadge>>(emptyList())
        internal set
    var previousNames by mutableStateOf<List<NameHistoryEntry>>(emptyList())
        private set

    var localHistory by mutableStateOf<List<ChatMessage>>(emptyList())
        internal set
    var remoteHistory by mutableStateOf<List<GqlUsercardMessage>>(emptyList())
        internal set
    var moderationHistory by mutableStateOf<List<ModerationHistoryEntry>>(emptyList())
        internal set
    var isModerationHistoryLoading by mutableStateOf(false)
        private set

    var isHistoryLoading by mutableStateOf(false)
        private set
    var hasMoreHistory by mutableStateOf(true)
        private set

    private var cursor: String? = null
    private var loadedIds = mutableSetOf<String>()

    val canLoadHistory: Boolean
        get() = canModerate && accessToken.isNotBlank() && channelId.isNotBlank() && userId.isNotBlank()

    fun loadMoreHistory() {
        if (isHistoryLoading || !hasMoreHistory || !canLoadHistory) return
        isHistoryLoading = true
        scope.launch { fetchPage(EMPTY_PAGE_SKIPS) }
    }

    private suspend fun fetchPage(emptyPageSkipsLeft: Int) {
        val token = runCatching { moderationAuthStore.resolveToken(accessToken) }
            .getOrDefault(accessToken)
        val page = runCatching {
            gqlClient.getUsercardMessagesBySender(channelId, userId, cursor, token)
        }.getOrNull()

        if (page == null) {
            hasMoreHistory = false
            isHistoryLoading = false
            return
        }

        cursor = page.nextCursor
        hasMoreHistory = page.hasNextPage && !page.nextCursor.isNullOrBlank()

        val fresh = page.messages.filter { loadedIds.add(it.id) }
        if (fresh.isNotEmpty()) {
            remoteHistory = (fresh.reversed() + remoteHistory)
        }

        if (fresh.isEmpty() && hasMoreHistory && emptyPageSkipsLeft > 0) {
            fetchPage(emptyPageSkipsLeft - 1)
            return
        }

        isHistoryLoading = false
    }

    internal suspend fun loadEverything(sessionMessageIds: Set<String>) {
        avatarUrl = ""
        createdAt = ""
        followedAt = null
        subAge = null
        dossier = null
        gqlBadges = emptyList()
        remoteHistory = emptyList()
        localHistory = emptyList()
        cursor = null
        loadedIds = mutableSetOf()
        hasMoreHistory = true

        if (username.isNotBlank() && channelLogin.isNotBlank()) {
            scope.launch { subAge = ivrApiClient.getSubAge(username, channelLogin) }
        }
        if (userId.isNotBlank()) {
            scope.launch {
                previousNames = bestLogsClient.nameHistory(userId).orEmpty()
                    .filterNot { it.login.equals(username, ignoreCase = true) }
            }
        }

        val localJob = if (channelId.isNotBlank() && userId.isNotBlank()) {
            scope.launch {
                localHistory = chatRepository.getLocalHistoryForUser(channelId, userId)
                    .filterNot { it.id in sessionMessageIds }
            }
        } else null
        if (channelId.isNotBlank() && userId.isNotBlank()) {
            if (canModerate) {
                isModerationHistoryLoading = true
                scope.launch {
                    val token = runCatching { moderationAuthStore.resolveToken(accessToken) }
                        .getOrDefault(accessToken)
                    moderationHistory =
                        moderationHistoryRepository.loadHistoryForUser(channelId, userId, token)
                    isModerationHistoryLoading = false
                }
            }
        }

        if (accessToken.isNotBlank() && userId.isNotBlank()) {
            scope.launch {
                val result = apiClient.getUsers(accessToken, ids = listOf(userId))
                if (result is Result.Success) {
                    result.data.data.firstOrNull()?.let { user ->
                        avatarUrl = user.profileImageUrl
                        createdAt = user.createdAt.take(10)
                    }
                }
            }
            if (channelId.isNotBlank()) {
                scope.launch {
                    runCatching {
                        val follow = apiClient.getChannelFollower(accessToken, channelId, userId)
                        if (follow is Result.Success) {
                            followedAt = follow.data.data.firstOrNull()?.followedAt?.take(10)
                        }
                    }
                }
            }
        }

        if (username.isNotBlank()) {
            isDossierLoading = true
            val token = runCatching { moderationAuthStore.resolveToken(accessToken) }
                .getOrDefault(accessToken)
            scope.launch {
                gqlBadges = runCatching {
                    gqlClient.getUserDisplayBadges(username, channelLogin, token)
                }.getOrDefault(emptyList())
            }
            dossier = runCatching {
                gqlClient.getUserDossier(username, channelId, token)
            }.getOrNull()
            isDossierLoading = false
        }

        loadMoreHistory()
        if (sessionMessageIds.isEmpty()) {
            scope.launch {
                localJob?.join()
                snapshotFlow { isHistoryLoading }.first { !it }
                if (localHistory.isEmpty() && remoteHistory.isEmpty()) archive.openIfLogged()
            }
        }
    }
}

@Composable
internal fun rememberUserProfileState(
    userId: String,
    username: String,
    channelId: String,
    channelLogin: String,
    accessToken: String,
    canModerate: Boolean,
    initialAvatarUrl: String,
    initialCreatedAt: String,
    sessionMessages: List<DisplayMessage.PrivMsg>
): UserProfileState {
    val scope = rememberCoroutineScope()
    val gqlClient: TwitchGqlClient = koinInject()
    val apiClient: TwitchApiClient = koinInject()
    val chatRepository: ChatRepository = koinInject()
    val moderationHistoryRepository: ModerationHistoryRepository = koinInject()
    val moderationAuthStore: ModerationAuthStore = koinInject()
    val ivrApiClient: IvrApiClient = koinInject()
    val bestLogsClient: BestLogsClient = koinInject()
    val rolesClient: RolesTvClient = koinInject()

    val state = remember(userId, channelId) {
        UserProfileState(
            scope = scope,
            gqlClient = gqlClient,
            apiClient = apiClient,
            chatRepository = chatRepository,
            moderationHistoryRepository = moderationHistoryRepository,
            moderationAuthStore = moderationAuthStore,
            ivrApiClient = ivrApiClient,
            bestLogsClient = bestLogsClient,
            userId = userId,
            username = username,
            channelId = channelId,
            channelLogin = channelLogin,
            accessToken = accessToken,
            canModerate = canModerate,
            archive = UserLogArchiveState(scope, bestLogsClient, channelId, userId),
            presence = UserPresenceState(scope, bestLogsClient, channelId, userId),
            roles = UserRolesState(scope, rolesClient, userId, username)
        )
    }

    LaunchedEffect(state) {
        state.avatarUrl = initialAvatarUrl
        state.createdAt = initialCreatedAt
        state.loadEverything(sessionMessages.mapTo(mutableSetOf()) { it.id })
    }

    return state
}
