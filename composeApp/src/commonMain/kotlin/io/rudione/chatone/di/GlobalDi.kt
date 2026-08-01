package io.rudione.chatone.di

import org.koin.mp.KoinPlatformTools

object GlobalDi {
    inline fun <reified T : Any> tryGet(): T? {
        return try {
            KoinPlatformTools.defaultContext().getOrNull()?.get<T>()
        } catch (_: Throwable) {
            null
        }
    }

    inline fun <reified T : Any> get(): T = KoinPlatformTools.defaultContext().get().get<T>()
}
