package io.rudione.chatone.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class CustomThemeConfig(
    val id: String,
    val name: String,
    val seedColor: Int,
    val isDark: Boolean,
    val contrastLevel: Float = 0f,
    val customOverrides: Map<String, Int> = emptyMap(),
    val createdAt: Long = Clock.System.now().toEpochMilliseconds()
)

@Stable
class CustomThemeManager {
    private val _currentTheme = MutableStateFlow<CustomThemeConfig?>(null)
    val currentTheme: StateFlow<CustomThemeConfig?> = _currentTheme.asStateFlow()

    private val _savedThemes = MutableStateFlow<List<CustomThemeConfig>>(emptyList())
    val savedThemes: StateFlow<List<CustomThemeConfig>> = _savedThemes.asStateFlow()

    fun setTheme(theme: CustomThemeConfig?) {
        _currentTheme.value = theme
    }

    fun saveTheme(theme: CustomThemeConfig) {
        val existing = _savedThemes.value.find { it.id == theme.id }
        _savedThemes.value = if (existing != null) {
            _savedThemes.value.map { if (it.id == theme.id) theme else it }
        } else {
            _savedThemes.value + theme
        }
    }

    fun deleteTheme(themeId: String) {
        _savedThemes.value = _savedThemes.value.filter { it.id != themeId }
        if (_currentTheme.value?.id == themeId) {
            _currentTheme.value = null
        }
    }

    fun resetToDefault() {
        _currentTheme.value = null
    }

    fun serialize(): String = Json.encodeToString(_savedThemes.value)

    fun deserialize(json: String) {
        _savedThemes.value = try {
            Json.decodeFromString<List<CustomThemeConfig>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        const val STORAGE_KEY = "custom_themes_v1"
    }
}

val LocalCustomThemeManager = compositionLocalOf<CustomThemeManager> {
    error("CustomThemeManager not provided")
}
