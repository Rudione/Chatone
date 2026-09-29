package io.rudione.chatone.util.security

import io.github.aakira.napier.Napier
import java.util.Base64

internal class CommandResult(val exitCode: Int, val output: String)

internal class MacKeychainKey(
    private val service: String,
    private val account: String,
    private val keySize: Int,
    private val newKey: () -> ByteArray,
    private val run: (command: List<String>, stdin: String?) -> CommandResult?
) {

    init {
        require(SAFE_TOKEN.matches(service)) { "Unsupported Keychain service name" }
        require(SAFE_TOKEN.matches(account)) { "Unsupported Keychain account name" }
    }

    fun resolve(): ByteArray? {
        when (val stored = read()) {
            is Lookup.Found -> {
                stored.key?.let { return it }
                Napier.w("Keychain master key is malformed, replacing it", tag = TAG)
            }
            Lookup.Missing -> Unit
            is Lookup.Failed -> {
                Napier.e("Keychain lookup failed (${stored.reason}), the stored key stays untouched", tag = TAG)
                return null
            }
        }
        val key = newKey()
        if (!write(key)) {
            Napier.w("Keychain write failed", tag = TAG)
            return null
        }
        val confirmed = (read() as? Lookup.Found)?.key
        if (confirmed == null || !confirmed.contentEquals(key)) {
            Napier.w("Keychain write could not be confirmed", tag = TAG)
            return null
        }
        return key
    }

    private fun read(): Lookup {
        val result = run(listOf(TOOL, "find-generic-password", "-s", service, "-a", account, "-w"), null)
            ?: return Lookup.Failed("timeout")
        return when (result.exitCode) {
            0 -> Lookup.Found(decode(result.output.trim()))
            ITEM_NOT_FOUND -> Lookup.Missing
            else -> Lookup.Failed("exit ${result.exitCode}")
        }
    }

    private fun write(key: ByteArray): Boolean {
        val encoded = Base64.getEncoder().encodeToString(key)
        val script = "add-generic-password -U -s $service -a $account -w $encoded\n"
        return run(listOf(TOOL, "-i"), script)?.exitCode == 0
    }

    private fun decode(secret: String): ByteArray? =
        runCatching { Base64.getDecoder().decode(secret) }.getOrNull()?.takeIf { it.size == keySize }

    private sealed interface Lookup {
        class Found(val key: ByteArray?) : Lookup
        data object Missing : Lookup
        class Failed(val reason: String) : Lookup
    }

    private companion object {
        const val TAG = "MacKeychain"
        const val TOOL = "/usr/bin/security"
        const val ITEM_NOT_FOUND = 44
        val SAFE_TOKEN = Regex("[A-Za-z0-9._-]+")
    }
}
