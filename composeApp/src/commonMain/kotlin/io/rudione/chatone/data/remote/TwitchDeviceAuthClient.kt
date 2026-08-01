package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

data class DeviceCodeInfo(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresInSeconds: Int,
    val intervalSeconds: Int
)

sealed class DevicePollResult {
    data class Success(val token: String) : DevicePollResult()
    object Pending : DevicePollResult()
    object SlowDown : DevicePollResult()
    object ExpiredOrDenied : DevicePollResult()
    data class Error(val message: String) : DevicePollResult()
}

@Serializable
private data class DeviceCodeResponse(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_uri") val verificationUri: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("interval") val interval: Int
)

class TwitchDeviceAuthClient(private val httpClient: HttpClient) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    companion object {
        private const val TAG = "TwitchDeviceAuth"
        private const val DEVICE_ENDPOINT = "https://id.twitch.tv/oauth2/device"
        private const val TOKEN_ENDPOINT = "https://id.twitch.tv/oauth2/token"
        private const val CLIENT_ID = "kimne78kx3ncx6brgo4mv6wki5h1ko"
        private const val SCOPES =
            "channel:moderate chat:edit chat:read whispers:edit whispers:read"
    }

    suspend fun requestDeviceCode(): DeviceCodeInfo? {
        return try {
            val response = httpClient.submitForm(
                url = DEVICE_ENDPOINT,
                formParameters = Parameters.build {
                    append("client_id", CLIENT_ID)
                    append("scopes", SCOPES)
                }
            )
            if (!response.status.isSuccess()) {
                Napier.w("Device code request failed: HTTP ${response.status.value}", tag = TAG)
                return null
            }
            val body = response.body<DeviceCodeResponse>()
            DeviceCodeInfo(
                deviceCode = body.deviceCode,
                userCode = body.userCode,
                verificationUri = body.verificationUri,
                expiresInSeconds = body.expiresIn,
                intervalSeconds = body.interval
            )
        } catch (e: Exception) {
            Napier.w("Device code request failed: ${e.message}", tag = TAG)
            null
        }
    }

    suspend fun pollToken(deviceCode: String): DevicePollResult {
        return try {
            val response = httpClient.submitForm(
                url = TOKEN_ENDPOINT,
                formParameters = Parameters.build {
                    append("client_id", CLIENT_ID)
                    append("device_code", deviceCode)
                    append("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                }
            )
            val text = response.bodyAsText()
            val root = json.parseToJsonElement(text).let {
                if (it is kotlinx.serialization.json.JsonObject) it else null
            }
            if (response.status.isSuccess()) {
                val accessToken = root?.get("access_token")?.jsonPrimitive?.contentOrNull
                return if (!accessToken.isNullOrBlank()) {
                    DevicePollResult.Success(accessToken)
                } else {
                    Napier.w(
                        "Device token poll succeeded but no access_token in body: $text",
                        tag = TAG
                    )
                    DevicePollResult.Error("Twitch returned no access token")
                }
            }
            val message = root?.get("message")?.jsonPrimitive?.contentOrNull
            when (message) {
                "authorization_pending" -> DevicePollResult.Pending
                "slow_down" -> DevicePollResult.SlowDown
                "expired_token", "access_denied" -> DevicePollResult.ExpiredOrDenied
                else -> DevicePollResult.Error(message ?: "HTTP ${response.status.value}")
            }
        } catch (e: Exception) {
            Napier.w("Device token poll failed: ${e.message}", tag = TAG)
            DevicePollResult.Error(e.message ?: "Network error")
        }
    }
}
