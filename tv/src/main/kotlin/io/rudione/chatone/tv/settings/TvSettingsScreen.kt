package io.rudione.chatone.tv.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.section_chat
import chatone.composeapp.generated.resources.settings_about
import chatone.composeapp.generated.resources.settings_account
import chatone.composeapp.generated.resources.settings_appearance
import chatone.composeapp.generated.resources.tv_nav_settings
import chatone.composeapp.generated.resources.tv_settings_player
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.stream.StreamPlayerHost
import io.rudione.chatone.tv.home.TvScreenTitle
import io.rudione.chatone.tv.theme.TvDimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private enum class TvSettingsSection(val label: StringResource) {
    PLAYER(Res.string.tv_settings_player),
    CHAT(Res.string.section_chat),
    APPEARANCE(Res.string.settings_appearance),
    ACCOUNT(Res.string.settings_account),
    ABOUT(Res.string.settings_about)
}


@Composable
fun TvSettingsScreen(
    host: StreamPlayerHost?,
    account: TwitchAccount?,
    hasExtendedRights: Boolean,
    onConfirmRights: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: SettingsViewModel = koinViewModel()
    val settings by viewModel.state.collectAsState()
    var section by rememberSaveable { mutableStateOf(TvSettingsSection.PLAYER) }
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }
    Column(modifier
        .fillMaxSize()
        .padding(top = TvDimens.ScreenVertical)) {
        TvScreenTitle(stringResource(Res.string.tv_nav_settings))
        Row(Modifier
            .fillMaxSize()
            .padding(top = 16.dp)) {
            LazyColumn(
                Modifier
                    .width(240.dp)
                    .fillMaxHeight()
                    .focusRestorer(),
                contentPadding = PaddingValues(start = TvDimens.ScreenHorizontal, end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(TvSettingsSection.entries, key = { it.name }) { item ->
                    ListItem(
                        selected = item == section,
                        onClick = { section = item },
                        modifier = Modifier
                            .then(if (item.ordinal == 0) Modifier.focusRequester(firstFocus) else Modifier)
                            .onFocusChanged { if (it.isFocused) section = item },
                        headlineContent = { Text(stringResource(item.label)) }
                    )
                }
            }
            Box(Modifier
                .weight(1f)
                .fillMaxHeight()) {
                when (section) {
                    TvSettingsSection.PLAYER -> TvPlayerSettings(host)
                    TvSettingsSection.CHAT -> TvChatSettings(settings, viewModel::sendEvent, host)
                    TvSettingsSection.APPEARANCE -> TvAppearanceSettings(
                        settings,
                        viewModel::sendEvent
                    )

                    TvSettingsSection.ACCOUNT -> TvAccountSettings(
                        account = account,
                        hasExtendedRights = hasExtendedRights,
                        onConfirmRights = onConfirmRights,
                        onSignIn = onSignIn,
                        onLogout = { viewModel.sendEvent(SettingsEvent.OnLogoutClicked) }
                    )

                    TvSettingsSection.ABOUT -> TvAboutSettings(
                        onClearCache = { viewModel.sendEvent(SettingsEvent.OnClearCacheClicked) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun TvSettingsList(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .focusRestorer(),
        contentPadding = PaddingValues(
            end = TvDimens.ScreenHorizontal,
            bottom = TvDimens.ScreenVertical
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content
    )
}

@Composable
internal fun TvSwitchRow(
    title: String,
    description: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    ListItem(
        selected = false,
        onClick = { onChange(!checked) },
        headlineContent = { Text(title) },
        supportingContent = description?.let { { Text(it, maxLines = 2) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) }
    )
}

@Composable
internal fun <T> TvCycleRow(
    title: String,
    value: String,
    options: List<T>,
    current: T,
    onChange: (T) -> Unit,
    description: String? = null
) {
    ListItem(
        selected = false,
        onClick = {
            val index = options.indexOf(current)
            onChange(options[(index + 1).mod(options.size)])
        },
        headlineContent = { Text(title) },
        supportingContent = description?.let { { Text(it, maxLines = 2) } },
        trailingContent = {
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    )
}
