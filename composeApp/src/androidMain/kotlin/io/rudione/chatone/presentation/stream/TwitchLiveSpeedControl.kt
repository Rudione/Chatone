package io.rudione.chatone.presentation.stream

import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.LivePlaybackSpeedControl
import io.rudione.chatone.domain.stream.LiveSpeedPolicy

@UnstableApi
internal class TwitchLiveSpeedControl(
    private val policy: LiveSpeedPolicy = LiveSpeedPolicy()
) : LivePlaybackSpeedControl {

    @Volatile
    var clockOffsetUs: Long = 0L

    val targetUs: Long get() = policy.targetUs

    override fun setLiveConfiguration(liveConfiguration: MediaItem.LiveConfiguration) {
        policy.configure(
            targetUs = liveConfiguration.targetOffsetMs.toPolicyUs(),
            minTargetUs = liveConfiguration.minOffsetMs.toPolicyUs(),
            maxTargetUs = liveConfiguration.maxOffsetMs.toPolicyUs(),
            minSpeed = liveConfiguration.minPlaybackSpeed.takeUnless { it == C.RATE_UNSET } ?: FALLBACK_MIN_SPEED,
            maxSpeed = liveConfiguration.maxPlaybackSpeed.takeUnless { it == C.RATE_UNSET } ?: FALLBACK_MAX_SPEED
        )
    }

    override fun setTargetLiveOffsetOverrideUs(liveOffsetUs: Long) {
        policy.override(if (liveOffsetUs == C.TIME_UNSET) LiveSpeedPolicy.UNSET else liveOffsetUs + clockOffsetUs)
    }

    override fun notifyRebuffer() {
        policy.onRebuffer(SystemClock.elapsedRealtime())
    }

    override fun getAdjustedPlaybackSpeed(liveOffsetUs: Long, bufferedDurationUs: Long): Float {
        if (liveOffsetUs == C.TIME_UNSET) return 1f
        return policy.speed(liveOffsetUs + clockOffsetUs, bufferedDurationUs, SystemClock.elapsedRealtime())
    }

    override fun getTargetLiveOffsetUs(): Long =
        policy.targetUs.let { if (it == LiveSpeedPolicy.UNSET) C.TIME_UNSET else it }

    private fun Long.toPolicyUs(): Long = if (this == C.TIME_UNSET) LiveSpeedPolicy.UNSET else this * 1_000L

    private companion object {
        const val FALLBACK_MIN_SPEED = 0.97f
        const val FALLBACK_MAX_SPEED = 1.03f
    }
}
