package io.rudione.chatone.presentation.liveactivity

import com.russhwolf.settings.Settings
import io.rudione.chatone.data.remote.TwitchGqlClient
import io.rudione.chatone.data.repository.AccountManager
import io.rudione.chatone.data.repository.AuthRepository
import io.rudione.chatone.util.system.NotificationLaunches
import io.rudione.chatone.util.system.NotificationTarget
import io.rudione.chatone.util.system.appForeground
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock

@Serializable
private data class LiveEventCommand(val action: String, val event: LiveChatEvent? = null)

object IosLiveEventBridge {
    var onCommand: ((String) -> Unit)? = null
    private var installed = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var polling: Job? = null
    private var settings: Settings? = null
    private var lastSent: LiveChatEvent? = null
    private const val KEY = "ios_followed_live_event_v1"

    fun setAvailable(available: Boolean) { LiveEventActivities.available.value = available }
    fun reportFailure() { LiveEventActivities.failed(); settings?.remove(KEY); polling?.cancel() }
    fun dismissed(eventKey: String) {
        if (LiveEventActivities.followed.value?.key != eventKey) return
        LiveEventActivities.clearSelection(); settings?.remove(KEY); polling?.cancel()
    }
    fun openChannel(login: String) { NotificationTarget.of(login)?.let(NotificationLaunches::publish) }

    internal fun install(settings: Settings, gql: TwitchGqlClient, auth: AuthRepository, accounts: AccountManager) {
        if (installed) return
        installed = true
        this.settings = settings
        LiveEventActivities.onFollow = { event ->
            polling?.cancel()
            lastSent = null
            if (event == null) {
                settings.remove(KEY)
                send("end", null)
            } else {
                save(event)
                send("start", event)
                observe(event, gql, auth, accounts)
            }
        }
        LiveEventActivities.onObserved = { event ->
            val previous = lastSent
            if (previous == null || event.copy(updatedAtMs = previous.updatedAtMs) != previous || event.updatedAtMs - previous.updatedAtMs >= 30_000) {
                save(event)
                send(if (event.terminal) "finish" else "update", event)
                if (event.terminal) { settings.remove(KEY); polling?.cancel() }
            }
        }
        val restored = settings.getStringOrNull(KEY)?.let { runCatching { Json.decodeFromString<LiveChatEvent>(it) }.getOrNull() }
        if (restored != null && Clock.System.now().toEpochMilliseconds() - restored.updatedAtMs < 8 * 60 * 60 * 1000L) {
            LiveEventActivities.restored(restored)
            send("restore", restored)
            observe(restored, gql, auth, accounts)
        } else settings.remove(KEY)
    }

    private fun observe(initial: LiveChatEvent, gql: TwitchGqlClient, auth: AuthRepository, accounts: AccountManager) {
        val ownerId = accounts.activeAccountId.value
        polling = scope.launch {
            appForeground.collectLatest { foreground ->
                if (!foreground) return@collectLatest
                while (isActive) {
                    val current = LiveEventActivities.followed.value ?: break
                    if (current.key != initial.key || current.terminal) break
                    if (Clock.System.now().toEpochMilliseconds() - initial.updatedAtMs >= 8 * 60 * 60 * 1000L) {
                        LiveEventActivities.stop()
                        break
                    }
                    val account = auth.getAccountById(accounts.activeAccountId.value)
                    if (account != null) {
                        val now = Clock.System.now().toEpochMilliseconds()
                        when (current.kind) {
                            "poll" -> gql.getViewablePollGql(current.channel, account.accessToken)?.takeIf { it.id == current.id }
                                ?.let { LiveEventActivities.observed(it.liveChatEvent(current.channel, now, current)) }
                            "prediction" -> gql.getActivePredictionGql(current.channel, account.accessToken)?.takeIf { it.id == current.id }
                                ?.let { LiveEventActivities.observed(it.liveChatEvent(current.channel, now)) }
                        }
                    } else if (ownerId.isNotBlank() && accounts.activeAccountId.value.isBlank()) {
                        LiveEventActivities.stop()
                        break
                    }
                    delay(30_000)
                }
            }
        }
    }

    private fun save(event: LiveChatEvent) { settings?.putString(KEY, Json.encodeToString(event)) }
    private fun send(action: String, event: LiveChatEvent?) {
        lastSent = event
        onCommand?.invoke(Json.encodeToString(LiveEventCommand(action, event)))
    }
}

