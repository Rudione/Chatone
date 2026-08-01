package io.rudione.chatone.util.font

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.inter_bold
import chatone.composeapp.generated.resources.inter_italic
import chatone.composeapp.generated.resources.inter_medium
import chatone.composeapp.generated.resources.inter_regular
import chatone.composeapp.generated.resources.inter_semibold
import org.jetbrains.compose.resources.Font

const val BUNDLED_FONT_INTER = "Inter"

@Composable
fun bundledInterFontFamily(): FontFamily = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal, FontStyle.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium, FontStyle.Normal),
    Font(Res.font.inter_semibold, FontWeight.SemiBold, FontStyle.Normal),
    Font(Res.font.inter_bold, FontWeight.Bold, FontStyle.Normal),
    Font(Res.font.inter_italic, FontWeight.Normal, FontStyle.Italic)
)

@Composable
fun resolveFontFamilyWithBundled(name: String, customPaths: List<String>): FontFamily =
    if (name == BUNDLED_FONT_INTER) bundledInterFontFamily() else resolveFontFamily(name, customPaths)
