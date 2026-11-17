package io.rudione.chatone.presentation.stream.lowlatency

internal class VideoDecodeGuard(
    private val persistenceMs: Long = DEFAULT_PERSISTENCE_MS,
    private val switchGraceMs: Long = DEFAULT_SWITCH_GRACE_MS
) {
    private val rejected = LinkedHashSet<String>()
    private val worked = LinkedHashSet<String>()
    private val retried = HashSet<String>()
    private var failedFrames = 0L
    private var decodedFrames = 0L
    private var failingSinceMs: Long? = null
    private var graceUntilMs = 0L
    private var healthy: String? = null

    val hasRejections: Boolean get() = rejected.isNotEmpty()

    fun forget() {
        rejected.clear()
        worked.clear()
        retried.clear()
        healthy = null
        failingSinceMs = null
        graceUntilMs = 0L
    }

    fun switched(nowMs: Long) {
        graceUntilMs = nowMs + switchGraceMs
        failingSinceMs = null
    }

    fun reject(quality: String) {
        rejected += quality
        if (healthy == quality) healthy = null
        failingSinceMs = null
    }

    fun observe(quality: String?, failedFrames: Long, decodedFrames: Long, nowMs: Long): String? {
        val failing = failedFrames > this.failedFrames
        val decoding = decodedFrames > this.decodedFrames
        this.failedFrames = failedFrames
        this.decodedFrames = decodedFrames
        if (quality == null || !failing || nowMs < graceUntilMs) {
            failingSinceMs = null
            if (quality != null && decoding && !failing && quality !in rejected) {
                healthy = quality
                worked += quality
            }
            return null
        }
        val since = failingSinceMs ?: nowMs.also { failingSinceMs = it }
        if (nowMs - since < persistenceMs) return null
        reject(quality)
        return quality
    }

    fun replacement(wanted: String?, candidates: List<String>): String? {
        if (wanted != null && wanted !in rejected) return wanted
        healthy?.takeIf { it in candidates && it !in rejected }?.let { return it }
        candidates.firstOrNull { it !in rejected }?.let { return it }
        val retry = candidates.firstOrNull { it in worked && it !in retried } ?: return null
        retried += retry
        rejected -= retry
        return retry
    }

    private companion object {
        const val DEFAULT_PERSISTENCE_MS = 1_000L
        const val DEFAULT_SWITCH_GRACE_MS = 2_000L
    }
}
