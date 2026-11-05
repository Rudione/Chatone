package io.rudione.chatone.util.format

import java.text.NumberFormat
import java.util.Locale

actual fun formatGroupedNumber(value: Long, languageTag: String): String =
    NumberFormat.getIntegerInstance(Locale.forLanguageTag(languageTag)).format(value)
