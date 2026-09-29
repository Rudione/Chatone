package io.rudione.chatone.util.system

import kotlin.concurrent.Volatile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class LaunchGate(private val maxHold: Duration = 2_500.milliseconds) {

    private val heldSince = TimeSource.Monotonic.markNow()

    @Volatile
    private var opened = false

    val isHolding: Boolean
        get() = !opened && heldSince.elapsedNow() < maxHold

    fun open() {
        opened = true
    }
}
