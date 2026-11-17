package io.rudione.chatone.presentation.main

import io.rudione.chatone.data.remote.TwitchIrcClient
import io.rudione.chatone.domain.model.ChannelFolder
import io.rudione.chatone.domain.model.ChannelTab
import io.rudione.chatone.base.UiState
import io.rudione.chatone.domain.model.Channel
import io.rudione.chatone.domain.model.MentionEntry
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.domain.model.WhisperConversation
import io.rudione.chatone.presentation.main.components.sidebar.FolderColors
import io.rudione.chatone.domain.model.SidebarLayout

data class MainState(
    val accounts: List<TwitchAccount> = emptyList(),
    val selectedAccount: TwitchAccount? = null,
    val isGuest: Boolean = false,
    val folders: List<ChannelFolder> = emptyList(),
    val unfolderedChannels: List<ChannelTab> = emptyList(),
    val activeChannelLogin: String? = null,
    val pendingScrollMessageId: String? = null,
    val mentionsChannelActive: Boolean = false,
    val openChannels: List<ChannelTab> = emptyList(),
    val monitorTabs: List<String> = emptyList(),
    val sidebarExpanded: Boolean = false,
    val sidebarCollapsed: Boolean = false,
    val sidebarWidthDp: Float = SidebarLayout.DEFAULT_WIDTH_DP,
    val miniRailCollapsed: Boolean = false,
    val isAddChannelDialogVisible: Boolean = false,
    val addChannelQuery: String = "",
    val searchResults: List<Channel> = emptyList(),
    val isSearching: Boolean = false,
    val isConnected: Boolean = false,
    val connectionState: TwitchIrcClient.ConnectionState = TwitchIrcClient.ConnectionState.Disconnected,
    val showSettings: Boolean = false,
    val isCreateFolderDialogVisible: Boolean = false,
    val newFolderName: String = "",
    val newFolderColor: String = FolderColors.DEFAULT_HEX,
    val newFolderSeedChannel: String? = null,
    val editingFolderId: String? = null,
    val editFolderName: String = "",
    val editFolderColor: String = FolderColors.DEFAULT_HEX,

    val whisperConversations: List<WhisperConversation> = emptyList(),
    val activeWhisperUserId: String? = null,
    val showWhisperPanel: Boolean = false,
    val totalUnreadWhispers: Int = 0,

    val mentions: List<MentionEntry> = emptyList(),
    val showMentionsFeed: Boolean = false,
    val unreadMentionsCount: Int = 0,

    val showChattersPanel: Boolean = false,
    val activeChatChannelId: String = "",
    val channelIdMap: Map<String, String> = emptyMap(),
    val moderatedChannelIds: Set<String> = emptySet(),
    val liveNotifyChannels: Set<String> = emptySet(),
    val needsReauth: Boolean = false,
    val recentChannels: List<ChannelTab> = emptyList(),
    val showBrowse: Boolean = false,
    val returnToBrowse: Boolean = false
) : UiState
