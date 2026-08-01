package io.rudione.chatone.presentation.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.domain.model.ChatoneColorTokens

data class ChatFontSettings(
    val fontFamily: FontFamily = FontFamily.Default,
    val fontFamilyName: String = "Default",
    val fontStyle: FontStyle = FontStyle.Normal,
    val textDecoration: TextDecoration? = null,
    val strikethrough: Boolean = false,
    val underline: Boolean = false
)

val LocalChatFont = compositionLocalOf { ChatFontSettings() }

val FirstMessageColor =
    Color(io.rudione.chatone.domain.model.HighlightRule.FIRST_MESSAGE_RULE.color)

object ChatoneColors {

    val Violet50 = Color(0xFFF5F0FF)
    val Violet100 = Color(0xFFE8DEFF)
    val Violet200 = Color(0xFFD0BAFF)
    val Violet300 = Color(0xFFB391FF)
    val Violet400 = Color(0xFF9B6DFF)
    val Violet500 = Color(0xFF7C4DFF)
    val Violet600 = Color(0xFF6B3DE8)
    val Violet700 = Color(0xFF5A2DC8)
    val Violet800 = Color(0xFF4520A0)
    val Violet900 = Color(0xFF2E1570)

    val Cyan50 = Color(0xFFE0FEFF)
    val Cyan100 = Color(0xFFB3FCFF)
    val Cyan200 = Color(0xFF7AF7FF)
    val Cyan300 = Color(0xFF3EF0FF)
    val Cyan400 = Color(0xFF00E5FF)
    val Cyan500 = Color(0xFF00C8E0)
    val Cyan600 = Color(0xFF009DB3)

    val Rose400 = Color(0xFFFF6B8A)
    val Rose500 = Color(0xFFFF4571)

    val Success = Color(0xFF34D399)
    val SuccessDark = Color(0xFF059669)
    val Warning = Color(0xFFFBBF24)
    val WarningDark = Color(0xFFF59E0B)
    val Error = Color(0xFFF87171)
    val ErrorDark = Color(0xFFDC2626)
    val Live = Color(0xFFEB0400)

    val DarkBg = Color(0xFF0A0A0F)
    val DarkSurface = Color(0xFF1E1E28)
    val DarkSurfaceElevated = Color(0xFF1A1A22)
    val DarkSurfaceHighest = Color(0xFF222230)
    val DarkBorder = Color(0xFF2A2A3A)
    val DarkBorderSubtle = Color(0xFF1E1E2C)
    val DarkTextPrimary = Color(0xFFF0F0F5)
    val DarkTextSecondary = Color(0xFFA0A0B8)
    val DarkTextTertiary = Color(0xFF6B6B82)

    val GlassDark = Color(0x1AFFFFFF)
    val GlassBorderDark = Color(0x26FFFFFF)
    val GlassLight = Color(0x33FFFFFF)
    val GlassBorderLight = Color(0x40FFFFFF)

    val LightBg = Color(0xFFF8F8FC)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceElevated = Color(0xFFF2F2F8)
    val LightSurfaceHighest = Color(0xFFEAEAF2)
    val LightBorder = Color(0xFFD8D8E4)
    val LightBorderSubtle = Color(0xFFE8E8F0)
    val LightTextPrimary = Color(0xFF0A0A14)
    val LightTextSecondary = Color(0xFF4A4A60)
    val LightTextTertiary = Color(0xFF8888A0)

    val MentionHighlightDark = Color(0x269B6DFF)
    val MentionHighlightLight = Color(0x1A7C4DFF)

    val ModTimeout = Color(0xFFFBBF24)
    val ModBan = Color(0xFFF87171)
    val ModDelete = Color(0xFFFF8C42)
    val ModUnban = Color(0xFF34D399)
}

data class ChatoneExtraColors(
    val mentionHighlight: Color,
    val deletedMessage: Color,
    val systemMessage: Color,
    val live: Color,
    val modTimeout: Color,
    val modBan: Color,
    val modDelete: Color,
    val modUnban: Color,
    val connected: Color,
    val sidebarSurface: Color,
    val sidebarSelected: Color,
    val chatInputSurface: Color,

    val glassOverlay: Color,
    val glassBorder: Color,

    val shadowColor: Color,
    val cardBorder: Color,
    val elevatedShadow: Color
)

val LocalChatoneColors = staticCompositionLocalOf {
    ChatoneExtraColors(
        mentionHighlight = Color.Transparent,
        deletedMessage = Color.Gray,
        systemMessage = Color.Gray,
        live = ChatoneColors.Live,
        modTimeout = ChatoneColors.ModTimeout,
        modBan = ChatoneColors.ModBan,
        modDelete = ChatoneColors.ModDelete,
        modUnban = ChatoneColors.ModUnban,
        connected = ChatoneColors.Success,
        sidebarSurface = Color.Black,
        sidebarSelected = Color.DarkGray,
        chatInputSurface = Color.DarkGray,
        glassOverlay = Color.Transparent,
        glassBorder = Color.Transparent,
        shadowColor = Color.Black,
        cardBorder = Color.Gray,
        elevatedShadow = Color.Black
    )
}

private val DarkExtraColors = ChatoneExtraColors(
    mentionHighlight = ChatoneColors.MentionHighlightDark,
    deletedMessage = Color(0x30FFFFFF),
    systemMessage = ChatoneColors.DarkTextTertiary,
    live = ChatoneColors.Live,
    modTimeout = ChatoneColors.ModTimeout,
    modBan = ChatoneColors.ModBan,
    modDelete = ChatoneColors.ModDelete,
    modUnban = ChatoneColors.ModUnban,
    connected = ChatoneColors.Success,

    sidebarSurface = Color(0xFF18182A),
    sidebarSelected = Color(0xFF2E2E46),
    chatInputSurface = Color(0xFF1C1C2E),
    glassOverlay = ChatoneColors.GlassDark,
    glassBorder = ChatoneColors.GlassBorderDark,
    shadowColor = Color(0x66000000),
    cardBorder = ChatoneColors.DarkBorderSubtle,
    elevatedShadow = Color(0x40000000)
)

private val LightExtraColors = ChatoneExtraColors(
    mentionHighlight = ChatoneColors.MentionHighlightLight,
    deletedMessage = Color(0x30000000),
    systemMessage = ChatoneColors.LightTextTertiary,
    live = ChatoneColors.Live,
    modTimeout = ChatoneColors.WarningDark,
    modBan = ChatoneColors.ErrorDark,
    modDelete = ChatoneColors.ModDelete,
    modUnban = ChatoneColors.SuccessDark,
    connected = ChatoneColors.SuccessDark,
    sidebarSurface = Color(0xFFF4F4FA),
    sidebarSelected = Color(0xFFE6DCFF),
    chatInputSurface = Color(0xFFF6F6FC),
    glassOverlay = ChatoneColors.GlassLight,
    glassBorder = ChatoneColors.GlassBorderLight,
    shadowColor = Color(0x1A000000),
    cardBorder = ChatoneColors.LightBorderSubtle,
    elevatedShadow = Color(0x14000000)
)

val LocalChatoneColorTokens = staticCompositionLocalOf { ChatoneColorTokens() }

object ChatoneTheme {
    val extraColors: ChatoneExtraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalChatoneColors.current

    val colorTokens: ChatoneColorTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalChatoneColorTokens.current
}

@Composable
fun ChatoneTheme(
    darkTheme: Boolean = true,
    accentColorIndex: Int = 0,
    customTheme: CustomThemeConfig? = null,
    fontSettings: ChatFontSettings = ChatFontSettings(),
    colorTokens: ChatoneColorTokens = ChatoneColorTokens(),
    content: @Composable () -> Unit
) {

    val colorScheme = if (customTheme != null) {
        var base = ColorSchemeGenerator.generateFromSeed(
            customTheme.seedColor,
            true,
            customTheme.contrastLevel
        )
        if (customTheme.customOverrides.isNotEmpty()) {
            base = ColorSchemeGenerator.applyOverrides(base, customTheme.customOverrides)
        }
        base
    } else {
        accentColorScheme(accentPaletteAt(accentColorIndex))
    }

    val extraColors = DarkExtraColors.copy(
        modTimeout = Color(colorTokens.modTimeout),
        modBan = Color(colorTokens.modBan),
        modDelete = Color(colorTokens.modDelete),
        modUnban = Color(colorTokens.modUnban),
        live = Color(colorTokens.live),
        connected = Color(colorTokens.connected),
        sidebarSurface = colorScheme.surfaceContainerLow,
        sidebarSelected = colorScheme.primary.copy(alpha = 0.20f).compositeOver(colorScheme.surfaceContainerLow),
        chatInputSurface = colorScheme.surfaceContainer,
        mentionHighlight = colorScheme.primary.copy(alpha = 0.14f)
    )

    CompositionLocalProvider(
        LocalChatoneColors provides extraColors,
        LocalChatoneColorTokens provides colorTokens,
        LocalChatFont provides fontSettings
    ) {
        val ff = fontSettings.fontFamily
        val typography = if (ff == FontFamily.Default) ChatoneTypography else ChatoneTypography.copy(
            displayLarge  = ChatoneTypography.displayLarge.copy(fontFamily = ff),
            headlineLarge = ChatoneTypography.headlineLarge.copy(fontFamily = ff),
            headlineMedium = ChatoneTypography.headlineMedium.copy(fontFamily = ff),
            headlineSmall = ChatoneTypography.headlineSmall.copy(fontFamily = ff),
            titleLarge    = ChatoneTypography.titleLarge.copy(fontFamily = ff),
            titleMedium   = ChatoneTypography.titleMedium.copy(fontFamily = ff),
            titleSmall    = ChatoneTypography.titleSmall.copy(fontFamily = ff),
            bodyLarge     = ChatoneTypography.bodyLarge.copy(fontFamily = ff),
            bodyMedium    = ChatoneTypography.bodyMedium.copy(fontFamily = ff),
            bodySmall     = ChatoneTypography.bodySmall.copy(fontFamily = ff),
            labelLarge    = ChatoneTypography.labelLarge.copy(fontFamily = ff),
            labelMedium   = ChatoneTypography.labelMedium.copy(fontFamily = ff),
            labelSmall    = ChatoneTypography.labelSmall.copy(fontFamily = ff),
        )
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = ChatoneShapes
        ) {
            CompositionLocalProvider(
                LocalIndication provides ChatoneIndication,
                content = content
            )
        }
    }
}

internal fun Color.compositeOver(background: Color): Color {
    val fg = this
    val a = fg.alpha
    return Color(
        red = fg.red * a + background.red * (1f - a),
        green = fg.green * a + background.green * (1f - a),
        blue = fg.blue * a + background.blue * (1f - a),
        alpha = 1f
    )
}

val ChatoneTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
)

val ChatoneShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)
