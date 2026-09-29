package io.rudione.chatone.icons.material.outlined

import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.materialIcon
import io.rudione.chatone.icons.material.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Outlined.Wallpaper: ImageVector
    get() {
        if (_wallpaper != null) {
            return _wallpaper!!
        }
        _wallpaper = materialIcon(name = "Outlined.Wallpaper") {
            materialPath {
                moveTo(4.0f, 4.0f)
                horizontalLineToRelative(7.0f)
                lineTo(11.0f, 2.0f)
                lineTo(4.0f, 2.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(7.0f)
                horizontalLineToRelative(2.0f)
                lineTo(4.0f, 4.0f)
                close()
                moveTo(10.0f, 13.0f)
                lineToRelative(-4.0f, 5.0f)
                horizontalLineToRelative(12.0f)
                lineToRelative(-3.0f, -4.0f)
                lineToRelative(-2.03f, 2.71f)
                lineTo(10.0f, 13.0f)
                close()
                moveTo(17.0f, 8.5f)
                curveToRelative(0.0f, -0.83f, -0.67f, -1.5f, -1.5f, -1.5f)
                reflectiveCurveTo(14.0f, 7.67f, 14.0f, 8.5f)
                reflectiveCurveToRelative(0.67f, 1.5f, 1.5f, 1.5f)
                reflectiveCurveTo(17.0f, 9.33f, 17.0f, 8.5f)
                close()
                moveTo(20.0f, 2.0f)
                horizontalLineToRelative(-7.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(7.0f)
                verticalLineToRelative(7.0f)
                horizontalLineToRelative(2.0f)
                lineTo(22.0f, 4.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                close()
                moveTo(20.0f, 20.0f)
                horizontalLineToRelative(-7.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(7.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineToRelative(-7.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(7.0f)
                close()
                moveTo(4.0f, 13.0f)
                lineTo(2.0f, 13.0f)
                verticalLineToRelative(7.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(7.0f)
                verticalLineToRelative(-2.0f)
                lineTo(4.0f, 20.0f)
                verticalLineToRelative(-7.0f)
                close()
            }
        }
        return _wallpaper!!
    }

private var _wallpaper: ImageVector? = null
