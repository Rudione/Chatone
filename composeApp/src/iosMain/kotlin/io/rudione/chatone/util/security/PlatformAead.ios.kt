package io.rudione.chatone.util.security

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.arc4random_buf

@OptIn(ExperimentalForeignApi::class)
actual object PlatformAead {

    actual val isSupported: Boolean = false

    actual fun secureRandomBytes(size: Int): ByteArray {
        if (size <= 0) return ByteArray(0)
        return ByteArray(size).apply {
            usePinned { pinned -> arc4random_buf(pinned.addressOf(0), size.toULong()) }
        }
    }

    actual fun aesGcmOpen(
        key: ByteArray,
        nonce: ByteArray,
        sealedBox: ByteArray,
        aad: ByteArray
    ): ByteArray? = null
}
