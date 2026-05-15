package io.rudione.chatone

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import coil3.compose.setSingletonImageLoaderFactory
import com.russhwolf.settings.Settings
import io.github.aakira.napier.DebugAntilog
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
import io.rudione.chatone.util.WallpaperLoader
import io.rudione.chatone.util.createAnimatedImageLoader
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.resolveFontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

sealed class Screen {
    object Loading : Screen()
    object Auth : Screen()
    object Main : Screen()
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
        Napier.base(DebugAntilog())
    }

    setSingletonImageLoaderFactory { context ->
        createAnimatedImageLoader(context)
    }

    val wallpaperController = remember { WallpaperController() }

    KoinContext {
        var isDarkTheme by remember { mutableStateOf(darkTheme) }
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Loading) }
        val getFirstValidAccount: GetFirstValidAccountUseCase = koinInject()
        val settingsViewModel: SettingsViewModel = koinViewModel()
        val settingsState by settingsViewModel.state.collectAsState()

        val wallpaperLoader: WallpaperLoader = koinInject()


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
                currentScreen = if (account != null) Screen.Main else Screen.Auth
            } catch (e: Exception) {
                Napier.w("Auto-login check failed: ${e.message}", tag = "App")
                currentScreen = Screen.Auth
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
        val chatFontSettings = remember(
            settingsState.fontFamilyName,
            settingsState.fontStyleItalic,
            settingsState.fontUnderline,
            settingsState.fontStrikethrough,
            settingsState.customFontPaths
        ) {
            val ff = resolveFontFamily(settingsState.fontFamilyName, settingsState.customFontPaths)
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
            LocalDensity provides Density(
                LocalDensity.current.density * uiScale,
                LocalDensity.current.fontScale
            )
        ) {
            ChatoneTheme(
                darkTheme = true,
                accentColorIndex = settingsState.accentColorIndex,
                customTheme = activeCustomTheme,
                fontSettings = chatFontSettings
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
                    when (currentScreen) {
                        Screen.Loading -> LoadingScreen()
                        Screen.Auth -> {
                            AuthScreen(onAuthSuccess = { currentScreen = Screen.Main })
                        }
                        Screen.Main -> {
                            MainScreen(
                                onNavigateToAuth = { currentScreen = Screen.Auth },
                                onThemeChanged = { dark ->
                                    isDarkTheme = dark
                                    onThemeChanged?.invoke(dark)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}