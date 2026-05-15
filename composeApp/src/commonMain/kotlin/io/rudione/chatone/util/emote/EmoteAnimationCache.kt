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
    private const val MAX_ENTRIES = 2048

    private val animationStates: SnapshotStateMap<String, Animatable<Float, *>> = mutableStateMapOf()
    private val tooltipTimings: SnapshotStateMap<String, Long> = mutableStateMapOf()
    private val accessOrder = LinkedHashSet<String>()
    private val mutex = Mutex()

    private val animationSpecs: SnapshotStateMap<String, AnimationSpec<Float>> = mutableStateMapOf()

    private fun touch(emoteId: String) {
        accessOrder.remove(emoteId)
        accessOrder.add(emoteId)
        if (accessOrder.size > MAX_ENTRIES) {
            val toEvict = accessOrder.size - MAX_ENTRIES
            val iter = accessOrder.iterator()
            var removed = 0
            while (removed < toEvict && iter.hasNext()) {
                val key = iter.next()
                iter.remove()
                animationStates.remove(key)
                animationSpecs.remove(key)
                tooltipTimings.remove(key)
                removed++
            }
        }
    }

    fun getOrCreateAnimation(
        emoteId: String,
        initialValue: Float = 0f,
        animationSpec: AnimationSpec<Float> = tween(durationMillis = 150)
    ): Animatable<Float, *> {
        animationSpecs[emoteId] = animationSpec
        touch(emoteId)
        return animationStates.getOrPut(emoteId) {
            Animatable(initialValue)
        }
    }

    suspend fun startAnimation(emoteId: String, targetValue: Float = 1f) {
        val anim = animationStates[emoteId] ?: return
        if (anim.isRunning || anim.value == targetValue) return
        mutex.withLock {
            if (!anim.isRunning && anim.value != targetValue) {
                val spec = animationSpecs[emoteId] ?: tween(durationMillis = 150)
                anim.animateTo(targetValue, animationSpec = spec)
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
        animationSpecs.remove(emoteId)
        accessOrder.remove(emoteId)
    }

    fun clearAll() {
        animationStates.clear()
        tooltipTimings.clear()
        animationSpecs.clear()
        accessOrder.clear()
    }
}