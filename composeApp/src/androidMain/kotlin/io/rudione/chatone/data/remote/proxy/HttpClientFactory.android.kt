package io.rudione.chatone.data.remote.proxy

import io.ktor.client.HttpClient
import io.rudione.chatone.domain.model.AccountProxyConfig

actual fun buildHttpClientWithProxy(proxy: AccountProxyConfig?): HttpClient {
    TODO("Not yet implemented")
}