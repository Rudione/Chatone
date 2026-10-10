package io.rudione.chatone.presentation.stream

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoCodecQuirksTest {

    @Test
    fun exynosOmxDecodersReopenInsteadOfSwitchingSurfaces() {
        assertTrue(VideoCodecQuirks.reopensOnSurfaceChange("OMX.Exynos.avc.dec"))
        assertTrue(VideoCodecQuirks.reopensOnSurfaceChange("omx.exynos.hevc.dec"))
    }

    @Test
    fun otherDecodersKeepSeamlessSurfaceSwitching() {
        assertFalse(VideoCodecQuirks.reopensOnSurfaceChange("c2.exynos.h264.decoder"))
        assertFalse(VideoCodecQuirks.reopensOnSurfaceChange("OMX.qcom.video.decoder.avc"))
        assertFalse(VideoCodecQuirks.reopensOnSurfaceChange("c2.android.avc.decoder"))
    }
}
