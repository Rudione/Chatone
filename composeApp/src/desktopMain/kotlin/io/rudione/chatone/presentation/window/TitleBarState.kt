package io.rudione.chatone.presentation.window

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import io.rudione.chatone.presentation.settings.TitleBarMode

object TitleBarState {
    var mode by mutableStateOf(TitleBarMode.DARK)
    var themeTopBarColor by mutableStateOf<Color?>(null)
    var isDarkTheme by mutableStateOf(true)

    val captionColor: Color
        get() = resolveTitleBar(mode, isDarkTheme, themeTopBarColor).first

    val captionIsDark: Boolean
        get() = resolveTitleBar(mode, isDarkTheme, themeTopBarColor).second
}
