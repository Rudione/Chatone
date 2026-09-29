package io.rudione.chatone.icons.material.outlined

import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.materialIcon
import io.rudione.chatone.icons.material.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Outlined.DeleteSweep: ImageVector
    get() {
        if (_deleteSweep != null) {
            return _deleteSweep!!
        }
        _deleteSweep = materialIcon(name = "Outlined.DeleteSweep") {
            materialPath {
                moveTo(15.0f, 16.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(-4.0f)
                close()
                moveTo(15.0f, 8.0f)
                horizontalLineToRelative(7.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(-7.0f)
                close()
                moveTo(15.0f, 12.0f)
                horizontalLineToRelative(6.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(-6.0f)
                close()
                moveTo(3.0f, 18.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(6.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                lineTo(13.0f, 8.0f)
                lineTo(3.0f, 8.0f)
                verticalLineToRelative(10.0f)
                close()
                moveTo(5.0f, 10.0f)
                horizontalLineToRelative(6.0f)
                verticalLineToRelative(8.0f)
                lineTo(5.0f, 18.0f)
                verticalLineToRelative(-8.0f)
                close()
                moveTo(10.0f, 4.0f)
                lineTo(6.0f, 4.0f)
                lineTo(5.0f, 5.0f)
                lineTo(2.0f, 5.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(12.0f)
                lineTo(14.0f, 5.0f)
                horizontalLineToRelative(-3.0f)
                close()
            }
        }
        return _deleteSweep!!
    }

private var _deleteSweep: ImageVector? = null
