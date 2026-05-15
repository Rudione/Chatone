package io.rudione.chatone.presentation.chat.rendering

import io.github.aakira.napier.Napier


object HotPathLog {

    @PublishedApi
    @Volatile
    internal var debugEnabled: Boolean = false

    fun enable(enabled: Boolean) {
        debugEnabled = enabled
    }


    inline fun debug(tag: String, message: () -> String) {
        if (debugEnabled) {
            Napier.d(message(), tag = tag)
        }
    }

    inline fun verbose(tag: String, message: () -> String) {
        if (debugEnabled) {
            Napier.v(message(), tag = tag)
        }
    }
}
