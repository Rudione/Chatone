package io.rudione.chatone.presentation.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


fun Color.adjustBrightness(b: Float): Color {
    if (b == 0f) return this
    val f = 1f + b
    return Color((red*f).coerceIn(0f,1f), (green*f).coerceIn(0f,1f), (blue*f).coerceIn(0f,1f), alpha)
}

fun Color.adjustSaturation(s: Float): Color {
    if (s == 1f) return this
    val l = 0.299f*red + 0.587f*green + 0.114f*blue
    return Color(
        (l + (red - l) * s).coerceIn(0f, 1f),
        (l + (green - l) * s).coerceIn(0f, 1f),
        (l + (blue - l) * s).coerceIn(0f, 1f),
        alpha
    )
}

fun Color.adjustContrast(c: Float): Color {
    if (c == 1f) return this
   
   
    val contrastFactor = (c - 1f) * 255f
    val f = (259f * (contrastFactor + 255f)) / (255f * (259f - contrastFactor))
    return Color(
        (f * (red - 0.5f) + 0.5f).coerceIn(0f, 1f),
        (f * (green - 0.5f) + 0.5f).coerceIn(0f, 1f),
        (f * (blue - 0.5f) + 0.5f).coerceIn(0f, 1f),
        alpha
    )
}

fun Color.luminance() = 0.299f*red + 0.587f*green + 0.114f*blue


fun Modifier.applyChatBlurOverlay(
    type: BlurType,
    radius: Float,
    dominantColor: Color,
    overlayDimming: Float
): Modifier {
    val overlayColor = Color(0xFF0A0A0F).copy(alpha = overlayDimming)
    return when (type) {
        BlurType.NONE -> drawWithContent { drawContent(); drawRect(color = overlayColor) }
        BlurType.UNIFORM -> blur(radius.dp).drawWithContent { drawContent(); drawRect(color = overlayColor) }
        BlurType.FROSTED -> blur((radius*2.2f).coerceAtMost(40f).dp).drawWithContent {
            drawContent(); drawRect(color = overlayColor.copy(alpha = (overlayDimming*0.75f).coerceAtMost(0.9f)))
        }
        BlurType.RADIAL -> blur(radius.dp).drawWithContent {
            drawContent()
            drawRect(brush = Brush.radialGradient(
                listOf(Color.Transparent, overlayColor.copy(alpha = overlayDimming*0.4f), overlayColor),
                center = Offset(size.width/2f, size.height/2f), radius = size.maxDimension*0.6f
            ))
            drawRect(color = overlayColor.copy(alpha = overlayDimming*0.25f))
        }
        BlurType.VIGNETTE -> blur((radius*0.4f).coerceAtLeast(1f).dp).drawWithContent {
            drawContent()
            drawRect(brush = Brush.radialGradient(
                listOf(Color.Transparent, Color.Transparent, overlayColor.copy(alpha = overlayDimming*0.5f), overlayColor),
                center = Offset(size.width/2f, size.height/2f), radius = size.maxDimension*0.5f
            ))
        }
        BlurType.LINEAR_H -> blur(radius.dp).drawWithContent {
            drawContent()
            drawRect(brush = Brush.horizontalGradient(
                listOf(overlayColor, overlayColor.copy(alpha = overlayDimming*0.3f), overlayColor)
            ))
        }
        BlurType.LINEAR_V -> blur(radius.dp).drawWithContent {
            drawContent()
            drawRect(brush = Brush.verticalGradient(
                listOf(overlayColor, overlayColor.copy(alpha = overlayDimming*0.3f), overlayColor)
            ))
        }
        BlurType.SUNSHINE -> blur(radius.dp).drawWithContent {
            drawContent()
            val cx = size.width/2f; val cy = size.height/2f
            listOf(0.12f,0.28f,0.46f,0.64f,0.85f).forEachIndexed { i, frac ->
                val a = (overlayDimming*0.10f*(i+1)).coerceAtMost(0.45f)
                drawCircle(brush = Brush.radialGradient(
                    listOf(Color.Transparent, dominantColor.copy(alpha = a), Color.Transparent),
                    center = Offset(cx,cy), radius = size.maxDimension*frac
                ), radius = size.maxDimension*frac, center = Offset(cx,cy))
            }
            drawRect(color = overlayColor.copy(alpha = overlayDimming*0.5f))
        }
    }
}


@Composable
fun ChatBackgroundLayer(
    wallpaper: WallpaperState,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        val bitmap = wallpaper.imageBitmap
        if (bitmap != null && wallpaper.isActive) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().applyChatBlurOverlay(
                    type = wallpaper.blurType,
                    radius = wallpaper.blurRadius,
                    dominantColor = wallpaper.dominantColor,
                    overlayDimming = wallpaper.overlayDimming
                )
            )
        }
        content()
    }
}


fun chatPaneBackgroundColor(wallpaper: WallpaperState, isDark: Boolean = true): Color {
    val cfg = wallpaper.chatColorConfig
    val default = if (isDark) Color(0xFF0D0D14) else Color(0xFFF2F2F8)

    val base = when {
       
        wallpaper.isActive && wallpaper.dominantColor != Color.Transparent ->
            wallpaper.dominantColor
       
        !wallpaper.isActive && cfg.useCustomColor && cfg.customBackgroundColor != null ->
            Color(cfg.customBackgroundColor)
       
        else -> default
    }

    return base
        .adjustBrightness(cfg.brightness)
        .adjustSaturation(cfg.saturation)
        .adjustContrast(cfg.contrast)
        .copy(alpha = cfg.backgroundAlpha)
}


fun sidebarBackgroundColor(wallpaper: WallpaperState, default: Color): Color {
    val cfg = wallpaper.panelColorConfig
    val dc = wallpaper.displayConfig
    val master = if (dc.useMasterColor && dc.masterColor != null) Color(dc.masterColor) else null
    val base = when {
        cfg.useSidebarCustomColor && cfg.sidebarCustomColor != null -> Color(cfg.sidebarCustomColor)
        master != null -> master
        else -> default
    }
    return base
        .adjustBrightness(cfg.sidebarBrightness + dc.globalBrightness)
        .adjustSaturation(dc.globalSaturation)
        .adjustContrast(dc.globalContrast)
        .copy(alpha = cfg.sidebarAlpha)
}

fun topBarBackgroundColor(wallpaper: WallpaperState, default: Color): Color {
    val cfg = wallpaper.panelColorConfig
    val dc = wallpaper.displayConfig
    val master = if (dc.useMasterColor && dc.masterColor != null) Color(dc.masterColor) else null
    val base = when {
        cfg.useTopBarCustomColor && cfg.topBarCustomColor != null -> Color(cfg.topBarCustomColor)
        master != null -> master
        else -> default
    }
    return base
        .adjustBrightness(cfg.topBarBrightness + dc.globalBrightness)
        .adjustSaturation(dc.globalSaturation)
        .adjustContrast(dc.globalContrast)
        .copy(alpha = cfg.topBarAlpha)
}

fun bottomBarBackgroundColor(wallpaper: WallpaperState, default: Color): Color {
    val cfg = wallpaper.panelColorConfig
    val dc = wallpaper.displayConfig
    val master = if (dc.useMasterColor && dc.masterColor != null) Color(dc.masterColor) else null
    val base = when {
        cfg.useBottomBarCustomColor && cfg.bottomBarCustomColor != null -> Color(cfg.bottomBarCustomColor)
        master != null -> master
        else -> default
    }
    return base
        .adjustBrightness(cfg.bottomBarBrightness + dc.globalBrightness)
        .adjustSaturation(dc.globalSaturation)
        .adjustContrast(dc.globalContrast)
        .copy(alpha = cfg.bottomBarAlpha)
}


fun Modifier.panelBlur(radius: Float): Modifier =
    if (radius > 0f) this.blur(radius.dp) else this

data class GlassAlphas(val high: Float, val low: Float)

fun glassAlphasFromIntensity(intensity: Float): GlassAlphas {
    val i = intensity.coerceIn(0.1f, 1f)
    return GlassAlphas(
        high = (i * 0.95f).coerceIn(0.05f, 1f),
        low  = (i * 0.78f).coerceIn(0.05f, 1f)
    )
}


@Composable
fun WallpaperGlowEdge(
    dominantColor: Color,
    fromRight: Boolean = false,
    glowWidth: Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.drawWithContent {
        drawContent()
        val c = listOf(
            dominantColor.copy(0.0f), dominantColor.copy(0.06f), dominantColor.copy(0.18f),
            dominantColor.copy(0.30f), dominantColor.copy(0.22f), dominantColor.copy(0.12f)
        )
        val brush = if (fromRight)
            Brush.horizontalGradient(c.reversed(), startX = size.width, endX = size.width - glowWidth.toPx())
        else
            Brush.verticalGradient(c.reversed(), startY = size.height, endY = size.height - glowWidth.toPx())
        drawRect(brush = brush, blendMode = BlendMode.SrcOver)
    })
}

fun Modifier.radialAura(color: Color, radiusFactor: Float = 1.2f, centerX: Float = 0.5f, centerY: Float = 0f): Modifier =
    this.then(Modifier.drawWithContent {
        drawContent()
        drawRect(brush = Brush.radialGradient(
            listOf(color.copy(0.35f), color.copy(0.25f), color.copy(0.15f), color.copy(0.08f), Color.Transparent),
            center = Offset(size.width*centerX, size.height*centerY),
            radius = size.maxDimension*radiusFactor
        ), blendMode = BlendMode.SrcOver)
    })