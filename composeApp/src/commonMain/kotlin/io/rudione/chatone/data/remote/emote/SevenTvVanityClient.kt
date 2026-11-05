package io.rudione.chatone.data.remote.emote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import io.rudione.chatone.data.remote.arr
import io.rudione.chatone.data.remote.bool
import io.rudione.chatone.data.remote.obj
import io.rudione.chatone.data.remote.objects
import io.rudione.chatone.data.remote.str
import io.rudione.chatone.domain.profile.SevenTvVanity
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

sealed interface SevenTvInventoryReply {
    data class Loaded(val vanity: SevenTvVanity, val twitchId: String?) : SevenTvInventoryReply
    data object Rejected : SevenTvInventoryReply
    data object Unreachable : SevenTvInventoryReply
}

class SevenTvVanityClient(private val httpClient: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val parser = SevenTvV4Client(httpClient)

    suspend fun inventory(token: String): SevenTvInventoryReply {
        val root = when (val reply = call(token, INVENTORY_QUERY, JsonObject(emptyMap()))) {
            is Call.Ok -> reply.root
            Call.Unauthorized -> return SevenTvInventoryReply.Rejected
            Call.Failed -> return SevenTvInventoryReply.Unreachable
        }
        val users = root.obj("data")?.obj("users") ?: return SevenTvInventoryReply.Unreachable
        val me = users.obj("me") ?: return SevenTvInventoryReply.Rejected
        val id = me.str("id") ?: return SevenTvInventoryReply.Rejected
        val twitch = me.arr("connections").objects().firstOrNull { it.str("platform") == "TWITCH" }
        val style = me.obj("style")
        val inventory = me.obj("inventory")
        val paints = inventory?.arr("paints")?.objects().orEmpty()
            .filter { it.bool("accessible") }
            .mapNotNull { it.obj("to")?.obj("paint")?.let(parser::parsePaint) }
            .distinctBy { it.id }
        val badges = inventory?.arr("badges")?.objects().orEmpty()
            .filter { it.bool("accessible") }
            .mapNotNull { it.obj("to")?.obj("badge")?.let(parser::parseBadge) }
            .distinctBy { it.id }
        return SevenTvInventoryReply.Loaded(
            vanity = SevenTvVanity(
                userId = id,
                displayName = twitch?.str("platformDisplayName").orEmpty(),
                paints = paints,
                badges = badges,
                activePaintId = style?.str("activePaintId")?.takeIf { it.isNotBlank() },
                activeBadgeId = style?.str("activeBadgeId")?.takeIf { it.isNotBlank() }
            ),
            twitchId = twitch?.str("platformId")
        )
    }

    suspend fun setPaint(token: String, userId: String, paintId: String?): Boolean =
        mutate(token, SET_PAINT_MUTATION, buildJsonObject {
            put("id", userId)
            if (paintId == null) put("paintId", JsonNull) else put("paintId", paintId)
        })

    suspend fun setBadge(token: String, userId: String, badgeId: String?): Boolean =
        mutate(token, SET_BADGE_MUTATION, buildJsonObject {
            put("id", userId)
            if (badgeId == null) put("badgeId", JsonNull) else put("badgeId", badgeId)
        })

    private suspend fun mutate(token: String, query: String, variables: JsonObject): Boolean {
        val root = (call(token, query, variables) as? Call.Ok)?.root ?: return false
        if (root["errors"] is JsonArray) return false
        return root.obj("data")?.obj("users")?.obj("user") != null
    }

    private suspend fun call(token: String, query: String, variables: JsonObject): Call = try {
        val payload = buildJsonObject {
            put("query", query)
            put("variables", variables)
        }
        val response = httpClient.post(ENDPOINT) {
            header("Authorization", "Bearer $token")
            header("Accept", "application/json")
            setBody(TextContent(payload.toString(), ContentType.Application.Json))
        }
        when {
            response.status == HttpStatusCode.Unauthorized -> Call.Unauthorized
            !response.status.isSuccess() -> Call.Failed
            else -> (json.parseToJsonElement(response.bodyAsText()) as? JsonObject)?.let(Call::Ok) ?: Call.Failed
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Napier.w("7TV vanity request failed: ${e::class.simpleName}", tag = TAG)
        Call.Failed
    }

    private sealed interface Call {
        data class Ok(val root: JsonObject) : Call
        data object Unauthorized : Call
        data object Failed : Call
    }

    private companion object {
        const val TAG = "7TV-Vanity"
        const val ENDPOINT = "https://api.7tv.app/v4/gql"
        const val INVENTORY_QUERY =
            "query ChatoneSevenTvInventory { users { me { id " +
                    "connections { platform platformId platformDisplayName } " +
                    "style { activeBadgeId activePaintId } " +
                    "inventory(includeInaccessible: false) { " +
                    "badges { accessible to { badge { ${SevenTvV4Client.BADGE_FIELDS} } } } " +
                    "paints { accessible to { paint { ${SevenTvV4Client.PAINT_FIELDS} } } } } } } }"
        const val SET_PAINT_MUTATION =
            "mutation ChatoneSetPaint(\$id: Id!, \$paintId: Id) { users { user(id: \$id) { activePaint(paintId: \$paintId) { id } } } }"
        const val SET_BADGE_MUTATION =
            "mutation ChatoneSetBadge(\$id: Id!, \$badgeId: Id) { users { user(id: \$id) { activeBadge(badgeId: \$badgeId) { id } } } }"
    }
}
