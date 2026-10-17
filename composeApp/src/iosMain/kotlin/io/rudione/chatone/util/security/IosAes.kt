package io.rudione.chatone.util.security

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreCrypto.CCCrypt
import platform.CoreCrypto.kCCAlgorithmAES
import platform.CoreCrypto.kCCEncrypt
import platform.CoreCrypto.kCCOptionECBMode
import platform.CoreCrypto.kCCSuccess
import platform.posix.size_tVar

@OptIn(ExperimentalForeignApi::class)
internal fun iosAesGcm(key: ByteArray): AesGcm? {
    if (key.size != AEAD_KEY_BYTES) return null
    val copy = key.copyOf()
    return AesGcm { block -> encryptAesBlock(copy, block) }
}

@OptIn(ExperimentalForeignApi::class)
private fun encryptAesBlock(key: ByteArray, block: ByteArray): ByteArray {
    val output = ByteArray(AES_BLOCK)
    val status = memScoped {
        val moved = alloc<size_tVar>()
        key.usePinned { pinnedKey ->
            block.usePinned { pinnedIn ->
                output.usePinned { pinnedOut ->
                    CCCrypt(
                        kCCEncrypt,
                        kCCAlgorithmAES,
                        kCCOptionECBMode,
                        pinnedKey.addressOf(0),
                        key.size.toULong(),
                        null,
                        pinnedIn.addressOf(0),
                        AES_BLOCK.toULong(),
                        pinnedOut.addressOf(0),
                        AES_BLOCK.toULong(),
                        moved.ptr
                    )
                }
            }
        }.takeIf { moved.value.toInt() == AES_BLOCK } ?: INCOMPLETE_BLOCK
    }
    check(status == kCCSuccess) { "AES block encryption failed: $status" }
    return output
}

private const val AES_BLOCK = 16
private const val INCOMPLETE_BLOCK = -1
