package io.rudione.chatone.util.security

import io.github.aakira.napier.Napier
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.security.SecureRandom
import java.util.Base64
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
            Napier.w("open failed: ${e.message}", tag = TAG)
            null
        }
    }
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
    val blobFile = File(chatoneDir(), "secret.dpapi")
    if (blobFile.isFile) {
        val plain = runCatching {
            com.sun.jna.platform.win32.Crypt32Util.cryptUnprotectData(
                Base64.getDecoder().decode(blobFile.readText().trim())
            )
        }.getOrNull()
        if (plain != null && plain.size == KEY_BYTES) return MasterKey(
            plain,
            KeyBackend.WINDOWS_DPAPI
        )
        Napier.w("DPAPI blob unreadable, regenerating master key", tag = TAG)
    }
    val key = newKey()
    val blob = runCatching {
        com.sun.jna.platform.win32.Crypt32Util.cryptProtectData(key)
    }.getOrNull() ?: return null
    blobFile.writeText(Base64.getEncoder().encodeToString(blob))
    return MasterKey(key, KeyBackend.WINDOWS_DPAPI)
}

private fun macKey(): MasterKey? {
    val existing = runProcess(
        listOf(
            "/usr/bin/security", "find-generic-password",
            "-s", KEYCHAIN_SERVICE, "-a", KEYCHAIN_ACCOUNT, "-w"
        )
    )?.trim()
    if (!existing.isNullOrEmpty()) {
        val decoded = runCatching { Base64.getDecoder().decode(existing) }.getOrNull()
        if (decoded != null && decoded.size == KEY_BYTES) {
            return MasterKey(decoded, KeyBackend.MACOS_KEYCHAIN)
        }
    }
    val key = newKey()
    val stored = runProcess(
        listOf(
            "/usr/bin/security", "add-generic-password",
            "-U",
            "-s", KEYCHAIN_SERVICE, "-a", KEYCHAIN_ACCOUNT,
            "-w", Base64.getEncoder().encodeToString(key)
        )
    ) != null
    return if (stored) MasterKey(key, KeyBackend.MACOS_KEYCHAIN) else null
}

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

private fun runProcess(command: List<String>, stdin: String? = null): String? = try {
    val process = ProcessBuilder(command)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start()
    if (stdin != null) {
        process.outputStream.use { it.write(stdin.toByteArray(StandardCharsets.UTF_8)) }
    } else {
        process.outputStream.close()
    }
    val output = process.inputStream.bufferedReader().readText()
    val finished = process.waitFor(10, TimeUnit.SECONDS)
    if (!finished) {
        process.destroyForcibly()
        null
    } else if (process.exitValue() == 0) {
        output
    } else {
        null
    }
} catch (e: Exception) {
    null
}
