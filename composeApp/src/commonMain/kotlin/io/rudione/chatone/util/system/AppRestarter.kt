package io.rudione.chatone.util.system

expect object AppRestarter {
    fun restart(delayMs: Long = 300L)
}
