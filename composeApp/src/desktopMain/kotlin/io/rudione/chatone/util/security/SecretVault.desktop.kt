package io.rudione.chatone.util.security

import com.russhwolf.settings.Settings
import com.sun.jna.platform.win32.Crypt32Util
import com.sun.jna.platform.win32.WinCrypt
import io.github.aakira.napier.Napier
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val TAG = "SecretVault"
private const val KEYCHAIN_SERVICE = "io.rudione.chatone"
private const val KEYCHAIN_ACCOUNT = "master-key"
private const val KEY_BYTES = 32
private const val IV_BYTES = 12
private const val GCM_TAG_BITS = 128
private const val DPAPI_BLOB_FILE = "secret.dpapi"
private const val DPAPI_ENTROPY_DOMAIN = "chatone-dpapi-entropy-v1"
private const val KEY_DPAPI_PEPPER = "vault_dpapi_pepper_v1"

private val osName: String = System.getProperty("os.name", "").lowercase()
private val isMac: Boolean = osName.contains("mac")
private val isWindows: Boolean = osName.contains("win")

private enum class KeyBackend(val displayName: String, val osProtected: Boolean) {
    WINDOWS_DPAPI("Windows DPAPI", true),
    MACOS_KEYCHAIN("macOS Keychain", true),
    LINUX_SECRET_SERVICE("libsecret (Secret Service)", true),
    LOCAL_FILE("local key file (0600)", false),
    UNAVAILABLE("unavailable", false)
}

private class MasterKey(val bytes: ByteArray, val backend: KeyBackend)

actual object PlatformSecretCipher {

    private val random = SecureRandom()

    private val master: MasterKey by lazy { resolveMasterKey() }

    actual val backendName: String get() = master.backend.displayName

    actual val isOsProtected: Boolean get() = master.backend.osProtected

    actual fun seal(plain: String): String? {
        val key = master.bytes.takeIf { it.isNotEmpty() } ?: return null
        return try {
            val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(GCM_TAG_BITS, iv)
            )
            val body = cipher.doFinal(plain.toByteArray(StandardCharsets.UTF_8))
            Base64.getEncoder().encodeToString(iv + body)
        } catch (e: Exception) {
            Napier.e("seal failed: ${e.message}", tag = TAG)
            null
        }
    }

    actual fun open(payload: String): String? {
        val key = master.bytes.takeIf { it.isNotEmpty() } ?: return null
        return try {
            val raw = Base64.getDecoder().decode(payload)
            if (raw.size <= IV_BYTES) return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(GCM_TAG_BITS, raw.copyOfRange(0, IV_BYTES))
            )
            String(
                cipher.doFinal(raw.copyOfRange(IV_BYTES, raw.size)),
                StandardCharsets.UTF_8
            )
        } catch (e: Exception) {
            if (reportedFailures.add(payload.hashCode())) {
                Napier.w("open failed: ${e.message}, the value was sealed with another master key", tag = TAG)
            }
            null
        }
    }

    private val reportedFailures: MutableSet<Int> = ConcurrentHashMap.newKeySet()
}

private fun resolveMasterKey(): MasterKey {
    val resolved = runCatching {
        when {
            isWindows -> windowsKey()
            isMac -> macKey()
            else -> linuxKey()
        }
    }.getOrElse { e ->
        Napier.e("master key backend failed: ${e.message}", tag = TAG)
        null
    } ?: fileKey()

    if (!resolved.backend.osProtected) {
        Napier.w(
            "Secrets are protected by a local key file only — OS keystore unavailable",
            tag = TAG
        )
    } else {
        Napier.d("Secrets protected by ${resolved.backend.displayName}", tag = TAG)
    }
    return resolved
}

private fun chatoneDir(): File =
    File(System.getProperty("user.home"), ".chatone").apply { mkdirs() }

private fun newKey(): ByteArray = ByteArray(KEY_BYTES).also { SecureRandom().nextBytes(it) }

private fun File.hardenPermissions() {
    runCatching {
        Files.setPosixFilePermissions(
            toPath(),
            setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)
        )
    }
}

private fun windowsKey(): MasterKey? {
    val blobFile = File(chatoneDir(), DPAPI_BLOB_FILE)
    val entropy = dpapiEntropy()
    if (blobFile.isFile) {
        val blob = runCatching { Base64.getDecoder().decode(blobFile.readText().trim()) }.getOrNull()
        if (blob != null) {
            val bound = entropy?.let { dpapiUnprotect(blob, it) }
            if (bound != null && bound.size == KEY_BYTES) {
                return MasterKey(bound, KeyBackend.WINDOWS_DPAPI)
            }
            val legacy = dpapiUnprotect(blob, null)
            if (legacy != null && legacy.size == KEY_BYTES) {
                if (entropy != null && writeDpapiBlob(blobFile, legacy, entropy)) {
                    Napier.i("DPAPI master key re-sealed with application entropy", tag = TAG)
                }
                return MasterKey(legacy, KeyBackend.WINDOWS_DPAPI)
            }
        }
        Napier.e("DPAPI blob unreadable, regenerating master key", tag = TAG)
    }
    val key = newKey()
    if (!writeDpapiBlob(blobFile, key, entropy)) return null
    return MasterKey(key, KeyBackend.WINDOWS_DPAPI)
}

private fun dpapiEntropy(): ByteArray? {
    val store = runCatching { Settings() }.getOrNull() ?: return null
    val stored = runCatching { store.getStringOrNull(KEY_DPAPI_PEPPER) }.getOrNull()
    val existing = stored
        ?.let { runCatching { Base64.getDecoder().decode(it) }.getOrNull() }
        ?.takeIf { it.size == KEY_BYTES }
    val pepper = existing ?: newKey().also { fresh ->
        val persisted = runCatching {
            store.putString(KEY_DPAPI_PEPPER, Base64.getEncoder().encodeToString(fresh))
        }.isSuccess
        if (!persisted) {
            Napier.w("Cannot persist DPAPI entropy, falling back to plain DPAPI", tag = TAG)
            return null
        }
    }
    return MessageDigest.getInstance("SHA-256").digest(
        DPAPI_ENTROPY_DOMAIN.toByteArray(StandardCharsets.UTF_8) + pepper
    )
}

private val NO_PROMPT: WinCrypt.CRYPTPROTECT_PROMPTSTRUCT? = null

private fun dpapiUnprotect(blob: ByteArray, entropy: ByteArray?): ByteArray? = runCatching {
    if (entropy == null) {
        Crypt32Util.cryptUnprotectData(blob)
    } else {
        Crypt32Util.cryptUnprotectData(
            blob,
            entropy,
            WinCrypt.CRYPTPROTECT_UI_FORBIDDEN,
            NO_PROMPT
        )
    }
}.getOrNull()

private fun writeDpapiBlob(file: File, key: ByteArray, entropy: ByteArray?): Boolean {
    val blob = runCatching {
        if (entropy == null) {
            Crypt32Util.cryptProtectData(key)
        } else {
            Crypt32Util.cryptProtectData(
                key,
                entropy,
                WinCrypt.CRYPTPROTECT_UI_FORBIDDEN,
                "",
                NO_PROMPT
            )
        }
    }.getOrNull() ?: return false
    return runCatching {
        file.writeText(Base64.getEncoder().encodeToString(blob))
        file.hardenPermissions()
    }.isSuccess
}

private fun macKey(): MasterKey? =
    MacKeychainKey(
        service = KEYCHAIN_SERVICE,
        account = KEYCHAIN_ACCOUNT,
        keySize = KEY_BYTES,
        newKey = ::newKey,
        run = ::runCommand
    ).resolve()?.let { MasterKey(it, KeyBackend.MACOS_KEYCHAIN) }

private fun linuxKey(): MasterKey? {
    val lookup = runProcess(
        listOf("secret-tool", "lookup", "service", KEYCHAIN_SERVICE, "key", KEYCHAIN_ACCOUNT)
    )?.trim()
    if (!lookup.isNullOrEmpty()) {
        val decoded = runCatching { Base64.getDecoder().decode(lookup) }.getOrNull()
        if (decoded != null && decoded.size == KEY_BYTES) {
            return MasterKey(decoded, KeyBackend.LINUX_SECRET_SERVICE)
        }
    }
    val key = newKey()
    val stored = runProcess(
        command = listOf(
            "secret-tool", "store", "--label=Chatone master key",
            "service", KEYCHAIN_SERVICE, "key", KEYCHAIN_ACCOUNT
        ),
        stdin = Base64.getEncoder().encodeToString(key)
    ) != null
    return if (stored) MasterKey(key, KeyBackend.LINUX_SECRET_SERVICE) else null
}

private fun fileKey(): MasterKey {
    val keyFile = File(chatoneDir(), "secret.key")
    if (keyFile.isFile) {
        val decoded =
            runCatching { Base64.getDecoder().decode(keyFile.readText().trim()) }.getOrNull()
        if (decoded != null && decoded.size == KEY_BYTES) return MasterKey(
            decoded,
            KeyBackend.LOCAL_FILE
        )
    }
    val key = newKey()
    return try {
        keyFile.writeText(Base64.getEncoder().encodeToString(key))
        keyFile.hardenPermissions()
        MasterKey(key, KeyBackend.LOCAL_FILE)
    } catch (e: Exception) {
        Napier.e("cannot persist local key: ${e.message}", tag = TAG)
        MasterKey(ByteArray(0), KeyBackend.UNAVAILABLE)
    }
}

private fun runProcess(command: List<String>, stdin: String? = null): String? =
    runCommand(command, stdin)?.takeIf { it.exitCode == 0 }?.output

private fun runCommand(command: List<String>, stdin: String?): CommandResult? = try {
    val process = ProcessBuilder(command)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start()
    if (stdin != null) {
        process.outputStream.use { it.write(stdin.toByteArray(StandardCharsets.UTF_8)) }
    } else {
        process.outputStream.close()
    }
    if (!process.waitFor(10, TimeUnit.SECONDS)) {
        process.destroyForcibly()
        null
    } else {
        CommandResult(process.exitValue(), process.inputStream.bufferedReader().readText())
    }
} catch (e: Exception) {
    null
}
