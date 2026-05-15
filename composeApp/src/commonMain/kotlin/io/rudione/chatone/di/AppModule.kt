package io.rudione.chatone.di

import com.russhwolf.settings.Settings
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
import io.rudione.chatone.data.remote.TwitchPubSubClient
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
import io.rudione.chatone.data.repository.AutomodRepository
import io.rudione.chatone.data.repository.EmoteRepository
import io.rudione.chatone.data.repository.UserNoteRepository
import io.rudione.chatone.domain.usecase.*
import io.rudione.chatone.presentation.auth.AuthViewModel
import io.rudione.chatone.presentation.chat.ChatViewModel
import io.rudione.chatone.presentation.chat.multichat.ChatPanelManager
import io.rudione.chatone.presentation.chat.multichat.PanelPersistence
import io.rudione.chatone.presentation.chat.multichat.PanelLifecycleSync
import io.rudione.chatone.presentation.account.AccountManager
import io.rudione.chatone.presentation.account.AccountListLoader
import io.rudione.chatone.presentation.account.AccountSwitchCoordinator
import io.rudione.chatone.data.remote.proxy.HttpClientFactory
import io.rudione.chatone.presentation.main.MainViewModel
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.data.repository.MentionRepository
import io.rudione.chatone.presentation.theme.CustomThemeManager
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
            clientId = get<PlatformAuthHandler>().getClientId()
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

    single { TwitchPubSubClient(httpClient = get(), scope = get()) }

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
            clientId = get<PlatformAuthHandler>().getClientId()
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

    single {
        AutomodRepository(
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

val appModule = module {
    single { CustomThemeManager() }
    single { ChatPanelManager() }
    single { AccountManager(settings = get()) }
    single { PanelPersistence(settings = get()) }
    single { AccountListLoader(authRepository = get()) }
    single { HttpClientFactory(accountManager = get()) }
    single { PanelLifecycleSync(ircClient = get(), scope = get()) }
    single { io.rudione.chatone.presentation.chat.multichat.PanelViewModelStoreRegistry() }
    single {
        io.rudione.chatone.data.repository.PersonalEmoteBackfiller(
            emoteRepository = get(),
            sevenTvApi = get(),
            scope = get()
        )
    }
    single {
        io.rudione.chatone.presentation.account.AccountActions(
            authRepository = get(),
            accountManager = get(),
            switchCoordinator = get(),
            scope = get()
        )
    }
    single { io.rudione.chatone.presentation.chat.multichat.PanelEventBus() }
    single {
        io.rudione.chatone.presentation.account.oauth.AddAccountOAuthHandler(
            platformAuth = get(),
            authRepository = get(),
            accountManager = get(),
            scope = get()
        )
    }
    single {
        io.rudione.chatone.data.remote.emote.PersonalSetExtractor(
            httpClient = get()
        )
    }
    single {
        io.rudione.chatone.data.repository.EnrichedPersonalEmoteBackfiller(
            emoteRepository = get(),
            extractor = get(),
            scope = get()
        )
    }
    single { io.rudione.chatone.presentation.account.PerAccountSettingsLoader(accountManager = get()) }
    single {
        io.rudione.chatone.presentation.account.AccountStateRefresher(
            authRepository = get(),
            accountManager = get(),
            scope = get()
        )
    }
    single {
        io.rudione.chatone.presentation.account.AccountInitializer(
            authRepository = get(),
            accountManager = get(),
            scope = get()
        )
    }
    single {
        io.rudione.chatone.presentation.account.AccountFlowGlue(
            authRepository = get(),
            accountManager = get()
        )
    }
    single { io.rudione.chatone.presentation.chat.multichat.PanelMessageInputBus() }
    single {
        io.rudione.chatone.presentation.account.AccountMigration(
            authRepository = get(),
            accountManager = get(),
            scope = get()
        )
    }
    single {
        io.rudione.chatone.data.remote.proxy.IrcConnectionFactory(
            httpClientFactory = get(),
            accountManager = get(),
            scope = get()
        )
    }
    single {
        io.rudione.chatone.data.repository.MultiAccountConnectionRegistry(
            ircFactory = get(),
            accountManager = get(),
            scope = get()
        )
    }
    single {
        AccountSwitchCoordinator(
            accountManager = get(),
            ircClient = get(),
            pubSubClient = get(),
            emoteRepository = get(),
            scope = get()
        )
    }
    single {
        io.rudione.chatone.presentation.account.AccountSettingsExporter(
            accountManager = get()
        )
    }
    single { io.rudione.chatone.presentation.chat.multichat.PanelMessageDispatcher() }
    single { io.rudione.chatone.presentation.chat.multichat.ChatViewModelPerPanelFactory() }
}

val settingsModule = module {
    single { com.russhwolf.settings.Settings() }
}

val viewModelModule = module {
    viewModelOf(::AuthViewModel)
    viewModelOf(::ChatViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::MainViewModel)
}

fun appModules(): List<Module> = listOf(
    settingsModule,
    networkModule,
    databaseModule,
    repositoryModule,
    useCaseModule,
    viewModelModule,
    appModule
)