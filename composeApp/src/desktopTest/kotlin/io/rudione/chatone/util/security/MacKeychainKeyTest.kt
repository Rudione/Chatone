package io.rudione.chatone.util.security

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MacKeychainKeyTest {

    private val existing = ByteArray(32) { 7 }
    private val fresh = ByteArray(32) { 9 }

    private fun encode(bytes: ByteArray) = Base64.getEncoder().encodeToString(bytes)

    private class FakeSecurity(
        var stored: String? = null,
        var findExit: Int? = null,
        var timeout: Boolean = false,
        var dropWrites: Boolean = false
    ) {
        val calls = mutableListOf<Pair<List<String>, String?>>()

        fun run(command: List<String>, stdin: String?): CommandResult? {
            calls += command to stdin
            if (timeout) return null
            return when (command[1]) {
                "find-generic-password" -> findExit?.let { CommandResult(it, "") }
                    ?: stored?.let { CommandResult(0, "$it\n") }
                    ?: CommandResult(44, "")
                "-i" -> {
                    if (!dropWrites) stored = stdin.orEmpty().trim().substringAfter(" -w ")
                    CommandResult(0, "")
                }
                else -> CommandResult(1, "")
            }
        }

        val writes get() = calls.filter { it.first[1] == "-i" }
    }

    private fun resolver(security: FakeSecurity) = MacKeychainKey(
        service = "io.rudione.chatone",
        account = "master-key",
        keySize = 32,
        newKey = { fresh.copyOf() },
        run = security::run
    )

    @Test
    fun storedKeyIsReturnedWithoutWriting() {
        val security = FakeSecurity(stored = encode(existing))

        assertContentEquals(existing, resolver(security).resolve())
        assertTrue(security.writes.isEmpty())
    }

    @Test
    fun missingKeyIsCreatedThroughStdinOnly() {
        val security = FakeSecurity()

        assertContentEquals(fresh, resolver(security).resolve())
        assertEquals(encode(fresh), security.stored)
        assertEquals(1, security.writes.size)
        assertTrue(security.calls.none { (command, _) -> command.any { it.contains(encode(fresh)) } })
    }

    @Test
    fun failedLookupNeverOverwritesTheStoredKey() {
        val security = FakeSecurity(stored = encode(existing), findExit = 51)

        assertNull(resolver(security).resolve())
        assertTrue(security.writes.isEmpty())
        assertEquals(encode(existing), security.stored)
    }

    @Test
    fun timedOutLookupNeverWrites() {
        val security = FakeSecurity(stored = encode(existing), timeout = true)

        assertNull(resolver(security).resolve())
        assertTrue(security.writes.isEmpty())
    }

    @Test
    fun malformedKeyIsReplaced() {
        val security = FakeSecurity(stored = "not-a-key")

        assertContentEquals(fresh, resolver(security).resolve())
        assertEquals(encode(fresh), security.stored)
    }

    @Test
    fun unconfirmedWriteIsNotUsed() {
        val security = FakeSecurity(dropWrites = true)

        assertNull(resolver(security).resolve())
    }
}
