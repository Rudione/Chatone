package io.rudione.chatone

import androidx.compose.runtime.*
import coil3.compose.setSingletonImageLoaderFactory
import io.rudione.chatone.util.createAnimatedImageLoader
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.rudione.chatone.domain.usecase.GetFirstValidAccountUseCase
import io.rudione.chatone.presentation.auth.AuthScreen
import io.rudione.chatone.presentation.main.MainScreen
import io.rudione.chatone.presentation.theme.ChatoneTheme
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

sealed class Screen {
    object Loading : Screen()
    object Auth : Screen()
    object Main : Screen()
}

@Composable
fun App(
    darkTheme: Boolean = true
) {
    LaunchedEffect(Unit) {
        Napier.base(DebugAntilog())
    }

    setSingletonImageLoaderFactory { context ->
        createAnimatedImageLoader(context)
    }

    KoinContext {
        var isDarkTheme by remember { mutableStateOf(darkTheme) }
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Loading) }
        val getFirstValidAccount: GetFirstValidAccountUseCase = koinInject()

        // Auto-skip auth if a saved account exists
        LaunchedEffect(Unit) {
            try {
                val account = getFirstValidAccount()
                currentScreen = if (account != null) Screen.Main else Screen.Auth
            } catch (e: Exception) {
                Napier.w("Auto-login check failed: ${e.message}", tag = "App")
                currentScreen = Screen.Auth
            }
        }

        ChatoneTheme(darkTheme = isDarkTheme) {
            when (currentScreen) {
                Screen.Loading -> {
                    // Splash / loading handled by theme background
                }
                Screen.Auth -> {
                    AuthScreen(
                        onAuthSuccess = {
                            currentScreen = Screen.Main
                        }
                    )
                }
                Screen.Main -> {
                    MainScreen(
                        onNavigateToAuth = {
                            currentScreen = Screen.Auth
                        },
                        onThemeChanged = { dark ->
                            isDarkTheme = dark
                        }
                    )
                }
            }
        }
    }
}
