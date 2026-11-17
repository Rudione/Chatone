package io.rudione.chatone.data.sound

import io.rudione.chatone.domain.sound.CustomSound
import io.rudione.chatone.domain.sound.PcmAudio
import io.rudione.chatone.domain.sound.SoundEdit
import io.rudione.chatone.domain.sound.SoundLimits
import io.rudione.chatone.domain.sound.SoundProcessing
import io.rudione.chatone.domain.sound.WavCodec
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface SoundImportResult {
    data class Added(val sound: CustomSound, val source: PcmAudio, val needsTrim: Boolean) : SoundImportResult
    data object Unsupported : SoundImportResult
    data object TooLarge : SoundImportResult
    data object Failed : SoundImportResult
}

data class RenderedSound(val path: String, val durationMs: Long, val peaks: List<Float>)

class SoundImporter(
    private val files: SoundFileStore,
    private val decoder: AudioDecoder,
    private val compute: CoroutineDispatcher = Dispatchers.Default
) {
    suspend fun import(sourcePath: String, id: String, now: Long): SoundImportResult {
        if (!SoundLimits.isSupported(sourcePath)) return SoundImportResult.Unsupported
        val size = files.size(sourcePath)
        if (size <= 0L) return SoundImportResult.Failed
        if (size > SoundLimits.MAX_SOURCE_BYTES) return SoundImportResult.TooLarge
        val copy = files.copyIn(sourcePath, "$LIBRARY/${folder(id)}/source.${SoundLimits.extensionOf(sourcePath)}")
            ?: return SoundImportResult.Failed
        val pcm = decoder.decode(copy)
        if (pcm == null) {
            files.delete(copy)
            return SoundImportResult.Unsupported
        }
        val edit = SoundProcessing.defaultEdit(pcm)
        val rendered = render(pcm, id, edit, now)
        if (rendered == null) {
            files.delete(copy)
            return SoundImportResult.Failed
        }
        val sound = CustomSound(
            id = id,
            name = displayName(sourcePath),
            file = rendered.path,
            source = copy,
            durationMs = rendered.durationMs,
            sourceDurationMs = pcm.durationMs,
            edit = edit,
            peaks = rendered.peaks,
            addedAt = now
        )
        return SoundImportResult.Added(sound, pcm, needsTrim = pcm.durationMs > SoundLimits.MAX_CLIP_MS)
    }

    suspend fun upgrade(sound: CustomSound, now: Long): CustomSound? {
        val pcm = decoder.decode(sound.file) ?: return null
        val copy = files.copyIn(sound.file, "$LIBRARY/${folder(sound.id)}/source.${SoundLimits.extensionOf(sound.file).ifBlank { "wav" }}")
            ?: return null
        val edit = SoundEdit(endMs = minOf(pcm.durationMs, SoundLimits.MAX_CLIP_MS))
        val rendered = render(pcm, sound.id, edit, now) ?: return null
        return sound.copy(
            file = rendered.path,
            source = copy,
            durationMs = rendered.durationMs,
            sourceDurationMs = pcm.durationMs,
            edit = edit,
            peaks = rendered.peaks
        )
    }

    suspend fun render(source: PcmAudio, id: String, edit: SoundEdit, now: Long): RenderedSound? {
        val out = withContext(compute) { SoundProcessing.render(source, edit) }
        if (out.frames == 0) return null
        val bytes = withContext(compute) { WavCodec.encode(out) }
        val path = files.write("$LIBRARY/${folder(id)}/sound-${now.toString(36)}.wav", bytes) ?: return null
        return RenderedSound(path, out.durationMs, SoundProcessing.peaks(out))
    }

    suspend fun writePreview(audio: PcmAudio, now: Long): String? =
        files.write("$TEMP/preview-${now.toString(36)}.wav", withContext(compute) { WavCodec.encode(audio) })

    companion object {
        const val LIBRARY = "library"
        const val TEMP = "tmp"
        private const val MAX_NAME = 40

        fun folder(id: String): String = id.replace(UNSAFE_PATH_CHARS, "_")

        private val UNSAFE_PATH_CHARS = Regex("[^A-Za-z0-9_-]")

        fun displayName(path: String): String =
            path.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.')
                .replace('_', ' ').trim().take(MAX_NAME)
    }
}
