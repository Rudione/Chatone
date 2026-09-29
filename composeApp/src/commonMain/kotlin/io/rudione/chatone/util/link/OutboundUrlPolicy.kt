package io.rudione.chatone.util.link

object OutboundUrlPolicy {

    private val BLOCKED_HOST_SUFFIXES = listOf(
        ".localhost", ".local", ".internal", ".home.arpa"
    )

    private val BLOCKED_HOSTS = setOf(
        "localhost", "metadata.google.internal", "metadata"
    )

    private val IDNA_DOTS = charArrayOf('。', '．', '｡')

    fun isFetchAllowed(rawUrl: String): Boolean {
        if (!isSafeHttpUrl(rawUrl)) return false
        val host = httpUrlHost(rawUrl) ?: return false
        return isPublicHost(host)
    }

    fun isPublicHost(rawHost: String): Boolean {
        val host = rawHost.trim().lowercase().removePrefix("[").removeSuffix("]").removeSuffix(".")
        if (host.isEmpty()) return false
        if (host in BLOCKED_HOSTS) return false
        if (BLOCKED_HOST_SUFFIXES.any { host.endsWith(it) }) return false
        if (host.contains(':')) {
            val words = parseIpv6(host) ?: return false
            return isPublicIpv6(words, literal = true)
        }
        val asciiForm = host.withAsciiDigitsAndDots()
        val labels = asciiForm.split('.')
        if (labels.all { it.isEmpty() || it.isNumericLabel() }) {
            if (asciiForm != host) return false
            val octets = parseDottedQuad(labels) ?: return false
            return isPublicIpv4(octets, literal = true)
        }
        return host.contains('.')
    }

    fun isPublicResolvedAddress(address: ByteArray): Boolean = when (address.size) {
        4 -> isPublicIpv4(IntArray(4) { address[it].toInt() and 0xFF }, literal = false)
        16 -> isPublicIpv6(
            IntArray(8) { ((address[it * 2].toInt() and 0xFF) shl 8) or (address[it * 2 + 1].toInt() and 0xFF) },
            literal = false
        )
        else -> false
    }

    private fun isPublicIpv4(o: IntArray, literal: Boolean): Boolean = when {
        o[0] == 0 -> false
        o[0] == 10 -> false
        o[0] == 127 -> false
        o[0] == 169 && o[1] == 254 -> false
        o[0] == 172 && o[1] in 16..31 -> false
        o[0] == 192 && o[1] == 168 -> false
        o[0] >= 224 -> false
        !literal -> true
        o[0] == 100 && o[1] in 64..127 -> false
        o[0] == 192 && o[1] == 0 && o[2] == 0 -> false
        o[0] == 198 && o[1] in 18..19 -> false
        else -> true
    }

    private fun isPublicIpv6(w: IntArray, literal: Boolean): Boolean {
        val upperZero = (0..4).all { w[it] == 0 }
        return when {
            upperZero && w[5] == 0 -> false
            upperZero && w[5] == 0xFFFF -> !literal && isPublicIpv4(embeddedIpv4(w), literal = false)
            w[0] == 0x64 && w[1] == 0xFF9B && (2..5).all { w[it] == 0 } -> isPublicIpv4(embeddedIpv4(w), literal)
            (w[0] and 0xFFC0) == 0xFE80 -> false
            (w[0] and 0xFFC0) == 0xFEC0 -> false
            (w[0] and 0xFF00) == 0xFF00 -> false
            (w[0] and 0xFE00) == 0xFC00 -> !literal
            else -> true
        }
    }

    private fun embeddedIpv4(w: IntArray): IntArray =
        intArrayOf(w[6] shr 8, w[6] and 0xFF, w[7] shr 8, w[7] and 0xFF)

    private fun parseDottedQuad(labels: List<String>): IntArray? {
        if (labels.size != 4) return null
        val octets = IntArray(4)
        labels.forEachIndexed { index, label ->
            if (label.isEmpty() || label.length > 3 || !label.all { it in '0'..'9' }) return null
            if (label.length > 1 && label[0] == '0') return null
            val value = label.toInt()
            if (value > 255) return null
            octets[index] = value
        }
        return octets
    }

    private fun parseIpv6(text: String): IntArray? {
        if (text.length > 45 || '%' in text) return null
        val gap = text.indexOf("::")
        if (gap == -1) {
            return parseIpv6Words(text, allowEmbeddedIpv4 = true)?.takeIf { it.size == 8 }?.toIntArray()
        }
        if (text.indexOf("::", gap + 1) != -1) return null
        val head = parseIpv6Words(text.substring(0, gap), allowEmbeddedIpv4 = false) ?: return null
        val tail = parseIpv6Words(text.substring(gap + 2), allowEmbeddedIpv4 = true) ?: return null
        val missing = 8 - head.size - tail.size
        if (missing < 1) return null
        return (head + List(missing) { 0 } + tail).toIntArray()
    }

    private fun parseIpv6Words(part: String, allowEmbeddedIpv4: Boolean): List<Int>? {
        if (part.isEmpty()) return emptyList()
        val groups = part.split(':')
        val words = ArrayList<Int>(8)
        groups.forEachIndexed { index, group ->
            if (allowEmbeddedIpv4 && index == groups.lastIndex && '.' in group) {
                val octets = parseDottedQuad(group.split('.')) ?: return null
                words += (octets[0] shl 8) or octets[1]
                words += (octets[2] shl 8) or octets[3]
            } else {
                if (group.isEmpty() || group.length > 4 || !group.all { it.isAsciiHexDigit() }) return null
                words += group.toInt(16)
            }
        }
        return words
    }

    private fun String.isNumericLabel(): Boolean =
        all { it in '0'..'9' } || (startsWith("0x") && drop(2).all { it.isAsciiHexDigit() })

    private fun String.withAsciiDigitsAndDots(): String = buildString(length) {
        for (c in this@withAsciiDigitsAndDots) {
            when {
                c in IDNA_DOTS -> append('.')
                c.isDigit() -> append(c.digitToInt())
                else -> append(c)
            }
        }
    }

    private fun Char.isAsciiHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f'
}
