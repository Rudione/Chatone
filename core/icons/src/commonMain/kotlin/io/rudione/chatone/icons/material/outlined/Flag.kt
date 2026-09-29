package io.rudione.chatone.icons.material.outlined

import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.materialIcon
import io.rudione.chatone.icons.material.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Outlined.Flag: ImageVector
    get() {
        if (_flag != null) {
            return _flag!!
        }
        _flag = materialIcon(name = "Outlined.Flag") {
            materialPath {
                moveTo(12.36f, 6.0f)
                lineToRelative(0.4f, 2.0f)
                horizontalLineTo(18.0f)
                verticalLineToRelative(6.0f)
                horizontalLineToRelative(-3.36f)
                lineToRelative(-0.4f, -2.0f)
                horizontalLineTo(7.0f)
                verticalLineTo(6.0f)
                horizontalLineToRelative(5.36f)
                moveTo(14.0f, 4.0f)
                horizontalLineTo(5.0f)
                verticalLineToRelative(17.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(-7.0f)
                horizontalLineToRelative(5.6f)
                lineToRelative(0.4f, 2.0f)
                horizontalLineToRelative(7.0f)
                verticalLineTo(6.0f)
                horizontalLineToRelative(-5.6f)
                lineTo(14.0f, 4.0f)
                close()
            }
        }
        return _flag!!
    }

private var _flag: ImageVector? = null
