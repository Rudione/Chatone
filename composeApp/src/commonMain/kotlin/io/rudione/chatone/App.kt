package io.rudione.chatone

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import coil3.compose.setSingletonImageLoaderFactory
import com.russhwolf.settings.Settings
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import io.rudione.chatone.domain.usecase.GetFirstValidAccountUseCase
import io.rudione.chatone.presentation.auth.AuthScreen
import io.rudione.chatone.presentation.loading.LoadingScreen
import io.rudione.chatone.presentation.main.MainScreen
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.settings.TitleBarMode
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.ChatFontSettings
import io.rudione.chatone.presentation.theme.CustomThemeManager
import io.rudione.chatone.presentation.theme.LocalCustomThemeManager
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.WallpaperController
import io.rudione.chatone.util.media.WallpaperLoader
import io.rudione.chatone.util.media.createAnimatedImageLoader
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.font.resolveFontFamilyWithBundled
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private data object LoadingRoute
private data object AuthRoute
private data object MainRoute

private fun MutableList<Any>.replaceWith(route: Any) {
    clear()
    add(route)
}

private class ChatoneAntilog : Antilog() {
    override fun isEnable(priority: LogLevel, tag: String?): Boolean =
        priority > LogLevel.VERBOSE

    override fun performLog(priority: LogLevel, tag: String?, throwable: Throwable?, message: String?) {
        val level = when (priority) {
            LogLevel.DEBUG -> "D"
            LogLevel.INFO -> "I"
            LogLevel.WARNING -> "W"
            LogLevel.ERROR -> "E"
            LogLevel.ASSERT -> "A"
            else -> "V"
        }
        val line = buildString {
            append("[").append(level).append("]")
            if (!tag.isNullOrBlank()) append(" ").append(tag)
            if (!message.isNullOrBlank()) append(": ").append(message)
        }
        println(line)
        throwable?.printStackTrace()
    }
}

@Composable
fun App(
    darkTheme: Boolean = true,
    onAlwaysOnTopChanged: (Boolean) -> Unit = {},
    onThemeChanged: ((Boolean) -> Unit)? = null,
    onDominantColorChanged: ((Color?) -> Unit)? = null,
    onTitleBarModeChanged: ((TitleBarMode) -> Unit)? = null
) {
    val customThemeManager: CustomThemeManager = koinInject()
    val activeCustomTheme by customThemeManager.currentTheme.collectAsState()

    val settings = Settings()

    LaunchedEffect(Unit) {
        Napier.base(ChatoneAntilog())
    }

    setSingletonImageLoaderFactory { context ->
        createAnimatedImageLoader(context)
    }

    val wallpaperController = remember { WallpaperController() }

    KoinContext {
        var isDarkTheme by remember { mutableStateOf(darkTheme) }
        val backStack = remember { mutableStateListOf<Any>(LoadingRoute) }
        val getFirstValidAccount: GetFirstValidAccountUseCase = koinInject()
        val settingsViewModel: SettingsViewModel = koinViewModel()
        val settingsState by settingsViewModel.state.collectAsState()

        val wallpaperLoader: WallpaperLoader = koinInject()

        val translationStore: io.rudione.chatone.presentation.chat.TranslationStore = koinInject()
        LaunchedEffect(settingsState.translationTargetLang) {
            translationStore.targetLang = settingsState.translationTargetLang
        }

        val nicknameRepository: io.rudione.chatone.data.repository.NicknameRepository = koinInject()
        val nicknames by nicknameRepository.nicknames.collectAsState()

        val thirdPartyBadgeRepository: io.rudione.chatone.data.repository.ThirdPartyBadgeRepository = koinInject()
        val ffzBadges by thirdPartyBadgeRepository.ffzByLogin.collectAsState()
        val bttvBadges by thirdPartyBadgeRepository.bttvByUserId.collectAsState()
        val chatoneBadges by thirdPartyBadgeRepository.chatoneByUserId.collectAsState()
        val thirdPartyBadgeMaps = remember(ffzBadges, bttvBadges, chatoneBadges) {
            io.rudione.chatone.presentation.chat.ThirdPartyBadgeMaps(ffzBadges, bttvBadges, chatoneBadges)
        }

        LaunchedEffect(settingsState.alwaysOnTop) {
            onAlwaysOnTopChanged(settingsState.alwaysOnTop)
        }

        LaunchedEffect(settingsState.titleBarMode) {
            onTitleBarModeChanged?.invoke(settingsState.titleBarMode)
        }

        LaunchedEffect(Unit) {
            val saved = settingsState.wallpaperDisplayConfig
            wallpaperController.setDisplayConfig(saved)
        }

        LaunchedEffect(settingsState.wallpaperPath, settingsState.wallpaperBlur) {
            if (settingsState.wallpaperPath.isBlank()) {

                wallpaperController.update(
                    io.rudione.chatone.presentation.theme.WallpaperState(
                        displayConfig = wallpaperController.state.displayConfig
                    )
                )
                onDominantColorChanged?.invoke(null)
            } else {
                val loaded = wallpaperLoader.load(
                    path = settingsState.wallpaperPath,
                    blurRadius = settingsState.wallpaperBlur
                )
                if (loaded != null) {

                    wallpaperController.update(
                        loaded.copy(displayConfig = wallpaperController.state.displayConfig)
                    )
                    onDominantColorChanged?.invoke(loaded.dominantColor)
                } else {
                    wallpaperController.update(
                        io.rudione.chatone.presentation.theme.WallpaperState(
                            displayConfig = wallpaperController.state.displayConfig
                        )
                    )
                    onDominantColorChanged?.invoke(null)
                    settingsViewModel.sendEvent(SettingsEvent.OnWallpaperPathChanged(""))
                }
            }
        }

        val wallpaper by remember { derivedStateOf { wallpaperController.state } }
        LaunchedEffect(wallpaper.dominantColor, wallpaper.isActive) {
            if (wallpaper.isActive) {
                onDominantColorChanged?.invoke(wallpaper.dominantColor)
            } else {
                onDominantColorChanged?.invoke(null)
            }
        }

        LaunchedEffect(customThemeManager.savedThemes.value, activeCustomTheme) {
            settingsViewModel.sendEvent(
                SettingsEvent.OnCustomThemesJsonChanged(customThemeManager.serialize())
            )
            customThemeManager.currentTheme.value?.let {
                settingsViewModel.sendEvent(SettingsEvent.OnActiveCustomThemeIdChanged(it.id))
            }
        }

        LaunchedEffect(Unit) {
            try {
                val account = getFirstValidAccount()
                backStack.replaceWith(if (account != null) MainRoute else AuthRoute)
            } catch (e: Exception) {
                Napier.w("Auto-login check failed: ${e.message}", tag = "App")
                backStack.replaceWith(AuthRoute)
            }
        }

        LaunchedEffect(Unit) {
            val themes = settingsState.customThemes
            if (themes.isNotEmpty()) {
                themes.forEach { customThemeManager.saveTheme(it) }
                settingsState.activeCustomThemeId?.let { id ->
                    themes.find { it.id == id }?.let { customThemeManager.setTheme(it) }
                }
            }
        }

        val uiScale = settingsState.uiScale
        val currentStrings = remember(settingsState.language) {
            AppStrings.forLocale(settingsState.language)
        }
        val resolvedChatFontFamily = resolveFontFamilyWithBundled(
            settingsState.fontFamilyName, settingsState.customFontPaths
        )
        val chatFontSettings = remember(
            settingsState.fontFamilyName,
            settingsState.fontStyleItalic,
            settingsState.fontUnderline,
            settingsState.fontStrikethrough,
            settingsState.customFontPaths,
            resolvedChatFontFamily
        ) {
            val ff = resolvedChatFontFamily
            val dec = when {
                settingsState.fontUnderline && settingsState.fontStrikethrough ->
                    TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                settingsState.fontUnderline -> TextDecoration.Underline
                settingsState.fontStrikethrough -> TextDecoration.LineThrough
                else -> null
            }
            ChatFontSettings(
                fontFamily = ff,
                fontFamilyName = settingsState.fontFamilyName,
                fontStyle = if (settingsState.fontStyleItalic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = dec,
                underline = settingsState.fontUnderline,
                strikethrough = settingsState.fontStrikethrough
            )
        }
        CompositionLocalProvider(
            LocalStrings provides currentStrings,
            LocalWallpaperController provides wallpaperController,
            LocalCustomThemeManager provides customThemeManager,
            io.rudione.chatone.presentation.chat.LocalNicknames provides nicknames,
            io.rudione.chatone.presentation.chat.LocalThirdPartyBadges provides thirdPartyBadgeMaps,
            LocalDensity provides Density(
                LocalDensity.current.density * uiScale,
                LocalDensity.current.fontScale
            )
        ) {
            ChatoneTheme(
                darkTheme = true,
                accentColorIndex = settingsState.accentColorIndex,
                customTheme = activeCustomTheme,
                fontSettings = chatFontSettings,
                colorTokens = settingsState.colorTokens
            ) {
                Box(
                    modifier = androidx.compose.ui.Modifier
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.type == PointerEventType.Scroll) {
                                        val isCtrl = event.keyboardModifiers.isCtrlPressed
                                        if (isCtrl) {
                                            val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                            val newScale = (uiScale - delta * 0.05f).coerceIn(0.7f, 2.0f)
                                            settingsViewModel.sendEvent(SettingsEvent.OnUiScaleChanged(newScale))
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = {},
                        entryProvider = entryProvider {
                            entry<LoadingRoute> { LoadingScreen() }
                            entry<AuthRoute> {
                                AuthScreen(onAuthSuccess = { backStack.replaceWith(MainRoute) })
                            }
                            entry<MainRoute> {
                                MainScreen(
                                    onNavigateToAuth = { backStack.replaceWith(AuthRoute) },
                                    onThemeChanged = { dark ->
                                        isDarkTheme = dark
                                        onThemeChanged?.invoke(dark)
                                    }
                                )
                            }
                        }
                    )
                    if (backStack.lastOrNull() == MainRoute) {
                        io.rudione.chatone.presentation.ai.AiAssistantOverlay()
                    }
                }
            }
        }
    }
}
