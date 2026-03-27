package io.rudione.chatone.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Brand Colors ───────────────────────────────────────────────────────
object ChatoneColors {
    // Primary — Rich indigo-violet
    val Violet50 = Color(0xFFF5F0FF)
    val Violet100 = Color(0xFFE8DEFF)
    val Violet200 = Color(0xFFD0BAFF)
    val Violet300 = Color(0xFFB391FF)
    val Violet400 = Color(0xFF9B6DFF)
    val Violet500 = Color(0xFF7C4DFF) // Primary
    val Violet600 = Color(0xFF6B3DE8)
    val Violet700 = Color(0xFF5A2DC8)
    val Violet800 = Color(0xFF4520A0)
    val Violet900 = Color(0xFF2E1570)

    // Accent — Electric cyan
    val Cyan50 = Color(0xFFE0FEFF)
    val Cyan100 = Color(0xFFB3FCFF)
    val Cyan200 = Color(0xFF7AF7FF)
    val Cyan300 = Color(0xFF3EF0FF)
    val Cyan400 = Color(0xFF00E5FF) // Accent
    val Cyan500 = Color(0xFF00C8E0)
    val Cyan600 = Color(0xFF009DB3)

    // Warm accent — Coral/rose for notifications
    val Rose400 = Color(0xFFFF6B8A)
    val Rose500 = Color(0xFFFF4571)

    // Semantic
    val Success = Color(0xFF34D399)
    val SuccessDark = Color(0xFF059669)
    val Warning = Color(0xFFFBBF24)
    val WarningDark = Color(0xFFF59E0B)
    val Error = Color(0xFFF87171)
    val ErrorDark = Color(0xFFDC2626)
    val Live = Color(0xFFEB0400)

    // Surface — Deep dark palette with blue undertone
    val DarkBg = Color(0xFF0A0A0F)
    val DarkSurface = Color(0xFF121218)
    val DarkSurfaceElevated = Color(0xFF1A1A22)
    val DarkSurfaceHighest = Color(0xFF222230)
    val DarkBorder = Color(0xFF2A2A3A)
    val DarkBorderSubtle = Color(0xFF1E1E2C)
    val DarkTextPrimary = Color(0xFFF0F0F5)
    val DarkTextSecondary = Color(0xFFA0A0B8)
    val DarkTextTertiary = Color(0xFF6B6B82)

    // Glass overlay
    val GlassDark = Color(0x1AFFFFFF)     // 10% white
    val GlassBorderDark = Color(0x26FFFFFF) // 15% white
    val GlassLight = Color(0x33FFFFFF)     // 20% white
    val GlassBorderLight = Color(0x40FFFFFF) // 25% white

    // Surface — Light palette with warm tint
    val LightBg = Color(0xFFF8F8FC)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceElevated = Color(0xFFF2F2F8)
    val LightSurfaceHighest = Color(0xFFEAEAF2)
    val LightBorder = Color(0xFFD8D8E4)
    val LightBorderSubtle = Color(0xFFE8E8F0)
    val LightTextPrimary = Color(0xFF0A0A14)
    val LightTextSecondary = Color(0xFF4A4A60)
    val LightTextTertiary = Color(0xFF8888A0)

    // Chat-specific
    val MentionHighlightDark = Color(0x269B6DFF)
    val MentionHighlightLight = Color(0x1A7C4DFF)

    // Mod action colors
    val ModTimeout = Color(0xFFFBBF24)
    val ModBan = Color(0xFFF87171)
    val ModDelete = Color(0xFFFF8C42)
    val ModUnban = Color(0xFF34D399)
}

// ─── Color Schemes ──────────────────────────────────────────────────────

private val ChatoneDarkScheme = darkColorScheme(
    primary = ChatoneColors.Violet500,
    onPrimary = Color.White,
    primaryContainer = ChatoneColors.Violet800,
    onPrimaryContainer = ChatoneColors.Violet200,
    secondary = ChatoneColors.Cyan400,
    onSecondary = Color.Black,
    secondaryContainer = ChatoneColors.Cyan600,
    onSecondaryContainer = ChatoneColors.Cyan50,
    tertiary = ChatoneColors.Rose400,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF5C1031),
    onTertiaryContainer = Color(0xFFFFD9E4),
    error = ChatoneColors.Error,
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = ChatoneColors.DarkBg,
    onBackground = ChatoneColors.DarkTextPrimary,
    surface = ChatoneColors.DarkSurface,
    onSurface = ChatoneColors.DarkTextPrimary,
    surfaceVariant = ChatoneColors.DarkSurfaceElevated,
    onSurfaceVariant = ChatoneColors.DarkTextSecondary,
    surfaceContainerLowest = Color(0xFF060609),
    surfaceContainerLow = Color(0xFF0E0E14),
    surfaceContainer = ChatoneColors.DarkSurface,
    surfaceContainerHigh = ChatoneColors.DarkSurfaceElevated,
    surfaceContainerHighest = ChatoneColors.DarkSurfaceHighest,
    outline = ChatoneColors.DarkBorder,
    outlineVariant = ChatoneColors.DarkBorderSubtle,
    inverseSurface = ChatoneColors.LightSurface,
    inverseOnSurface = ChatoneColors.LightTextPrimary,
    inversePrimary = ChatoneColors.Violet700,
    scrim = Color(0xCC000000)
)

private val ChatoneLightScheme = lightColorScheme(
    primary = ChatoneColors.Violet600,
    onPrimary = Color.White,
    primaryContainer = ChatoneColors.Violet100,
    onPrimaryContainer = ChatoneColors.Violet900,
    secondary = ChatoneColors.Cyan500,
    onSecondary = Color.White,
    secondaryContainer = ChatoneColors.Cyan100,
    onSecondaryContainer = Color(0xFF003540),
    tertiary = ChatoneColors.Rose500,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9E4),
    onTertiaryContainer = Color(0xFF3E001F),
    error = ChatoneColors.ErrorDark,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = ChatoneColors.LightBg,
    onBackground = ChatoneColors.LightTextPrimary,
    surface = ChatoneColors.LightSurface,
    onSurface = ChatoneColors.LightTextPrimary,
    surfaceVariant = ChatoneColors.LightSurfaceElevated,
    onSurfaceVariant = ChatoneColors.LightTextSecondary,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFCFCFF),
    surfaceContainer = ChatoneColors.LightSurfaceElevated,
    surfaceContainerHigh = ChatoneColors.LightSurfaceHighest,
    surfaceContainerHighest = Color(0xFFE2E2EC),
    outline = ChatoneColors.LightBorder,
    outlineVariant = ChatoneColors.LightBorderSubtle,
    inverseSurface = ChatoneColors.DarkSurface,
    inverseOnSurface = ChatoneColors.DarkTextPrimary,
    inversePrimary = ChatoneColors.Violet300,
    scrim = Color(0x66000000)
)

// ─── Chat-specific theme extras ─────────────────────────────────────────

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
    // Glass
    val glassOverlay: Color,
    val glassBorder: Color,
    // Shadows / elevation
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
    sidebarSurface = Color(0xFF0D0D12),
    sidebarSelected = Color(0xFF2A2A3C),
    chatInputSurface = Color(0xFF151520),
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

// ─── Theme Composable ───────────────────────────────────────────────────

object ChatoneTheme {
    val extraColors: ChatoneExtraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalChatoneColors.current
}

@Composable
fun ChatoneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ChatoneDarkScheme else ChatoneLightScheme
    val extraColors = if (darkTheme) DarkExtraColors else LightExtraColors

    CompositionLocalProvider(LocalChatoneColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ChatoneTypography,
            shapes = ChatoneShapes,
            content = content
        )
    }
}

// ─── Typography ─────────────────────────────────────────────────────────

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

// ─── Shapes ─────────────────────────────────────────────────────────────

val ChatoneShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)
