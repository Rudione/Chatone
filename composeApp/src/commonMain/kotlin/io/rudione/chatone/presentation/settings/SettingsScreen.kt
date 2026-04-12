package io.rudione.chatone.presentation.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.chatbubbles
import chatone.composeapp.generated.resources.chatbubbles_outline
import chatone.composeapp.generated.resources.chevronright
import chatone.composeapp.generated.resources.folder_outline
import chatone.composeapp.generated.resources.images
import chatone.composeapp.generated.resources.images_outline
import chatone.composeapp.generated.resources.keyboard_24_filled
import chatone.composeapp.generated.resources.keyboard_24_regular
import chatone.composeapp.generated.resources.musical_notes_outline
import chatone.composeapp.generated.resources.palette_fill_16
import chatone.composeapp.generated.resources.palette_stroke_12
import chatone.composeapp.generated.resources.panel_left_key_16_regular
import chatone.composeapp.generated.resources.shield_checkmark_outline
import chatone.composeapp.generated.resources.shield_checkmark_sharp
import chatone.composeapp.generated.resources.unfold_more
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.model.HighlightRule
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.util.NotificationSoundPlayer
import io.rudione.chatone.util.WallpaperLoader
import io.rudione.chatone.util.pickAudioFile
import io.rudione.chatone.util.pickImageFile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private enum class SettingsSection(
    val label: String,
    val icon: DrawableResource,
    val iconOutlined: DrawableResource? = null
) {
    APPEARANCE("Appearance", Res.drawable.palette_fill_16, Res.drawable.palette_stroke_12),
    CHAT("Chat", Res.drawable.chatbubbles, Res.drawable.chatbubbles_outline),
    NOTIFICATIONS("Notifications", Res.drawable.musical_notes_outline, null),
    HIGHLIGHTS("Highlights", Res.drawable.images, Res.drawable.images_outline),
    BACKGROUND("Background", Res.drawable.images, Res.drawable.images_outline),
    HOTKEYS("Hotkeys", Res.drawable.keyboard_24_filled, Res.drawable.keyboard_24_regular),
    MODERATION(
        "Moderation",
        Res.drawable.shield_checkmark_sharp,
        Res.drawable.shield_checkmark_outline
    ),
    ABOUT("About", Res.drawable.panel_left_key_16_regular, null),
}


@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onThemeChanged: (Boolean) -> Unit,
    isWideScreen: Boolean = false,
    modifier: Modifier = Modifier,
    wallpaperLoader: WallpaperLoader = koinInject(),
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    if (isWideScreen) {
        Dialog(
            onDismissRequest = onNavigateBack,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            SettingsDialogContent(
                state = state,
                onNavigateBack = onNavigateBack,
                onThemeChanged = onThemeChanged,
                viewModel = viewModel
            )
        }
    } else {
        SettingsFullScreen(
            state = state,
            onNavigateBack = onNavigateBack,
            onThemeChanged = onThemeChanged,
            modifier = modifier,
            viewModel = viewModel
        )
    }
}


@Composable
private fun SettingsDialogContent(
    state: SettingsState,
    onNavigateBack: () -> Unit,
    onThemeChanged: (Boolean) -> Unit,
    viewModel: SettingsViewModel
) {
    var selectedSection by remember { mutableStateOf(SettingsSection.APPEARANCE) }
    val extra = ChatoneTheme.extraColors

    Surface(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .fillMaxHeight(0.86f),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 32.dp
    ) {
        Row(modifier = Modifier.fillMaxSize()) {


            Column(
                modifier = Modifier
                    .width(216.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            listOf(extra.sidebarSurface, extra.sidebarSurface.copy(alpha = 0.95f))
                        )
                    )
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            color = extra.cardBorder,
                            topLeft = Offset(size.width - 1.dp.toPx(), 0f),
                            size = Size(1.dp.toPx(), size.height)
                        )
                    }
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Chatone",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(color = extra.cardBorder)
                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SettingsSection.entries.forEach { section ->
                        SidebarNavItem(
                            section = section,
                            isSelected = selectedSection == section,
                            onClick = { selectedSection = section }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = extra.cardBorder)
                TextButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Close")
                }
            }



            SectionContentLazy(
                section = selectedSection,
                state = state,
                onThemeChanged = onThemeChanged,
                viewModel = viewModel,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsFullScreen(
    state: SettingsState,
    onNavigateBack: () -> Unit,
    onThemeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel
) {
    var expandedSections by remember { mutableStateOf(setOf<SettingsSection>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection.entries.forEach { section ->
                val isExpanded = section in expandedSections


                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedSections = if (isExpanded)
                                expandedSections - section
                            else
                                expandedSections + section
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painterResource(
                            if (isExpanded) section.icon
                            else (section.iconOutlined ?: section.icon)
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isExpanded) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        section.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isExpanded) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isExpanded) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (isExpanded) Icons.Filled.KeyboardArrowDown
                        else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = if (isExpanded) 0f else -90f },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))


                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(tween(200)) + fadeIn(tween(150)),
                    exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
                ) {
                    SectionContentColumn(
                        section = section,
                        state = state,
                        onThemeChanged = onThemeChanged,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isExpanded) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}


@Composable
private fun SidebarNavItem(section: SettingsSection, isSelected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
        tween(150), label = "nav_bg"
    )
    val contentColor =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            painterResource(Res.drawable.chatbubbles),
            null,
            modifier = Modifier.size(18.dp),
            tint = contentColor
        )
        Text(
            section.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Box(
                modifier = Modifier.size(5.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}


@Composable
private fun SectionContentLazy(
    section: SettingsSection,
    state: SettingsState,
    onThemeChanged: (Boolean) -> Unit,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text(
                section.label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 18.dp)
            )
        }
        when (section) {
            SettingsSection.APPEARANCE -> appearanceLazyItems(state, onThemeChanged, viewModel)
            SettingsSection.CHAT -> chatLazyItems(state, viewModel)
            SettingsSection.NOTIFICATIONS -> notificationLazyItems(state, viewModel)
            SettingsSection.HIGHLIGHTS -> highlightLazyItems(state, viewModel)
            SettingsSection.BACKGROUND -> backgroundLazyItems(state, viewModel)
            SettingsSection.HOTKEYS -> hotkeyLazyItems(state, viewModel)
            SettingsSection.MODERATION -> moderationLazyItems(state, viewModel)
            SettingsSection.ABOUT -> aboutLazyItems()
        }
    }
}


@Composable
private fun SectionContentColumn(
    section: SettingsSection,
    state: SettingsState,
    onThemeChanged: (Boolean) -> Unit,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        when (section) {
            SettingsSection.APPEARANCE -> AppearanceContent(state, onThemeChanged, viewModel)
            SettingsSection.CHAT -> ChatContent(state, viewModel)
            SettingsSection.NOTIFICATIONS -> NotificationContent(state, viewModel)
            SettingsSection.HIGHLIGHTS -> HighlightContent(state, viewModel)
            SettingsSection.BACKGROUND -> BackgroundContent(state, viewModel)
            SettingsSection.HOTKEYS -> HotkeyContent(state, viewModel)
            SettingsSection.MODERATION -> ModerationContent(state, viewModel)
            SettingsSection.ABOUT -> AboutContent()
        }
    }
}


private fun LazyListScope.appearanceLazyItems(
    state: SettingsState,
    onThemeChanged: (Boolean) -> Unit,
    vm: SettingsViewModel
) {
    item {
        SettingsGroup("Theme") {
            SwitchRow("Dark Theme", "Use dark color scheme", state.darkTheme) {
                vm.sendEvent(SettingsEvent.OnDarkThemeChanged(it)); onThemeChanged(it)
            }
        }
    }
    item {
        SettingsGroup("Display") {
            ListRow(
                "Font Size", state.fontSize.name.lowercase().replaceFirstChar { it.uppercase() },
                SettingsState.FontSize.entries.map {
                    it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                }
            ) { vm.sendEvent(SettingsEvent.OnFontSizeChanged(SettingsState.FontSize.entries[it])) }
            UiScaleRow(state.uiScale) { vm.sendEvent(SettingsEvent.OnUiScaleChanged(it)) }
            UiScaleRow(state.uiScale) { vm.sendEvent(SettingsEvent.OnUiScaleChanged(it)) }
            RowDivider()
            ListRow(
                "Emote Size", state.emoteSize.name.lowercase().replaceFirstChar { it.uppercase() },
                SettingsState.EmoteSize.entries.map {
                    it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                }
            ) { vm.sendEvent(SettingsEvent.OnEmoteSizeChanged(SettingsState.EmoteSize.entries[it])) }
            RowDivider()
            ListRow(
                "Channel Navigation",
                when (state.channelNavigation) {
                    SettingsState.ChannelNavigation.TAB_BAR -> "Tab Bar"
                    SettingsState.ChannelNavigation.MINI_RAIL -> "Mini Rail"
                    SettingsState.ChannelNavigation.BOTH -> "Both"
                },
                listOf("Tab Bar", "Mini Rail", "Both")
            ) { vm.sendEvent(SettingsEvent.OnChannelNavigationChanged(SettingsState.ChannelNavigation.entries[it])) }
        }
    }
    item {
        SettingsGroup("Window") {
            SwitchRow("Always on Top", "Keep window above other windows", state.alwaysOnTop) {
                vm.sendEvent(SettingsEvent.OnAlwaysOnTopChanged(it))
            }
        }
    }
}

private fun LazyListScope.chatLazyItems(state: SettingsState, vm: SettingsViewModel) {
    item {
        SettingsGroup("Messages") {
            SwitchRow(
                "Show Timestamps", "Display message time in chat",
                state.timestampFormat != SettingsState.TimestampFormat.OFF
            ) { enabled ->
                vm.sendEvent(
                    SettingsEvent.OnTimestampFormatChanged(
                        if (enabled) SettingsState.TimestampFormat.H24 else SettingsState.TimestampFormat.OFF
                    )
                )
            }
            if (state.timestampFormat != SettingsState.TimestampFormat.OFF) {
                RowDivider()
                ListRow(
                    "Timestamp Format",
                    when (state.timestampFormat) {
                        SettingsState.TimestampFormat.H12 -> "12-hour"
                        SettingsState.TimestampFormat.H24 -> "24-hour"
                        else -> "Off"
                    },
                    listOf("12-hour", "24-hour")
                ) {
                    vm.sendEvent(
                        SettingsEvent.OnTimestampFormatChanged(
                            if (it == 0) SettingsState.TimestampFormat.H12 else SettingsState.TimestampFormat.H24
                        )
                    )
                }
            }
            RowDivider()
            SwitchRow("Show Badges", "Display user badges in chat", state.showBadges) {
                vm.sendEvent(SettingsEvent.OnShowBadgesChanged(it))
            }
            RowDivider()
            SwitchRow(
                "Show Deleted Messages",
                "Show deleted messages as grayed out",
                state.showDeletedMessages
            ) {
                vm.sendEvent(SettingsEvent.OnShowDeletedChanged(it))
            }
        }
    }
    item {
        SettingsGroup("Auto-scroll") {
            SwitchRow(
                "Pause on Hover",
                "Stop auto-scrolling when mouse is over chat",
                state.pauseOnHover
            ) {
                vm.sendEvent(SettingsEvent.OnPauseOnHoverChanged(it))
            }
        }
    }
    item {
        SettingsGroup("Emote Picker") {
            SwitchRow(
                "Close on Mouse Leave",
                "Hide emote picker when cursor leaves it",
                state.closeEmotePickerOnMouseLeave
            ) {
                vm.sendEvent(SettingsEvent.OnCloseEmotePickerOnMouseLeaveChanged(it))
            }
        }
    }
    item {
        SettingsGroup("History") {
            SliderRow(
                "Message History Limit", state.scrollbackLimit, 100f..2000f, 18,
                "${state.scrollbackLimit} messages"
            ) {
                vm.sendEvent(SettingsEvent.OnScrollbackLimitChanged(it.toInt()))
            }
        }
    }
}

private fun LazyListScope.notificationLazyItems(state: SettingsState, vm: SettingsViewModel) {
    item { NotificationGroupCard(state, vm) }
    if (state.mentionSoundEnabled) {
        item { CustomSoundCard(state, vm) }
    }
}

private fun LazyListScope.highlightLazyItems(state: SettingsState, vm: SettingsViewModel) {
    item {
        Text(
            "Rules matched against incoming messages. Your username is always highlighted.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
    items(state.highlightRules, key = { it.id }) { rule ->
        HightlightRuleCard(
            rule = rule,
            onToggle = { vm.sendEvent(SettingsEvent.OnHighlightRuleToggled(rule.id, it)) },
            onSoundToggle = {
                vm.sendEvent(
                    SettingsEvent.OnHighlightRuleSoundToggled(
                        rule.id,
                        it
                    )
                )
            },
            onRemove = if (!rule.id.startsWith("custom_")) null else {
                { vm.sendEvent(SettingsEvent.OnRemoveHighlightRule(rule.id)) }
            }
        )
        Spacer(Modifier.height(4.dp))
    }
    item {
        var newPattern by remember { mutableStateOf("") }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newPattern,
                onValueChange = { newPattern = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add highlight pattern...") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
            FilledTonalButton(
                onClick = {
                    if (newPattern.isNotBlank()) {
                        vm.sendEvent(SettingsEvent.OnAddHighlightRule(newPattern.trim()))
                        newPattern = ""
                    }
                },
                enabled = newPattern.isNotBlank()
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun LazyListScope.backgroundLazyItems(state: SettingsState, vm: SettingsViewModel) {
    item { BackgroundCard(state, vm) }
}

private fun LazyListScope.hotkeyLazyItems(state: SettingsState, vm: SettingsViewModel) {
    item {
        SettingsGroup("Chat Controls") {
            HotkeyRow("Pause Auto-scroll", "Hotkey to pause chat scrolling", state.pauseHotkey) {
                vm.sendEvent(SettingsEvent.OnPauseHotkeyChanged(it))
            }
            DropdownRow(
                label = "Pause Mode",
                description = if (state.pauseHotkeyMode == PauseHotkeyMode.HOLD) "Hold key to pause, release to resume" else "Press to toggle pause on/off",
                options = PauseHotkeyMode.entries.map { it.name },
                selected = state.pauseHotkeyMode.ordinal
            ) { idx ->
                vm.sendEvent(SettingsEvent.OnPauseHotkeyModeChanged(PauseHotkeyMode.entries[idx]))
            }
        }
    }
    item {
        SettingsGroup("Image Links") {
            DropdownRow(
                label = "Show inline images",
                description = "Preview image links (imgur, kappa, etc.) in chat",
                options = listOf("On", "Off", "Blur"),
                selected = state.showInlineImages.ordinal
            ) { idx ->
                vm.sendEvent(SettingsEvent.OnShowInlineImagesChanged(InlineImageMode.entries[idx]))
            }
            if (state.showInlineImages != InlineImageMode.OFF) {
                SliderRow(
                    label = "Image max height",
                    value = state.inlineImageMaxHeight.toFloat(),
                    valueRange = 50f..500f,
                    steps = 8,
                    valueLabel = "${state.inlineImageMaxHeight}px"
                ) { vm.sendEvent(SettingsEvent.OnInlineImageMaxHeightChanged(it.toInt())) }
            }
        }
    }
}

private fun LazyListScope.moderationLazyItems(state: SettingsState, vm: SettingsViewModel) {
    item {
        ModerationSettingsSection(
            state = state,
            onEvent = { vm.sendEvent(it) }
        )
    }
}

private fun LazyListScope.aboutLazyItems() {
    item { AboutCard() }
}


@Composable
private fun AppearanceContent(
    state: SettingsState,
    onThemeChanged: (Boolean) -> Unit,
    vm: SettingsViewModel
) {
    SettingsGroup("Theme") {
        SwitchRow("Dark Theme", "Use dark color scheme", state.darkTheme) {
            vm.sendEvent(SettingsEvent.OnDarkThemeChanged(it)); onThemeChanged(it)
        }
    }
    SettingsGroup("Display") {
        ListRow(
            "Font Size", state.fontSize.name.lowercase().replaceFirstChar { it.uppercase() },
            SettingsState.FontSize.entries.map {
                it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
            }
        ) { vm.sendEvent(SettingsEvent.OnFontSizeChanged(SettingsState.FontSize.entries[it])) }
        UiScaleRow(state.uiScale) { vm.sendEvent(SettingsEvent.OnUiScaleChanged(it)) }
        RowDivider()
        ListRow(
            "Emote Size", state.emoteSize.name.lowercase().replaceFirstChar { it.uppercase() },
            SettingsState.EmoteSize.entries.map {
                it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
            }
        ) { vm.sendEvent(SettingsEvent.OnEmoteSizeChanged(SettingsState.EmoteSize.entries[it])) }
        RowDivider()
        ListRow(
            "Channel Navigation",
            when (state.channelNavigation) {
                SettingsState.ChannelNavigation.TAB_BAR -> "Tab Bar"
                SettingsState.ChannelNavigation.MINI_RAIL -> "Mini Rail"
                SettingsState.ChannelNavigation.BOTH -> "Both"
            },
            listOf("Tab Bar", "Mini Rail", "Both")
        ) { vm.sendEvent(SettingsEvent.OnChannelNavigationChanged(SettingsState.ChannelNavigation.entries[it])) }
    }
    SettingsGroup("Window") {
        SwitchRow("Always on Top", "Keep window above other windows", state.alwaysOnTop) {
            vm.sendEvent(SettingsEvent.OnAlwaysOnTopChanged(it))
        }
    }
}

@Composable
private fun ChatContent(state: SettingsState, vm: SettingsViewModel) {
    SettingsGroup("Messages") {
        SwitchRow(
            "Show Timestamps", "Display message time in chat",
            state.timestampFormat != SettingsState.TimestampFormat.OFF
        ) { enabled ->
            vm.sendEvent(
                SettingsEvent.OnTimestampFormatChanged(
                    if (enabled) SettingsState.TimestampFormat.H24 else SettingsState.TimestampFormat.OFF
                )
            )
        }
        if (state.timestampFormat != SettingsState.TimestampFormat.OFF) {
            RowDivider()
            ListRow(
                "Timestamp Format",
                when (state.timestampFormat) {
                    SettingsState.TimestampFormat.H12 -> "12-hour"
                    SettingsState.TimestampFormat.H24 -> "24-hour"
                    else -> "Off"
                },
                listOf("12-hour", "24-hour")
            ) {
                vm.sendEvent(
                    SettingsEvent.OnTimestampFormatChanged(
                        if (it == 0) SettingsState.TimestampFormat.H12 else SettingsState.TimestampFormat.H24
                    )
                )
            }
        }
        RowDivider()
        SwitchRow("Show Badges", "Display user badges in chat", state.showBadges) {
            vm.sendEvent(SettingsEvent.OnShowBadgesChanged(it))
        }
        RowDivider()
        SwitchRow(
            "Show Deleted Messages",
            "Show deleted messages as grayed out",
            state.showDeletedMessages
        ) {
            vm.sendEvent(SettingsEvent.OnShowDeletedChanged(it))
        }
    }
    SettingsGroup("Auto-scroll") {
        SwitchRow(
            "Pause on Hover",
            "Stop auto-scrolling when mouse is over chat",
            state.pauseOnHover
        ) {
            vm.sendEvent(SettingsEvent.OnPauseOnHoverChanged(it))
        }
    }
    SettingsGroup("Emote Picker") {
        SwitchRow(
            "Close on Mouse Leave",
            "Hide emote picker when cursor leaves it",
            state.closeEmotePickerOnMouseLeave
        ) {
            vm.sendEvent(SettingsEvent.OnCloseEmotePickerOnMouseLeaveChanged(it))
        }
    }
    SettingsGroup("History") {
        SliderRow(
            "Message History Limit", state.scrollbackLimit, 100f..2000f, 18,
            "${state.scrollbackLimit} messages"
        ) {
            vm.sendEvent(SettingsEvent.OnScrollbackLimitChanged(it.toInt()))
        }
    }
}

@Composable
private fun NotificationContent(state: SettingsState, vm: SettingsViewModel) {
    NotificationGroupCard(state, vm)
    if (state.mentionSoundEnabled) {
        Spacer(Modifier.height(8.dp))
        CustomSoundCard(state, vm)
    }
}

@Composable
private fun HighlightContent(state: SettingsState, vm: SettingsViewModel) {
    Text(
        "Rules matched against incoming messages. Your username is always highlighted.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 12.dp)
    )
    state.highlightRules.forEach { rule ->
        HightlightRuleCard(
            rule = rule,
            onToggle = { vm.sendEvent(SettingsEvent.OnHighlightRuleToggled(rule.id, it)) },
            onSoundToggle = {
                vm.sendEvent(
                    SettingsEvent.OnHighlightRuleSoundToggled(
                        rule.id,
                        it
                    )
                )
            },
            onRemove = if (!rule.id.startsWith("custom_")) null else {
                { vm.sendEvent(SettingsEvent.OnRemoveHighlightRule(rule.id)) }
            }
        )
        Spacer(Modifier.height(4.dp))
    }
    var newPattern by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = newPattern,
            onValueChange = { newPattern = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Add highlight pattern...") },
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )
        FilledTonalButton(
            onClick = {
                if (newPattern.isNotBlank()) {
                    vm.sendEvent(SettingsEvent.OnAddHighlightRule(newPattern.trim()))
                    newPattern = ""
                }
            },
            enabled = newPattern.isNotBlank()
        ) {
            Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun BackgroundContent(state: SettingsState, vm: SettingsViewModel) {
    BackgroundCard(state, vm)
}

@Composable
private fun HotkeyContent(state: SettingsState, vm: SettingsViewModel) {
    SettingsGroup("Chat Controls") {
        HotkeyRow("Pause Auto-scroll", "Hotkey to pause chat scrolling", state.pauseHotkey) {
            vm.sendEvent(SettingsEvent.OnPauseHotkeyChanged(it))
        }
        DropdownRow(
            label = "Pause Mode",
            description = if (state.pauseHotkeyMode == PauseHotkeyMode.HOLD) "Hold key to pause, release to resume" else "Press to toggle pause on/off",
            options = PauseHotkeyMode.entries.map { it.name },
            selected = state.pauseHotkeyMode.ordinal
        ) { idx ->
            vm.sendEvent(SettingsEvent.OnPauseHotkeyModeChanged(PauseHotkeyMode.entries[idx]))
        }
    }
    SettingsGroup("Image Links") {
        DropdownRow(
            label = "Show inline images",
            description = "Preview image links in chat",
            options = listOf("On", "Off", "Blur"),
            selected = state.showInlineImages.ordinal
        ) { idx ->
            vm.sendEvent(SettingsEvent.OnShowInlineImagesChanged(InlineImageMode.entries[idx]))
        }
        if (state.showInlineImages != InlineImageMode.OFF) {
            SliderRow(
                label = "Image max height",
                value = state.inlineImageMaxHeight.toFloat(),
                valueRange = 50f..500f,
                steps = 8,
                valueLabel = "${state.inlineImageMaxHeight}px"
            ) { vm.sendEvent(SettingsEvent.OnInlineImageMaxHeightChanged(it.toInt())) }
        }
    }
}

@Composable
private fun ModerationContent(state: SettingsState, vm: SettingsViewModel) {
    ModerationSettingsSection(
        state = state,
        onEvent = { vm.sendEvent(it) }
    )
}

@Composable
private fun AboutContent() {
    AboutCard()
}


@Composable
private fun NotificationGroupCard(state: SettingsState, vm: SettingsViewModel) {
    LiquidGlassSurface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(16.dp),
        backgroundAlphaHigh = 0.80f,
        backgroundAlphaLow = 0.65f,
        borderAlphaHigh = 0f,
        borderAlphaLow = 0f
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Mention Sound",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enable Mention Sound", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Play sound when you are mentioned",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.mentionSoundEnabled,
                    onCheckedChange = { vm.sendEvent(SettingsEvent.OnMentionSoundChanged(it)) })
            }
            if (state.mentionSoundEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Volume",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${(state.mentionSoundVolume * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = state.mentionSoundVolume,
                        onValueChange = { vm.sendEvent(SettingsEvent.OnMentionSoundVolumeChanged(it)) },
                        valueRange = 0f..1f
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomSoundCard(state: SettingsState, vm: SettingsViewModel) {
    LiquidGlassSurface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(16.dp),
        backgroundAlphaHigh = 0.80f,
        backgroundAlphaLow = 0.65f,
        borderAlphaHigh = 0f,
        borderAlphaLow = 0f
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Custom Sound",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (state.customMentionSoundPath.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painterResource(Res.drawable.musical_notes_outline),
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            state.customMentionSoundPath.substringAfterLast('/')
                                .substringAfterLast('\\'),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = {
                            vm.sendEvent(
                                SettingsEvent.OnCustomMentionSoundPathChanged(
                                    ""
                                )
                            )
                        }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Filled.Close,
                                null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledIconButton(
                    onClick = {
                        val picked = pickAudioFile(); if (picked != null) vm.sendEvent(
                        SettingsEvent.OnCustomMentionSoundPathChanged(picked)
                    )
                    },
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        if (state.customMentionSoundPath.isBlank()) "Browse..." else "Change...",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                FilledIconButton(
                    onClick = {
                        NotificationSoundPlayer.playMentionSound(
                            volume = state.mentionSoundVolume,
                            customSoundPath = state.customMentionSoundPath
                        )
                    },
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) { Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp)) }
            }
            Text(
                text = if (state.customMentionSoundPath.isBlank()) "Using default tone. Select WAV or OGG." else "Supported: WAV, OGG",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun BackgroundCard(state: SettingsState, vm: SettingsViewModel) {
    SettingsGroup("Chat Background Image") {
        Column(modifier = Modifier.padding(16.dp)) {
            val scope = rememberCoroutineScope()
            if (state.wallpaperPath.isNotBlank()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = state.wallpaperPath, contentDescription = "Background preview",
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(Color.Black.copy(alpha = state.wallpaperBlur / 40f))
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            if (state.wallpaperPath.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(Res.drawable.images),
                            null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            state.wallpaperPath.substringAfterLast("/").substringAfterLast("\\"),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = { vm.sendEvent(SettingsEvent.OnWallpaperPathChanged("")) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Overlay Opacity", style = MaterialTheme.typography.bodyMedium)
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "${state.wallpaperBlur.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Slider(
                value = state.wallpaperBlur,
                onValueChange = { vm.sendEvent(SettingsEvent.OnWallpaperBlurChanged(it)) },
                valueRange = 0f..40f,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Transparent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Opaque",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val picked = pickImageFile(); if (picked != null) vm.sendEvent(
                        SettingsEvent.OnWallpaperPathChanged(
                            picked
                        )
                    )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painterResource(Res.drawable.images_outline),
                    null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (state.wallpaperPath.isBlank()) "Choose background image..." else "Change image...")
            }
            if (state.wallpaperPath.isBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Supports JPG, PNG, WebP.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AboutCard() {
    SettingsGroup("App Info") {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "C",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Chatone",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Version 1.0.10",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "TG",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "https://t.me/rudionee",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}


@Composable
private fun SettingsGroup(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.6.sp,
                modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ListRow(
    title: String,
    value: String,
    options: List<String>,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                painterResource(Res.drawable.unfold_more),
                null,
                modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { i, opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = { onSelected(i); expanded = false })
            }
        }
    }
}

@Composable
private fun DropdownRow(
    label: String,
    description: String,
    options: List<String>,
    selected: Int,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                options.getOrElse(selected) { "" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { i, opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = { onSelected(i); expanded = false })
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                valueLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, steps = steps)
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    isFloat: Boolean = false,
    onFloatChange: ((Float) -> Unit)? = null,
    onValueChange: ((Float) -> Unit)? = null
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                valueLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (isFloat && onFloatChange != null) {
            Slider(
                value = value / 100f,
                onValueChange = onFloatChange,
                valueRange = valueRange,
                steps = steps
            )
        } else if (onValueChange != null) {
            Slider(
                value = value.toFloat(),
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps
            )
        }
    }
}

@Composable
private fun HightlightRuleCard(
    rule: HighlightRule,
    onToggle: (Boolean) -> Unit,
    onSoundToggle: (Boolean) -> Unit,
    onRemove: (() -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(rule.color)))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    rule.pattern.ifEmpty {
                        rule.id.replace("_", " ").replaceFirstChar { it.uppercase() }
                    },
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium
                )
                if (rule.isRegex) Text(
                    "Regex",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { onSoundToggle(!rule.playSound) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    if (rule.playSound) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = if (rule.playSound) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = rule.enabled, onCheckedChange = onToggle)
            if (onRemove != null) {
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun HotkeyRow(
    title: String,
    subtitle: String,
    currentHotkey: String,
    onHotkeyChanged: (String) -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (isRecording) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                    1.dp,
                    if (isRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.widthIn(min = 110.dp)
                    .onKeyEvent { event ->
                        if (!isRecording) return@onKeyEvent false
                        if (event.type != KeyEventType.KeyDown) return@onKeyEvent true

                        if (event.key in listOf(
                                Key.CtrlLeft,
                                Key.CtrlRight,
                                Key.MetaLeft,
                                Key.MetaRight
                            )
                        ) {
                            if (!event.isAltPressed && !event.isShiftPressed) {
                                onHotkeyChanged("ctrl"); isRecording = false; return@onKeyEvent true
                            }
                        }
                        if (event.key in listOf(Key.AltLeft, Key.AltRight)) {
                            val parts2 = mutableListOf<String>()
                            if (event.isCtrlPressed || event.isMetaPressed) parts2.add("ctrl")
                            parts2.add("alt")
                            if (event.isShiftPressed) parts2.add("shift")
                            onHotkeyChanged(parts2.joinToString("+")); isRecording =
                                false; return@onKeyEvent true
                        }
                        if (event.key in listOf(Key.ShiftLeft, Key.ShiftRight)) {
                            if (!event.isAltPressed && !(event.isCtrlPressed || event.isMetaPressed)) {
                                onHotkeyChanged("shift"); isRecording =
                                    false; return@onKeyEvent true
                            }
                        }
                        if (event.key == Key.Escape) {
                            onHotkeyChanged(""); isRecording = false; return@onKeyEvent true
                        }
                        val parts = mutableListOf<String>()
                        if (event.isCtrlPressed || event.isMetaPressed) parts.add("ctrl")
                        if (event.isAltPressed) parts.add("alt")
                        if (event.isShiftPressed) parts.add("shift")
                        val keyName = hotkeyKeyToName(event.key)
                        if (keyName.isNotEmpty()) parts.add(keyName)
                        onHotkeyChanged(parts.joinToString("+"))
                        isRecording = false; true
                    }
                    .clickable { isRecording = true }
            ) {
                Text(
                    when {
                        isRecording -> "Recording..."; currentHotkey.isBlank() -> "Not set"; else -> currentHotkey.uppercase()
                        .replace("+", " + ")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        isRecording -> MaterialTheme.colorScheme.onPrimaryContainer; currentHotkey.isBlank() -> MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.45f
                        ); else -> MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
            if (currentHotkey.isNotBlank() || isRecording) {
                IconButton(
                    onClick = { onHotkeyChanged(""); isRecording = false },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}


private fun hotkeyKeyToName(key: Key): String = when (key) {
    Key.Spacebar -> "space"; Key.Enter -> "enter"; Key.Tab -> "tab"
    Key.Backspace -> "backspace"; Key.Delete -> "delete"
    Key.MoveHome -> "home"; Key.MoveEnd -> "end"
    Key.PageUp -> "pageup"; Key.PageDown -> "pagedown"
    Key.DirectionUp -> "up"; Key.DirectionDown -> "down"
    Key.DirectionLeft -> "left"; Key.DirectionRight -> "right"
    Key.F1 -> "f1"; Key.F2 -> "f2"; Key.F3 -> "f3"; Key.F4 -> "f4"
    Key.F5 -> "f5"; Key.F6 -> "f6"; Key.F7 -> "f7"; Key.F8 -> "f8"
    Key.F9 -> "f9"; Key.F10 -> "f10"; Key.F11 -> "f11"; Key.F12 -> "f12"
    else -> {
        val code = key.keyCode
        when {
            code in 65L..90L -> ('A' + (code - 65).toInt()).lowercaseChar().toString()
            code in 48L..57L -> (code - 48).toString()
            else -> ""
        }
    }
}


@Composable
private fun UiScaleRow(currentScale: Float, onScaleChanged: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("UI Scale", style = MaterialTheme.typography.bodyLarge)
            Text(
                "${(currentScale * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Slider(
            value = currentScale,
            onValueChange = onScaleChanged,
            valueRange = 0.7f..2.0f,
            steps = 12,
            modifier = Modifier.width(180.dp)
        )
    }
}

private fun formatDuration(seconds: Int): String = when {
    seconds < 60 -> "$seconds seconds"
    seconds < 3600 -> "${seconds / 60} minute${if (seconds / 60 > 1) "s" else ""}"
    seconds < 86400 -> "${seconds / 3600} hour${if (seconds / 3600 > 1) "s" else ""}"
    else -> "${seconds / 86400} day${if (seconds / 86400 > 1) "s" else ""}"
}