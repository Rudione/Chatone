package io.rudione.chatone.util

expect object AppRestarter {
    fun restart(delayMs: Long = 300L)
}
