package io.rudione.chatone.domain.model

data class SidebarLayout(
    val collapsed: Boolean = false,
    val widthDp: Float = DEFAULT_WIDTH_DP,
    val miniRailCollapsed: Boolean = false
) {
    companion object {
        const val DEFAULT_WIDTH_DP = 252f
    }
}
