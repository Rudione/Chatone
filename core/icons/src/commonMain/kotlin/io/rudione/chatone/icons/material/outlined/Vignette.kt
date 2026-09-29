package io.rudione.chatone.icons.material.outlined

import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.materialIcon
import io.rudione.chatone.icons.material.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Outlined.Vignette: ImageVector
    get() {
        if (_vignette != null) {
            return _vignette!!
        }
        _vignette = materialIcon(name = "Outlined.Vignette") {
            materialPath {
                moveTo(21.0f, 5.0f)
                verticalLineToRelative(14.0f)
                lineTo(3.0f, 19.0f)
                lineTo(3.0f, 5.0f)
                horizontalLineToRelative(18.0f)
                moveToRelative(0.0f, -2.0f)
                lineTo(3.0f, 3.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(14.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(18.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                lineTo(23.0f, 5.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                close()
                moveTo(12.0f, 8.0f)
                curveToRelative(3.25f, 0.0f, 6.0f, 1.83f, 6.0f, 4.0f)
                reflectiveCurveToRelative(-2.75f, 4.0f, -6.0f, 4.0f)
                reflectiveCurveToRelative(-6.0f, -1.83f, -6.0f, -4.0f)
                reflectiveCurveToRelative(2.75f, -4.0f, 6.0f, -4.0f)
                moveToRelative(0.0f, -2.0f)
                curveToRelative(-4.42f, 0.0f, -8.0f, 2.69f, -8.0f, 6.0f)
                reflectiveCurveToRelative(3.58f, 6.0f, 8.0f, 6.0f)
                reflectiveCurveToRelative(8.0f, -2.69f, 8.0f, -6.0f)
                reflectiveCurveToRelative(-3.58f, -6.0f, -8.0f, -6.0f)
                close()
            }
        }
        return _vignette!!
    }

private var _vignette: ImageVector? = null
