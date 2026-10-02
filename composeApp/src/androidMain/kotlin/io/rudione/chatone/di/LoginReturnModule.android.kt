package io.rudione.chatone.di

import io.rudione.chatone.data.auth.AppLinkLoginReturn
import io.rudione.chatone.data.auth.LoginReturnChannel
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformLoginReturnModule: Module = module {
    single { AppLinkLoginReturn(scope = get(IoScopeQualifier)) }
    single<LoginReturnChannel> { get<AppLinkLoginReturn>() }
}
