package io.rudione.chatone.util

import androidx.compose.ui.text.font.FontFamily

expect fun resolveFontFamily(name: String, customPaths: List<String> = emptyList()): FontFamily

expect fun listAvailableFontNames(customPaths: List<String> = emptyList()): List<String>
