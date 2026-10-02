package io.rudione.chatone.data.auth

sealed interface LoginReturnTarget {
    val fragmentParameter: String

    data class Loopback(val port: Int) : LoginReturnTarget {
        override val fragmentParameter: String get() = "r=$port"
    }

    data object AppLink : LoginReturnTarget {
        override val fragmentParameter: String get() = "app=1"
    }
}

sealed interface LoginReturnOutcome {
    data object Done : LoginReturnOutcome
    data class Continue(val url: String) : LoginReturnOutcome
    data object Rejected : LoginReturnOutcome
}

fun interface LoginReturnHandler {
    suspend fun accept(line: String): LoginReturnOutcome
}

interface LoginReturnChannel {
    val isAutomatic: Boolean get() = true
    suspend fun open(handler: LoginReturnHandler): LoginReturnTarget?
    fun close()
}

object NoLoginReturn : LoginReturnChannel {
    override val isAutomatic: Boolean get() = false
    override suspend fun open(handler: LoginReturnHandler): LoginReturnTarget? = null
    override fun close() = Unit
}

private val TWITCH_ACTIVATION = Regex("^https://www\\.twitch\\.tv/activate\\?device-code=[A-Za-z0-9]{4,16}$")

fun isTwitchActivationUrl(url: String): Boolean = TWITCH_ACTIVATION.matches(url)
