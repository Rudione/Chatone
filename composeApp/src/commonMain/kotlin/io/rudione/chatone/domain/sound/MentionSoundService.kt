package io.rudione.chatone.domain.sound

import kotlin.concurrent.Volatile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

interface SoundOutput {
    fun preload(paths: Collection<String>, mediaStream: Boolean)
    fun play(path: String, volume: Float, mediaStream: Boolean)
    fun stop()
}

interface MentionSoundSource {
    val config: StateFlow<MentionSoundConfig>
    suspend fun pathFor(id: String): String?
    fun exists(id: String): Boolean
}

interface MentionSoundPrefs {
    val enabled: Boolean
    val volume: Float
}

class MentionSoundService(
    private val source: MentionSoundSource,
    private val output: SoundOutput,
    private val prefs: MentionSoundPrefs,
    private val scope: CoroutineScope,
    private val now: () -> Long
) {
    @Volatile
    private var lastMentionAt = Long.MIN_VALUE / 2

    init {
        scope.launch {
            source.config
                .map { cfg -> Triple(cfg.selectedId, cfg.channelSounds.values.toSet(), cfg.mediaStream) }
                .distinctUntilChanged()
                .collect { (selected, channels, media) ->
                    val paths = (channels + selected + BuiltInSound.Default.id)
                        .filter { it != MentionSoundIds.MUTE }
                        .mapNotNull { source.pathFor(it) }
                    output.preload(paths, media)
                }
        }
    }

    fun soundIdFor(channelLogin: String?, config: MentionSoundConfig = source.config.value): String {
        val login = channelLogin?.trim()?.lowercase().orEmpty()
        val override = config.channelSounds[login]
        val candidate = when {
            override == MentionSoundIds.MUTE -> return MentionSoundIds.MUTE
            override != null && source.exists(override) -> override
            else -> config.selectedId
        }
        return if (source.exists(candidate)) candidate else BuiltInSound.Default.id
    }

    fun playMention(channelLogin: String?): Boolean {
        if (!prefs.enabled) return false
        val config = source.config.value
        val id = soundIdFor(channelLogin, config)
        if (id == MentionSoundIds.MUTE) return false
        val time = now()
        if (config.cooldownMs > 0 && time - lastMentionAt < config.cooldownMs) return false
        lastMentionAt = time
        launchPlay(id, prefs.volume, config.mediaStream)
        return true
    }

    fun playKeyword(channelLogin: String?) {
        val config = source.config.value
        val id = soundIdFor(channelLogin, config).takeIf { it != MentionSoundIds.MUTE } ?: soundIdFor(null, config)
        launchPlay(id, prefs.volume, config.mediaStream)
    }

    fun preview(id: String, volume: Float = prefs.volume) {
        output.stop()
        launchPlay(id, volume, source.config.value.mediaStream)
    }

    fun previewFile(path: String, volume: Float = prefs.volume) {
        output.stop()
        output.play(path, volume, source.config.value.mediaStream)
    }

    fun stop() = output.stop()

    private fun launchPlay(id: String, volume: Float, mediaStream: Boolean) {
        if (volume <= 0f) return
        scope.launch {
            val path = source.pathFor(id) ?: source.pathFor(BuiltInSound.Default.id) ?: return@launch
            output.play(path, volume.coerceIn(0f, 1f), mediaStream)
        }
    }
}
