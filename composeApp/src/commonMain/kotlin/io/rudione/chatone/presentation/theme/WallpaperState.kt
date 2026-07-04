package io.rudione.chatone.presentation.theme

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class BlurType {
    NONE, UNIFORM, FROSTED, RADIAL, VIGNETTE, LINEAR_H, LINEAR_V, SUNSHINE
}

@Serializable
data class ChatColorConfig(
    val useCustomColor: Boolean = false,
    val customBackgroundColor: Int? = null,
    val backgroundAlpha: Float = 0.85f,
    val brightness: Float = 0f,
    val saturation: Float = 1f,
    val contrast: Float = 1f
)

@Serializable
data class PanelColorConfig(
   
    val useSidebarCustomColor: Boolean = false,
    val sidebarCustomColor: Int? = null,
    val sidebarAlpha: Float = 0.92f,
    val sidebarBrightness: Float = 0f,
    
    val sidebarBlurRadius: Float = 0f,
    
    val sidebarGlassIntensity: Float = 0.72f,

   
    val useTopBarCustomColor: Boolean = false,
    val topBarCustomColor: Int? = null,
    val topBarAlpha: Float = 0.88f,
    val topBarBrightness: Float = 0f,
    val topBarBlurRadius: Float = 0f,
    val topBarGlassIntensity: Float = 0.80f,

   
    val useBottomBarCustomColor: Boolean = false,
    val bottomBarCustomColor: Int? = null,
    val bottomBarAlpha: Float = 0.90f,
    val bottomBarBrightness: Float = 0f,
    val bottomBarBlurRadius: Float = 0f,
    val bottomBarGlassIntensity: Float = 0.82f
)

@Serializable
data class WallpaperDisplayConfig(
    val blurType: BlurType = BlurType.UNIFORM,
    val blurRadius: Float = 12f,
    val overlayDimming: Float = 0.55f,
    val chatColorConfig: ChatColorConfig = ChatColorConfig(),
    val panelColorConfig: PanelColorConfig = PanelColorConfig(),
   
    
    val masterColor: Int? = null,
    val useMasterColor: Boolean = false,
   
    val globalBrightness: Float = 0f,
    val globalContrast: Float = 1f,
    val globalSaturation: Float = 1f,
    val glowEffectsEnabled: Boolean = true
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        const val SETTINGS_KEY = "wallpaper_display_config_v3"

        fun fromJson(raw: String?): WallpaperDisplayConfig = try {
            if (raw != null) json.decodeFromString(raw) else WallpaperDisplayConfig()
        } catch (_: Exception) { WallpaperDisplayConfig() }

        fun toJson(cfg: WallpaperDisplayConfig): String = json.encodeToString(cfg)
    }
}

data class WallpaperState(
    val imageBitmap: ImageBitmap? = null,
    val dominantColor: Color = Color.Transparent,
    val isActive: Boolean = false,
    val displayConfig: WallpaperDisplayConfig = WallpaperDisplayConfig()
) {
    val blurRadius: Float     get() = displayConfig.blurRadius
    val blurType: BlurType    get() = displayConfig.blurType
    val overlayDimming: Float get() = displayConfig.overlayDimming
    val chatColorConfig: ChatColorConfig   get() = displayConfig.chatColorConfig
    val panelColorConfig: PanelColorConfig get() = displayConfig.panelColorConfig
    val glowEffectsEnabled: Boolean        get() = displayConfig.glowEffectsEnabled
}

val LocalWallpaper = compositionLocalOf { WallpaperState() }

class WallpaperController {
    var state by mutableStateOf(WallpaperState())
        private set

    fun update(newState: WallpaperState) { state = newState }
    fun clear() { state = WallpaperState() }

    private fun patchDisplay(block: WallpaperDisplayConfig.() -> WallpaperDisplayConfig) {
        state = state.copy(displayConfig = block(state.displayConfig))
    }

    fun setDisplayConfig(cfg: WallpaperDisplayConfig) { state = state.copy(displayConfig = cfg) }
    fun updateBlurType(t: BlurType)            = patchDisplay { copy(blurType = t) }
    fun updateBlurRadius(r: Float)             = patchDisplay { copy(blurRadius = r.coerceIn(0f, 40f)) }
    fun updateOverlayDimming(d: Float)         = patchDisplay { copy(overlayDimming = d.coerceIn(0f, 1f)) }
    fun updateChatColor(cfg: ChatColorConfig)  = patchDisplay { copy(chatColorConfig = cfg) }
    fun updatePanelColors(cfg: PanelColorConfig) = patchDisplay { copy(panelColorConfig = cfg) }
    fun updateMasterColor(argb: Int?, enabled: Boolean) = patchDisplay { copy(masterColor = argb, useMasterColor = enabled) }
    fun updateGlowEffects(enabled: Boolean)    = patchDisplay { copy(glowEffectsEnabled = enabled) }
    fun updateGlobalAdjustments(brightness: Float, contrast: Float, saturation: Float) =
        patchDisplay { copy(globalBrightness = brightness, globalContrast = contrast, globalSaturation = saturation) }
    fun resetToDefault()                       = setDisplayConfig(WallpaperDisplayConfig())
}

val LocalWallpaperController = staticCompositionLocalOf<WallpaperController> {
    error("WallpaperController not provided")
}