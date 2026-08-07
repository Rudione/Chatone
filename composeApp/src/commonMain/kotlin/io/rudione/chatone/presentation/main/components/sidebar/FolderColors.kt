package io.rudione.chatone.presentation.main.components.sidebar

import androidx.compose.ui.graphics.Color
import io.rudione.chatone.presentation.theme.i18n.AppStrings

data class FolderColorOption(
    val hex: String,
    val color: Color,
    val label: (AppStrings) -> String
)

object FolderColors {
    const val DEFAULT_HEX = "#9B6DFF"

    val options: List<FolderColorOption> = listOf(
        FolderColorOption(DEFAULT_HEX, Color(0xFF9B6DFF)) { it.folderColorPurple },
        FolderColorOption("#FF6B6B", Color(0xFFFF6B6B)) { it.folderColorRed },
        FolderColorOption("#FF9F43", Color(0xFFFF9F43)) { it.folderColorOrange },
        FolderColorOption("#FFD93D", Color(0xFFFFD93D)) { it.folderColorYellow },
        FolderColorOption("#34D399", Color(0xFF34D399)) { it.folderColorGreen },
        FolderColorOption("#4A9DFF", Color(0xFF4A9DFF)) { it.folderColorBlue },
        FolderColorOption("#9AA0AE", Color(0xFF9AA0AE)) { it.folderColorGray }
    )

    fun normalize(hex: String): String =
        options.firstOrNull { it.hex.equals(hex, ignoreCase = true) }?.hex ?: DEFAULT_HEX
}
