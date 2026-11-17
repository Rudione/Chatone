package io.rudione.chatone.domain.sound

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MentionSoundServiceTest {

    private class RecordingOutput : SoundOutput {
        val played = mutableListOf<Triple<String, Float, Boolean>>()
        val preloaded = mutableListOf<Collection<String>>()
        var stops = 0
        override fun preload(paths: Collection<String>, mediaStream: Boolean) {
            preloaded += paths
        }
        override fun play(path: String, volume: Float, mediaStream: Boolean) {
            played += Triple(path, volume, mediaStream)
        }
        override fun stop() {
            stops++
        }
    }

    private class FakeSource(config: MentionSoundConfig, private val custom: Set<String> = emptySet()) : MentionSoundSource {
        val state = MutableStateFlow(config)
        override val config: StateFlow<MentionSoundConfig> = state
        override suspend fun pathFor(id: String): String? = if (exists(id)) "/sounds/$id.wav" else null
        override fun exists(id: String): Boolean = BuiltInSound.byId(id) != null || id in custom
    }

    private class FakePrefs(override var enabled: Boolean = true, override var volume: Float = 0.6f) : MentionSoundPrefs

    private var clock = 0L

    private fun TestScope.service(source: FakeSource, output: RecordingOutput, prefs: FakePrefs = FakePrefs()) =
        MentionSoundService(source, output, prefs, TestScope(UnconfinedTestDispatcher(testScheduler)), now = { clock })

    @Test
    fun channelOverrideWinsOverSelection() = runTest {
        val output = RecordingOutput()
        val source = FakeSource(MentionSoundConfig(selectedId = BuiltInSound.Chime.id, channelSounds = mapOf("forsen" to "custom:a")), setOf("custom:a"))
        val service = service(source, output)
        assertTrue(service.playMention("Forsen"))
        assertTrue(service.playMention("xqc"))
        assertEquals("/sounds/custom:a.wav", output.played.first().first)
        assertEquals("/sounds/builtin:chime.wav", output.played.last().first)
    }

    @Test
    fun muteSilencesOnlyThatChannel() = runTest {
        val output = RecordingOutput()
        val service = service(FakeSource(MentionSoundConfig(channelSounds = mapOf("quiet" to MentionSoundIds.MUTE))), output)
        assertFalse(service.playMention("quiet"))
        assertTrue(service.playMention("loud"))
        assertEquals(1, output.played.size)
    }

    @Test
    fun missingSoundsFallBackToDefault() = runTest {
        val output = RecordingOutput()
        val source = FakeSource(MentionSoundConfig(selectedId = "custom:gone", channelSounds = mapOf("a" to "custom:gone2")))
        val service = service(source, output)
        assertEquals(BuiltInSound.Default.id, service.soundIdFor("a"))
        assertEquals(BuiltInSound.Default.id, service.soundIdFor(null))
    }

    @Test
    fun cooldownDropsBurstsButNotLaterMentions() = runTest {
        val output = RecordingOutput()
        val service = service(FakeSource(MentionSoundConfig(cooldownMs = 3_000)), output)
        clock = 10_000
        assertTrue(service.playMention("a"))
        clock = 11_000
        assertFalse(service.playMention("b"))
        clock = 13_500
        assertTrue(service.playMention("c"))
        assertEquals(2, output.played.size)
    }

    @Test
    fun disabledOrSilentPrefsPlayNothing() = runTest {
        val output = RecordingOutput()
        val prefs = FakePrefs(enabled = false)
        val service = service(FakeSource(MentionSoundConfig()), output, prefs)
        assertFalse(service.playMention("a"))
        prefs.enabled = true
        prefs.volume = 0f
        service.playMention("a")
        assertTrue(output.played.isEmpty())
    }

    @Test
    fun keywordSoundIgnoresMentionSwitchAndMute() = runTest {
        val output = RecordingOutput()
        val prefs = FakePrefs(enabled = false, volume = 0.4f)
        val source = FakeSource(MentionSoundConfig(selectedId = BuiltInSound.Bell.id, channelSounds = mapOf("a" to MentionSoundIds.MUTE), mediaStream = true))
        service(source, output, prefs).playKeyword("a")
        assertEquals(Triple("/sounds/builtin:bell.wav", 0.4f, true), output.played.single())
    }

    @Test
    fun previewStopsCurrentSoundAndUsesGivenVolume() = runTest {
        val output = RecordingOutput()
        val service = service(FakeSource(MentionSoundConfig()), output)
        service.preview(BuiltInSound.Drop.id, 0.9f)
        assertEquals(1, output.stops)
        assertEquals(0.9f, output.played.single().second)
    }

    @Test
    fun preloadsSelectedAndChannelSounds() = runTest {
        val output = RecordingOutput()
        val source = FakeSource(MentionSoundConfig(selectedId = BuiltInSound.Bell.id), setOf("custom:x"))
        service(source, output)
        source.state.value = source.state.value.copy(channelSounds = mapOf("a" to "custom:x", "b" to MentionSoundIds.MUTE))
        val last = output.preloaded.last()
        assertTrue("/sounds/custom:x.wav" in last)
        assertTrue("/sounds/builtin:bell.wav" in last)
        assertTrue(last.none { it.contains(MentionSoundIds.MUTE) })
    }
}
