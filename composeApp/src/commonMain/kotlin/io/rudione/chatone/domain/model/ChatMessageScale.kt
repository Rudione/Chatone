package io.rudione.chatone.domain.model

import kotlin.jvm.JvmInline
import kotlin.math.roundToInt

@JvmInline
value class ChatMessageScale private constructor(val percent: Int) {

    val factor: Float get() = percent / 100f

    val isDefault: Boolean get() = percent == DEFAULT_PERCENT

    fun larger(): ChatMessageScale = of(percent + STEP_PERCENT)

    fun smaller(): ChatMessageScale = of(percent - STEP_PERCENT)

    companion object {
        const val MIN_PERCENT = 50
        const val MAX_PERCENT = 200
        const val STEP_PERCENT = 10
        const val DEFAULT_PERCENT = 100

        val Default = ChatMessageScale(DEFAULT_PERCENT)

        fun of(percent: Int): ChatMessageScale {
            val snapped = (percent / STEP_PERCENT.toFloat()).roundToInt() * STEP_PERCENT
            return ChatMessageScale(snapped.coerceIn(MIN_PERCENT, MAX_PERCENT))
        }

        fun of(percent: Float): ChatMessageScale =
            if (percent.isFinite()) of(percent.roundToInt()) else Default
    }
}
