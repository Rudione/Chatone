package io.rudione.chatone.presentation.components.expressive

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.theme.ChatoneBrand

private const val VIEWPORT = 66f
private const val BODY = "M54.61 17.3A26.71 26.71 0 1 0 54.61 48.7L43.05 40.3A12.42 12.42 0 1 1 43.05 25.7Z"
private const val DAY_CLIP = "M240.5 -153.83L54.6 13.55A29.07 9.81 -42 0 1 11.4 52.45L-174.5 219.83L-340.76 35.18L74.24 -338.48Z"
private const val NIGHT_CLIP = "M240.5 -153.83L57.1 11.3A32.43 13.17 -42 0 1 8.9 54.7L-174.5 219.83L-8.24 404.48L406.76 30.82Z"
private const val ORBIT = "M55.85 12.43A30.75 11.49 -42 1 1 10.15 53.57A30.75 11.49 -42 1 1 55.85 12.43"
private const val ORBIT_CLIP = "M0 0L0 66L66 66L66 0ZM54.61 17.3L234.02 -113.05L234.02 179.05L54.61 48.7A26.71 26.71 0 1 1 54.61 17.3Z"
private const val ORBIT_WIDTH = 1.86f

val ChatoneMark: ImageVector by lazy {
    ImageVector.Builder(
        name = "ChatoneMark",
        defaultWidth = VIEWPORT.dp,
        defaultHeight = VIEWPORT.dp,
        viewportWidth = VIEWPORT,
        viewportHeight = VIEWPORT
    ).apply {
        addGroup(name = "orbit", clipPathData = addPathNodes(ORBIT_CLIP))
        addPath(pathData = addPathNodes(ORBIT), stroke = SolidColor(ChatoneBrand.Orbit), strokeLineWidth = ORBIT_WIDTH)
        clearGroup()
        addGroup(name = "day", clipPathData = addPathNodes(DAY_CLIP))
        addPath(pathData = addPathNodes(BODY), fill = SolidColor(ChatoneBrand.Accent))
        clearGroup()
        addGroup(name = "night", clipPathData = addPathNodes(NIGHT_CLIP))
        addPath(pathData = addPathNodes(BODY), fill = SolidColor(ChatoneBrand.Night))
        clearGroup()
    }.build()
}
