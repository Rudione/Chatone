package io.rudione.chatone.data.sound

import io.rudione.chatone.domain.sound.PcmAudio
import io.rudione.chatone.domain.sound.WavCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalSoundFileStoreTest {
    private val base: File = Files.createTempDirectory("chatone-sounds").toFile()
    private val root = File(base, "sounds")
    private val store = LocalSoundFileStore(root.absolutePath, Dispatchers.IO)

    @AfterTest
    fun cleanup() {
        base.deleteRecursively()
    }

    @Test
    fun writeReadAndDeleteStayInsideRoot() = runBlocking {
        val path = assertNotNull(store.write("library/a/sound.wav", byteArrayOf(1, 2, 3)))
        assertTrue(path.startsWith(root.absolutePath))
        assertContentEquals(byteArrayOf(1, 2, 3), store.read(path))
        assertEquals(3L, store.size(path))
        assertFalse(File("$path.part").exists())
        store.delete(path)
        assertFalse(store.exists(path))
        val folder = store.resolve("library/a")
        store.delete(folder)
        assertFalse(File(folder).exists())
    }

    @Test
    fun copyKeepsTheOriginalAndRefusesTraversal() = runBlocking {
        val outside = File(base, "picked.wav").apply { writeBytes(byteArrayOf(9, 8, 7)) }
        val copy = assertNotNull(store.copyIn(outside.absolutePath, "library/b/source.wav"))
        outside.delete()
        assertContentEquals(byteArrayOf(9, 8, 7), store.read(copy))
        assertFailsWith<IllegalArgumentException> { store.resolve("../evil.wav") }
        assertNull(store.write("../evil.wav", byteArrayOf(1)))
        assertFalse(File(base, "evil.wav").exists())
        val stranger = File(base, "keep.wav").apply { writeBytes(byteArrayOf(1)) }
        store.delete(stranger.absolutePath)
        assertTrue(stranger.exists())
        assertFalse(store.contains(File(root, "../keep.wav").path))
    }

    @Test
    fun readRespectsSizeLimit() = runBlocking {
        val path = assertNotNull(store.write("big.bin", ByteArray(64)))
        assertNull(store.read(path, maxBytes = 10))
    }

    @Test
    fun desktopDecodesWavAndAiff() = runBlocking {
        val pcm = PcmAudio(22_050, 2, FloatArray(22_050) { if (it % 2 == 0) 0.3f else -0.3f })
        val wav = File(base, "tone.wav").apply { writeBytes(WavCodec.encode(pcm)) }
        val aiff = File(base, "tone.aiff")
        AudioSystem.getAudioInputStream(wav).use { stream ->
            val bigEndian = AudioFormat(22_050f, 16, 2, true, true)
            AudioSystem.write(AudioInputStream(AudioSystem.getAudioInputStream(bigEndian, stream), bigEndian, 11_025), AudioFileFormat.Type.AIFF, aiff)
        }
        val decoder = AudioDecoder(store)
        listOf(wav, aiff).forEach { file ->
            val decoded = assertNotNull(decoder.decode(file.absolutePath), file.name)
            assertEquals(2, decoded.channels)
            assertEquals(500L, decoded.durationMs)
        }
    }
}
