package io.rudione.chatone.icons.material.outlined

import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.materialIcon
import io.rudione.chatone.icons.material.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Outlined.EditNote: ImageVector
    get() {
        if (_editNote != null) {
            return _editNote!!
        }
        _editNote = materialIcon(name = "Outlined.EditNote") {
            materialPath {
                moveTo(3.0f, 10.0f)
                horizontalLineToRelative(11.0f)
                verticalLineToRelative(2.0f)
                horizontalLineTo(3.0f)
                verticalLineTo(10.0f)
                close()
                moveTo(3.0f, 8.0f)
                horizontalLineToRelative(11.0f)
                verticalLineTo(6.0f)
                horizontalLineTo(3.0f)
                verticalLineTo(8.0f)
                close()
                moveTo(3.0f, 16.0f)
                horizontalLineToRelative(7.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineTo(3.0f)
                verticalLineTo(16.0f)
                close()
                moveTo(18.01f, 12.87f)
                lineToRelative(0.71f, -0.71f)
                curveToRelative(0.39f, -0.39f, 1.02f, -0.39f, 1.41f, 0.0f)
                lineToRelative(0.71f, 0.71f)
                curveToRelative(0.39f, 0.39f, 0.39f, 1.02f, 0.0f, 1.41f)
                lineToRelative(-0.71f, 0.71f)
                lineTo(18.01f, 12.87f)
                close()
                moveTo(17.3f, 13.58f)
                lineToRelative(-5.3f, 5.3f)
                verticalLineTo(21.0f)
                horizontalLineToRelative(2.12f)
                lineToRelative(5.3f, -5.3f)
                lineTo(17.3f, 13.58f)
                close()
            }
        }
        return _editNote!!
    }

private var _editNote: ImageVector? = null
