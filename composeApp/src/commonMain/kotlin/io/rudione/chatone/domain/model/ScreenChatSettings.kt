package io.rudione.chatone.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ScreenChatLayer { ALL_WINDOWS, APP_WINDOWS, NORMAL }

@Serializable
data class ScreenChatSettings(
    val layer: ScreenChatLayer = ScreenChatLayer.ALL_WINDOWS,
    val overFullScreen: Boolean = false,
    val keepDockIcon: Boolean = false,
    val backgroundPercent: Int = DEFAULT_BACKGROUND_PERCENT,
    val backdrop: Boolean = false,
    val backdropPercent: Int = DEFAULT_BACKDROP_PERCENT,
    val textPercent: Int = MAX_PERCENT,
    val scalePercent: Int = DEFAULT_SCALE_PERCENT,
    val fontSize: Int = DEFAULT_FONT_SIZE,
    val showModeration: Boolean = true,
    val replyAbove: Boolean = false,
    val hideDelaySeconds: Int = DEFAULT_HIDE_DELAY_SECONDS,
    val locked: Boolean = false
) {
    val backgroundAlpha: Float get() = backgroundPercent / 100f
    val backdropAlpha: Float get() = if (backdrop) backdropPercent / 100f else 0f
    val textAlpha: Float get() = textPercent / 100f
    val scale: Float get() = scalePercent / 100f
    val hideDelayMs: Long get() = hideDelaySeconds * 1_000L

    fun sanitized(): ScreenChatSettings = copy(
        backgroundPercent = backgroundPercent.coerceIn(0, MAX_PERCENT),
        backdropPercent = backdropPercent.coerceIn(MIN_BACKDROP_PERCENT, MAX_PERCENT),
        textPercent = textPercent.coerceIn(MIN_TEXT_PERCENT, MAX_PERCENT),
        scalePercent = scalePercent.coerceIn(MIN_SCALE_PERCENT, MAX_SCALE_PERCENT),
        fontSize = fontSize.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE),
        hideDelaySeconds = hideDelaySeconds.coerceIn(MIN_HIDE_DELAY_SECONDS, MAX_HIDE_DELAY_SECONDS)
    )

    fun scaledBy(steps: Int): ScreenChatSettings =
        copy(scalePercent = scalePercent + steps * SCALE_STEP_PERCENT).sanitized()

    companion object {
        const val MAX_PERCENT = 100
        const val MIN_TEXT_PERCENT = 30
        const val MIN_BACKDROP_PERCENT = 5
        const val DEFAULT_BACKGROUND_PERCENT = 45
        const val DEFAULT_BACKDROP_PERCENT = 35
        const val DEFAULT_SCALE_PERCENT = 100
        const val MIN_SCALE_PERCENT = 60
        const val MAX_SCALE_PERCENT = 200
        const val SCALE_STEP_PERCENT = 10
        const val DEFAULT_FONT_SIZE = 14
        const val MIN_FONT_SIZE = 9
        const val MAX_FONT_SIZE = 28
        const val DEFAULT_HIDE_DELAY_SECONDS = 3
        const val MIN_HIDE_DELAY_SECONDS = 1
        const val MAX_HIDE_DELAY_SECONDS = 15
    }
}
