package io.rudione.chatone.icons.material.outlined

import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.materialIcon
import io.rudione.chatone.icons.material.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Outlined.ViewSidebar: ImageVector
    get() {
        if (_viewSidebar != null) {
            return _viewSidebar!!
        }
        _viewSidebar = materialIcon(name = "Outlined.ViewSidebar") {
            materialPath {
                moveTo(2.0f, 4.0f)
                verticalLineToRelative(16.0f)
                horizontalLineToRelative(20.0f)
                verticalLineTo(4.0f)
                horizontalLineTo(2.0f)
                close()
                moveTo(20.0f, 8.67f)
                horizontalLineToRelative(-2.5f)
                verticalLineTo(6.0f)
                horizontalLineTo(20.0f)
                verticalLineTo(8.67f)
                close()
                moveTo(17.5f, 10.67f)
                horizontalLineTo(20.0f)
                verticalLineToRelative(2.67f)
                horizontalLineToRelative(-2.5f)
                verticalLineTo(10.67f)
                close()
                moveTo(4.0f, 6.0f)
                horizontalLineToRelative(11.5f)
                verticalLineToRelative(12.0f)
                horizontalLineTo(4.0f)
                verticalLineTo(6.0f)
                close()
                moveTo(17.5f, 18.0f)
                verticalLineToRelative(-2.67f)
                horizontalLineTo(20.0f)
                verticalLineTo(18.0f)
                horizontalLineTo(17.5f)
                close()
            }
        }
        return _viewSidebar!!
    }

private var _viewSidebar: ImageVector? = null
