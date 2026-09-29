package io.rudione.chatone.icons.material.outlined

import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.materialIcon
import io.rudione.chatone.icons.material.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Outlined.Send: ImageVector
    get() {
        if (_send != null) {
            return _send!!
        }
        _send = materialIcon(name = "Outlined.Send") {
            materialPath {
                moveTo(4.01f, 6.03f)
                lineToRelative(7.51f, 3.22f)
                lineToRelative(-7.52f, -1.0f)
                lineToRelative(0.01f, -2.22f)
                moveToRelative(7.5f, 8.72f)
                lineTo(4.0f, 17.97f)
                verticalLineToRelative(-2.22f)
                lineToRelative(7.51f, -1.0f)
                moveTo(2.01f, 3.0f)
                lineTo(2.0f, 10.0f)
                lineToRelative(15.0f, 2.0f)
                lineToRelative(-15.0f, 2.0f)
                lineToRelative(0.01f, 7.0f)
                lineTo(23.0f, 12.0f)
                lineTo(2.01f, 3.0f)
                close()
            }
        }
        return _send!!
    }

private var _send: ImageVector? = null
