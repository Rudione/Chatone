package io.rudione.chatone.di

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import io.rudione.chatone.auth.PlatformAuthHandler
import io.rudione.chatone.data.local.DatabaseDriverFactory
import io.rudione.chatone.data.local.createDatabase
import io.rudione.chatone.data.remote.RecentMessagesClient
import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.data.remote.TwitchIrcClient
import io.rudione.chatone.data.remote.emote.BttvApiClient
import io.rudione.chatone.data.remote.emote.FfzApiClient
import io.rudione.chatone.data.remote.emote.SevenTvApiClient
import io.rudione.chatone.data.remote.emote.SevenTvCosmeticsClient
import io.rudione.chatone.data.remote.emote.SevenTvEventApi
import io.rudione.chatone.data.repository.AuthRepository
import io.rudione.chatone.data.repository.AuthRepositoryImpl
import io.rudione.chatone.data.repository.BadgeRepository
import io.rudione.chatone.data.repository.ChannelFolderRepository
import io.rudione.chatone.data.repository.ChatRepository
import io.rudione.chatone.data.repository.ChatRepositoryImpl
import io.rudione.chatone.data.repository.EmoteRepository
import io.rudione.chatone.data.repository.UserNoteRepository
import io.rudione.chatone.domain.usecase.*
import io.rudione.chatone.presentation.auth.AuthViewModel
import io.rudione.chatone.presentation.chat.ChatViewModel
import io.rudione.chatone.presentation.main.MainViewModel
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.data.repository.MentionRepository
import io.rudione.chatone.util.AppConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val networkModule = module {
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = true
                })
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Napier.v(message, tag = "HTTP")
                    }
                }
                level = LogLevel.INFO
            }
            install(WebSockets)
        }
    }

    single {
        TwitchApiClient(
            httpClient = get(),
            clientId = AppConfig.TWITCH_CLIENT_ID
        )
    }

    single {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    single {
        TwitchIrcClient(
            httpClient = get(),
            scope = get()
        )
    }

    single { PlatformAuthHandler() }
    single { RecentMessagesClient(httpClient = get()) }
    single { MentionRepository(get()) }


    single { SevenTvApiClient(httpClient = get()) }
    single { BttvApiClient(httpClient = get()) }
    single { FfzApiClient(httpClient = get()) }


    single { SevenTvCosmeticsClient(httpClient = get()) }
    single { SevenTvEventApi(httpClient = get(), scope = get()) }
}

expect val databaseModule: Module

val repositoryModule = module {
    single<AuthRepository> {
        AuthRepositoryImpl(
            apiClient = get(),
            database = get(),
            clientId = AppConfig.TWITCH_CLIENT_ID
        )
    }

    single<ChatRepository> {
        ChatRepositoryImpl(
            ircClient = get(),
            apiClient = get(),
            database = get()
        )
    }

    single {
        EmoteRepository(
            sevenTvApi = get(),
            bttvApi = get(),
            ffzApi = get()
        )
    }

    single {
        BadgeRepository(
            apiClient = get()
        )
    }

    single {
        ChannelFolderRepository(
            database = get()
        )
    }

    single {
        UserNoteRepository(
            database = get()
        )
    }
}

val useCaseModule = module {
    singleOf(::AuthenticateWithTokenUseCase)
    singleOf(::GetAccountsUseCase)
    singleOf(::DeleteAccountUseCase)
    singleOf(::ValidateTokenUseCase)
    singleOf(::GetFirstValidAccountUseCase)
    singleOf(::ConnectChatUseCase)
    singleOf(::DisconnectChatUseCase)
    singleOf(::JoinChannelUseCase)
    singleOf(::PartChannelUseCase)
    singleOf(::SendMessageUseCase)
    singleOf(::SearchChannelsUseCase)
    singleOf(::GetChannelInfoUseCase)
}

val viewModelModule = module {
    viewModelOf(::AuthViewModel)
    viewModelOf(::ChatViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::MainViewModel)
}

fun appModules(): List<Module> = listOf(
    networkModule,
    databaseModule,
    repositoryModule,
    useCaseModule,
    viewModelModule
)