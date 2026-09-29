package io.rudione.chatone.domain.stream

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StreamQualityResolverTest {

    private fun variant(group: String, name: String, height: Int, fps: Float, bandwidth: Long, codecs: String = "avc1.4D401F,mp4a.40.2") =
        StreamVariant(group, name, "https://x/$group.m3u8", bandwidth, height * 16 / 9, height, fps, codecs)

    private val variants = listOf(
        variant("chunked", "1080p60 (source)", 1080, 60f, 6_800_000),
        variant("720p60", "720p60", 720, 60f, 3_400_000),
        variant("480p30", "480p", 480, 30f, 1_400_000),
        variant("160p30", "160p", 160, 30f, 230_000),
        StreamVariant("audio_only", "audio_only", "https://x/a.m3u8", 160_000, 0, 0, 0f, "mp4a.40.2")
    )

    @Test
    fun exactGroupWins() {
        assertEquals(StreamQualitySelection.Fixed("720p60"), StreamQualityResolver.selectionFor("720p60", variants))
    }

    @Test
    fun missingGroupFallsBackToTheClosestLowerQuality() {
        assertEquals(StreamQualitySelection.Fixed("720p60"), StreamQualityResolver.selectionFor("720p30", variants))
        assertEquals(StreamQualitySelection.Fixed("480p30"), StreamQualityResolver.selectionFor("540p30", variants))
        assertEquals(StreamQualitySelection.Fixed("160p30"), StreamQualityResolver.selectionFor("144p30", variants))
    }

    @Test
    fun specialPreferencesMapToSelections() {
        assertEquals(StreamQualitySelection.Auto, StreamQualityResolver.selectionFor("auto", variants))
        assertEquals(StreamQualitySelection.AudioOnly, StreamQualityResolver.selectionFor("audio_only", variants))
        assertEquals(StreamQualitySelection.Fixed("chunked"), StreamQualityResolver.selectionFor("chunked", variants))
        assertEquals(StreamQualitySelection.Auto, StreamQualityResolver.selectionFor("garbage", variants))
        assertEquals(StreamQualitySelection.Auto, StreamQualityResolver.selectionFor("audio_only", variants.dropLast(1)))
    }

    @Test
    fun preferenceRoundTrips() {
        listOf(StreamQualitySelection.Auto, StreamQualitySelection.AudioOnly, StreamQualitySelection.Fixed("480p30")).forEach {
            assertEquals(it, StreamQualityResolver.selectionFor(StreamQualityResolver.preferenceOf(it), variants))
        }
    }

    @Test
    fun activeVariantIsMatchedByHeightAndBandwidth() {
        assertEquals("720p60", StreamQualityResolver.activeVariant(3_390_000, 720, variants)?.groupId)
        assertEquals("480p30", StreamQualityResolver.activeVariant(null, 480, variants)?.groupId)
        assertNull(StreamQualityResolver.activeVariant(null, null, variants))
    }

    @Test
    fun menuPutsSourceFirstAudioLastAndDropsDuplicateNames() {
        val withHevc = variants + variant("720p60_hevc", "720p60", 720, 60f, 2_000_000, "hvc1.1.6.L93.B0,mp4a.40.2")
        val menu = StreamQualityResolver.orderedForMenu(withHevc)
        assertEquals(listOf("chunked", "720p60", "480p30", "160p30", "audio_only"), menu.map { it.groupId })
    }
}
