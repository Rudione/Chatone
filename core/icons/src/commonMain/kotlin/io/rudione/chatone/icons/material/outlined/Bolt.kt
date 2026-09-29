package io.rudione.chatone.icons.material.outlined

import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.materialIcon
import io.rudione.chatone.icons.material.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Outlined.Bolt: ImageVector
    get() {
        if (_bolt != null) {
            return _bolt!!
        }
        _bolt = materialIcon(name = "Outlined.Bolt") {
            materialPath {
                moveTo(11.0f, 21.0f)
                horizontalLineToRelative(-1.0f)
                lineToRelative(1.0f, -7.0f)
                horizontalLineTo(7.5f)
                curveToRelative(-0.88f, 0.0f, -0.33f, -0.75f, -0.31f, -0.78f)
                curveTo(8.48f, 10.94f, 10.42f, 7.54f, 13.01f, 3.0f)
                horizontalLineToRelative(1.0f)
                lineToRelative(-1.0f, 7.0f)
                horizontalLineToRelative(3.51f)
                curveToRelative(0.4f, 0.0f, 0.62f, 0.19f, 0.4f, 0.66f)
                curveTo(12.97f, 17.55f, 11.0f, 21.0f, 11.0f, 21.0f)
                close()
            }
        }
        return _bolt!!
    }

private var _bolt: ImageVector? = null
