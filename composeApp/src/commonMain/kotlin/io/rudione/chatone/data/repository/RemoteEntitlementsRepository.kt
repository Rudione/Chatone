package io.rudione.chatone.data.repository

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.client.statement.HttpResponse
import io.rudione.chatone.domain.entitlements.ChatoneUserEntitlement
import io.rudione.chatone.domain.entitlements.EntitlementsManifest
import io.rudione.chatone.domain.entitlements.EntitlementsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

class RemoteEntitlementsRepository(
    private val httpClient: HttpClient,
    private val manifestUrl: String = DEFAULT_MANIFEST_URL
) : EntitlementsRepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _manifest = MutableStateFlow<EntitlementsManifest?>(null)
    override val manifest: StateFlow<EntitlementsManifest?> = _manifest

    private var byUserId: Map<String, ChatoneUserEntitlement> = emptyMap()

    override suspend fun refresh(): Boolean {
        if (manifestUrl.isBlank()) return false
        return try {
            val response: HttpResponse = httpClient.get(manifestUrl)
            if (!response.status.isSuccess()) {
                Napier.w("Entitlements manifest HTTP ${response.status.value}", tag = TAG)
                return false
            }
            val parsed = json.decodeFromString<EntitlementsManifest>(response.bodyAsText())
            _manifest.value = parsed
            byUserId = parsed.users.associateBy { it.twitchUserId }
            Napier.d(
                "Entitlements loaded: v${parsed.version}, ${parsed.users.size} users, ${parsed.badges.size} badges",
                tag = TAG
            )
            true
        } catch (e: Exception) {
            Napier.w("Entitlements refresh failed: ${e.message}", tag = TAG)
            false
        }
    }

    override fun entitlementFor(twitchUserId: String): ChatoneUserEntitlement? =
        byUserId[twitchUserId]

    companion object {
        private const val TAG = "ChatoneEntitlements"
        const val DEFAULT_MANIFEST_URL = ""
    }
}
