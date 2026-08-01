package io.rudione.chatone.util.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val TwitchBitsIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "TwitchBits",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.Black),
        pathFillType = PathFillType.EvenOdd
    ) {
        moveTo(12f, 22f)
        lineToRelative(-9f, -8f)
        lineToRelative(9f, -13f)
        lineToRelative(9f, 13f)
        lineToRelative(-9f, 8f)
        close()
        moveToRelative(-4.844f, -6.982f)
        lineToRelative(-1.503f, -1.336f)
        lineTo(12f, 4.514f)
        lineToRelative(6.347f, 9.168f)
        lineToRelative(-1.503f, 1.336f)
        lineTo(12f, 13f)
        lineToRelative(-4.844f, 2.018f)
        close()
    }.build()
}

val TwitchPointsIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "TwitchPoints",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 5f)
            verticalLineToRelative(2f)
            arcToRelative(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 5f, dy1 = 5f)
            horizontalLineToRelative(2f)
            arcToRelative(7f, 7f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -7f, dy1 = -7f)
            close()
        }
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.EvenOdd
        ) {
            moveTo(1f, 12f)
            curveTo(1f, 5.925f, 5.925f, 1f, 12f, 1f)
            reflectiveCurveToRelative(11f, 4.925f, 11f, 11f)
            reflectiveCurveToRelative(-4.925f, 11f, -11f, 11f)
            reflectiveCurveTo(1f, 18.075f, 1f, 12f)
            close()
            moveToRelative(11f, 9f)
            arcToRelative(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = -18f)
            arcToRelative(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 0f, dy1 = 18f)
            close()
        }
    }.build()
}
