package io.rudione.chatone.icons.material.automirrored.outlined

import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.materialIcon
import io.rudione.chatone.icons.material.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.AutoMirrored.Outlined.KeyboardArrowLeft: ImageVector
    get() {
        if (_keyboardArrowLeft != null) {
            return _keyboardArrowLeft!!
        }
        _keyboardArrowLeft = materialIcon(name = "AutoMirrored.Outlined.KeyboardArrowLeft",
                autoMirror = true) {
            materialPath {
                moveTo(15.41f, 16.59f)
                lineTo(10.83f, 12.0f)
                lineToRelative(4.58f, -4.59f)
                lineTo(14.0f, 6.0f)
                lineToRelative(-6.0f, 6.0f)
                lineToRelative(6.0f, 6.0f)
                lineToRelative(1.41f, -1.41f)
                close()
            }
        }
        return _keyboardArrowLeft!!
    }

private var _keyboardArrowLeft: ImageVector? = null
