package io.rudione.chatone.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.screenchat.screenChatFloatsOverFullScreen
import io.rudione.chatone.presentation.settings.components.AboutCard
import io.rudione.chatone.presentation.settings.components.ActionsSection
import io.rudione.chatone.presentation.settings.components.AiAssistantConfigCard
import io.rudione.chatone.presentation.settings.components.AiAutoModCard
import io.rudione.chatone.presentation.settings.components.BackgroundCard
import io.rudione.chatone.presentation.settings.components.BackupCard
import io.rudione.chatone.presentation.settings.components.ChatCommandsSection
import io.rudione.chatone.presentation.settings.components.CustomSoundCard
import io.rudione.chatone.presentation.settings.components.MentionAlertsCard
import io.rudione.chatone.presentation.settings.components.NotificationGroupCard
import io.rudione.chatone.presentation.settings.components.SettingsPair
import io.rudione.chatone.presentation.settings.components.ScreenChatSettingsContent
import io.rudione.chatone.presentation.settings.sections.AccountContent
import io.rudione.chatone.presentation.settings.sections.AppearanceContent
import io.rudione.chatone.presentation.settings.sections.ChatContent
import io.rudione.chatone.presentation.settings.sections.HighlightContent
import io.rudione.chatone.presentation.settings.sections.HotkeyContent
import io.rudione.chatone.presentation.settings.sections.MediaContent
import io.rudione.chatone.presentation.settings.sections.ModerationContent
import io.rudione.chatone.presentation.settings.sections.isPhoneFormFactor

@Composable
internal fun SettingsSectionContent(
    section: SettingsSection,
    state: SettingsState,
    viewModel: SettingsViewModel,
    onOpenThemeCreator: (seedColor: Int?) -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = 4.dp
) {
    val onEvent: (SettingsEvent) -> Unit = { viewModel.sendEvent(it) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing)) {
        when (section) {
            SettingsSection.APPEARANCE -> AppearanceContent(state, viewModel, onOpenThemeCreator)
            SettingsSection.CHAT -> ChatContent(state, viewModel)
            SettingsSection.SCREEN_CHAT -> ScreenChatSettingsContent(state.screenChat, screenChatFloatsOverFullScreen) {
                onEvent(SettingsEvent.OnScreenChatChanged(it))
            }
            SettingsSection.MEDIA -> MediaContent(state, viewModel)
            SettingsSection.NOTIFICATIONS -> NotificationContent(state, viewModel)
            SettingsSection.HIGHLIGHTS -> HighlightContent(state, viewModel)
            SettingsSection.BACKGROUND -> BackgroundCard(state, viewModel)
            SettingsSection.HOTKEYS -> HotkeyContent(state, viewModel)
            SettingsSection.COMMANDS -> ChatCommandsSection(state = state, onEvent = onEvent)
            SettingsSection.ACTIONS -> ActionsSection(state = state, onEvent = onEvent)
            SettingsSection.MODERATION -> ModerationContent(state, onEvent)
            SettingsSection.AI -> SettingsPair(first = { AiAssistantConfigCard() }, second = { AiAutoModCard() })
            SettingsSection.ACCOUNT -> AccountContent(viewModel)
            SettingsSection.ABOUT -> SettingsPair(first = { BackupCard(viewModel) }, second = { AboutCard() })
        }
    }
}

@Composable
private fun NotificationContent(state: SettingsState, viewModel: SettingsViewModel) {
    if (isPhoneFormFactor) MentionAlertsCard()
    SettingsPair(
        first = { NotificationGroupCard(state, viewModel) },
        second = if (state.mentionSoundEnabled) {
            { CustomSoundCard(state, viewModel) }
        } else null
    )
}
