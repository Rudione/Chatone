package io.rudione.chatone.presentation.main

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.model.Channel
import io.rudione.chatone.presentation.chat.ChatScreen
import io.rudione.chatone.presentation.settings.SettingsScreen
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.theme.ChatoneColors
import io.rudione.chatone.presentation.theme.ChatoneTheme
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToAuth: () -> Unit,
    onThemeChanged: (Boolean) -> Unit,
    viewModel: MainViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val settingsState by settingsViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                MainEffect.NavigateToAuth -> onNavigateToAuth()
                is MainEffect.ShowError -> {
                    scope.launch { snackbarHostState.showSnackbar(effect.message) }
                }
                is MainEffect.ShowEmoteUpdate -> {
                    scope.launch { snackbarHostState.showSnackbar(effect.text) }
                }
            }
        }
    }

    // Show settings overlay
    if (state.showSettings) {
        SettingsScreen(
            onNavigateBack = { viewModel.sendEvent(MainEvent.HideSettings) },
            onThemeChanged = onThemeChanged
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { scaffoldPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isWideScreen = maxWidth >= 900.dp

            if (isWideScreen) {
                // ── Wide layout: persistent sidebar + chat ────────
                Row(modifier = Modifier.fillMaxSize()) {
                    ChannelSidebar(
                        state = state,
                        onEvent = { viewModel.sendEvent(it) }
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        val showTabBar = settingsState.channelNavigation != SettingsState.ChannelNavigation.MINI_RAIL
                        if (showTabBar && state.openChannels.size > 1) {
                            ChannelTabBar(
                                channels = state.openChannels,
                                activeLogin = state.activeChannelLogin,
                                onSelect = { viewModel.sendEvent(MainEvent.SelectChannel(it)) },
                                onClose = { viewModel.sendEvent(MainEvent.CloseChannel(it)) }
                            )
                        }

                        val activeChannel = state.activeChannelLogin
                        if (activeChannel != null) {
                            ChatScreen(
                                channelLogin = activeChannel,
                                onNavigateBack = {},
                                modifier = Modifier.weight(1f),
                                accessToken = state.selectedAccount?.accessToken ?: "",
                                currentUserId = state.selectedAccount?.userId ?: "",
                                currentUserLogin = state.selectedAccount?.login ?: "",
                                currentDisplayName = state.selectedAccount?.displayName ?: "",
                                onMentionDetected = { login -> viewModel.sendEvent(MainEvent.IncrementMentionCount(login)) }
                            )
                        } else {
                            Box(modifier = Modifier.weight(1f)) {
                                EmptyState(
                                    isGuest = state.isGuest,
                                    onAddChannel = { viewModel.sendEvent(MainEvent.ShowAddChannelDialog) },
                                    onLogin = { viewModel.sendEvent(MainEvent.NavigateToAuth) }
                                )
                            }
                        }
                    }
                }
            } else {
                // ── Compact layout: overlay sidebar ──────────────
                Column(modifier = Modifier.fillMaxSize()) {
                    val showTabBar = settingsState.channelNavigation != SettingsState.ChannelNavigation.MINI_RAIL
                    if (showTabBar && state.openChannels.size > 1) {
                        ChannelTabBar(
                            channels = state.openChannels,
                            activeLogin = state.activeChannelLogin,
                            onSelect = { viewModel.sendEvent(MainEvent.SelectChannel(it)) },
                            onClose = { viewModel.sendEvent(MainEvent.CloseChannel(it)) }
                        )
                    }

                    val activeChannel = state.activeChannelLogin
                    if (activeChannel != null) {
                        ChatScreen(
                            channelLogin = activeChannel,
                            onNavigateBack = {
                                viewModel.sendEvent(MainEvent.ToggleSidebar)
                            },
                            modifier = Modifier.weight(1f),
                            accessToken = state.selectedAccount?.accessToken ?: "",
                            currentUserId = state.selectedAccount?.userId ?: "",
                            currentUserLogin = state.selectedAccount?.login ?: "",
                            currentDisplayName = state.selectedAccount?.displayName ?: ""
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f)) {
                            EmptyState(
                                isGuest = state.isGuest,
                                onAddChannel = { viewModel.sendEvent(MainEvent.ShowAddChannelDialog) },
                                onLogin = { viewModel.sendEvent(MainEvent.NavigateToAuth) }
                            )
                        }
                    }
                }

                // ── Floating Mini Rail (top-left, compact — shown based on setting) ──
                val showMiniRail = settingsState.channelNavigation != SettingsState.ChannelNavigation.TAB_BAR
                if (showMiniRail && !state.sidebarExpanded) {
                    MiniRail(
                        state = state,
                        onEvent = { viewModel.sendEvent(it) },
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                }

                // ── Sidebar overlay (slides in from left) ────────
                AnimatedVisibility(
                    visible = state.sidebarExpanded,
                    enter = slideInHorizontally(tween(250)) + fadeIn(tween(200)),
                    exit = slideOutHorizontally(tween(250)) + fadeOut(tween(200)),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    ChannelSidebar(
                        state = state,
                        onEvent = { viewModel.sendEvent(it) }
                    )
                }
            }
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────────
    if (state.isAddChannelDialogVisible) {
        AddChannelDialog(
            query = state.addChannelQuery,
            searchResults = state.searchResults,
            isSearching = state.isSearching,
            onQueryChange = { viewModel.sendEvent(MainEvent.UpdateAddChannelQuery(it)) },
            onSearch = { viewModel.sendEvent(MainEvent.SearchChannels) },
            onChannelSelected = { login, profileImageUrl, displayName ->
                viewModel.sendEvent(MainEvent.AddChannel(login, profileImageUrl, displayName))
            },
            onDismiss = { viewModel.sendEvent(MainEvent.HideAddChannelDialog) }
        )
    }

    if (state.isCreateFolderDialogVisible) {
        CreateFolderDialog(
            name = state.newFolderName,
            onNameChange = { viewModel.sendEvent(MainEvent.UpdateNewFolderName(it)) },
            onCreate = { viewModel.sendEvent(MainEvent.CreateFolder) },
            onDismiss = { viewModel.sendEvent(MainEvent.HideCreateFolderDialog) }
        )
    }
}

// ─── Mini Rail (collapsed sidebar) ──────────────────────────────────────

@Composable
private fun MiniRail(
    state: MainState,
    onEvent: (MainEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val extra = ChatoneTheme.extraColors
    Row(
        modifier = modifier
            .padding(8.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = extra.shadowColor, spotColor = extra.elevatedShadow)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        extra.sidebarSurface,
                        extra.sidebarSurface.copy(alpha = 0.95f)
                    )
                )
            )
            .border(1.dp, extra.glassBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Expand button
        IconButton(onClick = { onEvent(MainEvent.ToggleSidebar) }, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Filled.Menu,
                contentDescription = "Open sidebar",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }

        // Channel icons (compact horizontal row)
        state.openChannels.take(6).forEach { channel ->
            val isActive = channel.login == state.activeChannelLogin
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                        .clickable { onEvent(MainEvent.SelectChannel(channel.login)) }
                        .then(
                            if (isActive) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (channel.profileImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = channel.profileImageUrl,
                            contentDescription = channel.displayName,
                            modifier = Modifier.size(26.dp).clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = channel.displayName.take(2).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Red mention badge (bottom-end)
                if (channel.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                            .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (channel.unreadCount > 9) "9+" else "${channel.unreadCount}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Add channel
        IconButton(onClick = { onEvent(MainEvent.ShowAddChannelDialog) }, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Add channel",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        // Settings
        IconButton(onClick = { onEvent(MainEvent.ShowSettings) }, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ─── Full Sidebar ───────────────────────────────────────────────────────

@Composable
private fun ChannelSidebar(
    state: MainState,
    onEvent: (MainEvent) -> Unit
) {
    val extra = ChatoneTheme.extraColors
    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .shadow(12.dp, RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp), ambientColor = extra.shadowColor, spotColor = extra.elevatedShadow)
            .clip(RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp))
            .background(extra.sidebarSurface)
            .border(1.dp, extra.glassBorder, RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chatone",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { onEvent(MainEvent.CloseSidebar) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Account indicator
        state.selectedAccount?.let { account ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (account.profileImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = account.profileImageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = account.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (state.isConnected) ChatoneTheme.extraColors.connected
                            else MaterialTheme.colorScheme.error
                        )
                )
            }
        } ?: run {
            if (state.isGuest) {
                TextButton(
                    onClick = { onEvent(MainEvent.NavigateToAuth) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Login to Twitch")
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )

        // Channel list with folders
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            // Folders
            state.folders.forEach { folder ->
                item(key = "folder_${folder.id}") {
                    FolderItem(
                        folder = folder,
                        activeChannelLogin = state.activeChannelLogin,
                        allFolders = state.folders,
                        unfolderedChannels = state.unfolderedChannels,
                        onToggle = { onEvent(MainEvent.ToggleFolder(folder.id)) },
                        onChannelSelect = { onEvent(MainEvent.SelectChannel(it)) },
                        onChannelClose = { onEvent(MainEvent.CloseChannel(it)) },
                        onMoveChannel = { login, targetFolderId ->
                            onEvent(MainEvent.MoveChannelToFolder(login, targetFolderId))
                        },
                        onDelete = { onEvent(MainEvent.DeleteFolder(folder.id)) }
                    )
                }
            }

            // Unfoldered channels header
            if (state.unfolderedChannels.isNotEmpty()) {
                item(key = "channels_header") {
                    Text(
                        text = "CHANNELS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // Unfoldered channels
            items(
                items = state.unfolderedChannels,
                key = { "channel_${it.login}" }
            ) { channel ->
                ChannelItem(
                    channel = channel,
                    isActive = channel.login == state.activeChannelLogin,
                    folders = state.folders,
                    currentFolderId = null,
                    onClick = { onEvent(MainEvent.SelectChannel(channel.login)) },
                    onClose = { onEvent(MainEvent.CloseChannel(channel.login)) },
                    onMoveToFolder = { folderId ->
                        onEvent(MainEvent.MoveChannelToFolder(channel.login, folderId))
                    }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        // Bottom actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Add channel
            FilledTonalButton(
                onClick = { onEvent(MainEvent.ShowAddChannelDialog) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Channel", style = MaterialTheme.typography.labelMedium)
            }

            Spacer(Modifier.width(8.dp))

            // Add folder
            OutlinedButton(
                onClick = { onEvent(MainEvent.ShowCreateFolderDialog) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Outlined.Create, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Folder", style = MaterialTheme.typography.labelMedium)
            }
        }

        // Settings
        TextButton(
            onClick = { onEvent(MainEvent.ShowSettings) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Settings")
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(4.dp))
    }
}

// ─── Folder Item ────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderItem(
    folder: ChannelFolder,
    activeChannelLogin: String?,
    allFolders: List<ChannelFolder>,
    unfolderedChannels: List<ChannelTab> = emptyList(),
    onToggle: () -> Unit,
    onChannelSelect: (String) -> Unit,
    onChannelClose: (String) -> Unit,
    onMoveChannel: (String, String?) -> Unit,
    onDelete: () -> Unit
) {
    var showFolderMenu by remember { mutableStateOf(false) }
    var showAddToFolderMenu by remember { mutableStateOf(false) }

    Column {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onToggle,
                        onLongClick = { showFolderMenu = true }
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (folder.isExpanded) Icons.Filled.KeyboardArrowDown
                    else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (folder.isExpanded) Icons.AutoMirrored.Filled.List
                    else Icons.AutoMirrored.Outlined.List,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = parseFolderColor(folder.color)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${folder.channels.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // + button to add channel to this folder
                Box {
                    IconButton(
                        onClick = { showAddToFolderMenu = true },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add channel to folder",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                    // Dropdown: pick from unfoldered channels
                    DropdownMenu(
                        expanded = showAddToFolderMenu,
                        onDismissRequest = { showAddToFolderMenu = false }
                    ) {
                        if (unfolderedChannels.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No channels to add", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = { showAddToFolderMenu = false },
                                enabled = false
                            )
                        } else {
                            unfolderedChannels.forEach { ch ->
                                DropdownMenuItem(
                                    text = { Text("#${ch.displayName}") },
                                    onClick = {
                                        showAddToFolderMenu = false
                                        onMoveChannel(ch.login, folder.id)
                                    },
                                    leadingIcon = {
                                        Text("#", fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Folder context menu
            DropdownMenu(
                expanded = showFolderMenu,
                onDismissRequest = { showFolderMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Delete folder") },
                    onClick = {
                        showFolderMenu = false
                        onDelete()
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
            }
        }

        // Expanded channels
        AnimatedVisibility(visible = folder.isExpanded) {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                folder.channels.forEach { channel ->
                    ChannelItem(
                        channel = channel,
                        isActive = channel.login == activeChannelLogin,
                        folders = allFolders,
                        currentFolderId = folder.id,
                        onClick = { onChannelSelect(channel.login) },
                        onClose = { onChannelClose(channel.login) },
                        onMoveToFolder = { targetFolderId ->
                            onMoveChannel(channel.login, targetFolderId)
                        }
                    )
                }
            }
        }
    }
}

// ─── Channel Item ───────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChannelItem(
    channel: ChannelTab,
    isActive: Boolean,
    folders: List<ChannelFolder> = emptyList(),
    currentFolderId: String? = null,
    onClick: () -> Unit,
    onClose: () -> Unit,
    onMoveToFolder: (String?) -> Unit = {}
) {
    var showContextMenu by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 1.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isActive) ChatoneTheme.extraColors.sidebarSelected
                    else Color.Transparent
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { if (folders.isNotEmpty()) showContextMenu = true }
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar or live indicator or hash
            if (channel.profileImageUrl.isNotEmpty()) {
                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = channel.profileImageUrl,
                        contentDescription = channel.displayName,
                        modifier = Modifier.size(22.dp).clip(CircleShape)
                    )
                    if (channel.isLive) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ChatoneTheme.extraColors.live)
                                .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        )
                    }
                }
            } else if (channel.isLive) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ChatoneTheme.extraColors.live)
                )
            } else {
                Text(
                    text = "#",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = channel.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActive) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Unread indicator
            if (channel.unreadCount > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    Text(
                        text = if (channel.unreadCount > 99) "99+" else "${channel.unreadCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Move to folder icon (when folders exist)
            if (folders.isNotEmpty()) {
                IconButton(
                    onClick = { showContextMenu = true },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.List,
                        contentDescription = "Move to folder",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Spacer(Modifier.width(2.dp))
            }

            // Close button on hover/active
            if (isActive) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Context menu: Move to folder
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            // Show "Remove from folder" if currently in a folder
            if (currentFolderId != null) {
                DropdownMenuItem(
                    text = { Text("Remove from folder") },
                    onClick = {
                        showContextMenu = false
                        onMoveToFolder(null)
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
                HorizontalDivider()
            }

            // Show available folders to move to
            folders.filter { it.id != currentFolderId }.forEach { folder ->
                DropdownMenuItem(
                    text = { Text(folder.name) },
                    onClick = {
                        showContextMenu = false
                        onMoveToFolder(folder.id)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Outlined.List,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = parseFolderColor(folder.color)
                        )
                    }
                )
            }

            // Close channel option
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Close channel") },
                onClick = {
                    showContextMenu = false
                    onClose()
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            )
        }
    }
}

// ─── Empty State ────────────────────────────────────────────────────────

@Composable
private fun EmptyState(
    isGuest: Boolean,
    onAddChannel: () -> Unit,
    onLogin: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Outlined.MailOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                text = "No channels open",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Add a channel to start chatting",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            FilledTonalButton(onClick = onAddChannel) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Channel")
            }

            if (isGuest) {
                OutlinedButton(onClick = onLogin) {
                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Login to Twitch")
                }
            }
        }
    }
}

// ─── Add Channel Dialog ─────────────────────────────────────────────────

@Composable
private fun AddChannelDialog(
    query: String,
    searchResults: List<Channel>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onChannelSelected: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Channel") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Channel name...") },
                    leadingIcon = {
                        Text("#", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            if (query.isNotBlank()) {
                                onChannelSelected(query.trim().lowercase().removePrefix("#"), "", "")
                            }
                        }
                    )
                )

                if (searchResults.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(searchResults) { channel ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onChannelSelected(channel.login, channel.profileImageUrl, channel.displayName) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (channel.profileImageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = channel.profileImageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp).clip(CircleShape)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = channel.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (channel.gameName.isNotEmpty()) {
                                        Text(
                                            text = channel.gameName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (channel.isLive) {
                                    Surface(
                                        color = ChatoneTheme.extraColors.live,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "LIVE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (query.isNotBlank()) {
                        onChannelSelected(query.trim().lowercase().removePrefix("#"), "", "")
                    }
                },
                enabled = query.isNotBlank()
            ) {
                Text("Join")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ─── Create Folder Dialog ───────────────────────────────────────────────

@Composable
private fun CreateFolderDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Folder") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Folder name...") },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onCreate() })
            )
        },
        confirmButton = {
            Button(
                onClick = onCreate,
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ─── Channel Tab Bar ────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChannelTabBar(
    channels: List<ChannelTab>,
    activeLogin: String?,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit
) {
    val extra = ChatoneTheme.extraColors
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, extra.cardBorder)
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            channels.forEach { channel ->
                val isActive = channel.login == activeLogin
                Surface(
                    onClick = { onSelect(channel.login) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isActive) MaterialTheme.colorScheme.surface
                    else Color.Transparent,
                    tonalElevation = if (isActive) 2.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (channel.isLive) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(ChatoneTheme.extraColors.live)
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = "#${channel.displayName}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isActive) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        if (channel.unreadCount > 0) {
                            Spacer(Modifier.width(4.dp))
                            Surface(
                                color = Color.Red,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = if (channel.unreadCount > 99) "99+" else "${channel.unreadCount}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = { onClose(channel.login) },
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Close ${channel.displayName}",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Helpers ────────────────────────────────────────────────────────────

private fun parseFolderColor(hex: String): Color {
    return try {
        val colorInt = hex.removePrefix("#").toLong(16)
        Color(
            red = ((colorInt shr 16) and 0xFF) / 255f,
            green = ((colorInt shr 8) and 0xFF) / 255f,
            blue = (colorInt and 0xFF) / 255f
        )
    } catch (e: Exception) {
        ChatoneColors.Violet400
    }
}
