package io.rudione.chatone.util.link

object OutboundUrlPolicy {

    private val BLOCKED_HOST_SUFFIXES = listOf(
        ".localhost", ".local", ".internal", ".home.arpa"
    )

    private val BLOCKED_HOSTS = setOf(
        "localhost", "metadata.google.internal", "metadata"
    )

    fun isFetchAllowed(rawUrl: String): Boolean {
        if (!isSafeHttpUrl(rawUrl)) return false
        val host = httpUrlHost(rawUrl) ?: return false
        return isPublicHost(host)
    }

    fun isPublicHost(host: String): Boolean {
        if (host in BLOCKED_HOSTS) return false
        if (BLOCKED_HOST_SUFFIXES.any { host.endsWith(it) }) return false
        if (host.contains(':')) return isPublicIpv6(host)

        val octets = host.split('.')
        if (octets.size == 4 && octets.all { it.isNotEmpty() && it.all(Char::isDigit) }) {
            val parsed = octets.map { it.toIntOrNull() ?: return false }
            if (parsed.any { it !in 0..255 }) return false
            return isPublicIpv4(parsed)
        }
        return host.contains('.')
    }

    private fun isPublicIpv4(o: List<Int>): Boolean = when {
        o[0] == 0 -> false
        o[0] == 10 -> false
        o[0] == 127 -> false
        o[0] == 100 && o[1] in 64..127 -> false
        o[0] == 169 && o[1] == 254 -> false
        o[0] == 172 && o[1] in 16..31 -> false
        o[0] == 192 && o[1] == 168 -> false
        o[0] == 192 && o[1] == 0 && o[2] == 0 -> false
        o[0] == 198 && o[1] in 18..19 -> false
        o[0] >= 224 -> false
        else -> true
    }

    private fun isPublicIpv6(host: String): Boolean {
        val normalized = host.removePrefix("[").removeSuffix("]").lowercase()
        if (normalized == "::1" || normalized == "::") return false
        if (normalized.startsWith("fe8") || normalized.startsWith("fe9") ||
            normalized.startsWith("fea") || normalized.startsWith("feb")
        ) return false
        if (normalized.startsWith("fc") || normalized.startsWith("fd")) return false
        if (normalized.startsWith("::ffff:")) return false
        return true
    }
}
