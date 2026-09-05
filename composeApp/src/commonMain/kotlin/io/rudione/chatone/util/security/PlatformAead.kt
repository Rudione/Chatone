package io.rudione.chatone.util.security

expect object PlatformAead {
    val isSupported: Boolean
    fun secureRandomBytes(size: Int): ByteArray
    fun aesGcmOpen(key: ByteArray, nonce: ByteArray, sealedBox: ByteArray, aad: ByteArray): ByteArray?
}

const val AEAD_KEY_BYTES = 32
const val AEAD_NONCE_BYTES = 12
const val AEAD_TAG_BYTES = 16

fun randomHex(byteCount: Int): String =
    PlatformAead.secureRandomBytes(byteCount).joinToString("") { byte ->
        val value = byte.toInt() and 0xFF
        val high = value ushr 4
        val low = value and 0x0F
        "${hexDigit(high)}${hexDigit(low)}"
    }

private fun hexDigit(value: Int): Char =
    if (value < 10) ('0' + value) else ('a' + (value - 10))
