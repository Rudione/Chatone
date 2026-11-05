package io.rudione.chatone.presentation.theme.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

actual object LocalAppLocale {
    private var systemDefault: Locale? = null
    private val language = staticCompositionLocalOf { Locale.getDefault().language }

    actual val current: String
        @Composable get() = language.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val base = systemDefault ?: Locale.getDefault().also { systemDefault = it }
        val locale = value?.let { Locale.forLanguageTag(it) } ?: base
        Locale.setDefault(locale)
        return language.provides(locale.language)
    }
}

actual fun applyAppLocale(language: String) {
    Locale.setDefault(Locale.forLanguageTag(language))
}
