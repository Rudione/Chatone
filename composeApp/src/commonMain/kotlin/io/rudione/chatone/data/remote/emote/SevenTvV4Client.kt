package io.rudione.chatone.data.remote.emote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.rudione.chatone.domain.model.SevenTvConnection
import io.rudione.chatone.domain.model.SevenTvCosmetics
import io.rudione.chatone.domain.model.SevenTvProfile
import io.rudione.chatone.domain.model.SevenTvUserCosmetic
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class SevenTvV4Client(private val httpClient: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun fetchUser(twitchUserId: String): SevenTvUserCosmetic? {
        val body = buildJsonObject {
            put("query", USER_QUERY)
            put("variables", buildJsonObject { put("id", twitchUserId) })
        }
        val text = try {
            val response = httpClient.post(ENDPOINT) {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(JsonObject.serializer(), body))
            }
            if (!response.status.isSuccess()) {
                Napier.w("7TV v4 HTTP ${response.status.value}", tag = TAG)
                return null
            }
            response.bodyAsText()
        } catch (e: Exception) {
            Napier.w("7TV v4 request failed for $twitchUserId: ${e.message}", tag = TAG)
            return null
        }

        return try {
            val root = json.parseToJsonElement(text).jsonObject
            root["errors"]?.let {
                Napier.w("7TV v4 errors for $twitchUserId: $it", tag = TAG)
            }
            val user = root["data"]?.jsonObject
                ?.get("users")?.jsonObject
                ?.get("userByConnection")?.asObject() ?: return null
            parseUser(user)
        } catch (e: Exception) {
            Napier.w("7TV v4 parse failed for $twitchUserId: ${e.message}", tag = TAG)
            null
        }
    }

    private fun parseUser(user: JsonObject): SevenTvUserCosmetic? {
        val id = user["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val style = user["style"]?.asObject()
        val main = user["mainConnection"]?.asObject()

        val roles = user["roles"]?.asArray().orEmpty().mapNotNull {
            it.asObject()?.get("name")?.jsonPrimitive?.contentOrNull
        }
        val connections = user["connections"]?.asArray().orEmpty().mapNotNull { element ->
            val node = element.asObject() ?: return@mapNotNull null
            SevenTvConnection(
                platform = node["platform"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                username = node["platformUsername"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                displayName = node["platformDisplayName"]?.jsonPrimitive?.contentOrNull.orEmpty()
            )
        }

        val profile = SevenTvProfile(
            sevenTvId = id,
            username = main?.get("platformUsername")?.jsonPrimitive?.contentOrNull.orEmpty(),
            displayName = main?.get("platformDisplayName")?.jsonPrimitive?.contentOrNull.orEmpty(),
            roles = roles,
            connections = connections,
            emoteSetId = style?.get("activeEmoteSetId")?.jsonPrimitive?.contentOrNull
        )

        return SevenTvUserCosmetic(
            sevenTvId = id,
            paint = style?.get("activePaint")?.asObject()?.let { parsePaint(it) },
            badge = style?.get("activeBadge")?.asObject()?.let { parseBadge(it) },
            nameColor = null,
            profile = profile
        )
    }

    private fun parsePaint(node: JsonObject): SevenTvCosmetics.Paint? {
        val id = node["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val name = node["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val data = node["data"]?.asObject()
        val layer = data?.get("layers")?.asArray()?.firstOrNull()?.asObject()
        val ty = layer?.get("ty")?.asObject()
        val kind = ty?.get("__typename")?.jsonPrimitive?.contentOrNull.orEmpty()

        val stops = ty?.get("stops")?.asArray().orEmpty().mapNotNull { element ->
            val stop = element.asObject() ?: return@mapNotNull null
            val at = stop["at"]?.jsonPrimitive?.floatOrNull ?: return@mapNotNull null
            val color = stop["color"]?.asObject()?.let { parseHexRgba(it) } ?: return@mapNotNull null
            SevenTvCosmetics.PaintStop(at, color)
        }

        val shadows = data?.get("shadows")?.asArray().orEmpty().mapNotNull { element ->
            val shadow = element.asObject() ?: return@mapNotNull null
            val color = shadow["color"]?.asObject()?.let { parseHexRgba(it) } ?: return@mapNotNull null
            SevenTvCosmetics.PaintShadow(
                xOffset = shadow["offsetX"]?.jsonPrimitive?.floatOrNull ?: 0f,
                yOffset = shadow["offsetY"]?.jsonPrimitive?.floatOrNull ?: 0f,
                radius = shadow["blur"]?.jsonPrimitive?.floatOrNull ?: 0f,
                color = color
            )
        }

        val function = when {
            kind.contains("Radial", ignoreCase = true) -> "RADIAL_GRADIENT"
            kind.contains("Image", ignoreCase = true) -> "URL"
            kind.contains("SingleColor", ignoreCase = true) -> "SINGLE_COLOR"
            else -> "LINEAR_GRADIENT"
        }

        val imageUrl = selectPaintImage(ty?.get("images")?.asArray())

        return SevenTvCosmetics.Paint(
            id = id,
            name = name,
            function = function,
            color = ty?.get("color")?.asObject()?.let { parseHexRgba(it) },
            stops = stops,
            repeat = ty?.get("repeating")?.jsonPrimitive?.contentOrNull?.toBoolean() == true,
            angle = ty?.get("angle")?.jsonPrimitive?.intOrNull ?: 0,
            imageUrl = imageUrl,
            shadows = shadows
        )
    }

    private fun selectPaintImage(images: JsonArray?): String {
        val candidates = decodableImages(images)
        if (candidates.isEmpty()) return ""
        val pool = candidates.filter { it.frameCount > 1 }.ifEmpty { candidates }
        return (pool.filter { it.width >= PAINT_TARGET_WIDTH }.minByOrNull { it.width }
            ?: pool.maxByOrNull { it.width })
            ?.url
            .orEmpty()
    }

    private fun parseBadge(node: JsonObject): SevenTvCosmetics.Badge? {
        val id = node["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val images = decodableImages(node["images"]?.asArray())
            .sortedBy { it.width }
            .map { it.url }
        if (images.isEmpty()) return null
        return SevenTvCosmetics.Badge(
            id = id,
            name = node["name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            tooltip = node["description"]?.jsonPrimitive?.contentOrNull
                ?: node["name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            url1x = images.first(),
            url2x = images.getOrElse(1) { images.first() },
            url3x = images.last()
        )
    }

    private fun decodableImages(images: JsonArray?): List<CosmeticImage> {
        val all = images.orEmpty().mapNotNull { element ->
            val node = element.asObject() ?: return@mapNotNull null
            val url = node["url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            CosmeticImage(
                url = url,
                width = node["width"]?.jsonPrimitive?.intOrNull ?: 0,
                frameCount = node["frameCount"]?.jsonPrimitive?.intOrNull ?: 1,
                mime = node["mime"]?.jsonPrimitive?.contentOrNull.orEmpty().lowercase()
            )
        }
        return SUPPORTED_MIMES.firstNotNullOfOrNull { mime ->
            all.filter { it.mime == mime || (it.mime.isBlank() && it.url.endsWith(mime.substringAfter('/'))) }
                .takeIf { it.isNotEmpty() }
        } ?: all.filterNot { it.mime.contains("avif") }
    }

    private data class CosmeticImage(
        val url: String,
        val width: Int,
        val frameCount: Int,
        val mime: String
    )

    private fun parseHexRgba(color: JsonObject): Int? {
        val hex = color["hex"]?.jsonPrimitive?.contentOrNull?.removePrefix("#") ?: return null
        val normalized = when (hex.length) {
            8 -> hex
            6 -> hex + "FF"
            else -> return null
        }
        return normalized.toUIntOrNull(16)?.toInt()
    }

    private fun kotlinx.serialization.json.JsonElement.asObject(): JsonObject? = this as? JsonObject
    private fun kotlinx.serialization.json.JsonElement.asArray(): JsonArray? = this as? JsonArray

    companion object {
        private const val TAG = "7TV-v4"
        private const val ENDPOINT = "https://7tv.io/v4/gql"

        private val SUPPORTED_MIMES = listOf("image/webp", "image/png", "image/gif")
        private const val PAINT_TARGET_WIDTH = 128

        private val USER_QUERY = """
            query ChatoneSevenTvUser(${'$'}id: String!) {
              users {
                userByConnection(platform: TWITCH, platformId: ${'$'}id) {
                  id
                  roles { name }
                  mainConnection { platformUsername platformDisplayName }
                  connections { platform platformUsername platformDisplayName }
                  style {
                    activeEmoteSetId
                    activePaint {
                      id
                      name
                      data {
                        layers {
                          opacity
                          ty {
                            __typename
                            ... on PaintLayerTypeLinearGradient {
                              angle repeating stops { at color { hex } }
                            }
                            ... on PaintLayerTypeRadialGradient {
                              repeating stops { at color { hex } }
                            }
                            ... on PaintLayerTypeSingleColor { color { hex } }
                            ... on PaintLayerTypeImage { images { url width mime frameCount } }
                          }
                        }
                        shadows { color { hex } offsetX offsetY blur }
                      }
                    }
                    activeBadge { id name description images { url width mime } }
                  }
                }
              }
            }
        """.trimIndent()
    }
}
