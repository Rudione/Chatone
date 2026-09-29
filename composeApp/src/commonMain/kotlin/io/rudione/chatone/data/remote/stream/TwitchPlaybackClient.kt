package io.rudione.chatone.data.remote.stream

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.preparePost
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.utils.io.readRemaining
import io.rudione.chatone.domain.stream.StreamErrorKind
import io.rudione.chatone.domain.stream.StreamManifest
import io.rudione.chatone.domain.stream.StreamManifestResult
import io.rudione.chatone.domain.stream.StreamManifestSource
import io.rudione.chatone.util.link.OutboundUrlPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.math.abs
import kotlin.random.Random
import kotlin.time.Clock

class TwitchPlaybackClient(
    private val httpClient: () -> HttpClient,
    private val random: Random = Random.Default,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) : StreamManifestSource {

    private val json = Json { ignoreUnknownKeys = true; isLenient = false }

    override suspend fun fetchManifest(channelLogin: String, supportedCodecs: String): StreamManifestResult {
        var result: StreamManifestResult = StreamManifestResult.Failed(StreamErrorKind.UNKNOWN)
        for (playerType in PRIMARY_PLAYER_TYPES) {
            result = fetchManifest(channelLogin, supportedCodecs, playerType)
            if (!result.worthAnotherPlayerType()) return result
        }
        return result
    }

    internal suspend fun fetchManifest(
        channelLogin: String,
        supportedCodecs: String,
        playerType: String
    ): StreamManifestResult {
        val login = channelLogin.trim().lowercase()
        if (!LOGIN_PATTERN.matches(login)) return StreamManifestResult.Failed(StreamErrorKind.NOT_FOUND)
        return try {
            withTimeout(REQUEST_TIMEOUT_MS) { fetch(login, sanitizeCodecs(supportedCodecs), playerType) }
        } catch (e: TimeoutCancellationException) {
            StreamManifestResult.Failed(StreamErrorKind.NETWORK)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("Stream manifest request failed: ${e::class.simpleName}", tag = TAG)
            StreamManifestResult.Failed(StreamErrorKind.NETWORK)
        }
    }

    internal suspend fun fetchMediaPlaylist(url: String): String? {
        if (!url.startsWith("https://") || !OutboundUrlPolicy.isFetchAllowed(url)) return null
        return try {
            withTimeout(MEDIA_PLAYLIST_TIMEOUT_MS) {
                httpClient().prepareGet(url).execute { response ->
                    if (response.status != HttpStatusCode.OK) return@execute null
                    response.readTextLimited(TwitchHlsPlaylist.MAX_PLAYLIST_CHARS.toLong())
                }
            }
        } catch (e: TimeoutCancellationException) {
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("Media playlist request failed: ${e::class.simpleName}", tag = TAG)
            null
        }
    }

    private fun StreamManifestResult.worthAnotherPlayerType(): Boolean =
        this is StreamManifestResult.Failed && (kind == StreamErrorKind.FORBIDDEN || kind == StreamErrorKind.UNKNOWN)

    private suspend fun fetch(login: String, codecs: String, playerType: String): StreamManifestResult {
        val token = when (val access = requestAccessToken(login, playerType)) {
            is AccessTokenResult.Granted -> access
            is AccessTokenResult.Denied -> return StreamManifestResult.Failed(access.kind)
            AccessTokenResult.PersistedQueryMissing -> return StreamManifestResult.Failed(StreamErrorKind.UNKNOWN)
        }
        val playSessionId = randomHex(16)
        val startedAt = nowMs()
        return httpClient().prepareGet("$USHER_ENDPOINT/$login.m3u8") {
            parameter("sig", token.signature)
            parameter("token", token.value)
            parameter("allow_source", "true")
            parameter("allow_audio_only", "true")
            parameter("fast_bread", "true")
            parameter("p", random.nextInt(0, 10_000_000).toString())
            parameter("player_backend", "mediaplayer")
            parameter("playlist_include_framerate", "true")
            parameter("reassignments_supported", "true")
            parameter("supported_codecs", codecs)
            parameter("cdm", "wv")
            parameter("platform", "web")
            parameter("play_session_id", playSessionId)
            header("Client-Id", WEB_CLIENT_ID)
        }.execute { response ->
            val body = response.readTextLimited(TwitchHlsPlaylist.MAX_PLAYLIST_CHARS.toLong())
            val finishedAt = nowMs()
            when (response.status) {
                HttpStatusCode.OK -> manifestFrom(login, body, playSessionId, (startedAt + finishedAt) / 2)
                HttpStatusCode.NotFound -> StreamManifestResult.Offline
                HttpStatusCode.Forbidden -> StreamManifestResult.Failed(usherErrorKind(body))
                else -> StreamManifestResult.Failed(StreamErrorKind.NETWORK)
            }
        }
    }

    private fun manifestFrom(
        login: String,
        body: String?,
        playSessionId: String,
        localMidpointMs: Long
    ): StreamManifestResult {
        val parsed = body?.let(TwitchHlsPlaylist::parseMaster)
            ?: return StreamManifestResult.Failed(StreamErrorKind.UNKNOWN)
        val playable = parsed.variants.filter { it.url.startsWith("https://") && OutboundUrlPolicy.isFetchAllowed(it.url) }
        if (playable.size != parsed.variants.size || playable.isEmpty()) {
            return StreamManifestResult.Failed(StreamErrorKind.UNKNOWN)
        }
        val offset = parsed.serving.serverTimeMs
            ?.let { it - localMidpointMs }
            ?.takeIf { abs(it) <= MAX_CLOCK_OFFSET_MS }
            ?: 0L
        return StreamManifestResult.Ready(
            StreamManifest(
                channelLogin = login,
                playlist = parsed.sanitizedPlaylist,
                variants = playable,
                serving = parsed.serving,
                playSessionId = playSessionId,
                clockOffsetMs = offset
            )
        )
    }

    private suspend fun requestAccessToken(login: String, playerType: String): AccessTokenResult {
        val persisted = postAccessTokenQuery(persistedAccessTokenPayload(login, playerType))
        if (persisted !is AccessTokenResult.PersistedQueryMissing) return persisted
        return postAccessTokenQuery(inlineAccessTokenPayload(login, playerType))
    }

    private suspend fun postAccessTokenQuery(payload: JsonObject): AccessTokenResult =
        httpClient().preparePost(GQL_ENDPOINT) {
            header("Client-Id", WEB_CLIENT_ID)
            setBody(TextContent(payload.toString(), ContentType.Application.Json))
        }.execute { response ->
            if (response.status != HttpStatusCode.OK) return@execute AccessTokenResult.Denied(StreamErrorKind.NETWORK)
            val text = response.readTextLimited(MAX_GQL_RESPONSE_BYTES)
                ?: return@execute AccessTokenResult.Denied(StreamErrorKind.UNKNOWN)
            accessTokenFrom(text)
        }

    private fun persistedAccessTokenPayload(login: String, playerType: String): JsonObject = buildJsonObject {
        put("operationName", "PlaybackAccessToken")
        putJsonObject("extensions") {
            putJsonObject("persistedQuery") {
                put("version", 1)
                put("sha256Hash", PLAYBACK_ACCESS_TOKEN_HASH)
            }
        }
        putJsonObject("variables") { accessTokenVariables(login, playerType) }
    }

    private fun inlineAccessTokenPayload(login: String, playerType: String): JsonObject = buildJsonObject {
        put("operationName", "PlaybackAccessToken_Template")
        put("query", PLAYBACK_ACCESS_TOKEN_QUERY)
        putJsonObject("variables") { accessTokenVariables(login, playerType) }
    }

    private fun JsonObjectBuilder.accessTokenVariables(login: String, playerType: String) {
        put("isLive", true)
        put("login", login)
        put("isVod", false)
        put("vodID", "")
        put("playerType", playerType)
        put("platform", "web")
    }

    private fun accessTokenFrom(text: String): AccessTokenResult {
        val root = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()
            ?: return AccessTokenResult.Denied(StreamErrorKind.UNKNOWN)
        val persistedMissing = (root["errors"] as? JsonArray).orEmpty().any { error ->
            (error as? JsonObject)?.get("message")?.jsonPrimitive?.contentOrNull == PERSISTED_QUERY_NOT_FOUND
        }
        if (persistedMissing) return AccessTokenResult.PersistedQueryMissing
        val data = root["data"] as? JsonObject ?: return AccessTokenResult.Denied(StreamErrorKind.UNKNOWN)
        val token = data["streamPlaybackAccessToken"] as? JsonObject
            ?: return AccessTokenResult.Denied(StreamErrorKind.NOT_FOUND)
        val value = token["value"]?.jsonPrimitive?.contentOrNull
        val signature = token["signature"]?.jsonPrimitive?.contentOrNull
        if (value.isNullOrEmpty() || signature.isNullOrEmpty() || signature.length > MAX_SIGNATURE_LENGTH) {
            return AccessTokenResult.Denied(StreamErrorKind.UNKNOWN)
        }
        val gqlForbidden = (token["authorization"] as? JsonObject)
            ?.get("isForbidden")?.jsonPrimitive?.booleanOrNull == true
        val claims = runCatching { json.parseToJsonElement(value) as? JsonObject }.getOrNull()
        val claimsForbidden = (claims?.get("authorization") as? JsonObject)
            ?.get("forbidden")?.jsonPrimitive?.booleanOrNull == true
        if (gqlForbidden || claimsForbidden) {
            val geoblocked = !claims?.get("geoblock_reason")?.jsonPrimitive?.contentOrNull.isNullOrEmpty()
            return AccessTokenResult.Denied(if (geoblocked) StreamErrorKind.GEOBLOCKED else StreamErrorKind.FORBIDDEN)
        }
        return AccessTokenResult.Granted(value = value, signature = signature)
    }

    private fun usherErrorKind(body: String?): StreamErrorKind {
        val code = runCatching {
            (json.parseToJsonElement(body.orEmpty()) as? JsonArray)
                ?.firstOrNull()
                ?.let { it as? JsonObject }
                ?.get("error_code")
                ?.jsonPrimitive
                ?.contentOrNull
        }.getOrNull().orEmpty().lowercase()
        return when {
            "geoblock" in code -> StreamErrorKind.GEOBLOCKED
            "entitlement" in code || "subscri" in code -> StreamErrorKind.SUBSCRIBERS_ONLY
            else -> StreamErrorKind.FORBIDDEN
        }
    }

    private suspend fun HttpResponse.readTextLimited(limitBytes: Long): String? {
        val bytes = bodyAsChannel().readRemaining(limitBytes + 1).readByteArray()
        if (bytes.size > limitBytes) return null
        return bytes.decodeToString()
    }

    private fun randomHex(byteCount: Int): String =
        random.nextBytes(byteCount).joinToString("") { byte -> (byte.toInt() and 0xFF).toString(16).padStart(2, '0') }

    private fun sanitizeCodecs(raw: String): String =
        raw.split(',')
            .map { it.trim().lowercase() }
            .filter { it in KNOWN_CODECS }
            .distinct()
            .ifEmpty { listOf(DEFAULT_CODEC) }
            .joinToString(",")

    private sealed interface AccessTokenResult {
        class Granted(val value: String, val signature: String) : AccessTokenResult {
            override fun toString(): String = "Granted"
        }

        data class Denied(val kind: StreamErrorKind) : AccessTokenResult

        data object PersistedQueryMissing : AccessTokenResult
    }

    private companion object {
        const val TAG = "TwitchPlayback"
        const val GQL_ENDPOINT = "https://gql.twitch.tv/gql"
        const val USHER_ENDPOINT = "https://usher.ttvnw.net/api/channel/hls"
        const val WEB_CLIENT_ID = "kimne78kx3ncx6brgo4mv6wki5h1ko"
        const val PLAYBACK_ACCESS_TOKEN_HASH = "ed230aa1e33e07eebb8928504583da78a5173989fadfb1ac94be06a04f3cdbe9"
        const val PERSISTED_QUERY_NOT_FOUND = "PersistedQueryNotFound"
        const val PLAYBACK_ACCESS_TOKEN_QUERY =
            "query PlaybackAccessToken_Template(\$login: String!, \$isLive: Boolean!, \$vodID: ID!, " +
                "\$isVod: Boolean!, \$playerType: String!, \$platform: String!) { " +
                "streamPlaybackAccessToken(channelName: \$login, params: {platform: \$platform, " +
                "playerBackend: \"mediaplayer\", playerType: \$playerType}) @include(if: \$isLive) { " +
                "value signature authorization { isForbidden forbiddenReasonCode } __typename } " +
                "videoPlaybackAccessToken(id: \$vodID, params: {platform: \$platform, " +
                "playerBackend: \"mediaplayer\", playerType: \$playerType}) @include(if: \$isVod) { " +
                "value signature __typename } }"
        const val REQUEST_TIMEOUT_MS = 15_000L
        const val MEDIA_PLAYLIST_TIMEOUT_MS = 4_000L
        const val MAX_GQL_RESPONSE_BYTES = 64_000L
        const val MAX_SIGNATURE_LENGTH = 256
        const val MAX_CLOCK_OFFSET_MS = 86_400_000L
        const val DEFAULT_CODEC = "h264"
        val KNOWN_CODECS = setOf("av1", "h265", "h264")
        val LOGIN_PATTERN = Regex("^[a-z0-9_]{1,25}$")
        val PRIMARY_PLAYER_TYPES = listOf(TwitchPlayerType.EMBED, TwitchPlayerType.SITE)
    }
}

internal object TwitchPlayerType {
    const val EMBED = "embed"
    const val SITE = "site"
    const val POPOUT = "popout"
    const val AUTOPLAY = "autoplay"
    const val PICTURE_BY_PICTURE = "picture-by-picture"
}
