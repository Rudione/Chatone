package io.rudione.chatone.presentation.theme.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import io.rudione.chatone.util.i18n.StringsEn

enum class AppLocale(val code: String, val displayName: String) {
    English("en", "English"),
    Russian("ru", "Русский");

    companion object {
        fun fromCode(code: String): AppLocale =
            entries.firstOrNull { it.code == code } ?: English

        val all: List<AppLocale> = entries
    }
}

val LocalStrings = staticCompositionLocalOf<AppStrings> { StringsEn }
