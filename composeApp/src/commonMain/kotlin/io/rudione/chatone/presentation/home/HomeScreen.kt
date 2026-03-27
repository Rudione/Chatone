package io.rudione.chatone.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.Channel
import io.rudione.chatone.domain.model.TwitchAccount
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAuth: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var directChannelInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                HomeEffect.NavigateToAuth -> onNavigateToAuth()
                is HomeEffect.NavigateToChat -> onNavigateToChat(effect.channelLogin)
                is HomeEffect.ShowError -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chatone") },
                actions = {
                    if (state.isGuest) {
                        TextButton(onClick = { viewModel.sendEvent(HomeEvent.OnAddAccount) }) {
                            Text("Login")
                        }
                    } else {
                        IconButton(onClick = { viewModel.sendEvent(HomeEvent.OnAddAccount) }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Account")
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Guest mode banner
            if (state.isGuest) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Guest Mode",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Read-only. Login to send messages.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Account selector
            if (state.accounts.isNotEmpty()) {
                AccountSelector(
                    accounts = state.accounts,
                    selectedAccount = state.selectedAccount,
                    onAccountSelected = { viewModel.sendEvent(HomeEvent.OnAccountSelected(it)) },
                    onDeleteAccount = { viewModel.sendEvent(HomeEvent.OnDeleteAccount(it)) }
                )
            }

            // Direct channel join
            OutlinedTextField(
                value = directChannelInput,
                onValueChange = { directChannelInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Enter channel name to join...") },
                leadingIcon = { Text("#", style = MaterialTheme.typography.titleMedium) },
                trailingIcon = {
                    if (directChannelInput.isNotBlank()) {
                        IconButton(onClick = {
                            val channel = directChannelInput.trim().lowercase().removePrefix("#")
                            if (channel.isNotEmpty()) {
                                viewModel.sendEvent(HomeEvent.OnOpenChannel(channel))
                                directChannelInput = ""
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Join")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    val channel = directChannelInput.trim().lowercase().removePrefix("#")
                    if (channel.isNotEmpty()) {
                        viewModel.sendEvent(HomeEvent.OnOpenChannel(channel))
                        directChannelInput = ""
                    }
                })
            )

            // Search bar (only for logged in users)
            if (!state.isGuest) {
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = { viewModel.sendEvent(HomeEvent.OnSearchQueryChanged(it)) },
                    onSearch = { viewModel.sendEvent(HomeEvent.OnSearchChannels) },
                    isSearching = state.isSearching,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Search results or channels list
            if (state.searchResults.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.searchResults) { channel ->
                        ChannelCard(
                            channel = channel,
                            onClick = { viewModel.sendEvent(HomeEvent.OnOpenChannel(channel.login)) }
                        )
                    }
                }
            } else if (state.joinedChannels.isNotEmpty()) {
                Text(
                    text = "Joined Channels",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.joinedChannels) { channel ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.sendEvent(HomeEvent.OnOpenChannel(channel)) }
                        ) {
                            Text(
                                text = "#$channel",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Enter a channel name above to start chatting",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountSelector(
    accounts: List<TwitchAccount>,
    selectedAccount: TwitchAccount?,
    onAccountSelected: (TwitchAccount) -> Unit,
    onDeleteAccount: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Active Account",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            selectedAccount?.let { account ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (account.profileImageUrl.isNotEmpty()) {
                        coil3.compose.AsyncImage(
                            model = account.profileImageUrl,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = account.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    isSearching: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search channels...") },
        leadingIcon = {
            if (isSearching) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Icon(Icons.Default.Search, contentDescription = null)
            }
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() })
    )
}

@Composable
private fun ChannelCard(
    channel: Channel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (channel.profileImageUrl.isNotEmpty()) {
                coil3.compose.AsyncImage(
                    model = channel.profileImageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                if (channel.gameName.isNotEmpty()) {
                    Text(
                        text = channel.gameName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (channel.title.isNotEmpty()) {
                    Text(
                        text = channel.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
            if (channel.isLive) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.error,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "LIVE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    }
}
