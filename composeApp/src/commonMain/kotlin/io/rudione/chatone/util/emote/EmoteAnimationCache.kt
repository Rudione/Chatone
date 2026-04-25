package io.rudione.chatone.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

@Stable
object EmoteAnimationCache {
    private val animationStates: SnapshotStateMap<String, Animatable<Float, *>> = mutableStateMapOf()
    private val tooltipTimings: SnapshotStateMap<String, Long> = mutableStateMapOf()
    private val mutex = Mutex()

    fun getOrCreateAnimation(
        emoteId: String,
        initialValue: Float = 0f,
        animationSpec: AnimationSpec<Float> = tween(durationMillis = 800)
    ): Animatable<Float, *> {
        return animationStates.getOrPut(emoteId) {
            Animatable(initialValue)
        }
    }

    suspend fun startAnimation(emoteId: String, targetValue: Float = 1f) {
        mutex.withLock {
            val anim = animationStates[emoteId] ?: return@withLock
            if (!anim.isRunning && anim.value != targetValue) {
                anim.animateTo(targetValue, animationSpec = tween(durationMillis = 800))
            }
        }
    }

    fun getTooltipTiming(emoteId: String): Long {
        return tooltipTimings[emoteId] ?: Clock.System.now().toEpochMilliseconds()
    }

    fun recordTooltipTiming(emoteId: String) {
        if (!tooltipTimings.containsKey(emoteId)) {
            tooltipTimings[emoteId] = Clock.System.now().toEpochMilliseconds()
        }
    }

    fun clear(emoteId: String) {
        animationStates.remove(emoteId)
        tooltipTimings.remove(emoteId)
    }

    fun clearAll() {
        animationStates.clear()
        tooltipTimings.clear()
    }
}