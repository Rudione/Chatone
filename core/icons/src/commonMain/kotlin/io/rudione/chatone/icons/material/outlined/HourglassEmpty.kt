package io.rudione.chatone.icons.material.outlined

import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.materialIcon
import io.rudione.chatone.icons.material.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Outlined.HourglassEmpty: ImageVector
    get() {
        if (_hourglassEmpty != null) {
            return _hourglassEmpty!!
        }
        _hourglassEmpty = materialIcon(name = "Outlined.HourglassEmpty") {
            materialPath {
                moveTo(6.0f, 2.0f)
                verticalLineToRelative(6.0f)
                horizontalLineToRelative(0.01f)
                lineTo(6.0f, 8.01f)
                lineTo(10.0f, 12.0f)
                lineToRelative(-4.0f, 4.0f)
                lineToRelative(0.01f, 0.01f)
                lineTo(6.0f, 16.01f)
                lineTo(6.0f, 22.0f)
                horizontalLineToRelative(12.0f)
                verticalLineToRelative(-5.99f)
                horizontalLineToRelative(-0.01f)
                lineTo(18.0f, 16.0f)
                lineToRelative(-4.0f, -4.0f)
                lineToRelative(4.0f, -3.99f)
                lineToRelative(-0.01f, -0.01f)
                lineTo(18.0f, 8.0f)
                lineTo(18.0f, 2.0f)
                lineTo(6.0f, 2.0f)
                close()
                moveTo(16.0f, 16.5f)
                lineTo(16.0f, 20.0f)
                lineTo(8.0f, 20.0f)
                verticalLineToRelative(-3.5f)
                lineToRelative(4.0f, -4.0f)
                lineToRelative(4.0f, 4.0f)
                close()
                moveTo(12.0f, 11.5f)
                lineToRelative(-4.0f, -4.0f)
                lineTo(8.0f, 4.0f)
                horizontalLineToRelative(8.0f)
                verticalLineToRelative(3.5f)
                lineToRelative(-4.0f, 4.0f)
                close()
            }
        }
        return _hourglassEmpty!!
    }

private var _hourglassEmpty: ImageVector? = null
