package io.rudione.chatone.icons.material.outlined

import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.materialIcon
import io.rudione.chatone.icons.material.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Outlined.PlayArrow: ImageVector
    get() {
        if (_playArrow != null) {
            return _playArrow!!
        }
        _playArrow = materialIcon(name = "Outlined.PlayArrow") {
            materialPath {
                moveTo(10.0f, 8.64f)
                lineTo(15.27f, 12.0f)
                lineTo(10.0f, 15.36f)
                verticalLineTo(8.64f)
                moveTo(8.0f, 5.0f)
                verticalLineToRelative(14.0f)
                lineToRelative(11.0f, -7.0f)
                lineTo(8.0f, 5.0f)
                close()
            }
        }
        return _playArrow!!
    }

private var _playArrow: ImageVector? = null
