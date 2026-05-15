package io.rudione.chatone.presentation.account

import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.rudione.chatone.data.remote.proxy.buildHttpClientWithProxy
import io.rudione.chatone.domain.model.AccountProxyConfig
import kotlinx.coroutines.withTimeoutOrNull


object ProxyConnectionTester {
    sealed class Result {
        data class Ok(val latencyMs: Long) : Result()
        data class Failure(val reason: String) : Result()
    }

    suspend fun test(proxy: AccountProxyConfig, probeUrl: String = "https://api.twitch.tv/helix"): Result {
        if (!proxy.isValid) return Result.Failure("Invalid host/port")
        val client = try { buildHttpClientWithProxy(proxy) } catch (e: Exception) {
            return Result.Failure("Build failed: ${e.message ?: "unknown"}")
        }
        return try {
            val start = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            val response: HttpResponse? = withTimeoutOrNull(8_000L) { client.get(probeUrl) }
            if (response == null) Result.Failure("Timeout")
            else {

                val ok = response.status.isSuccess() || response.status.value == 401
                if (ok) Result.Ok(kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - start)
                else Result.Failure("HTTP ${response.status.value}")
            }
        } catch (e: Exception) {
            Result.Failure(e.message ?: "Connection error")
        } finally {
            try { client.close() } catch (_: Exception) {}
        }
    }
}
