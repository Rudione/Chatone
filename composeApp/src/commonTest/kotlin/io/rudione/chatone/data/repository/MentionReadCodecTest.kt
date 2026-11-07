package io.rudione.chatone.data.repository

import io.rudione.chatone.domain.mention.MentionReadMark
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MentionReadCodecTest {

    @Test
    fun marksSurviveARoundTrip() {
        val mark = MentionReadMark(1_700_000_000_000, linkedSetOf("6f1c2a7e-0d1b-4c55-9a3e-2b8d1f0c9e11"))
        val encoded = MentionReadCodec.encode(mapOf("rudionee" to MentionReadCodec.toStored(mark, nowMs = 5)))
        val decoded = MentionReadCodec.decode(encoded)
        assertEquals(mark, decoded["rudionee"]?.let(MentionReadCodec::toMark))
    }

    @Test
    fun channelKeysAreNormalisedAndValidated() {
        assertEquals("rudionee", MentionReadCodec.channelKey(" #Rudionee "))
        assertNull(MentionReadCodec.channelKey("../prefs"))
        assertNull(MentionReadCodec.channelKey(""))
    }

    @Test
    fun brokenOrHostileDataIsIgnored() {
        assertTrue(MentionReadCodec.decode("{not json").isEmpty())
        val decoded = MentionReadCodec.decode("""{"Bad Key":{"watermarkMs":1},"ok":{"watermarkMs":2,"readIds":["a,b","good-id"]}}""")
        assertEquals(setOf("ok"), decoded.keys)
        assertEquals(setOf("good-id"), MentionReadCodec.toMark(decoded.getValue("ok")).readIds)
    }

    @Test
    fun onlyTheMostRecentChannelsAreKept() {
        val marks = (0 until 200).associate { index ->
            "channel_$index" to StoredMentionMark(watermarkMs = 1, updatedAtMs = index.toLong())
        }
        val decoded = MentionReadCodec.decode(MentionReadCodec.encode(marks))
        assertEquals(150, decoded.size)
        assertTrue("channel_199" in decoded)
        assertTrue("channel_0" !in decoded)
    }
}
