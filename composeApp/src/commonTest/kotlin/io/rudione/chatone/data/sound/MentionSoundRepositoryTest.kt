package io.rudione.chatone.data.sound

import com.russhwolf.settings.Settings
import io.rudione.chatone.domain.sound.BuiltInSound
import io.rudione.chatone.domain.sound.MentionSoundIds
import io.rudione.chatone.domain.sound.PcmAudio
import io.rudione.chatone.domain.sound.SoundEdit
import io.rudione.chatone.domain.sound.SoundLimits
import io.rudione.chatone.domain.sound.WavCodec
import io.rudione.chatone.presentation.settings.SettingsKeys
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MentionSoundRepositoryTest {

    private class MemoryFiles(override val root: String = "/app/sounds") : SoundFileStore {
        val files = LinkedHashMap<String, ByteArray>()
        override fun exists(path: String) = path in files
        override fun size(path: String) = files[path]?.size?.toLong() ?: -1L
        override fun contains(path: String) = path.startsWith("$root/") && path.split('/').none { it == ".." }
        override fun resolve(relative: String) = "$root/$relative"
        override suspend fun read(path: String, maxBytes: Long) = files[path]?.takeIf { it.size <= maxBytes }
        override suspend fun write(relative: String, bytes: ByteArray) = resolve(relative).also { files[it] = bytes }
        override suspend fun copyIn(source: String, relative: String) = files[source]?.let { bytes -> resolve(relative).also { files[it] = bytes } }
        override suspend fun delete(path: String) {
            if (contains(path)) files.remove(path)
        }
    }

    private class InMemorySettings : Settings {
        private val values = HashMap<String, Any>()
        override val keys: Set<String> get() = values.keys
        override val size: Int get() = values.size
        override fun clear() = values.clear()
        override fun remove(key: String) {
            values.remove(key)
        }
        override fun hasKey(key: String): Boolean = key in values
        override fun putInt(key: String, value: Int) = put(key, value)
        override fun getInt(key: String, defaultValue: Int): Int = values[key] as? Int ?: defaultValue
        override fun getIntOrNull(key: String): Int? = values[key] as? Int
        override fun putLong(key: String, value: Long) = put(key, value)
        override fun getLong(key: String, defaultValue: Long): Long = values[key] as? Long ?: defaultValue
        override fun getLongOrNull(key: String): Long? = values[key] as? Long
        override fun putString(key: String, value: String) = put(key, value)
        override fun getString(key: String, defaultValue: String): String = values[key] as? String ?: defaultValue
        override fun getStringOrNull(key: String): String? = values[key] as? String
        override fun putFloat(key: String, value: Float) = put(key, value)
        override fun getFloat(key: String, defaultValue: Float): Float = values[key] as? Float ?: defaultValue
        override fun getFloatOrNull(key: String): Float? = values[key] as? Float
        override fun putDouble(key: String, value: Double) = put(key, value)
        override fun getDouble(key: String, defaultValue: Double): Double = values[key] as? Double ?: defaultValue
        override fun getDoubleOrNull(key: String): Double? = values[key] as? Double
        override fun putBoolean(key: String, value: Boolean) = put(key, value)
        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = values[key] as? Boolean ?: defaultValue
        override fun getBooleanOrNull(key: String): Boolean? = values[key] as? Boolean

        private fun put(key: String, value: Any) {
            values[key] = value
        }
    }

    private val files = MemoryFiles()
    private val settings = InMemorySettings()
    private val reloads = MutableSharedFlow<Unit>()
    private var clock = 1_700_000_000_000L

    private fun wav(ms: Int, rate: Int = 8_000, leadSilenceMs: Int = 0): ByteArray {
        val lead = rate * leadSilenceMs / 1000
        val frames = lead + rate * ms / 1000
        return WavCodec.encode(PcmAudio(rate, 1, FloatArray(frames) { if (it < lead) 0f else 0.4f }))
    }

    private fun TestScope.repository(): MentionSoundRepository {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val decoder = AudioDecoder(files) { _, _ -> null }
        return MentionSoundRepository(
            settings = settings,
            files = files,
            builtIns = BuiltInSounds(files) { wav(400) },
            importer = SoundImporter(files, decoder, dispatcher),
            decoder = decoder,
            scope = TestScope(dispatcher),
            now = { clock++ },
            reloads = reloads,
            random = Random(7)
        )
    }

    @Test
    fun importCopiesIntoLibrarySelectsAndRemovesPickedTemp() = runTest {
        val picked = "${files.root}/Ping_Sound.wav"
        files.files[picked] = wav(600, leadSilenceMs = 300)
        val repo = repository()
        val result = assertIs<SoundImportResult.Added>(repo.import(picked))
        val sound = result.sound
        assertEquals("Ping Sound", sound.name)
        assertFalse(result.needsTrim)
        assertEquals(sound.id, repo.config.value.selectedId)
        assertTrue(files.exists(sound.file))
        assertTrue(sound.source!!.startsWith("${files.root}/library/${SoundImporter.folder(sound.id)}/"))
        assertFalse(sound.file.removePrefix(files.root).contains(':'))
        assertFalse(files.exists(picked))
        assertTrue(sound.edit.startMs >= 280)
        assertEquals(SoundLimits.PEAK_BUCKETS, sound.peaks.size)
        assertEquals(sound.id, repo.library.value.single().id)
    }

    @Test
    fun importKeepsFilesOutsideAppFolder() = runTest {
        val external = "/Users/me/Downloads/tag.wav"
        files.files[external] = wav(300)
        assertIs<SoundImportResult.Added>(repository().import(external))
        assertTrue(files.exists(external))
    }

    @Test
    fun longSoundIsCutToLimitAndAsksForTrim() = runTest {
        files.files["/x/long.wav"] = wav(9_000)
        val result = assertIs<SoundImportResult.Added>(repository().import("/x/long.wav"))
        assertTrue(result.needsTrim)
        assertEquals(SoundLimits.MAX_CLIP_MS, result.sound.edit.endMs - result.sound.edit.startMs)
    }

    @Test
    fun unsupportedFilesLeaveNothingBehind() = runTest {
        files.files["/x/notes.txt"] = byteArrayOf(1, 2, 3)
        files.files["/x/broken.mp3"] = byteArrayOf(1, 2, 3)
        val repo = repository()
        val before = files.files.keys.toSet()
        assertEquals(SoundImportResult.Unsupported, repo.import("/x/notes.txt"))
        assertEquals(SoundImportResult.Unsupported, repo.import("/x/broken.mp3"))
        assertEquals(before, files.files.keys.toSet())
        assertTrue(repo.library.value.isEmpty())
        assertEquals(BuiltInSound.Default.id, repo.config.value.selectedId)
    }

    @Test
    fun legacyCustomSoundIsMigratedAndUpgraded() = runTest {
        val legacy = "${files.root}/mention_sound_123.wav"
        files.files[legacy] = wav(700)
        settings.putString(SettingsKeys.CUSTOM_SOUND_PATH, legacy)
        val repo = repository()
        val sound = repo.library.value.single()
        assertEquals(sound.id, repo.config.value.selectedId)
        assertNull(settings.getStringOrNull(SettingsKeys.CUSTOM_SOUND_PATH))
        assertNotNull(sound.source)
        assertTrue(sound.peaks.isNotEmpty())
        assertFalse(files.exists(legacy))
        assertEquals(sound.file, repo.pathFor(sound.id))
    }

    @Test
    fun legacyMigrationKeepsAnExplicitSelection() = runTest {
        files.files["${files.root}/old.wav"] = wav(300)
        settings.putString(SettingsKeys.CUSTOM_SOUND_PATH, "${files.root}/old.wav")
        settings.putString(MentionSoundRepository.KEY_SELECTED, BuiltInSound.Bell.id)
        assertEquals(BuiltInSound.Bell.id, repository().config.value.selectedId)
    }

    @Test
    fun deletingSelectedSoundResetsSelectionAndChannels() = runTest {
        files.files["/x/a.wav"] = wav(300)
        val repo = repository()
        val sound = assertIs<SoundImportResult.Added>(repo.import("/x/a.wav")).sound
        repo.setChannelSound("Forsen", sound.id)
        repo.setChannelSound("xqc", MentionSoundIds.MUTE)
        repo.delete(sound.id)
        assertEquals(BuiltInSound.Default.id, repo.config.value.selectedId)
        assertEquals(mapOf("xqc" to MentionSoundIds.MUTE), repo.config.value.channelSounds)
        assertFalse(files.exists(sound.file))
        assertFalse(files.exists(sound.source!!))
        assertFalse(repo.exists(sound.id))
    }

    @Test
    fun savingAnEditRendersNewFileAndDropsOldOne() = runTest {
        files.files["/x/a.wav"] = wav(2_000)
        val repo = repository()
        val added = assertIs<SoundImportResult.Added>(repo.import("/x/a.wav"))
        val saved = assertNotNull(repo.saveEdit(added.sound.id, "  My ping  ", SoundEdit(startMs = 500, endMs = 900), added.source))
        assertEquals("My ping", saved.name)
        assertEquals(400L, saved.durationMs)
        assertTrue(files.exists(saved.file))
        assertFalse(files.exists(added.sound.file))
    }

    @Test
    fun builtInsAreWrittenOnDemandAndConfigPersists() = runTest {
        val repo = repository()
        val path = assertNotNull(repo.pathFor(BuiltInSound.Chime.id))
        assertTrue(files.exists(path))
        assertNotNull(repo.infoFor(BuiltInSound.Tag.id))
        repo.setCooldown(10_000)
        repo.setMediaStream(true)
        repo.setChannelSound(" XQC ", BuiltInSound.Drop.id)
        val reloaded = repository().config.value
        assertEquals(10_000L, reloaded.cooldownMs)
        assertTrue(reloaded.mediaStream)
        assertEquals(mapOf("xqc" to BuiltInSound.Drop.id), reloaded.channelSounds)
    }

    @Test
    fun externalSettingsReloadRefreshesConfig() = runTest {
        val repo = repository()
        settings.putString(MentionSoundRepository.KEY_SELECTED, BuiltInSound.Beep.id)
        reloads.emit(Unit)
        assertEquals(BuiltInSound.Beep.id, repo.config.value.selectedId)
    }
}
