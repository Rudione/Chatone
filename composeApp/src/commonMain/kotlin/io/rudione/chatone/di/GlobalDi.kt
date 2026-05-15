package io.rudione.chatone.di

import org.koin.core.context.GlobalContext


object GlobalDi {
    inline fun <reified T : Any> tryGet(): T? {
        return try {
            GlobalContext.getOrNull()?.get<T>()
        } catch (_: Throwable) {
            null
        }
    }

    inline fun <reified T : Any> get(): T = GlobalContext.get().get<T>()
}
