package io.rudione.chatone.presentation.stream.lowlatency

internal class VideoDecodeGuard(private val persistenceMs: Long = DEFAULT_PERSISTENCE_MS) {
    private val rejected = LinkedHashSet<String>()
    private var failedFrames = 0L
    private var decodedFrames = 0L
    private var failingSinceMs: Long? = null
    private var healthy: String? = null

    val hasRejections: Boolean get() = rejected.isNotEmpty()

    fun forget() {
        rejected.clear()
        healthy = null
        failingSinceMs = null
    }

    fun observe(quality: String?, failedFrames: Long, decodedFrames: Long, nowMs: Long): String? {
        val failing = failedFrames > this.failedFrames
        val decoding = decodedFrames > this.decodedFrames
        this.failedFrames = failedFrames
        this.decodedFrames = decodedFrames
        if (quality == null || !failing) {
            failingSinceMs = null
            if (quality != null && decoding && quality !in rejected) healthy = quality
            return null
        }
        val since = failingSinceMs ?: nowMs.also { failingSinceMs = it }
        if (nowMs - since < persistenceMs) return null
        failingSinceMs = null
        rejected += quality
        if (healthy == quality) healthy = null
        return quality
    }

    fun replacement(wanted: String?, candidates: List<String>): String? {
        if (wanted != null && wanted !in rejected) return wanted
        healthy?.takeIf { it in candidates && it !in rejected }?.let { return it }
        return candidates.firstOrNull { it !in rejected }
    }

    private companion object {
        const val DEFAULT_PERSISTENCE_MS = 1_000L
    }
}
