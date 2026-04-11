package io.rudione.chatone.base

import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.rudione.chatone.util.Result

suspend inline fun <reified T> safeRequest(
    tag: String,
    crossinline block: suspend () -> HttpResponse
): Result<T> {
    return try {
        val response = block()
        Result.Success(response.body())
    } catch (e: Exception) {
        Napier.e("API error: ${e.message}", e, tag = tag)
        Result.Error(e)
    }
}

fun HttpRequestBuilder.auth(token: String) {
    header(HttpHeaders.Authorization, "Bearer $token")
}