package io.rudione.chatone.data.sound

import io.rudione.chatone.domain.sound.BuiltInSound
import io.rudione.chatone.domain.sound.PcmAudio
import io.rudione.chatone.domain.sound.SoundInfo
import io.rudione.chatone.domain.sound.SoundProcessing
import io.rudione.chatone.domain.sound.SoundSynth
import io.rudione.chatone.domain.sound.WavCodec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BuiltInSounds(
    private val files: SoundFileStore,
    private val tagBytes: suspend () -> ByteArray
) {
    private val mutex = Mutex()
    private val paths = mutableMapOf<BuiltInSound, String>()
    private val _info = MutableStateFlow<Map<String, SoundInfo>>(emptyMap())
    val info: StateFlow<Map<String, SoundInfo>> = _info.asStateFlow()

    suspend fun path(sound: BuiltInSound): String? = mutex.withLock {
        paths[sound] ?: materialize(sound)?.also { paths[sound] = it }
    }

    suspend fun warmUp() {
        BuiltInSound.entries.forEach { path(it) }
    }

    private suspend fun materialize(sound: BuiltInSound): String? {
        val audio = audioFor(sound) ?: return null
        val relative = "builtin/${sound.name.lowercase()}-$VERSION.wav"
        val target = files.resolve(relative)
        val path = if (files.exists(target) && files.size(target) > 44) target else files.write(relative, WavCodec.encode(audio))
        if (path != null) {
            _info.update { it + (sound.id to SoundInfo(audio.durationMs, SoundProcessing.peaks(audio))) }
        }
        return path
    }

    private suspend fun audioFor(sound: BuiltInSound): PcmAudio? =
        SoundSynth.render(sound) ?: runCatching { WavCodec.decode(tagBytes()) }.getOrNull()

    private companion object {
        const val VERSION = 1
    }
}
