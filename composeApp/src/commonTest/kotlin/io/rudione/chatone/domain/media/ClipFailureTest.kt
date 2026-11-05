package io.rudione.chatone.domain.media

import kotlin.test.Test
import kotlin.test.assertEquals

class ClipFailureTest {

    @Test
    fun twitchCodesMapToReasonsTheViewerCanAct() {
        assertEquals(ClipFailure.NOT_LIVE, ClipFailure.fromTwitchCode("CHANNEL_NOT_LIVE"))
        assertEquals(ClipFailure.FORBIDDEN, ClipFailure.fromTwitchCode("CHANNEL_NOT_CLIPPABLE"))
        assertEquals(ClipFailure.FORBIDDEN, ClipFailure.fromTwitchCode("CHANNEL_BANNED"))
        assertEquals(ClipFailure.BANNED, ClipFailure.fromTwitchCode("USER_BANNED"))
        assertEquals(ClipFailure.BANNED, ClipFailure.fromTwitchCode("USER_TIMED_OUT"))
        assertEquals(ClipFailure.RESTRICTED, ClipFailure.fromTwitchCode("USER_RESTRICTED"))
        assertEquals(ClipFailure.THROTTLED, ClipFailure.fromTwitchCode("request_throttled"))
    }

    @Test
    fun unknownCodesStayGenericUnlessTheyMentionDisabledClips() {
        assertEquals(ClipFailure.FAILED, ClipFailure.fromTwitchCode("UNKNOWN"))
        assertEquals(ClipFailure.FORBIDDEN, ClipFailure.fromTwitchCode("CLIPS_DISABLED"))
    }
}
