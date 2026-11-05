package io.rudione.chatone.util.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.rudione.chatone.presentation.theme.i18n.LocalAppLocale

expect fun formatGroupedNumber(value: Long, languageTag: String): String

@Composable
fun groupedNumber(value: Long): String {
    val language = LocalAppLocale.current
    return remember(value, language) { formatGroupedNumber(value, language) }
}
