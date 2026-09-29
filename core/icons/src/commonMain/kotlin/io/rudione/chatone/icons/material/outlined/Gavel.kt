package io.rudione.chatone.icons.material.outlined

import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.materialIcon
import io.rudione.chatone.icons.material.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Outlined.Gavel: ImageVector
    get() {
        if (_gavel != null) {
            return _gavel!!
        }
        _gavel = materialIcon(name = "Outlined.Gavel") {
            materialPath {
                moveTo(1.0f, 21.0f)
                horizontalLineToRelative(12.0f)
                verticalLineToRelative(2.0f)
                horizontalLineTo(1.0f)
                verticalLineToRelative(-2.0f)
                close()
                moveTo(5.24f, 8.07f)
                lineToRelative(2.83f, -2.83f)
                lineToRelative(14.14f, 14.14f)
                lineToRelative(-2.83f, 2.83f)
                lineTo(5.24f, 8.07f)
                close()
                moveTo(12.32f, 1.0f)
                lineToRelative(5.66f, 5.66f)
                lineToRelative(-2.83f, 2.83f)
                lineToRelative(-5.66f, -5.66f)
                lineTo(12.32f, 1.0f)
                close()
                moveTo(3.83f, 9.48f)
                lineToRelative(5.66f, 5.66f)
                lineToRelative(-2.83f, 2.83f)
                lineTo(1.0f, 12.31f)
                lineToRelative(2.83f, -2.83f)
                close()
            }
        }
        return _gavel!!
    }

private var _gavel: ImageVector? = null
