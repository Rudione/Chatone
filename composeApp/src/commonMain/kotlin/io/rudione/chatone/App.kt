package io.rudione.chatone

import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import coil3.compose.setSingletonImageLoaderFactory
import io.rudione.chatone.util.createAnimatedImageLoader
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.rudione.chatone.domain.usecase.GetFirstValidAccountUseCase
import io.rudione.chatone.presentation.auth.AuthScreen
import io.rudione.chatone.presentation.main.MainScreen
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.WallpaperController
import io.rudione.chatone.util.WallpaperLoader
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
    onAlwaysOnTopChanged: (Boolean) -> Unit = {}
) {
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
        // WallpaperLoader comes from Koin — DesktopWallpaperLoader on JVM
        val wallpaperLoader: WallpaperLoader = koinInject()

        LaunchedEffect(settingsState.alwaysOnTop) {
            onAlwaysOnTopChanged(settingsState.alwaysOnTop)
        }

        // Reactively load/clear wallpaper whenever path or blur changes
        LaunchedEffect(settingsState.wallpaperPath, settingsState.wallpaperBlur) {
            if (settingsState.wallpaperPath.isBlank()) {
                wallpaperController.clear()
            } else {
                val loaded = wallpaperLoader.load(
                    path = settingsState.wallpaperPath,
                    blurRadius = settingsState.wallpaperBlur
                )
                if (loaded != null) {
                    wallpaperController.update(loaded)
                } else {
                    // File gone/unreadable — clear stored path too
                    wallpaperController.clear()
                    settingsViewModel.sendEvent(SettingsEvent.OnWallpaperPathChanged(""))
                }
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

        val uiScale = settingsState.uiScale
        CompositionLocalProvider(
            LocalWallpaperController provides wallpaperController,
            LocalDensity provides Density(LocalDensity.current.density * uiScale, LocalDensity.current.fontScale)
        ) {
            ChatoneTheme(darkTheme = isDarkTheme) {
                androidx.compose.foundation.layout.Box(
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
                        Screen.Loading -> {}
                        Screen.Auth -> {
                            AuthScreen(onAuthSuccess = { currentScreen = Screen.Main })
                        }
                        Screen.Main -> {
                            MainScreen(
                                onNavigateToAuth = { currentScreen = Screen.Auth },
                                onThemeChanged = { dark -> isDarkTheme = dark }
                            )
                        }
                    }
                }
            }
        }
    }
}