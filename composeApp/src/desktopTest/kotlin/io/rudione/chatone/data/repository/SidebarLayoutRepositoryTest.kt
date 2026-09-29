package io.rudione.chatone.data.repository

import com.russhwolf.settings.PreferencesSettings
import io.rudione.chatone.domain.model.SidebarLayout
import java.util.prefs.Preferences
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SidebarLayoutRepositoryTest {

    private val node = Preferences.userRoot().node("chatone-test-sidebar-${System.nanoTime()}")
    private val repository = SidebarLayoutRepository(PreferencesSettings(node))

    @AfterTest
    fun removeNode() {
        node.removeNode()
    }

    @Test
    fun defaultsWhenNothingWasSaved() {
        assertEquals(SidebarLayout(), repository.load())
    }

    @Test
    fun restoresEverythingThatWasSaved() {
        repository.saveCollapsed(true)
        repository.saveWidth(318f)
        repository.saveMiniRailCollapsed(true)
        assertEquals(SidebarLayout(collapsed = true, widthDp = 318f, miniRailCollapsed = true), repository.load())
    }

    @Test
    fun rejectsWidthsThatCannotBeLaidOut() {
        repository.saveWidth(Float.NaN)
        repository.saveWidth(-5f)
        repository.saveWidth(10_000f)
        assertEquals(SidebarLayout.DEFAULT_WIDTH_DP, repository.load().widthDp)
    }
}
