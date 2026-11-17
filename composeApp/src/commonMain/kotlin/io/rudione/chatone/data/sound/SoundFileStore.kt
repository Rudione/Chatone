package io.rudione.chatone.data.sound

import io.rudione.chatone.domain.sound.PcmAudio
import io.rudione.chatone.domain.sound.SoundLimits
import io.rudione.chatone.domain.sound.WavCodec
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

expect fun soundsRootDirectory(): String

expect val mediaStreamChoiceSupported: Boolean

expect suspend fun decodePlatformAudio(path: String, maxDurationMs: Long): PcmAudio?

interface SoundFileStore {
    val root: String
    fun exists(path: String): Boolean
    fun size(path: String): Long
    fun contains(path: String): Boolean
    fun resolve(relative: String): String
    suspend fun read(path: String, maxBytes: Long = SoundLimits.MAX_SOURCE_BYTES): ByteArray?
    suspend fun write(relative: String, bytes: ByteArray): String?
    suspend fun copyIn(source: String, relative: String): String?
    suspend fun delete(path: String)
}

class LocalSoundFileStore(
    override val root: String,
    private val io: CoroutineDispatcher
) : SoundFileStore {
    private val fs = SystemFileSystem
    private val normalizedRoot = normalize(root).trimEnd('/')

    override fun exists(path: String): Boolean = runCatching { fs.exists(Path(path)) }.getOrDefault(false)

    override fun size(path: String): Long =
        runCatching { fs.metadataOrNull(Path(path))?.size ?: -1L }.getOrDefault(-1L)

    override fun contains(path: String): Boolean {
        val normalized = normalize(path)
        return normalized.startsWith("$normalizedRoot/") && normalized.split('/').none { it == ".." }
    }

    override fun resolve(relative: String): String {
        require(relative.split('/', '\\').none { it == ".." || it.isEmpty() }) { "Bad sound path" }
        return Path(root, *relative.split('/').toTypedArray()).toString()
    }

    override suspend fun read(path: String, maxBytes: Long): ByteArray? = withContext(io) {
        runCatching {
            val length = size(path)
            if (length < 0 || length > maxBytes) return@runCatching null
            fs.source(Path(path)).buffered().use { it.readByteArray() }
        }.getOrNull()
    }

    override suspend fun write(relative: String, bytes: ByteArray): String? = withContext(io) {
        runCatching {
            val target = Path(resolve(relative))
            target.parent?.let { fs.createDirectories(it) }
            val temp = Path("$target.part")
            fs.sink(temp).buffered().use { it.write(bytes) }
            fs.atomicMove(temp, target)
            target.toString()
        }.getOrNull()
    }

    override suspend fun copyIn(source: String, relative: String): String? = withContext(io) {
        runCatching {
            val length = size(source)
            if (length <= 0 || length > SoundLimits.MAX_SOURCE_BYTES) return@runCatching null
            val target = Path(resolve(relative))
            target.parent?.let { fs.createDirectories(it) }
            val temp = Path("$target.part")
            fs.source(Path(source)).buffered().use { input ->
                fs.sink(temp).buffered().use { output -> input.transferTo(output) }
            }
            fs.atomicMove(temp, target)
            target.toString()
        }.getOrNull()
    }

    override suspend fun delete(path: String) {
        if (!contains(path)) return
        withContext(io) { runCatching { fs.delete(Path(path), mustExist = false) } }
    }

    private fun normalize(path: String): String = path.replace('\\', '/')
}

class AudioDecoder(
    private val files: SoundFileStore,
    private val platform: suspend (String, Long) -> PcmAudio? = ::decodePlatformAudio
) {
    suspend fun decode(path: String, maxDurationMs: Long = SoundLimits.MAX_SOURCE_MS): PcmAudio? {
        if (SoundLimits.extensionOf(path) == "wav") {
            files.read(path)?.let { bytes -> WavCodec.decode(bytes, maxDurationMs)?.let { return it } }
        }
        return runCatching { platform(path, maxDurationMs) }.getOrNull()?.takeIf { it.frames > 0 }
    }
}
