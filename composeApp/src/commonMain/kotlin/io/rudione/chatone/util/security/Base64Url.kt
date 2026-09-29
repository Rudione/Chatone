package io.rudione.chatone.util.security

private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

private val DECODE_TABLE = IntArray(128) { -1 }.also { table ->
    ALPHABET.forEachIndexed { index, c -> table[c.code] = index }
    table['+'.code] = 62
    table['/'.code] = 63
}

object Base64Url {

    fun encode(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        val out = StringBuilder((bytes.size + 2) / 3 * 4)
        var index = 0
        while (index + 2 < bytes.size) {
            val chunk = (bytes[index].toInt() and 0xFF shl 16) or
                    (bytes[index + 1].toInt() and 0xFF shl 8) or
                    (bytes[index + 2].toInt() and 0xFF)
            out.append(ALPHABET[chunk ushr 18 and 0x3F])
            out.append(ALPHABET[chunk ushr 12 and 0x3F])
            out.append(ALPHABET[chunk ushr 6 and 0x3F])
            out.append(ALPHABET[chunk and 0x3F])
            index += 3
        }
        when (bytes.size - index) {
            1 -> {
                val chunk = bytes[index].toInt() and 0xFF shl 16
                out.append(ALPHABET[chunk ushr 18 and 0x3F])
                out.append(ALPHABET[chunk ushr 12 and 0x3F])
            }
            2 -> {
                val chunk = (bytes[index].toInt() and 0xFF shl 16) or
                        (bytes[index + 1].toInt() and 0xFF shl 8)
                out.append(ALPHABET[chunk ushr 18 and 0x3F])
                out.append(ALPHABET[chunk ushr 12 and 0x3F])
                out.append(ALPHABET[chunk ushr 6 and 0x3F])
            }
        }
        return out.toString()
    }

    fun decodeOrNull(value: String): ByteArray? {
        val trimmed = value.trimEnd('=')
        if (trimmed.isEmpty()) return ByteArray(0)
        if (trimmed.length % 4 == 1) return null

        val output = ByteArray(trimmed.length * 3 / 4)
        var accumulator = 0
        var bits = 0
        var written = 0

        for (c in trimmed) {
            val code = c.code
            if (code >= 128) return null
            val digit = DECODE_TABLE[code]
            if (digit < 0) return null
            accumulator = accumulator shl 6 or digit
            bits += 6
            if (bits >= 8) {
                bits -= 8
                output[written++] = (accumulator ushr bits and 0xFF).toByte()
            }
        }
        return if (written == output.size) output else output.copyOf(written)
    }
}
