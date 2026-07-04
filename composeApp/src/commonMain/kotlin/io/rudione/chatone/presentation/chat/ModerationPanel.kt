package io.rudione.chatone.presentation.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.domain.model.Macro
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.i18n.LocalStrings

@Composable
fun ModerationPanel(
    roomState: RoomState,
    channelLogin: String,
    isMod: Boolean,
    pinnedMacros: List<Macro> = emptyList(),
    onUpdateChatSettings: (Map<String, Any>) -> Unit,
    onClearChat: () -> Unit,
    onSendAnnouncement: (message: String, color: String) -> Unit,
    onStartRaid: (targetLogin: String) -> Unit,
    onCancelRaid: () -> Unit,
    onExecuteMacro: (Macro) -> Unit = {},
    onSendPinMessage: (message: String) -> Unit = {},
    onShoutout: (targetLogin: String) -> Unit = {},
    onSendRawCommand: (String) -> Unit = {},
    onOpenLocalAutomod: () -> Unit = {},
    onClose: () -> Unit,
    accessToken: String = "",
    channelId: String = "",
    isBroadcaster: Boolean = false,
    modifier: Modifier = Modifier
) {
    val extra = ChatoneTheme.extraColors

    var showAnnouncement by remember { mutableStateOf(false) }
    var announcementText by remember { mutableStateOf("") }
    var announcementColor by remember { mutableStateOf("primary") }
    var showRaid by remember { mutableStateOf(false) }
    var raidTarget by remember { mutableStateOf("") }
    var showPoll by remember { mutableStateOf(false) }
    var pollTitle by remember { mutableStateOf("") }
    var pollChoicesText by remember { mutableStateOf("") }
    var pollDurationSec by remember { mutableStateOf("60") }
    var showPrediction by remember { mutableStateOf(false) }
    var predictionTitle by remember { mutableStateOf("") }
    var predictionOutcomesText by remember { mutableStateOf("") }
    var predictionWindowSec by remember { mutableStateOf("120") }
    var showPin by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }
    var showShoutout by remember { mutableStateOf(false) }
    var shoutoutTarget by remember { mutableStateOf("") }
    var showChannelPoints by remember { mutableStateOf(false) }


    LiquidGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 460.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        contentPadding = PaddingValues(0.dp),
        backgroundAlphaHigh = 0.94f,
        backgroundAlphaLow = 0.86f,
        borderAlphaHigh = 0.18f,
        borderAlphaLow = 0.06f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    LocalStrings.current.panelModeration,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }


            if (pinnedMacros.isNotEmpty()) {
                PanelSectionLabel("Macros")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    pinnedMacros.forEach { macro ->
                        ModPanelButton(
                            label = macro.name,
                            iconText = macro.icon,
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = { onExecuteMacro(macro) }
                        )
                    }
                }
                PanelDivider()
            }


            PanelSectionLabel("Chat Modes")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ModPanelButton(
                    label = "Emote",
                    icon = Icons.Outlined.Face,
                    tint = MaterialTheme.colorScheme.primary,
                    isToggled = roomState.emoteOnly
                ) { onUpdateChatSettings(mapOf("emote_mode" to !roomState.emoteOnly)) }

                ModPanelButton(
                    label = "Subs",
                    icon = Icons.Outlined.Star,
                    tint = MaterialTheme.colorScheme.primary,
                    isToggled = roomState.subsOnly
                ) { onUpdateChatSettings(mapOf("subscriber_mode" to !roomState.subsOnly)) }

                ModPanelButton(
                    label = "Unique",
                    icon = Icons.Outlined.Lock,
                    tint = MaterialTheme.colorScheme.primary,
                    isToggled = roomState.r9k
                ) { onUpdateChatSettings(mapOf("unique_chat_mode" to !roomState.r9k)) }

                ModPanelButton(
                    label = "Followers",
                    icon = Icons.Outlined.Favorite,
                    tint = MaterialTheme.colorScheme.primary,
                    isToggled = roomState.followersOnly >= 0
                ) {
                    if (roomState.followersOnly >= 0) {
                        onUpdateChatSettings(mapOf("follower_mode" to false))
                    } else {
                        onUpdateChatSettings(mapOf("follower_mode" to true, "follower_mode_duration" to 10))
                    }
                }
            }


            PanelDivider()
            PanelSectionLabel("Slow Mode")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(0 to "Off", 3 to "3s", 5 to "5s", 10 to "10s", 30 to "30s", 60 to "1m", 120 to "2m")
                    .forEach { (secs, label) ->
                        val isActive = if (secs == 0) roomState.slowMode == 0 else roomState.slowMode == secs
                        ModPanelButton(
                            label = label,
                            tint = MaterialTheme.colorScheme.primary,
                            isToggled = isActive
                        ) {
                            if (secs == 0) {
                                onUpdateChatSettings(mapOf("slow_mode" to false))
                            } else {
                                onUpdateChatSettings(mapOf("slow_mode" to true, "slow_mode_wait_time" to secs))
                            }
                        }
                    }
            }


            AnimatedVisibility(
                visible = roomState.followersOnly >= 0,
                enter = expandVertically(tween(200)) + fadeIn(tween(150)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
            ) {
                Column {
                    PanelDivider()
                    PanelSectionLabel("Min. Follow Time")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(0 to "Any", 10 to "10m", 30 to "30m", 60 to "1h", 1440 to "1d", 10080 to "1w")
                            .forEach { (min, label) ->
                                ModPanelButton(
                                    label = label,
                                    tint = MaterialTheme.colorScheme.primary,
                                    isToggled = roomState.followersOnly == min
                                ) {
                                    onUpdateChatSettings(mapOf("follower_mode" to true, "follower_mode_duration" to min))
                                }
                            }
                    }
                }
            }


            PanelDivider()
            PanelSectionLabel("Quick Actions")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ModPanelButton(
                    label = "Automod",
                    icon = Icons.Outlined.Build,
                    tint = MaterialTheme.colorScheme.primary
                ) { onOpenLocalAutomod() }

                ModPanelButton(
                    label = "Clear",
                    icon = Icons.Outlined.Delete,
                    tint = extra.modDelete
                ) { onClearChat() }

                ModPanelButton(
                    label = "Announce",
                    icon = Icons.Outlined.Notifications,
                    tint = MaterialTheme.colorScheme.tertiary,
                    isToggled = showAnnouncement
                ) { showAnnouncement = !showAnnouncement }

                ModPanelButton(
                    label = "Raid",
                    icon = Icons.Filled.PlayArrow,
                    tint = MaterialTheme.colorScheme.secondary,
                    isToggled = showRaid
                ) { showRaid = !showRaid }

                ModPanelButton(
                    label = "Poll",
                    icon = Icons.Outlined.HowToVote,
                    tint = MaterialTheme.colorScheme.secondary,
                    isToggled = showPoll
                ) { showPoll = !showPoll }

                ModPanelButton(
                    label = "Predict",
                    icon = Icons.Outlined.TrendingUp,
                    tint = MaterialTheme.colorScheme.tertiary,
                    isToggled = showPrediction
                ) { showPrediction = !showPrediction }

                ModPanelButton(
                    label = "Points",
                    icon = Icons.Outlined.Star,
                    tint = MaterialTheme.colorScheme.primary,
                    isToggled = showChannelPoints
                ) { showChannelPoints = !showChannelPoints }


                
            }


            AnimatedVisibility(
                visible = showPin,
                enter = expandVertically(tween(200)) + fadeIn(tween(150)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
            ) {
                ModPanelInputRow(
                    value = pinText,
                    onValueChange = { pinText = it },
                    placeholder = "Message to pin via /pin …",
                    sendColor = MaterialTheme.colorScheme.secondary,
                    onSend = {
                        if (pinText.isNotBlank()) {
                            onSendPinMessage(pinText.trim())
                            pinText = ""
                            showPin = false
                        }
                    }
                )
            }


            AnimatedVisibility(
                visible = showShoutout,
                enter = expandVertically(tween(200)) + fadeIn(tween(150)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
            ) {
                ModPanelInputRow(
                    value = shoutoutTarget,
                    onValueChange = { shoutoutTarget = it },
                    placeholder = "Channel to shoutout…",
                    sendColor = MaterialTheme.colorScheme.tertiary,
                    onSend = {
                        if (shoutoutTarget.isNotBlank()) {
                            onShoutout(shoutoutTarget.trim().removePrefix("@"))
                            shoutoutTarget = ""
                            showShoutout = false
                        }
                    }
                )
            }


            AnimatedVisibility(
                visible = showAnnouncement,
                enter = expandVertically(tween(200)) + fadeIn(tween(150)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            LocalStrings.current.panelColor,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        listOf(
                            "primary" to MaterialTheme.colorScheme.primary,
                            "blue" to Color(0xFF4A90D9),
                            "green" to Color(0xFF4CAF50),
                            "orange" to Color(0xFFFF9800),
                            "purple" to Color(0xFF9C27B0)
                        ).forEach { (name, color) ->
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = if (announcementColor == name) 1f else 0.3f))
                                    .then(
                                        if (announcementColor == name)
                                            Modifier.border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                                        else Modifier
                                    )
                                    .clickable { announcementColor = name }
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))

                    ModPanelInputRow(
                        value = announcementText,
                        onValueChange = { announcementText = it },
                        placeholder = "Announcement message…",
                        sendColor = MaterialTheme.colorScheme.primary,
                        onSend = {
                            if (announcementText.isNotBlank()) {
                                onSendAnnouncement(announcementText, announcementColor)
                                announcementText = ""
                                showAnnouncement = false
                            }
                        },
                        maxLines = 3,
                        outerPadding = false
                    )
                }
            }


            AnimatedVisibility(
                visible = showRaid,
                enter = expandVertically(tween(200)) + fadeIn(tween(150)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
            ) {
                ModPanelInputRow(
                    value = raidTarget,
                    onValueChange = { raidTarget = it },
                    placeholder = LocalStrings.current.raidChannelPlaceholder,
                    sendColor = MaterialTheme.colorScheme.secondary,
                    sendIcon = Icons.Filled.PlayArrow,
                    onSend = {
                        if (raidTarget.isNotBlank()) {
                            onStartRaid(raidTarget.trim().removePrefix("@"))
                            raidTarget = ""
                            showRaid = false
                        }
                    }
                )
            }

            AnimatedVisibility(
                visible = showPoll,
                enter = expandVertically(tween(200)) + fadeIn(tween(150)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
            ) {
                Column {
                    PollPredictionForm(
                        title = pollTitle,
                        onTitleChange = { pollTitle = it },
                        duration = pollDurationSec,
                        onDurationChange = { pollDurationSec = it.filter { c -> c.isDigit() }.take(4) },
                        durationLabel = LocalStrings.current.pollDurationLabel,
                        options = pollChoicesText,
                        onOptionsChange = { pollChoicesText = it },
                        optionsLabel = LocalStrings.current.pollChoicesLabel,
                        submitLabel = LocalStrings.current.pollStart,
                        submitColor = MaterialTheme.colorScheme.secondary,
                        onSubmit = {
                            val duration = pollDurationSec.toIntOrNull()?.coerceIn(15, 1800) ?: 60
                            val choices = pollChoicesText.lines().map { it.trim() }.filter { it.isNotEmpty() }.take(5)
                            if (pollTitle.isNotBlank() && choices.size >= 2) {
                                onSendRawCommand("/poll $duration ${pollTitle.trim()} | ${choices.joinToString(" | ")}")
                                pollTitle = ""
                                pollChoicesText = ""
                                showPoll = false
                            }
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = { onSendRawCommand("/endpoll") }) {
                            Text(LocalStrings.current.pollEnd, style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { onSendRawCommand("/cancelpoll") }) {
                            Text(
                                LocalStrings.current.pollCancel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showPrediction,
                enter = expandVertically(tween(200)) + fadeIn(tween(150)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
            ) {
                Column {
                    PollPredictionForm(
                        title = predictionTitle,
                        onTitleChange = { predictionTitle = it },
                        duration = predictionWindowSec,
                        onDurationChange = { predictionWindowSec = it.filter { c -> c.isDigit() }.take(4) },
                        durationLabel = LocalStrings.current.predictionWindowLabel,
                        options = predictionOutcomesText,
                        onOptionsChange = { predictionOutcomesText = it },
                        optionsLabel = LocalStrings.current.predictionOutcomesLabel,
                        submitLabel = LocalStrings.current.predictionStart,
                        submitColor = MaterialTheme.colorScheme.tertiary,
                        onSubmit = {
                            val window = predictionWindowSec.toIntOrNull()?.coerceIn(30, 1800) ?: 120
                            val outcomes = predictionOutcomesText.lines().map { it.trim() }.filter { it.isNotEmpty() }.take(10)
                            if (predictionTitle.isNotBlank() && outcomes.size >= 2) {
                                onSendRawCommand("/prediction $window ${predictionTitle.trim()} | ${outcomes.joinToString(" | ")}")
                                predictionTitle = ""
                                predictionOutcomesText = ""
                                showPrediction = false
                            }
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = { onSendRawCommand("/lockprediction") }) {
                            Text(LocalStrings.current.predictionLock, style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { onSendRawCommand("/completeprediction 1") }) {
                            Text(LocalStrings.current.predictionWinner.replace("{0}", "1"), style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { onSendRawCommand("/completeprediction 2") }) {
                            Text(LocalStrings.current.predictionWinner.replace("{0}", "2"), style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { onSendRawCommand("/cancelprediction") }) {
                            Text(
                                "Cancel",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showChannelPoints,
                enter = expandVertically(tween(200)) + fadeIn(tween(150)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
            ) {
                ChannelPointsSection(
                    accessToken = accessToken,
                    channelId = channelId,
                    isBroadcaster = isBroadcaster
                )
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ChannelPointsSection(
    accessToken: String,
    channelId: String,
    isBroadcaster: Boolean
) {
    val apiClient: io.rudione.chatone.data.remote.TwitchApiClient = org.koin.compose.koinInject()
    val scope = rememberCoroutineScope()
    var rewards by remember { mutableStateOf<List<io.rudione.chatone.data.remote.dto.CustomRewardData>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var newTitle by remember { mutableStateOf("") }
    var newCost by remember { mutableStateOf("") }

    var manageableIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    fun reload() {
        if (accessToken.isBlank() || channelId.isBlank()) return
        loading = true; error = null
        scope.launch {
            when (val r = apiClient.getCustomRewards(accessToken, channelId)) {
                is io.rudione.chatone.util.Result.Success -> rewards = r.data.data
                is io.rudione.chatone.util.Result.Error ->
                    error = r.exception.message ?: "Failed to load rewards"
                else -> {}
            }
            // Separate call: Helix only reveals which rewards this app may PATCH/DELETE
            // through the only_manageable_rewards filter.
            when (val m = apiClient.getCustomRewards(accessToken, channelId, onlyManageable = true)) {
                is io.rudione.chatone.util.Result.Success ->
                    manageableIds = m.data.data.map { it.id }.toSet()
                else -> {}
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        if (!isBroadcaster) {
            Text(
                "Channel points are only available for the broadcaster.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }
        when {
            loading -> Text(
                "Loading rewards…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            error != null -> Text(
                error ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            rewards.isEmpty() -> Text(
                "No custom rewards yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> rewards.take(12).forEach { reward ->
                val manageable = reward.id in manageableIds
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                parseHexColorOrDefault(
                                    reward.backgroundColor,
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                    )
                    Text(
                        reward.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${reward.cost}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!reward.isEnabled || reward.isPaused) {
                        Text(
                            if (reward.isPaused) "paused" else "off",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    if (manageable) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val r = apiClient.updateCustomReward(
                                        accessToken, channelId, reward.id,
                                        isPaused = !reward.isPaused
                                    )
                                    if (r is io.rudione.chatone.util.Result.Error)
                                        error = "Update failed: ${r.exception.message}"
                                    reload()
                                }
                            },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                if (reward.isPaused) Icons.Filled.PlayArrow else Icons.Outlined.Lock,
                                contentDescription = if (reward.isPaused) "Resume" else "Pause",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val r = apiClient.deleteCustomReward(accessToken, channelId, reward.id)
                                    if (r is io.rudione.chatone.util.Result.Error)
                                        error = "Delete failed: ${r.exception.message}"
                                    reload()
                                }
                            },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Delete reward",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompactModField(
                value = newTitle,
                onValueChange = { newTitle = it.take(45) },
                placeholder = "New reward title",
                modifier = Modifier.weight(1f)
            )
            CompactModField(
                value = newCost,
                onValueChange = { newCost = it.filter { c -> c.isDigit() }.take(7) },
                placeholder = "Cost",
                modifier = Modifier.width(76.dp),
                numeric = true
            )
            FilledTonalButton(
                onClick = {
                    val cost = newCost.toIntOrNull() ?: return@FilledTonalButton
                    if (newTitle.isBlank() || cost < 1) return@FilledTonalButton
                    scope.launch {
                        when (val r = apiClient.createCustomReward(accessToken, channelId, newTitle.trim(), cost)) {
                            is io.rudione.chatone.util.Result.Success -> {
                                newTitle = ""; newCost = ""
                                reload()
                            }
                            is io.rudione.chatone.util.Result.Error ->
                                error = r.exception.message ?: "Failed to create reward"
                            else -> {}
                        }
                    }
                },
                enabled = newTitle.isNotBlank() && (newCost.toIntOrNull() ?: 0) > 0,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Text(LocalStrings.current.create, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = { reload() }, modifier = Modifier.size(26.dp)) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Refresh rewards",
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            "Rewards created here can be managed by Chatone; dashboard-created ones are read-only (Twitch API limitation).",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        )
    }
}

private fun parseHexColorOrDefault(hex: String, fallback: Color): Color = try {
    if (hex.startsWith("#") && (hex.length == 7 || hex.length == 9)) {
        val v = hex.removePrefix("#").toLong(16)
        if (hex.length == 7) Color(0xFF000000 or v) else Color(v)
    } else fallback
} catch (_: Exception) {
    fallback
}

/** 34dp bordered pill field — OutlinedTextField's 56dp minimum makes the mod panel forms
 * twice as tall as they need to be. */
@Composable
internal fun CompactModField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    numeric: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor =
        if (isFocused) accent.copy(alpha = 0.6f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        interactionSource = interactionSource,
        textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(accent),
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number)
        else KeyboardOptions.Default,
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp)),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        maxLines = 1
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun PollPredictionForm(
    title: String,
    onTitleChange: (String) -> Unit,
    duration: String,
    onDurationChange: (String) -> Unit,
    durationLabel: String,
    options: String,
    onOptionsChange: (String) -> Unit,
    optionsLabel: String,
    submitLabel: String,
    submitColor: Color,
    onSubmit: () -> Unit,
    maxOptions: Int = 5
) {
    val rows = remember(options) {
        val initial = options.lines().filter { it.isNotEmpty() }
        val seed = if (initial.size >= 2) initial else (initial + List(2 - initial.size) { "" })
        mutableStateListOf<String>().apply { addAll(seed) }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(submitColor.copy(alpha = 0.05f))
            .border(1.dp, submitColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CompactModField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = "Title",
                modifier = Modifier.weight(1f),
                accent = submitColor
            )
            CompactModField(
                value = duration,
                onValueChange = onDurationChange,
                placeholder = durationLabel.substringBefore(" ("),
                modifier = Modifier.width(76.dp),
                accent = submitColor,
                numeric = true
            )
        }
        rows.forEachIndexed { idx, value ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CompactModField(
                    value = value,
                    onValueChange = { v ->
                        rows[idx] = v
                        onOptionsChange(rows.joinToString("\n"))
                    },
                    placeholder = "Option ${idx + 1}",
                    modifier = Modifier.weight(1f),
                    accent = submitColor
                )
                if (rows.size > 2) {
                    IconButton(
                        onClick = {
                            rows.removeAt(idx)
                            onOptionsChange(rows.joinToString("\n"))
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Remove option",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (rows.size < maxOptions) {
                FilledTonalButton(
                    onClick = {
                        rows.add("")
                        onOptionsChange(rows.joinToString("\n"))
                    },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = submitColor.copy(alpha = 0.10f),
                        contentColor = submitColor
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(LocalStrings.current.pollOptionPlaceholder, style = MaterialTheme.typography.labelSmall)
                }
            }
            FilledTonalButton(
                onClick = onSubmit,
                modifier = Modifier.weight(1f).height(32.dp),
                contentPadding = PaddingValues(horizontal = 10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = submitColor.copy(alpha = 0.2f),
                    contentColor = submitColor
                )
            ) {
                Text(submitLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


@Composable
private fun ModPanelButton(
    label: String,
    tint: Color,
    icon: ImageVector? = null,
    iconText: String? = null,
    isToggled: Boolean = false,
    onClick: () -> Unit
) {
    LiquidGlassSurface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
        backgroundAlphaHigh = if (isToggled) 0.18f else 0.04f,
        backgroundAlphaLow = if (isToggled) 0.10f else 0.02f,
        borderAlphaHigh = 0f,
        borderAlphaLow = 0f
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            when {
                icon != null -> Icon(icon, null, modifier = Modifier.size(14.dp), tint = tint)
                iconText != null -> Text(iconText, fontSize = 13.sp, color = tint)
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isToggled) tint else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isToggled) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                fontSize = 11.sp
            )
            if (isToggled) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(tint)
                )
            }
        }
    }
}

@Composable
private fun ModPanelInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    sendColor: Color,
    onSend: () -> Unit,
    sendIcon: ImageVector = Icons.Filled.Send,
    maxLines: Int = 1,
    outerPadding: Boolean = true
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (outerPadding) 12.dp else 0.dp,
                vertical = if (outerPadding) 6.dp else 0.dp
            ),
        verticalAlignment = if (maxLines == 1) Alignment.CenterVertically else Alignment.Top
    ) {
        LiquidGlassSurface(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 40.dp, max = if (maxLines > 1) 96.dp else 40.dp)
                .clip(RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            backgroundAlphaHigh = 0.06f,
            backgroundAlphaLow = 0.03f,
            borderAlphaHigh = 0f,
            borderAlphaLow = 0f
        ) {
            val onSurface = MaterialTheme.colorScheme.onSurface
            val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.bodySmall.copy(color = placeholderColor)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = onSurface),
                    cursorBrush = SolidColor(sendColor),
                    singleLine = maxLines == 1,
                    maxLines = maxLines,
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (maxLines == 1) ImeAction.Send else ImeAction.Default
                    ),
                    keyboardActions = KeyboardActions(onSend = { onSend() })
                )
            }
        }
        Spacer(Modifier.width(6.dp))


        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (value.isNotBlank()) sendColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                .clickable(
                    enabled = value.isNotBlank(),
                    onClick = onSend,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                sendIcon,
                null,
                modifier = Modifier.size(16.dp),
                tint = if (value.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
    }
}


@Composable
private fun PanelSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun PanelDivider() {
    HorizontalDivider(
        color = Color.White.copy(alpha = 0.06f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
}
