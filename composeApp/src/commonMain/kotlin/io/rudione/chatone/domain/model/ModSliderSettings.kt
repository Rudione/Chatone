package io.rudione.chatone.domain.model

import io.rudione.chatone.domain.moderation.ModSliderDurations
import kotlinx.serialization.Serializable

@Serializable
data class ModSliderSettings(
    val enabled: Boolean = true,
    val twitchScale: Boolean = true,
    val timeoutSeconds: List<Int>? = null,
    val deleteAtStart: Boolean = true,
    val banAtEnd: Boolean = true,
    val travelPercent: Int = DEFAULT_TRAVEL_PERCENT
) {
    val hasCustomDurations: Boolean get() = timeoutSeconds != null

    val travelFraction: Float get() = travelPercent / 100f

    fun durations(buttons: List<ModActionButton>, defaultTimeoutSeconds: Int): List<Int> =
        if (twitchScale) ModSliderDurations.TWITCH_SCALE else customDurations(buttons, defaultTimeoutSeconds)

    fun customDurations(buttons: List<ModActionButton>, defaultTimeoutSeconds: Int): List<Int> =
        timeoutSeconds?.let(::normalize) ?: durationsFromButtons(buttons, defaultTimeoutSeconds)

    fun withDurations(seconds: List<Int>): ModSliderSettings = copy(timeoutSeconds = normalize(seconds))

    fun sanitized(): ModSliderSettings = copy(
        timeoutSeconds = timeoutSeconds?.let(::normalize),
        travelPercent = travelPercent.coerceIn(MIN_TRAVEL_PERCENT, MAX_TRAVEL_PERCENT)
    )

    companion object {
        const val DEFAULT_TRAVEL_PERCENT = 60
        const val MIN_TRAVEL_PERCENT = 30
        const val MAX_TRAVEL_PERCENT = 90
        const val TRAVEL_STEP_PERCENT = 5
        const val MAX_DURATIONS = 12

        fun durationsFromButtons(buttons: List<ModActionButton>, defaultTimeoutSeconds: Int): List<Int> {
            val fromButtons = buttons
                .filter { it.enabled && it.id != ModActionButton.DEFAULT_DELETE.id && it.id != ModActionButton.DEFAULT_BAN.id }
                .map { if (it.id == ModActionButton.DEFAULT_TIMEOUT.id) defaultTimeoutSeconds else it.durationSeconds }
            return normalize(fromButtons).ifEmpty { normalize(listOf(defaultTimeoutSeconds)) }
        }

        private fun normalize(seconds: List<Int>): List<Int> =
            seconds.filter { it in 1..ModSliderDurations.MAX_SECONDS }.distinct().sorted().take(MAX_DURATIONS)
    }
}
