package io.rudione.chatone.util.system

import java.awt.Desktop
import java.awt.EventQueue
import java.awt.Frame

object DesktopMainWindow {
    @Volatile
    var frame: Frame? = null

    fun bringToFront() {
        val target = frame ?: return
        EventQueue.invokeLater {
            if (target.extendedState and Frame.ICONIFIED != 0) {
                target.extendedState = target.extendedState and Frame.ICONIFIED.inv()
            }
            target.isVisible = true
            target.toFront()
            target.requestFocus()
            runCatching {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_REQUEST_FOREGROUND)) {
                    Desktop.getDesktop().requestForeground(true)
                }
            }
        }
    }
}
