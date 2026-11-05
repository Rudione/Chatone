package io.rudione.chatone.presentation.theme.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.key

expect object LocalAppLocale {
    val current: String
        @Composable get

    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}

@Composable
fun AppLocaleEnvironment(language: String, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppLocale provides language) {
        key(language) { content() }
    }
}

expect fun applyAppLocale(language: String)
