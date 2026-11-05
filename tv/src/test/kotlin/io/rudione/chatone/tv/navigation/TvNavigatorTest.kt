package io.rudione.chatone.tv.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TvNavigatorTest {

    @Test
    fun rootIsNeverPopped() {
        val navigator = TvNavigator()
        assertFalse(navigator.back())
        assertEquals(TvRoute.Browse, navigator.top)
    }

    @Test
    fun openingAnotherStreamReplacesThePlayer() {
        val navigator = TvNavigator()
        navigator.open(TvRoute.Player("first"))
        navigator.open(TvRoute.Channel("first"))
        navigator.open(TvRoute.Player("second"))
        assertEquals(listOf(TvRoute.Browse, TvRoute.Channel("first"), TvRoute.Player("second")), navigator.stack)
    }

    @Test
    fun theSameRouteIsNotStacked() {
        val navigator = TvNavigator()
        navigator.open(TvRoute.Channel("one"))
        navigator.open(TvRoute.Channel("one"))
        assertEquals(2, navigator.stack.size)
        assertTrue(navigator.back())
        assertEquals(TvRoute.Browse, navigator.top)
    }

    @Test
    fun clipResultReplacesTheEditor() {
        val navigator = TvNavigator()
        navigator.open(TvRoute.Player("one"))
        navigator.open(TvRoute.ClipEditor("one", 2f))
        navigator.replaceTop(TvRoute.Channel("one"))
        assertEquals(listOf(TvRoute.Browse, TvRoute.Player("one"), TvRoute.Channel("one")), navigator.stack)
    }
}
