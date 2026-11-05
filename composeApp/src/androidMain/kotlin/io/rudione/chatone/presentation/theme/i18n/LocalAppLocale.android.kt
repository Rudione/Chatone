package io.rudione.chatone.presentation.theme.i18n

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

actual object LocalAppLocale {
    private var systemDefault: Locale? = null

    actual val current: String
        @Composable get() = LocalConfiguration.current.locales[0].language

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val base = systemDefault ?: Locale.getDefault().also { systemDefault = it }
        val locale = value?.let { Locale.forLanguageTag(it) } ?: base
        Locale.setDefault(locale)
        val configuration = Configuration(LocalConfiguration.current).apply { setLocale(locale) }
        val resources = LocalContext.current.resources
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)
        return LocalConfiguration.provides(configuration)
    }
}

actual fun applyAppLocale(language: String) {
    Locale.setDefault(Locale.forLanguageTag(language))
}
