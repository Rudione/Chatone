package io.rudione.chatone.di

import io.rudione.chatone.util.concurrent.IoDispatcher
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
import io.rudione.chatone.data.auth.LoginSiteResolver
import io.rudione.chatone.data.local.DatabaseDriverFactory
import io.rudione.chatone.data.local.createDatabase
import io.rudione.chatone.data.remote.AiAssistantClient
import io.rudione.chatone.data.remote.BestLogsClient
import io.rudione.chatone.data.remote.ImageUploaderClient
import io.rudione.chatone.data.remote.IvrApiClient
import io.rudione.chatone.data.remote.OllamaClient
import io.rudione.chatone.data.remote.TwitchChatterFameClient
import io.rudione.chatone.data.remote.RecentMessagesClient
import io.rudione.chatone.data.remote.RolesTvClient
import io.rudione.chatone.data.remote.TranslationClient
import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.data.remote.TwitchBadgeGqlClient
import io.rudione.chatone.data.remote.TwitchDeviceAuthClient
import io.rudione.chatone.data.remote.TwitchIrcClient
import io.rudione.chatone.data.remote.TwitchPubSubClient
import io.rudione.chatone.data.remote.TwitchEventSubClient
import io.rudione.chatone.data.remote.TwitchGqlClient
import io.rudione.chatone.data.remote.emote.BttvApiClient
import io.rudione.chatone.data.remote.emote.FfzApiClient
import io.rudione.chatone.data.remote.emote.PersonalSetExtractor
import io.rudione.chatone.data.remote.emote.SevenTvApiClient
import io.rudione.chatone.data.remote.emote.SevenTvCosmeticsClient
import io.rudione.chatone.data.remote.emote.SevenTvEventApi
import io.rudione.chatone.data.remote.gif.GiphyApiClient
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
import io.rudione.chatone.data.repository.AccountManager
import io.rudione.chatone.presentation.account.AccountListLoader
import io.rudione.chatone.presentation.account.AccountSwitchCoordinator
import io.rudione.chatone.data.remote.proxy.HttpClientFactory
import io.rudione.chatone.data.remote.proxy.IrcConnectionFactory
import io.rudione.chatone.data.remote.proxy.buildHttpClientWithProxy
import io.rudione.chatone.presentation.main.MainViewModel
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.data.repository.MentionRepository
import io.rudione.chatone.data.repository.SidebarLayoutRepository
import io.rudione.chatone.data.repository.StreamPlayerPreferencesRepository
import io.rudione.chatone.data.remote.stream.TwitchAdBreakResolver
import io.rudione.chatone.data.remote.stream.TwitchPlaybackClient
import io.rudione.chatone.data.repository.AccountAgeRepository
import io.rudione.chatone.data.repository.AiAssistantController
import io.rudione.chatone.data.repository.ChatMessageScaleRepository
import io.rudione.chatone.data.repository.EnrichedPersonalEmoteBackfiller
import io.rudione.chatone.data.repository.FirstPartyDeviceAuthController
import io.rudione.chatone.data.repository.GifRepository
import io.rudione.chatone.data.repository.MentionMuteRepository
import io.rudione.chatone.data.repository.MessagePersistenceQueue
import io.rudione.chatone.data.repository.ModelDownloadRepository
import io.rudione.chatone.data.repository.ModerationAuthStore
import io.rudione.chatone.data.repository.ModerationHistoryRepository
import io.rudione.chatone.data.repository.MultiAccountConnectionRegistry
import io.rudione.chatone.data.repository.NicknameRepository
import io.rudione.chatone.data.repository.RecentChannelsRepository
import io.rudione.chatone.data.repository.RemoteEntitlementsRepository
import io.rudione.chatone.data.repository.StreamerModeController
import io.rudione.chatone.data.repository.ThirdPartyBadgeRepository
import io.rudione.chatone.data.repository.WebLoginController
import io.rudione.chatone.domain.entitlements.EntitlementsRepository
import io.rudione.chatone.domain.entitlements.HasFeatureUseCase
import io.rudione.chatone.domain.entitlements.RefreshEntitlementsUseCase
import io.rudione.chatone.domain.entitlements.ResolveUserPerksUseCase
import io.rudione.chatone.domain.stream.StreamAdBreakSource
import io.rudione.chatone.domain.stream.StreamManifestSource
import io.rudione.chatone.presentation.account.AccountActions
import io.rudione.chatone.presentation.account.AccountFlowGlue
import io.rudione.chatone.presentation.account.AccountInitializer
import io.rudione.chatone.presentation.account.AccountMigration
import io.rudione.chatone.presentation.stream.StreamPlayerViewModel
import io.rudione.chatone.presentation.account.AccountSettingsExporter
import io.rudione.chatone.presentation.account.AccountStateRefresher
import io.rudione.chatone.presentation.account.PerAccountSettingsLoader
import io.rudione.chatone.presentation.chat.TranslationStore
import io.rudione.chatone.presentation.chat.multichat.ChatViewModelPerPanelFactory
import io.rudione.chatone.presentation.chat.multichat.PanelEventBus
import io.rudione.chatone.presentation.chat.multichat.PanelMessageDispatcher
import io.rudione.chatone.presentation.chat.multichat.PanelMessageInputBus
import io.rudione.chatone.presentation.chat.multichat.PanelViewModelStoreRegistry
import io.rudione.chatone.presentation.settings.SettingsNavigator
import io.rudione.chatone.presentation.startup.LaunchReadiness
import io.rudione.chatone.presentation.theme.CustomThemeManager
import io.rudione.chatone.util.settings.AppConfig
import io.rudione.chatone.util.system.LaunchGate
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
val UntrustedContentClientQualifier = named("untrusted-content")

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

    single(UntrustedContentClientQualifier) {
        buildHttpClientWithProxy(proxy = null, blockPrivateNetworks = true)
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

    single(IoScopeQualifier) {
        CoroutineScope(SupervisorJob() + IoDispatcher)
    }

    single {
        TwitchIrcClient(
            httpClient = get(),
            scope = get(IoScopeQualifier)
        )
    }

    single {
        TwitchPubSubClient(httpClient = get(), scope = get(IoScopeQualifier))
    }
    single {
        TwitchEventSubClient(
            httpClient = get(),
            apiClient = get(),
            scope = get(IoScopeQualifier)
        )
    }
    single { ImageUploaderClient(httpClient = get()) }
    single { MentionMuteRepository() }

    single { RecentMessagesClient(httpClient = get()) }
    single { BestLogsClient(httpClient = get()) }
    single { TwitchChatterFameClient(httpClient = get()) }
    single { RolesTvClient(httpClient = get()) }
    single { MentionRepository(get()) }

    single { SevenTvApiClient(httpClient = get()) }
    single { BttvApiClient(httpClient = get()) }
    single { FfzApiClient(httpClient = get()) }
    single { GiphyApiClient(httpClient = get()) }

    single { SevenTvCosmeticsClient(httpClient = get(), scope = get(IoScopeQualifier)) }
    single { SevenTvEventApi(httpClient = get(), scope = get(IoScopeQualifier)) }

    single { TranslationClient(httpClient = get()) }
    single { TranslationStore(client = get()) }
    single { TwitchGqlClient(httpClient = get()) }
    single {
        ModerationAuthStore(
            settings = get(),
            gqlClient = get(),
            accountManager = get(),
            scope = get()
        )
    }
    single { TwitchDeviceAuthClient(httpClient = get()) }
    single {
        FirstPartyDeviceAuthController(
            deviceAuthClient = get(),
            moderationAuthStore = get(),
            scope = get()
        )
    }
    single { LoginSiteResolver(httpClient = get()) }
    single {
        WebLoginController(
            authRepository = get(),
            deviceAuthController = get(),
            moderationAuthStore = get(),
            siteResolver = get(),
            returnChannel = get(),
            scope = get()
        )
    }
    single { StreamerModeController(settings = get()) }
    single { AiAssistantClient(httpClient = get()) }
    single { OllamaClient(httpClient = get()) }
    single { AiAssistantController(settings = get()) }
    single(createdAtStart = true) {
        ModelDownloadRepository(
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
            apiClient = get(),
            gqlBadges = TwitchBadgeGqlClient(httpClient = get())
        )
    }

    single {
        GifRepository(
            giphyApi = get(),
            settings = get()
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
        ModerationHistoryRepository(
            database = get(),
            scope = get(IoScopeQualifier),
            gqlClient = get()
        )
    }

    single {
        MessagePersistenceQueue(
            chatRepository = get(),
            scope = get(IoScopeQualifier)
        )
    }

    single {
        NicknameRepository(
            database = get()
        )
    }

    single {
        ThirdPartyBadgeRepository(
            httpClient = get()
        )
    }

    single {
        IvrApiClient(
            httpClient = get()
        )
    }

    single<EntitlementsRepository> {
        RemoteEntitlementsRepository(
            httpClient = get()
        )
    }
    single { RefreshEntitlementsUseCase(repository = get()) }
    single { ResolveUserPerksUseCase(repository = get()) }
    single { HasFeatureUseCase(resolvePerks = get()) }

    single {
        AutomodRepository(
            database = get()
        )
    }

    single {
        AccountAgeRepository(
            apiClient = get(),
            scope = get(IoScopeQualifier)
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
    single { LaunchGate() }
    single { LaunchReadiness() }
    single { ChatPanelManager() }
    single { AccountManager(settings = get()) }
    single { PanelPersistence(settings = get()) }
    single { AccountListLoader(authRepository = get()) }
    single { HttpClientFactory(accountManager = get()) }
    single { PanelLifecycleSync(ircClient = get(), scope = get()) }
    single { PanelViewModelStoreRegistry() }
    single {
        AccountActions(
            authRepository = get(),
            accountManager = get(),
            moderationAuthStore = get(),
            switchCoordinator = get(),
            scope = get()
        )
    }
    single { PanelEventBus() }
    single {
        PersonalSetExtractor(
            httpClient = get()
        )
    }
    single {
        EnrichedPersonalEmoteBackfiller(
            emoteRepository = get(),
            extractor = get(),
            scope = get(IoScopeQualifier)
        )
    }
    single { PerAccountSettingsLoader(accountManager = get()) }
    single {
        AccountStateRefresher(
            authRepository = get(),
            accountManager = get(),
            scope = get()
        )
    }
    single {
        AccountInitializer(
            authRepository = get(),
            accountManager = get(),
            scope = get()
        )
    }
    single {
        AccountFlowGlue(
            authRepository = get(),
            accountManager = get()
        )
    }
    single { PanelMessageInputBus() }
    single {
        AccountMigration(
            authRepository = get(),
            accountManager = get(),
            scope = get()
        )
    }
    single {
        IrcConnectionFactory(
            httpClientFactory = get(),
            accountManager = get(),
            scope = get(IoScopeQualifier)
        )
    }
    single {
        MultiAccountConnectionRegistry(
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
        AccountSettingsExporter(
            accountManager = get()
        )
    }
    single { PanelMessageDispatcher() }
    single { ChatViewModelPerPanelFactory() }
}

val settingsModule = module {
    single { Settings() }
    single { SidebarLayoutRepository(settings = get()) }
    single { ChatMessageScaleRepository(settings = get()) }
    single { RecentChannelsRepository(settings = get()) }
    single { SettingsNavigator() }
}

val viewModelModule = module {
    viewModelOf(::AuthViewModel)
    viewModelOf(::ChatViewModel)
    viewModel { SettingsViewModel(get(), get(), get()) }
    viewModelOf(::MainViewModel)
}

expect val platformStreamModule: Module

val streamModule = module {
    single { StreamPlayerPreferencesRepository(settings = get()) }
    single {
        val httpClientFactory = get<HttpClientFactory>()
        val accountManager = get<AccountManager>()
        TwitchPlaybackClient(
            httpClient = {
                httpClientFactory.forAccount(accountManager.activeAccountId.value)
            }
        )
    }
    single<StreamManifestSource> { get<TwitchPlaybackClient>() }
    single<StreamAdBreakSource> { TwitchAdBreakResolver(client = get()) }
    viewModel {
        StreamPlayerViewModel(
            manifestSource = get(),
            preferencesRepository = get(),
            engineFactory = get()
        )
    }
}

fun appModules(): List<Module> = listOf(
    settingsModule,
    networkModule,
    databaseModule,
    repositoryModule,
    useCaseModule,
    viewModelModule,
    appModule,
    streamModule,
    platformStreamModule,
    notificationModule,
    platformBrowseModule,
    platformLoginReturnModule
)
