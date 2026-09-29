package io.rudione.chatone.icons.material.outlined

import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.materialIcon
import io.rudione.chatone.icons.material.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Outlined.VerifiedUser: ImageVector
    get() {
        if (_verifiedUser != null) {
            return _verifiedUser!!
        }
        _verifiedUser = materialIcon(name = "Outlined.VerifiedUser") {
            materialPath {
                moveTo(12.0f, 1.0f)
                lineTo(3.0f, 5.0f)
                verticalLineToRelative(6.0f)
                curveToRelative(0.0f, 5.55f, 3.84f, 10.74f, 9.0f, 12.0f)
                curveToRelative(5.16f, -1.26f, 9.0f, -6.45f, 9.0f, -12.0f)
                lineTo(21.0f, 5.0f)
                lineToRelative(-9.0f, -4.0f)
                close()
                moveTo(19.0f, 11.0f)
                curveToRelative(0.0f, 4.52f, -2.98f, 8.69f, -7.0f, 9.93f)
                curveToRelative(-4.02f, -1.24f, -7.0f, -5.41f, -7.0f, -9.93f)
                lineTo(5.0f, 6.3f)
                lineToRelative(7.0f, -3.11f)
                lineToRelative(7.0f, 3.11f)
                lineTo(19.0f, 11.0f)
                close()
                moveTo(7.41f, 11.59f)
                lineTo(6.0f, 13.0f)
                lineToRelative(4.0f, 4.0f)
                lineToRelative(8.0f, -8.0f)
                lineToRelative(-1.41f, -1.42f)
                lineTo(10.0f, 14.17f)
                close()
            }
        }
        return _verifiedUser!!
    }

private var _verifiedUser: ImageVector? = null
