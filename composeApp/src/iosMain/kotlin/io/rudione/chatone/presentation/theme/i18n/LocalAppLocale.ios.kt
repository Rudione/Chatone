package io.rudione.chatone.presentation.theme.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

actual object LocalAppLocale {
    private const val LANGUAGES_KEY = "AppleLanguages"
    private val systemDefault: String = (NSLocale.preferredLanguages.firstOrNull() as? String)?.substringBefore('-') ?: "en"
    private val language = staticCompositionLocalOf { systemDefault }

    actual val current: String
        @Composable get() = language.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val defaults = NSUserDefaults.standardUserDefaults
        if (value == null) defaults.removeObjectForKey(LANGUAGES_KEY) else defaults.setObject(listOf(value), LANGUAGES_KEY)
        return language.provides(value ?: systemDefault)
    }
}

actual fun applyAppLocale(language: String) {
    NSUserDefaults.standardUserDefaults.setObject(listOf(language), "AppleLanguages")
}
