package io.rudione.chatone.util.format

import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle

actual fun formatGroupedNumber(value: Long, languageTag: String): String {
    val formatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
        locale = NSLocale(localeIdentifier = languageTag)
        maximumFractionDigits = 0u
    }
    return formatter.stringFromNumber(NSNumber(longLong = value)) ?: value.toString()
}
