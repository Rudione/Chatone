package io.rudione.chatone.icons.material.outlined

import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.materialIcon
import io.rudione.chatone.icons.material.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Outlined.PersonRemove: ImageVector
    get() {
        if (_personRemove != null) {
            return _personRemove!!
        }
        _personRemove = materialIcon(name = "Outlined.PersonRemove") {
            materialPath {
                moveTo(14.0f, 8.0f)
                curveToRelative(0.0f, -2.21f, -1.79f, -4.0f, -4.0f, -4.0f)
                curveTo(7.79f, 4.0f, 6.0f, 5.79f, 6.0f, 8.0f)
                curveToRelative(0.0f, 2.21f, 1.79f, 4.0f, 4.0f, 4.0f)
                curveTo(12.21f, 12.0f, 14.0f, 10.21f, 14.0f, 8.0f)
                close()
                moveTo(12.0f, 8.0f)
                curveToRelative(0.0f, 1.1f, -0.9f, 2.0f, -2.0f, 2.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, -0.9f, -2.0f, -2.0f)
                reflectiveCurveToRelative(0.9f, -2.0f, 2.0f, -2.0f)
                curveTo(11.1f, 6.0f, 12.0f, 6.9f, 12.0f, 8.0f)
                close()
            }
            materialPath {
                moveTo(2.0f, 18.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(16.0f)
                verticalLineToRelative(-2.0f)
                curveToRelative(0.0f, -2.66f, -5.33f, -4.0f, -8.0f, -4.0f)
                curveTo(7.33f, 14.0f, 2.0f, 15.34f, 2.0f, 18.0f)
                close()
                moveTo(4.0f, 18.0f)
                curveToRelative(0.2f, -0.71f, 3.3f, -2.0f, 6.0f, -2.0f)
                curveToRelative(2.69f, 0.0f, 5.77f, 1.28f, 6.0f, 2.0f)
                horizontalLineTo(4.0f)
                close()
            }
            materialPath {
                moveTo(17.0f, 10.0f)
                horizontalLineToRelative(6.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(-6.0f)
                close()
            }
        }
        return _personRemove!!
    }

private var _personRemove: ImageVector? = null
