package io.rudione.chatone.icons.material.outlined

import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.materialIcon
import io.rudione.chatone.icons.material.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Outlined.Crop75: ImageVector
    get() {
        if (_crop75 != null) {
            return _crop75!!
        }
        _crop75 = materialIcon(name = "Outlined.Crop75") {
            materialPath {
                moveTo(19.0f, 5.0f)
                horizontalLineTo(5.0f)
                curveTo(3.9f, 5.0f, 3.0f, 5.9f, 3.0f, 7.0f)
                verticalLineToRelative(10.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(14.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineTo(7.0f)
                curveTo(21.0f, 5.9f, 20.1f, 5.0f, 19.0f, 5.0f)
                close()
                moveTo(19.0f, 17.0f)
                horizontalLineTo(5.0f)
                verticalLineTo(7.0f)
                horizontalLineToRelative(14.0f)
                verticalLineTo(17.0f)
                close()
            }
        }
        return _crop75!!
    }

private var _crop75: ImageVector? = null
