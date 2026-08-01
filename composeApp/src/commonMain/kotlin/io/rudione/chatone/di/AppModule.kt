package io.rudione.chatone.di

import com.russhwolf.settings.Settings
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import io.rudione.chatone.data.auth.PlatformAuthHandler
import io.rudione.chatone.data.local.DatabaseDriverFactory
import io.rudione.chatone.data.local.createDatabase
import io.rudione.chatone.data.remote.RecentMessagesClient
import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.data.remote.TwitchIrcClient
import io.rudione.chatone.data.remote.TwitchPubSubClient
import io.rudione.chatone.data.remote.TwitchEventSubClient
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
import io.rudione.chatone.util.settings.AppConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val IoScopeQualifier = named("io-scope")

val networkModule = module {
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Napier.v(message, tag = "HTTP")
                    }
                }
                level = LogLevel.NONE
            }
            install(WebSockets)
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 60_000
                socketTimeoutMillis = 30_000
            }
            followRedirects = true
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

    single(IoScopeQualifier) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    single {
        TwitchIrcClient(
            httpClient = get(),
            scope = get(IoScopeQualifier)
        )
    }

    single { TwitchPubSubClient(httpClient = get(), scope = get(IoScopeQualifier)) }
    single { TwitchEventSubClient(httpClient = get(), apiClient = get(), scope = get(IoScopeQualifier)) }
    single { io.rudione.chatone.data.remote.ImageUploaderClient(httpClient = get()) }
    single { io.rudione.chatone.data.repository.MentionMuteRepository() }

    single { PlatformAuthHandler() }
    single { RecentMessagesClient(httpClient = get()) }
    single { MentionRepository(get()) }

    single { SevenTvApiClient(httpClient = get()) }
    single { BttvApiClient(httpClient = get()) }
    single { FfzApiClient(httpClient = get()) }

    single { SevenTvCosmeticsClient(httpClient = get(), scope = get(IoScopeQualifier)) }
    single { SevenTvEventApi(httpClient = get(), scope = get(IoScopeQualifier)) }

    single { io.rudione.chatone.data.remote.TranslationClient(httpClient = get()) }
    single { io.rudione.chatone.presentation.chat.TranslationStore(client = get()) }
    single { io.rudione.chatone.data.remote.TwitchGqlClient(httpClient = get()) }
    single { io.rudione.chatone.data.repository.ModerationAuthStore(settings = get(), gqlClient = get()) }
    single { io.rudione.chatone.data.remote.TwitchDeviceAuthClient(httpClient = get()) }
    single {
        io.rudione.chatone.data.repository.FirstPartyDeviceAuthController(
            deviceAuthClient = get(),
            moderationAuthStore = get(),
            scope = get()
        )
    }
    single { io.rudione.chatone.data.repository.StreamerModeController(settings = get()) }
    single { io.rudione.chatone.data.remote.AiAssistantClient(httpClient = get()) }
    single { io.rudione.chatone.data.remote.OllamaClient(httpClient = get()) }
    single { io.rudione.chatone.data.repository.AiAssistantController(settings = get()) }
    single(createdAtStart = true) {
        io.rudione.chatone.data.repository.ModelDownloadRepository(
            ollama = get(),
            settings = get(),
            scope = get(IoScopeQualifier)
        )
    }
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
        io.rudione.chatone.data.repository.ModerationHistoryRepository(
            database = get(),
            scope = get(IoScopeQualifier)
        )
    }

    single {
        io.rudione.chatone.data.repository.MessagePersistenceQueue(
            chatRepository = get(),
            scope = get(IoScopeQualifier)
        )
    }

    single {
        io.rudione.chatone.data.repository.NicknameRepository(
            database = get()
        )
    }

    single {
        io.rudione.chatone.data.repository.ThirdPartyBadgeRepository(
            httpClient = get()
        )
    }

    single {
        io.rudione.chatone.data.remote.IvrApiClient(
            httpClient = get()
        )
    }

    single<io.rudione.chatone.domain.entitlements.EntitlementsRepository> {
        io.rudione.chatone.data.repository.RemoteEntitlementsRepository(
            httpClient = get()
        )
    }
    single { io.rudione.chatone.domain.entitlements.RefreshEntitlementsUseCase(repository = get()) }
    single { io.rudione.chatone.domain.entitlements.ResolveUserPerksUseCase(repository = get()) }
    single {
        io.rudione.chatone.domain.entitlements.HasFeatureUseCase(resolvePerks = get())
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
    singleOf(::ObserveModelDownloadsUseCase)
    singleOf(::DownloadModelUseCase)
    singleOf(::CancelModelDownloadUseCase)
    singleOf(::RetryModelDownloadUseCase)
    singleOf(::ListInstalledModelsUseCase)
    singleOf(::DeleteModelUseCase)
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
            scope = get(IoScopeQualifier)
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
            scope = get(IoScopeQualifier)
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
            eventSubClient = get(),
            emoteRepository = get(),
            perAccountSettings = get(),
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
    viewModel { SettingsViewModel(get(), get(), get(), get()) }
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
