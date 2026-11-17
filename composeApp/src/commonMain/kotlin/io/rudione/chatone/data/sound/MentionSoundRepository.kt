package io.rudione.chatone.data.sound

import com.russhwolf.settings.Settings
import io.rudione.chatone.domain.sound.BuiltInSound
import io.rudione.chatone.domain.sound.CustomSound
import io.rudione.chatone.domain.sound.MentionSoundConfig
import io.rudione.chatone.domain.sound.MentionSoundIds
import io.rudione.chatone.domain.sound.MentionSoundPrefs
import io.rudione.chatone.domain.sound.MentionSoundSource
import io.rudione.chatone.domain.sound.PcmAudio
import io.rudione.chatone.domain.sound.SoundEdit
import io.rudione.chatone.domain.sound.SoundInfo
import io.rudione.chatone.domain.sound.SoundProcessing
import io.rudione.chatone.presentation.settings.SettingsKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

class SettingsMentionPrefs(private val settings: Settings) : MentionSoundPrefs {
    override val enabled: Boolean get() = settings.getBoolean(SettingsKeys.MENTION_SOUND, true)
    override val volume: Float get() = settings.getFloat(SettingsKeys.MENTION_VOLUME, 0.8f)
}

class MentionSoundRepository(
    private val settings: Settings,
    private val files: SoundFileStore,
    private val builtIns: BuiltInSounds,
    private val importer: SoundImporter,
    private val decoder: AudioDecoder,
    private val scope: CoroutineScope,
    private val now: () -> Long,
    private val reloads: Flow<Unit> = emptyFlow(),
    private val random: Random = Random.Default
) : MentionSoundSource {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val _config = MutableStateFlow(loadConfig())
    override val config: StateFlow<MentionSoundConfig> = _config.asStateFlow()
    private val _library = MutableStateFlow(loadLibrary())
    val library: StateFlow<List<CustomSound>> = _library.asStateFlow()
    val builtInInfo: StateFlow<Map<String, SoundInfo>> = builtIns.info
    private var lastPreview: String? = null

    init {
        migrateLegacy()
        scope.launch {
            builtIns.warmUp()
            upgradeRawSounds()
        }
        scope.launch {
            reloads.collect {
                _config.value = loadConfig()
                _library.value = loadLibrary()
                migrateLegacy()
            }
        }
    }

    override fun exists(id: String): Boolean =
        BuiltInSound.byId(id) != null || _library.value.any { it.id == id && files.exists(it.file) }

    override suspend fun pathFor(id: String): String? {
        BuiltInSound.byId(id)?.let { return builtIns.path(it) }
        return _library.value.firstOrNull { it.id == id }?.file?.takeIf { files.exists(it) }
    }

    fun infoFor(id: String): SoundInfo? =
        builtIns.info.value[id] ?: _library.value.firstOrNull { it.id == id }?.let { SoundInfo(it.durationMs, it.peaks) }

    fun select(id: String) = updateConfig { it.copy(selectedId = id) }

    fun setCooldown(ms: Long) = updateConfig { it.copy(cooldownMs = ms.coerceIn(0L, MAX_COOLDOWN_MS)) }

    fun setMediaStream(enabled: Boolean) = updateConfig { it.copy(mediaStream = enabled) }

    fun setChannelSound(login: String, id: String?) {
        val key = login.trim().lowercase()
        if (key.isEmpty()) return
        updateConfig { cfg ->
            cfg.copy(channelSounds = if (id == null) cfg.channelSounds - key else cfg.channelSounds + (key to id))
        }
    }

    suspend fun import(path: String): SoundImportResult = mutex.withLock {
        val id = MentionSoundIds.newCustomId(now(), random.nextInt())
        val result = importer.import(path, id, now())
        if (result is SoundImportResult.Added) {
            saveLibrary(_library.value + result.sound)
            select(result.sound.id)
        }
        cleanupPicked(path)
        result
    }

    suspend fun loadSource(id: String): PcmAudio? {
        val sound = _library.value.firstOrNull { it.id == id } ?: return null
        val path = sound.source?.takeIf { files.exists(it) } ?: sound.file
        return decoder.decode(path)
    }

    suspend fun saveEdit(id: String, name: String, edit: SoundEdit, source: PcmAudio): CustomSound? = mutex.withLock {
        val sound = _library.value.firstOrNull { it.id == id } ?: return@withLock null
        val safe = SoundProcessing.clampEdit(edit, source.durationMs)
        val rendered = importer.render(source, id, safe, now()) ?: return@withLock null
        val updated = sound.copy(
            name = name.trim().take(MAX_NAME).ifBlank { sound.name },
            file = rendered.path,
            durationMs = rendered.durationMs,
            sourceDurationMs = source.durationMs,
            edit = safe,
            peaks = rendered.peaks
        )
        saveLibrary(_library.value.map { if (it.id == id) updated else it })
        if (sound.file != rendered.path && sound.file != sound.source) files.delete(sound.file)
        updated
    }

    suspend fun preview(source: PcmAudio, edit: SoundEdit): String? {
        val rendered = SoundProcessing.render(source, SoundProcessing.clampEdit(edit, source.durationMs))
        val path = importer.writePreview(rendered, now()) ?: return null
        lastPreview?.let { files.delete(it) }
        lastPreview = path
        return path
    }

    suspend fun delete(id: String) = mutex.withLock {
        val sound = _library.value.firstOrNull { it.id == id } ?: return@withLock
        saveLibrary(_library.value.filterNot { it.id == id })
        updateConfig { cfg ->
            cfg.copy(
                selectedId = if (cfg.selectedId == id) BuiltInSound.Default.id else cfg.selectedId,
                channelSounds = cfg.channelSounds.filterValues { it != id }
            )
        }
        files.delete(sound.file)
        sound.source?.let { files.delete(it) }
        files.delete(files.resolve("${SoundImporter.LIBRARY}/${SoundImporter.folder(id)}"))
    }

    private suspend fun upgradeRawSounds() = mutex.withLock {
        _library.value.filter { it.source == null }.forEach { raw ->
            val upgraded = importer.upgrade(raw, now()) ?: return@forEach
            saveLibrary(_library.value.map { if (it.id == raw.id) upgraded else it })
            files.delete(raw.file)
        }
    }

    private suspend fun cleanupPicked(path: String) {
        if (!files.contains(path)) return
        val relative = path.replace('\\', '/').removePrefix(files.root.replace('\\', '/')).trimStart('/')
        if (relative.contains('/')) return
        if (_library.value.any { it.file == path || it.source == path }) return
        files.delete(path)
    }

    private fun migrateLegacy() {
        val legacy = settings.getStringOrNull(SettingsKeys.CUSTOM_SOUND_PATH)?.trim().orEmpty()
        if (legacy.isEmpty()) return
        settings.remove(SettingsKeys.CUSTOM_SOUND_PATH)
        if (_library.value.any { it.file == legacy || it.source == legacy } || !files.exists(legacy)) return
        val sound = CustomSound(
            id = MentionSoundIds.newCustomId(now(), random.nextInt()),
            name = "",
            file = legacy,
            addedAt = now()
        )
        saveLibrary(_library.value + sound)
        if (!settings.hasKey(KEY_SELECTED)) select(sound.id)
    }

    private fun updateConfig(transform: (MentionSoundConfig) -> MentionSoundConfig) {
        val next = transform(_config.value)
        _config.value = next
        settings.putString(KEY_SELECTED, next.selectedId)
        settings.putInt(KEY_COOLDOWN, next.cooldownMs.toInt())
        settings.putBoolean(KEY_MEDIA_STREAM, next.mediaStream)
        settings.putString(KEY_CHANNELS, json.encodeToString(next.channelSounds))
    }

    private fun saveLibrary(next: List<CustomSound>) {
        _library.update { next }
        settings.putString(KEY_LIBRARY, json.encodeToString(next))
    }

    private fun loadConfig(): MentionSoundConfig = MentionSoundConfig(
        selectedId = settings.getStringOrNull(KEY_SELECTED) ?: BuiltInSound.Default.id,
        cooldownMs = runCatching { settings.getIntOrNull(KEY_COOLDOWN) }.getOrNull()?.toLong()?.coerceIn(0L, MAX_COOLDOWN_MS) ?: 0L,
        channelSounds = settings.getStringOrNull(KEY_CHANNELS)
            ?.let { runCatching { json.decodeFromString<Map<String, String>>(it) }.getOrNull() }
            .orEmpty(),
        mediaStream = settings.getBooleanOrNull(KEY_MEDIA_STREAM) ?: false
    )

    private fun loadLibrary(): List<CustomSound> =
        settings.getStringOrNull(KEY_LIBRARY)
            ?.let { runCatching { json.decodeFromString<List<CustomSound>>(it) }.getOrNull() }
            .orEmpty()

    companion object {
        const val KEY_SELECTED = "mention_sound_id"
        const val KEY_COOLDOWN = "mention_sound_cooldown_ms"
        const val KEY_CHANNELS = "mention_sound_channels"
        const val KEY_MEDIA_STREAM = "mention_sound_media_volume"
        const val KEY_LIBRARY = "mention_sound_library"
        const val MAX_COOLDOWN_MS = 120_000L
        private const val MAX_NAME = 40
    }
}
