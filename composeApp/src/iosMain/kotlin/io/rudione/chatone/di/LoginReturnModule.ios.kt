package io.rudione.chatone.di

import io.rudione.chatone.data.auth.LoginReturnChannel
import io.rudione.chatone.data.auth.NoLoginReturn
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformLoginReturnModule: Module = module {
    single<LoginReturnChannel> { NoLoginReturn }
}
