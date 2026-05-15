package io.rudione.chatone.presentation.theme

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

data class ChatFontSettings(
    val fontFamily: FontFamily = FontFamily.Default,
    val fontFamilyName: String = "Default",
    val fontStyle: FontStyle = FontStyle.Normal,
    val textDecoration: TextDecoration? = null,
    val strikethrough: Boolean = false,
    val underline: Boolean = false
)

val LocalChatFont = compositionLocalOf { ChatFontSettings() }

val FirstMessageColor = Color(0xFF7B2FBE)

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


object ChatoneTheme {
    val extraColors: ChatoneExtraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalChatoneColors.current
}

@Composable
fun ChatoneTheme(
    darkTheme: Boolean = true,  
    accentColorIndex: Int = 0,
    customTheme: CustomThemeConfig? = null,
    fontSettings: ChatFontSettings = ChatFontSettings(),
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
        applyAccentPalette(ChatoneDarkScheme, accentColorIndex, true)
    }
    val palette = ExpressivePalettes.getOrElse(accentColorIndex) { ExpressivePalettes[0] }

    val baseExtra = DarkExtraColors
    val accentHint = palette.darkPrimary
    val extraColors = if (accentColorIndex == 0) baseExtra else baseExtra.copy(
        sidebarSurface = accentHint.copy(alpha = 0.10f).compositeOver(baseExtra.sidebarSurface),
        sidebarSelected = accentHint.copy(alpha = 0.20f).compositeOver(baseExtra.sidebarSelected),
        chatInputSurface = accentHint.copy(alpha = 0.07f).compositeOver(baseExtra.chatInputSurface),
        mentionHighlight = accentHint.copy(alpha = 0.14f)
    )

    CompositionLocalProvider(
        LocalChatoneColors provides extraColors,
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
            shapes = ChatoneShapes,
            content = content
        )
    }
}

data class AccentPalette(
    val name: String,
    val darkPrimary: Color,
    val darkSecondary: Color,
    val darkTertiary: Color,
    val lightPrimary: Color,
    val lightSecondary: Color,
    val lightTertiary: Color,
    val previewColor: Color
)

val ExpressivePalettes = listOf(
    AccentPalette(
        name = "Violet",
        darkPrimary = ChatoneColors.Violet500,
        darkSecondary = ChatoneColors.Cyan400,
        darkTertiary = ChatoneColors.Rose400,
        lightPrimary = ChatoneColors.Violet600,
        lightSecondary = ChatoneColors.Cyan500,
        lightTertiary = ChatoneColors.Rose500,
        previewColor = ChatoneColors.Violet500
    ),
    AccentPalette(
        name = "Aurora",
        darkPrimary = Color(0xFF00BFA5),
        darkSecondary = Color(0xFF80DEEA),
        darkTertiary = Color(0xFFFF8A65),
        lightPrimary = Color(0xFF00897B),
        lightSecondary = Color(0xFF00ACC1),
        lightTertiary = Color(0xFFFF7043),
        previewColor = Color(0xFF00BFA5)
    ),
    AccentPalette(
        name = "Ember",
        darkPrimary = Color(0xFFFF6E6E),
        darkSecondary = Color(0xFFFFB347),
        darkTertiary = Color(0xFFB39DDB),
        lightPrimary = Color(0xFFD32F2F),
        lightSecondary = Color(0xFFF57C00),
        lightTertiary = Color(0xFF7B1FA2),
        previewColor = Color(0xFFFF6E6E)
    ),
    AccentPalette(
        name = "Ocean",
        darkPrimary = Color(0xFF4FC3F7),
        darkSecondary = Color(0xFF80CBC4),
        darkTertiary = Color(0xFFCE93D8),
        lightPrimary = Color(0xFF0277BD),
        lightSecondary = Color(0xFF00695C),
        lightTertiary = Color(0xFF6A1B9A),
        previewColor = Color(0xFF4FC3F7)
    ),
    AccentPalette(
        name = "Forest",
        darkPrimary = Color(0xFF81C784),
        darkSecondary = Color(0xFFA5D6A7),
        darkTertiary = Color(0xFFFFD54F),
        lightPrimary = Color(0xFF2E7D32),
        lightSecondary = Color(0xFF388E3C),
        lightTertiary = Color(0xFFF9A825),
        previewColor = Color(0xFF81C784)
    ),
    AccentPalette(
        name = "Candy",
        darkPrimary = Color(0xFFF48FB1),
        darkSecondary = Color(0xFFCE93D8),
        darkTertiary = Color(0xFF80DEEA),
        lightPrimary = Color(0xFFC2185B),
        lightSecondary = Color(0xFF7B1FA2),
        lightTertiary = Color(0xFF0097A7),
        previewColor = Color(0xFFF48FB1)
    ),
    AccentPalette(
        name = "Solar",
        darkPrimary = Color(0xFFFFD54F),
        darkSecondary = Color(0xFFFFCC02),
        darkTertiary = Color(0xFF80CBC4),
        lightPrimary = Color(0xFFF57F17),
        lightSecondary = Color(0xFFE65100),
        lightTertiary = Color(0xFF00695C),
        previewColor = Color(0xFFFFD54F)
    ),
    AccentPalette(
        name = "Midnight",
        darkPrimary = Color(0xFF9FA8DA),
        darkSecondary = Color(0xFF80DEEA),
        darkTertiary = Color(0xFFEF9A9A),
        lightPrimary = Color(0xFF283593),
        lightSecondary = Color(0xFF00838F),
        lightTertiary = Color(0xFFB71C1C),
        previewColor = Color(0xFF9FA8DA)
    )
)

private fun applyAccentPalette(base: ColorScheme, index: Int, dark: Boolean): ColorScheme {
    val palette = ExpressivePalettes.getOrElse(index) { ExpressivePalettes[0] }
    return if (dark) {
        val tintedBg = palette.darkPrimary.copy(alpha = 0.1f).compositeOver(ChatoneColors.DarkBg)
        val tintedSurface = palette.darkPrimary.copy(alpha = 0.04f).compositeOver(ChatoneColors.DarkSurface)
        val tintedSurfaceElevated = palette.darkPrimary.copy(alpha = 0.05f).compositeOver(ChatoneColors.DarkSurfaceElevated)
        base.copy(
            primary = palette.darkPrimary,
            onPrimary = Color.White,
            primaryContainer = palette.darkPrimary.copy(alpha = 0.25f).compositeOver(Color(0xFF1A1A22)),
            onPrimaryContainer = palette.darkPrimary.copy(alpha = 0.9f),
            secondary = palette.darkSecondary,
            onSecondary = Color.Black,
            secondaryContainer = palette.darkSecondary.copy(alpha = 0.22f).compositeOver(Color(0xFF1A1A22)),
            onSecondaryContainer = palette.darkSecondary,
            tertiary = palette.darkTertiary,
            onTertiary = Color.Black,
            tertiaryContainer = palette.darkTertiary.copy(alpha = 0.22f).compositeOver(Color(0xFF1A1A22)),
            onTertiaryContainer = palette.darkTertiary,
            inversePrimary = palette.lightPrimary,
           
            background = tintedBg,
            surface = tintedSurface,
            surfaceVariant = tintedSurfaceElevated,
            surfaceContainerHigh = palette.darkPrimary.copy(alpha = 0.06f).compositeOver(ChatoneColors.DarkSurfaceElevated),
            surfaceContainerHighest = palette.darkPrimary.copy(alpha = 0.07f).compositeOver(ChatoneColors.DarkSurfaceHighest)
        )
    } else {
        val tintedBg = palette.lightPrimary.copy(alpha = 0.02f).compositeOver(ChatoneColors.LightBg)
        val tintedSurface = palette.lightPrimary.copy(alpha = 0.1f).compositeOver(ChatoneColors.LightSurface)
        base.copy(
            primary = palette.lightPrimary,
            onPrimary = Color.White,
            primaryContainer = palette.lightPrimary.copy(alpha = 0.12f).compositeOver(Color.White),
            onPrimaryContainer = palette.lightPrimary,
            secondary = palette.lightSecondary,
            onSecondary = Color.White,
            secondaryContainer = palette.lightSecondary.copy(alpha = 0.12f).compositeOver(Color.White),
            onSecondaryContainer = palette.lightSecondary,
            tertiary = palette.lightTertiary,
            onTertiary = Color.White,
            tertiaryContainer = palette.lightTertiary.copy(alpha = 0.12f).compositeOver(Color.White),
            onTertiaryContainer = palette.lightTertiary,
            inversePrimary = palette.darkPrimary,
            background = tintedBg,
            surface = tintedSurface
        )
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