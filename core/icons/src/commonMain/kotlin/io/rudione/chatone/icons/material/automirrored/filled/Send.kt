package io.rudione.chatone.icons.material.automirrored.filled

import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.materialIcon
import io.rudione.chatone.icons.material.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.AutoMirrored.Filled.Send: ImageVector
    get() {
        if (_send != null) {
            return _send!!
        }
        _send = materialIcon(name = "AutoMirrored.Filled.Send", autoMirror = true) {
            materialPath {
                moveTo(2.01f, 21.0f)
                lineTo(23.0f, 12.0f)
                lineTo(2.01f, 3.0f)
                lineTo(2.0f, 10.0f)
                lineToRelative(15.0f, 2.0f)
                lineToRelative(-15.0f, 2.0f)
                close()
            }
        }
        return _send!!
    }

private var _send: ImageVector? = null
