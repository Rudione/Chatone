package io.rudione.chatone.presentation.chat.points

import androidx.compose.runtime.Immutable

@Immutable
data class PointsClaim(
    val id: String,
    val channelId: String,
    val points: Int = 0
)

enum class PointsGainReason {
    WATCH, CLAIM, STREAK, RAID, FOLLOW, OTHER;

    companion object {
        fun fromTwitch(code: String): PointsGainReason = when (code.uppercase()) {
            "WATCH" -> WATCH
            "CLAIM" -> CLAIM
            "WATCH_STREAK" -> STREAK
            "RAID" -> RAID
            "FOLLOW" -> FOLLOW
            else -> OTHER
        }
    }
}

@Immutable
data class PointsGain(
    val seq: Long,
    val amount: Int,
    val reason: PointsGainReason
)
