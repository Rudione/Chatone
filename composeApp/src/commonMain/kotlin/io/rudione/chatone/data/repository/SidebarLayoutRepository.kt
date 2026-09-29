package io.rudione.chatone.data.repository

import com.russhwolf.settings.Settings
import io.rudione.chatone.domain.model.SidebarLayout

class SidebarLayoutRepository(private val settings: Settings) {

    fun load(): SidebarLayout = SidebarLayout(
        collapsed = settings.getBoolean(KEY_COLLAPSED, false),
        widthDp = settings.getFloat(KEY_WIDTH, SidebarLayout.DEFAULT_WIDTH_DP)
            .takeIf { it.isValidWidth() }
            ?: SidebarLayout.DEFAULT_WIDTH_DP,
        miniRailCollapsed = settings.getBoolean(KEY_MINI_RAIL_COLLAPSED, false)
    )

    fun saveCollapsed(collapsed: Boolean) {
        settings.putBoolean(KEY_COLLAPSED, collapsed)
    }

    fun saveWidth(widthDp: Float) {
        if (widthDp.isValidWidth()) settings.putFloat(KEY_WIDTH, widthDp)
    }

    fun saveMiniRailCollapsed(collapsed: Boolean) {
        settings.putBoolean(KEY_MINI_RAIL_COLLAPSED, collapsed)
    }

    private fun Float.isValidWidth(): Boolean = isFinite() && this in MIN_WIDTH_DP..MAX_WIDTH_DP

    private companion object {
        const val KEY_COLLAPSED = "sidebar_collapsed"
        const val KEY_WIDTH = "sidebar_width_dp"
        const val KEY_MINI_RAIL_COLLAPSED = "mini_rail_collapsed"
        const val MIN_WIDTH_DP = 120f
        const val MAX_WIDTH_DP = 2_000f
    }
}
