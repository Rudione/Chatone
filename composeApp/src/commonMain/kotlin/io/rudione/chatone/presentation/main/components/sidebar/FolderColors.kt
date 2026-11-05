package io.rudione.chatone.presentation.main.components.sidebar

import androidx.compose.ui.graphics.Color
import io.rudione.chatone.domain.model.DEFAULT_FOLDER_COLOR_HEX
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.folder_color_blue
import chatone.composeapp.generated.resources.folder_color_gray
import chatone.composeapp.generated.resources.folder_color_green
import chatone.composeapp.generated.resources.folder_color_orange
import chatone.composeapp.generated.resources.folder_color_purple
import chatone.composeapp.generated.resources.folder_color_red
import chatone.composeapp.generated.resources.folder_color_yellow
import org.jetbrains.compose.resources.StringResource

data class FolderColorOption(
    val hex: String,
    val color: Color,
    val label: StringResource
)

object FolderColors {
    const val DEFAULT_HEX = DEFAULT_FOLDER_COLOR_HEX

    val options: List<FolderColorOption> = listOf(
        FolderColorOption(DEFAULT_HEX, Color(0xFF9B6DFF), Res.string.folder_color_purple),
        FolderColorOption("#FF6B6B", Color(0xFFFF6B6B), Res.string.folder_color_red),
        FolderColorOption("#FF9F43", Color(0xFFFF9F43), Res.string.folder_color_orange),
        FolderColorOption("#FFD93D", Color(0xFFFFD93D), Res.string.folder_color_yellow),
        FolderColorOption("#34D399", Color(0xFF34D399), Res.string.folder_color_green),
        FolderColorOption("#4A9DFF", Color(0xFF4A9DFF), Res.string.folder_color_blue),
        FolderColorOption("#9AA0AE", Color(0xFF9AA0AE), Res.string.folder_color_gray)
    )

    fun normalize(hex: String): String =
        options.firstOrNull { it.hex.equals(hex, ignoreCase = true) }?.hex ?: DEFAULT_HEX
}
