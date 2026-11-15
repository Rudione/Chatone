package io.rudione.chatone

import io.rudione.chatone.presentation.components.LocalStreamerPrivacy
import io.rudione.chatone.data.repository.StreamerModeController
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.foundation.layout.Column
import io.rudione.chatone.presentation.settings.SettingsState
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import io.rudione.chatone.domain.usecase.GetFirstValidAccountUseCase
import io.rudione.chatone.presentation.auth.AuthScreen
import io.rudione.chatone.presentation.chat.ChatMediaSettings
import io.rudione.chatone.presentation.chat.LocalChatMediaSettings
import io.rudione.chatone.presentation.components.AppSystemBars
import io.rudione.chatone.presentation.components.LocalStatusBarOverrides
import io.rudione.chatone.presentation.components.StatusBarOverrides
import io.rudione.chatone.presentation.main.MainScreen
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.settings.AlwaysOnTopMode
import io.rudione.chatone.presentation.settings.TitleBarMode
import io.rudione.chatone.presentation.startup.LaunchReadiness
import io.rudione.chatone.presentation.startup.LaunchSplashOverlay
import io.rudione.chatone.presentation.startup.LaunchStage
import io.rudione.chatone.presentation.theme.ChatFontSettings
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.CustomThemeManager
import io.rudione.chatone.presentation.theme.LocalCustomThemeManager
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.WallpaperController
import io.rudione.chatone.presentation.theme.i18n.LocalAppLocale
import io.rudione.chatone.util.concurrent.IoDispatcher
import io.rudione.chatone.util.font.resolveFontFamilyWithBundled
import io.rudione.chatone.util.media.WallpaperLoader
import kotlinx.coroutines.withContext
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import io.rudione.chatone.data.remote.emote.SevenTvCosmeticsClient
import io.rudione.chatone.data.repository.NicknameRepository
import io.rudione.chatone.data.repository.ThirdPartyBadgeRepository
import io.rudione.chatone.presentation.chat.LocalChatGifsEnabled
import io.rudione.chatone.presentation.chat.LocalNicknames
import io.rudione.chatone.presentation.chat.LocalReadableNickColors
import io.rudione.chatone.domain.entitlements.EntitlementsRepository
import io.rudione.chatone.presentation.chat.ChatoneBadgeAssignment
import io.rudione.chatone.presentation.components.ImagePreviewHost
import io.rudione.chatone.presentation.chat.LocalChatoneBadges
import io.rudione.chatone.presentation.chat.LocalSevenTvCosmetics
import io.rudione.chatone.presentation.chat.LocalSevenTvPaints
import io.rudione.chatone.presentation.chat.LocalThirdPartyBadges
import io.rudione.chatone.presentation.chat.ThirdPartyBadgeMaps
import io.rudione.chatone.presentation.chat.TranslationStore
import io.rudione.chatone.presentation.theme.WallpaperState
import io.rudione.chatone.presentation.theme.topBarBackgroundColor

@Immutable
class AppScreens(
    val auth: @Composable (onAuthenticated: () -> Unit) -> Unit,
    val main: @Composable (onSignedOut: () -> Unit) -> Unit
)

private sealed interface RootRoute {
    data object Loading : RootRoute
    data object Auth : RootRoute
    data object Main : RootRoute
}

private fun MutableList<RootRoute>.replaceWith(route: RootRoute) {
    if (lastOrNull() == route) return
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
    onAlwaysOnTopModeChanged: (AlwaysOnTopMode) -> Unit = {},
    onThemeChanged: ((Boolean) -> Unit)? = null,
    onThemeTopBarColorChanged: ((Color?) -> Unit)? = null,
    onTitleBarModeChanged: ((TitleBarMode) -> Unit)? = null,
    screens: AppScreens? = null,
    windowTitleBar: (@Composable (SettingsState, (SettingsEvent) -> Unit) -> Unit)? = null
) {
    val streamerPrivacy by koinInject<StreamerModeController>().state.collectAsState()
    val customThemeManager: CustomThemeManager = koinInject()
    val activeCustomTheme by customThemeManager.currentTheme.collectAsState()

    SideEffect(Unit) {
        Napier.base(ChatoneAntilog())
    }

    val wallpaperController = remember { WallpaperController() }

    KoinContext {
        var isDarkTheme by remember { mutableStateOf(darkTheme) }
        val backStack = remember { mutableStateListOf<RootRoute>(RootRoute.Loading) }
        val getFirstValidAccount: GetFirstValidAccountUseCase = koinInject()
        val settingsViewModel: SettingsViewModel = koinViewModel()
        val settingsState by settingsViewModel.state.collectAsState()

        val wallpaperLoader: WallpaperLoader = koinInject()

        val translationStore: TranslationStore = koinInject()
        SideEffect(settingsState.translationTargetLang) {
            translationStore.targetLang = settingsState.translationTargetLang
        }

        val nicknameRepository: NicknameRepository = koinInject()
        val nicknames by nicknameRepository.nicknames.collectAsState()

        val thirdPartyBadgeRepository: ThirdPartyBadgeRepository = koinInject()
        val ffzBadges by thirdPartyBadgeRepository.ffzByLogin.collectAsState()
        val bttvBadges by thirdPartyBadgeRepository.bttvByUserId.collectAsState()
        val thirdPartyBadgeMaps = remember(ffzBadges, bttvBadges) {
            ThirdPartyBadgeMaps(ffzBadges, bttvBadges)
        }

        val entitlementsRepository: EntitlementsRepository = koinInject()
        val entitlements by entitlementsRepository.manifest.collectAsState()
        val chatoneBadges = remember(entitlements) {
            val badgesById = entitlements?.badges.orEmpty().associateBy { it.id }
            entitlements?.users.orEmpty().mapNotNull { user ->
                val equipped = user.equippedBadgeId ?: user.badgeIds.singleOrNull()
                val badge = equipped?.takeIf { it in user.badgeIds }?.let(badgesById::get)
                badge?.let { user.twitchUserId to ChatoneBadgeAssignment(user, it) }
            }.toMap()
        }

        val sevenTvCosmeticsClient: SevenTvCosmeticsClient =
            koinInject()
        val sevenTvCosmetics = sevenTvCosmeticsClient.cosmetics.collectAsState()
        val sevenTvPaints = sevenTvCosmeticsClient.paints.collectAsState()

        SideEffect(settingsState.alwaysOnTop) {
            onAlwaysOnTopChanged(settingsState.alwaysOnTop)
        }

        SideEffect(settingsState.alwaysOnTopMode) {
            onAlwaysOnTopModeChanged(settingsState.alwaysOnTopMode)
        }

        SideEffect(settingsState.titleBarMode) {
            onTitleBarModeChanged?.invoke(settingsState.titleBarMode)
        }

        SideEffect(Unit) {
            wallpaperController.setDisplayConfig(settingsState.wallpaperDisplayConfig)
        }

        LaunchedEffect(settingsState.wallpaperPath) {
            if (settingsState.wallpaperPath.isBlank()) {

                wallpaperController.update(
                    WallpaperState(
                        displayConfig = wallpaperController.state.displayConfig
                    )
                )
            } else {
                val loaded = withContext(IoDispatcher) {
                    wallpaperLoader.load(settingsState.wallpaperPath)
                }
                if (loaded != null) {

                    wallpaperController.update(
                        loaded.copy(displayConfig = wallpaperController.state.displayConfig)
                    )
    
                } else {
                    wallpaperController.update(
                        WallpaperState(
                            displayConfig = wallpaperController.state.displayConfig
                        )
                    )
                    settingsViewModel.sendEvent(SettingsEvent.OnWallpaperPathChanged(""))
                }
            }
        }

        val wallpaper by remember { derivedStateOf { wallpaperController.state } }

        var customThemesRestored by remember { mutableStateOf(false) }
        val savedCustomThemes by customThemeManager.savedThemes.collectAsState()
        LaunchedEffect(customThemesRestored, savedCustomThemes, activeCustomTheme) {
            if (!customThemesRestored) return@LaunchedEffect
            settingsViewModel.sendEvent(SettingsEvent.OnCustomThemesJsonChanged(customThemeManager.serialize()))
            settingsViewModel.sendEvent(SettingsEvent.OnActiveCustomThemeIdChanged(activeCustomTheme?.id))
        }

        val launchReadiness: LaunchReadiness = koinInject()
        LaunchedEffect(Unit) {
            val themes = settingsState.customThemes
            if (themes.isNotEmpty()) {
                themes.forEach { customThemeManager.saveTheme(it) }
                settingsState.activeCustomThemeId?.let { id ->
                    themes.find { it.id == id }?.let { customThemeManager.setTheme(it) }
                }
            }
            customThemesRestored = true
            val route = try {
                if (getFirstValidAccount() != null) RootRoute.Main else RootRoute.Auth
            } catch (e: Exception) {
                Napier.w("Auto-login check failed: ${e.message}", tag = "App")
                RootRoute.Auth
            }
            backStack.replaceWith(route)
            if (route == RootRoute.Main) launchReadiness.advance(LaunchStage.Connecting)
            else launchReadiness.finish()
        }

        val uiScale = settingsState.uiScale
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
        val statusBarOverrides = remember { StatusBarOverrides() }
        CompositionLocalProvider(
            LocalStatusBarOverrides provides statusBarOverrides,
            LocalStreamerPrivacy provides streamerPrivacy,
            LocalAppLocale provides settingsState.language,
            LocalWallpaperController provides wallpaperController,
            LocalCustomThemeManager provides customThemeManager,
            LocalNicknames provides nicknames,
            LocalThirdPartyBadges provides thirdPartyBadgeMaps,
            LocalChatGifsEnabled provides settingsState.showChatGifs,
            LocalReadableNickColors provides settingsState.readableNickColors,
            LocalChatMediaSettings provides ChatMediaSettings(
                linkOpenMode = settingsState.linkOpenMode,
                showInlineImages = settingsState.showInlineImages,
                inlineImageMaxHeight = settingsState.inlineImageMaxHeight,
                clipPreviewWidth = settingsState.clipPreviewWidth
            ),
            LocalChatoneBadges provides chatoneBadges,
            LocalSevenTvCosmetics provides sevenTvCosmetics,
            LocalSevenTvPaints provides sevenTvPaints,
            LocalDensity provides Density(
                LocalDensity.current.density * uiScale,
                LocalDensity.current.fontScale
            )
        ) {
            key(settingsState.language) {
                ChatoneTheme(
                    darkTheme = true,
                    accentColorIndex = settingsState.accentColorIndex,
                    customTheme = activeCustomTheme,
                    fontSettings = chatFontSettings,
                    colorTokens = settingsState.colorTokens
                ) {
                    AppSystemBars(
                        lightBackground = MaterialTheme.colorScheme.background.luminance() > 0.5f
                    )
                    val resolvedTopBarColor = topBarBackgroundColor(
                        wallpaper,
                        MaterialTheme.colorScheme.surfaceContainer
                    )
                    val captionBackground = resolvedTopBarColor.compositeOver(MaterialTheme.colorScheme.background)
                    LaunchedEffect(captionBackground) {
                        onThemeTopBarColorChanged?.invoke(captionBackground)
                    }
                    ImagePreviewHost {
                        Column(Modifier.fillMaxSize()) {
                            windowTitleBar?.invoke(settingsState, settingsViewModel::sendEvent)
                            Box(
                                modifier = Modifier
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
                                val currentRoute = backStack.lastOrNull() ?: RootRoute.Loading
                                AnimatedContent(
                                    targetState = currentRoute,
                                    transitionSpec = {
                                        fadeIn(tween(180)) togetherWith fadeOut(tween(180))
                                    },
                                    label = "root-route"
                                ) { route ->
                                    when (route) {
                                        RootRoute.Loading -> Box(Modifier.fillMaxSize())
                                        RootRoute.Auth -> if (screens != null) {
                                            screens.auth { backStack.replaceWith(RootRoute.Main) }
                                        } else {
                                            AuthScreen(
                                                onAuthSuccess = { backStack.replaceWith(RootRoute.Main) },
                                                language = settingsState.language,
                                                onLanguageChange = { code ->
                                                    settingsViewModel.sendEvent(SettingsEvent.OnLanguageChanged(code))
                                                }
                                            )
                                        }

                                        RootRoute.Main -> if (screens != null) {
                                            screens.main { backStack.replaceWith(RootRoute.Auth) }
                                        } else {
                                            MainScreen(
                                                onNavigateToAuth = { backStack.replaceWith(RootRoute.Auth) },
                                                onThemeChanged = { dark ->
                                                    isDarkTheme = dark
                                                    onThemeChanged?.invoke(dark)
                                                }
                                            )
                                        }
                                    }
                                }
                                LaunchSplashOverlay()
                            }
                        }
                    }
                }
            }
        }
    }
}
