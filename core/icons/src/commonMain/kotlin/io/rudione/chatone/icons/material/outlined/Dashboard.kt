package io.rudione.chatone.icons.material.outlined

import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.materialIcon
import io.rudione.chatone.icons.material.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Outlined.Dashboard: ImageVector
    get() {
        if (_dashboard != null) {
            return _dashboard!!
        }
        _dashboard = materialIcon(name = "Outlined.Dashboard") {
            materialPath {
                moveTo(19.0f, 5.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(-4.0f)
                lineTo(15.0f, 5.0f)
                horizontalLineToRelative(4.0f)
                moveTo(9.0f, 5.0f)
                verticalLineToRelative(6.0f)
                lineTo(5.0f, 11.0f)
                lineTo(5.0f, 5.0f)
                horizontalLineToRelative(4.0f)
                moveToRelative(10.0f, 8.0f)
                verticalLineToRelative(6.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineToRelative(-6.0f)
                horizontalLineToRelative(4.0f)
                moveTo(9.0f, 17.0f)
                verticalLineToRelative(2.0f)
                lineTo(5.0f, 19.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(4.0f)
                moveTo(21.0f, 3.0f)
                horizontalLineToRelative(-8.0f)
                verticalLineToRelative(6.0f)
                horizontalLineToRelative(8.0f)
                lineTo(21.0f, 3.0f)
                close()
                moveTo(11.0f, 3.0f)
                lineTo(3.0f, 3.0f)
                verticalLineToRelative(10.0f)
                horizontalLineToRelative(8.0f)
                lineTo(11.0f, 3.0f)
                close()
                moveTo(21.0f, 11.0f)
                horizontalLineToRelative(-8.0f)
                verticalLineToRelative(10.0f)
                horizontalLineToRelative(8.0f)
                lineTo(21.0f, 11.0f)
                close()
                moveTo(11.0f, 15.0f)
                lineTo(3.0f, 15.0f)
                verticalLineToRelative(6.0f)
                horizontalLineToRelative(8.0f)
                verticalLineToRelative(-6.0f)
                close()
            }
        }
        return _dashboard!!
    }

private var _dashboard: ImageVector? = null
