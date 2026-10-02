package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.concurrent.Volatile

data class DeviceCodeInfo(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresInSeconds: Int,
    val intervalSeconds: Int,
    val client: TwitchFirstPartyClient
) {
    val activationUri: String
        get() = if (verificationUri.contains("device-code=")) {
            verificationUri
        } else {
            val separator = if (verificationUri.contains('?')) '&' else '?'
            "$verificationUri${separator}device-code=${userCode.encodeURLParameter()}"
        }
}

sealed interface DeviceCodeResult {
    data class Issued(val info: DeviceCodeInfo) : DeviceCodeResult
    data class Failed(val retryable: Boolean, val message: String) : DeviceCodeResult
}

sealed class DevicePollResult {
    data class Success(val token: String) : DevicePollResult()
    object Pending : DevicePollResult()
    object SlowDown : DevicePollResult()
    object ExpiredOrDenied : DevicePollResult()
    data class Error(val message: String) : DevicePollResult()
}

class TwitchDeviceAuthClient(
    private val httpClient: HttpClient,
    private val clients: List<TwitchFirstPartyClient> = TwitchFirstPartyClient.deviceFlowOrder
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Volatile
    private var acceptedClient: TwitchFirstPartyClient? = null

    suspend fun requestDeviceCode(): DeviceCodeResult {
        var rejection = DeviceCodeResult.Failed(retryable = false, message = "No Twitch client accepted the request")
        for (client in candidates()) {
            when (val result = requestWith(client)) {
                is DeviceCodeResult.Issued -> {
                    acceptedClient = client
                    return result
                }

                is DeviceCodeResult.Failed -> {
                    if (result.retryable) return result
                    Napier.w("Device code rejected for ${client.name}: ${result.message}", tag = TAG)
                    if (acceptedClient == client) acceptedClient = null
                    rejection = result
                }
            }
        }
        return rejection
    }

    suspend fun pollToken(info: DeviceCodeInfo): DevicePollResult {
        return try {
            val response = httpClient.submitForm(
                url = TOKEN_ENDPOINT,
                formParameters = Parameters.build {
                    append("client_id", info.client.clientId)
                    append("device_code", info.deviceCode)
                    append("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                }
            )
            val root = parseObject(response.bodyAsText())
            if (response.status.isSuccess()) {
                val accessToken = root?.string("access_token")
                return if (!accessToken.isNullOrBlank()) {
                    DevicePollResult.Success(accessToken)
                } else {
                    Napier.w("Device token poll succeeded without an access token", tag = TAG)
                    DevicePollResult.Error("Twitch returned no access token")
                }
            }
            when (val message = root?.string("message")) {
                "authorization_pending" -> DevicePollResult.Pending
                "slow_down" -> DevicePollResult.SlowDown
                "expired_token", "access_denied", "invalid device code" -> DevicePollResult.ExpiredOrDenied
                else -> DevicePollResult.Error(message ?: "HTTP ${response.status.value}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("Device token poll failed: ${e.message}", tag = TAG)
            DevicePollResult.Error(e.message ?: "Network error")
        }
    }

    private fun candidates(): List<TwitchFirstPartyClient> {
        val preferred = acceptedClient ?: return clients
        return listOf(preferred) + clients.filter { it != preferred }
    }

    private suspend fun requestWith(client: TwitchFirstPartyClient): DeviceCodeResult = try {
        val response = httpClient.submitForm(
            url = DEVICE_ENDPOINT,
            formParameters = Parameters.build {
                append("client_id", client.clientId)
                append("scopes", SCOPES)
            }
        )
        val root = parseObject(response.bodyAsText())
        when {
            response.status.isSuccess() -> root?.toDeviceCode(client)
                ?.let { DeviceCodeResult.Issued(it) }
                ?: DeviceCodeResult.Failed(retryable = true, message = "Malformed device code response")

            response.status.isPermanentRejection() -> DeviceCodeResult.Failed(
                retryable = false,
                message = root?.string("message") ?: "HTTP ${response.status.value}"
            )

            else -> DeviceCodeResult.Failed(retryable = true, message = "HTTP ${response.status.value}")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Napier.w("Device code request failed: ${e.message}", tag = TAG)
        DeviceCodeResult.Failed(retryable = true, message = e.message ?: "Network error")
    }

    private fun HttpStatusCode.isPermanentRejection(): Boolean =
        value in 400..499 && this != HttpStatusCode.TooManyRequests && this != HttpStatusCode.RequestTimeout

    private fun JsonObject.toDeviceCode(client: TwitchFirstPartyClient): DeviceCodeInfo? {
        val deviceCode = string("device_code")?.takeIf { it.isNotBlank() } ?: return null
        val userCode = string("user_code")?.takeIf { it.isNotBlank() } ?: return null
        return DeviceCodeInfo(
            deviceCode = deviceCode,
            userCode = userCode,
            verificationUri = string("verification_uri")?.takeIf { it.startsWith("https://") }
                ?: DEFAULT_VERIFICATION_URI,
            expiresInSeconds = int("expires_in") ?: DEFAULT_EXPIRES_SECONDS,
            intervalSeconds = int("interval") ?: DEFAULT_INTERVAL_SECONDS,
            client = client
        )
    }

    private fun parseObject(text: String): JsonObject? =
        runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull

    private companion object {
        const val TAG = "TwitchDeviceAuth"
        const val DEVICE_ENDPOINT = "https://id.twitch.tv/oauth2/device"
        const val TOKEN_ENDPOINT = "https://id.twitch.tv/oauth2/token"
        const val DEFAULT_VERIFICATION_URI = "https://www.twitch.tv/activate"
        const val DEFAULT_EXPIRES_SECONDS = 1800
        const val DEFAULT_INTERVAL_SECONDS = 5
        const val SCOPES = "channel:moderate chat:edit chat:read whispers:edit whispers:read"
    }
}
