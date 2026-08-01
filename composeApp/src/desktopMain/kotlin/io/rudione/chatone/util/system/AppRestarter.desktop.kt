package io.rudione.chatone.util.system

import io.github.aakira.napier.Napier
import java.lang.management.ManagementFactory
import kotlin.concurrent.thread
import kotlin.system.exitProcess

actual object AppRestarter {
    actual fun restart(delayMs: Long) {
        thread(isDaemon = true, name = "app-restarter") {
            try {
                Thread.sleep(delayMs)
                val jvm = ProcessHandle.current()
                val info = jvm.info()
                val command = info.command().orElse(null)
                val args = info.arguments().map { it.toList() }.orElse(emptyList())

                if (command != null) {
                    val cmd = mutableListOf(command) + args
                    Napier.d("Restarting app with: $cmd", tag = "AppRestarter")
                    ProcessBuilder(cmd)
                        .inheritIO()
                        .start()
                }
            } catch (e: Exception) {
                Napier.e("Failed to restart: ${e.message}", e, tag = "AppRestarter")
            } finally {
                exitProcess(0)
            }
        }
    }
}
