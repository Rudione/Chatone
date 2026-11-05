package io.rudione.chatone.presentation.profile

import io.rudione.chatone.domain.media.ClipOrigin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PageNavigatorTest {

    @Test
    fun opensNormalizedChannelsAndGoesBack() {
        val navigator = PageNavigator(isSupported = true)
        navigator.openChannel("#LapShad")
        navigator.openEditor(null)
        assertEquals(
            listOf(PageDestination.Channel("lapshad"), PageDestination.Editor(null)),
            navigator.stack.value
        )
        navigator.back()
        assertEquals(listOf(PageDestination.Channel("lapshad")), navigator.stack.value)
    }

    @Test
    fun reopeningMovesTheChannelToTheTopWithoutDuplicates() {
        val navigator = PageNavigator(isSupported = true)
        navigator.openChannel("a")
        navigator.openChannel("b")
        navigator.openChannel("a")
        navigator.openChannel("a")
        assertEquals(listOf("b", "a"), navigator.stack.value.map { (it as PageDestination.Channel).login })
    }

    @Test
    fun mediaPagesValidateIds() {
        val navigator = PageNavigator(isSupported = true)
        navigator.openClip("../bad slug")
        navigator.openVideo("abc")
        navigator.openClip("ExuberantDrab-pQvpycm")
        navigator.openVideo("2911576040", 90)
        navigator.createClip("#LapShad")
        assertEquals(
            listOf(
                PageDestination.Clip("ExuberantDrab-pQvpycm"),
                PageDestination.Video("2911576040", 90),
                PageDestination.ClipCreate("lapshad", ClipOrigin.Live(null))
            ),
            navigator.stack.value
        )
    }

    @Test
    fun unsupportedPlatformNeverOpens() {
        val navigator = PageNavigator(isSupported = false)
        navigator.openChannel("a")
        navigator.openEditor("b")
        assertTrue(navigator.stack.value.isEmpty())
    }
}
