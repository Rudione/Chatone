package io.rudione.chatone.presentation.account

import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.domain.model.AccountProxyConfig
import io.rudione.chatone.domain.model.ProxyType

object AccountSnapshotJson {

    fun toJsonSafe(account: TwitchAccount): String {
        val l = account.login.replace("\"", "\\\"")
        val d = account.displayName.replace("\"", "\\\"")
        return "{\"userId\":\"${account.userId}\",\"login\":\"$l\",\"displayName\":\"$d\"}"
    }

    fun proxyToJsonSafe(proxy: AccountProxyConfig?): String {
        if (proxy == null) return "null"
        val h = proxy.host.replace("\"", "\\\"")
        val u = proxy.username?.replace("\"", "\\\"")?.let { "\"$it\"" } ?: "null"
        return "{\"type\":\"${proxy.type.name}\",\"host\":\"$h\",\"port\":${proxy.port},\"username\":$u,\"enabled\":${proxy.enabled}}"
    }
}
