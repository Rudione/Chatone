package io.rudione.chatone.util

import platform.Foundation.NSThread
import kotlin.system.exitProcess

actual object AppRestarter {
    actual fun restart(delayMs: Long) {
        runCatching { NSThread.sleepForTimeInterval(delayMs / 1000.0) }
        exitProcess(0)
    }
}
