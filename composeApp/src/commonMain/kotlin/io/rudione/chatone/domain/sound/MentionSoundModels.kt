package io.rudione.chatone.domain.sound

import kotlinx.serialization.Serializable

class PcmAudio(val sampleRate: Int, val channels: Int, val samples: FloatArray) {
    val frames: Int get() = if (channels <= 0) 0 else samples.size / channels
    val durationMs: Long get() = if (sampleRate <= 0) 0L else frames * 1000L / sampleRate

    fun frameAt(ms: Long): Int =
        if (sampleRate <= 0) 0 else (ms * sampleRate / 1000L).coerceIn(0L, frames.toLong()).toInt()
}

@Serializable
data class SoundEdit(
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val fadeInMs: Int = 0,
    val fadeOutMs: Int = 0,
    val gain: Float = 1f,
    val normalize: Boolean = false
)

@Serializable
data class CustomSound(
    val id: String,
    val name: String,
    val file: String,
    val source: String? = null,
    val durationMs: Long = 0L,
    val sourceDurationMs: Long = 0L,
    val edit: SoundEdit = SoundEdit(),
    val peaks: List<Float> = emptyList(),
    val addedAt: Long = 0L
)

@Serializable
data class MentionSoundConfig(
    val selectedId: String = BuiltInSound.Default.id,
    val cooldownMs: Long = 0L,
    val channelSounds: Map<String, String> = emptyMap(),
    val mediaStream: Boolean = false
)

data class SoundInfo(val durationMs: Long, val peaks: List<Float>)

enum class BuiltInSound(val id: String) {
    Tag("builtin:tag"),
    Chime("builtin:chime"),
    Drop("builtin:drop"),
    Bell("builtin:bell"),
    Beep("builtin:beep");

    companion object {
        val Default = Tag
        fun byId(id: String): BuiltInSound? = entries.firstOrNull { it.id == id }
    }
}

object MentionSoundIds {
    const val MUTE = "mute"
    private const val CUSTOM_PREFIX = "custom:"

    fun isCustom(id: String): Boolean = id.startsWith(CUSTOM_PREFIX)

    fun newCustomId(nowMs: Long, salt: Int): String =
        CUSTOM_PREFIX + nowMs.toString(36) + (salt and 0x7fffffff).toString(36)
}

object SoundLimits {
    const val MAX_CLIP_MS = 5_000L
    const val MAX_SOURCE_MS = 60_000L
    const val MAX_SOURCE_BYTES = 40L * 1024 * 1024
    const val MAX_FADE_MS = 1_000
    const val MIN_CLIP_MS = 50L
    const val PEAK_BUCKETS = 48
    const val MAX_RENDER_RATE = 48_000
    val supportedExtensions = setOf("wav", "mp3", "m4a", "mp4", "aac", "ogg", "oga", "opus", "flac", "aif", "aiff", "caf", "webm")

    fun extensionOf(path: String): String =
        path.substringAfterLast('/').substringAfterLast('\\').substringAfterLast('.', "").lowercase()

    fun isSupported(path: String): Boolean = extensionOf(path) in supportedExtensions
}
