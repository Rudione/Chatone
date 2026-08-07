package io.rudione.chatone.util.system

import com.russhwolf.settings.Settings
import io.github.aakira.napier.Napier
import io.rudione.chatone.util.BuildConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

object AutoUpdater {

    private const val GITHUB_API = "https://api.github.com"
    private const val REPO = "Rudione/Chatone"
    private const val KEY_SKIPPED_VERSION = "updater_skipped_version"

    private val CURRENT_VERSION: String = BuildConfig.VERSION

    private val settings by lazy { Settings() }

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        expectSuccess = true
    }

    @Serializable
    data class GitHubRelease(
        @SerialName("tag_name") val tagName: String,
        @SerialName("name") val name: String = "",
        @SerialName("body") val body: String = "",
        @SerialName("assets") val assets: List<Asset> = emptyList()
    ) {
        @Serializable
        data class Asset(
            @SerialName("name") val name: String,
            @SerialName("browser_download_url") val downloadUrl: String,
            @SerialName("size") val size: Long = 0,
            @SerialName("digest") val digest: String? = null
        )
    }

    sealed interface Stage {
        data object Idle : Stage
        data class Downloading(val percent: Int, val downloadedBytes: Long, val totalBytes: Long) : Stage
        data object Verifying : Stage
        data object Installing : Stage
        data class Failed(val reason: String) : Stage
    }

    private val _stage = MutableStateFlow<Stage>(Stage.Idle)
    val stage: StateFlow<Stage> = _stage.asStateFlow()

    private val _available = MutableStateFlow<Available?>(null)
    val available: StateFlow<Available?> = _available.asStateFlow()

    data class Available(val release: GitHubRelease, val version: String, val notes: String)

    fun currentVersion(): String = CURRENT_VERSION

    fun dismiss() {
        _available.value = null
        _stage.value = Stage.Idle
    }

    fun skipVersion(version: String) {
        settings.putString(KEY_SKIPPED_VERSION, version)
        dismiss()
    }

    suspend fun checkForUpdates(respectSkipped: Boolean = true): UpdateResult =
        withContext(Dispatchers.IO) {
            try {
                Napier.d("Checking for updates... current: $CURRENT_VERSION", tag = "AutoUpdater")

                val release: GitHubRelease =
                    httpClient.get("$GITHUB_API/repos/$REPO/releases/latest").body()
                val latestVersion = release.tagName.removePrefix("v")

                if (!isVersionNewer(latestVersion, CURRENT_VERSION)) {
                    return@withContext UpdateResult.UpToDate
                }
                if (respectSkipped &&
                    settings.getStringOrNull(KEY_SKIPPED_VERSION) == latestVersion
                ) {
                    Napier.d("Update $latestVersion was skipped by the user", tag = "AutoUpdater")
                    return@withContext UpdateResult.UpToDate
                }

                Napier.i("Update available: $CURRENT_VERSION -> $latestVersion", tag = "AutoUpdater")
                val entry = Available(release, latestVersion, release.body.trim())
                _available.value = entry
                UpdateResult.Available(release, latestVersion)
            } catch (e: Exception) {
                Napier.e("Failed to check for updates: ${e.message}", e, tag = "AutoUpdater")
                UpdateResult.Error(e)
            }
        }

    suspend fun downloadAndUpdate(release: GitHubRelease): Boolean = withContext(Dispatchers.IO) {
        try {
            val asset = selectAssetForCurrentOs(release.assets)
                ?: throw IllegalStateException("No installer for ${System.getProperty("os.name")}")

            val safeName = sanitizeAssetName(asset.name)
                ?: throw IllegalStateException("Rejected update asset name: ${asset.name}")
            require(isTrustedDownloadUrl(asset.downloadUrl)) {
                "Rejected update download host"
            }

            val stagingDir = File(System.getProperty("java.io.tmpdir"), "chatone-update").apply {
                mkdirs()
            }
            val updateFile = File(stagingDir, safeName)

            _stage.value = Stage.Downloading(0, 0, asset.size)
            downloadFile(asset.downloadUrl, updateFile) { downloaded, total ->
                val percent = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                _stage.value = Stage.Downloading(percent.coerceIn(0, 100), downloaded, total)
            }

            _stage.value = Stage.Verifying
            val expected = asset.digest?.substringAfter("sha256:", "")?.takeIf { it.isNotBlank() }
            if (expected == null || !sha256(updateFile).equals(expected, ignoreCase = true)) {
                runCatching { updateFile.delete() }
                throw IllegalStateException(
                    if (expected == null) "Update asset $safeName has no sha256 digest"
                    else "Checksum mismatch for $safeName"
                )
            }
            Napier.i("Update checksum verified", tag = "AutoUpdater")

            _stage.value = Stage.Installing
            launchInstallerAndExit(updateFile)
            true
        } catch (e: Exception) {
            Napier.e("Failed to download/update: ${e.message}", e, tag = "AutoUpdater")
            _stage.value = Stage.Failed(e.message ?: "unknown error")
            false
        }
    }

    private val ALLOWED_EXTENSIONS = setOf("msi", "zip", "dmg", "deb", "exe")

    private val TRUSTED_DOWNLOAD_HOSTS = setOf(
        "github.com", "api.github.com", "objects.githubusercontent.com",
        "release-assets.githubusercontent.com"
    )

    internal fun sanitizeAssetName(rawName: String): String? {
        val name = rawName.trim()
        if (name.isEmpty() || name.length > 128) return null
        if (name.any { it.isISOControl() }) return null
        if (name.contains('/') || name.contains('\\')) return null
        if (name.startsWith(".")) return null
        if (name.any { it !in 'a'..'z' && it !in 'A'..'Z' && it !in '0'..'9' && it !in "._-+" }) return null
        val extension = name.substringAfterLast('.', "").lowercase()
        if (extension !in ALLOWED_EXTENSIONS) return null
        return name
    }

    internal fun isTrustedDownloadUrl(url: String): Boolean {
        if (!url.startsWith("https://", ignoreCase = true)) return false
        val host = runCatching { java.net.URI(url).host }.getOrNull()?.lowercase() ?: return false
        return host in TRUSTED_DOWNLOAD_HOSTS
    }

    internal fun isVersionNewer(latest: String, current: String): Boolean {
        val l = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(l.size, c.size)) {
            val a = l.getOrElse(i) { 0 }
            val b = c.getOrElse(i) { 0 }
            if (a > b) return true
            if (a < b) return false
        }
        return false
    }

    internal fun isPerUserWindowsInstall(installPath: String?): Boolean {
        val path = installPath?.replace('/', '\\')?.lowercase() ?: return false
        return path.contains("\\appdata\\local\\")
    }

    private fun currentInstallPath(): String? =
        System.getProperty("jpackage.app-path") ?: System.getProperty("java.home")

    private fun selectAssetForCurrentOs(assets: List<GitHubRelease.Asset>): GitHubRelease.Asset? {
        val os = System.getProperty("os.name").lowercase()
        fun endingWith(vararg suffixes: String) = suffixes.firstNotNullOfOrNull { suffix ->
            assets.firstOrNull { it.name.lowercase().endsWith(suffix) }
        }
        return when {
            os.contains("win") ->
                if (isPerUserWindowsInstall(currentInstallPath())) {
                    endingWith(SETUP_SUFFIX, ".msi", ".zip")
                } else {
                    endingWith(".msi", SETUP_SUFFIX, ".zip")
                }

            os.contains("mac") -> endingWith(".dmg", ".zip")
            else -> endingWith(".deb")
        }
    }

    private const val SETUP_SUFFIX = "-setup.exe"

    private suspend fun downloadFile(
        url: String,
        destination: File,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            httpClient.prepareGet(url).execute { response ->
                val totalBytes = response.headers["Content-Length"]?.toLongOrNull() ?: 0L
                var downloadedBytes = 0L
                val buffer = ByteArray(64 * 1024)

                FileOutputStream(destination).use { output ->
                    response.bodyAsChannel().toInputStream().use { input ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            onProgress(downloadedBytes, totalBytes)
                        }
                    }
                }
            }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun launchInstallerAndExit(file: File) {
        val os = System.getProperty("os.name").lowercase()
        val extension = file.extension.lowercase()

        val command = when {
            os.contains("win") && extension == "exe" -> listOf(
                file.absolutePath,
                "/VERYSILENT", "/SUPPRESSMSGBOXES", "/NORESTART", "/CLOSEAPPLICATIONS",
                "/RESTARTAPPLICATIONS"
            )

            os.contains("win") && extension == "msi" -> listOf(
                windowsSystem32Path("msiexec.exe"), "/i", file.absolutePath, "/qn", "/norestart"
            )

            os.contains("win") -> listOf(
                windowsSystem32Path("rundll32.exe"),
                "url.dll,FileProtocolHandler",
                file.absolutePath
            )

            os.contains("mac") -> listOf("open", file.absolutePath)
            else -> listOf("xdg-open", file.absolutePath)
        }

        Napier.i("Launching installer: ${command.first()}", tag = "AutoUpdater")
        runCatching { ProcessBuilder(command).start() }
            .onFailure { e ->
                Napier.e("Installer launch failed: ${e.message}", e, tag = "AutoUpdater")
                runCatching { java.awt.Desktop.getDesktop().open(file) }
            }

        Runtime.getRuntime().addShutdownHook(Thread { runCatching { file.parentFile?.deleteOnExit() } })
        kotlin.system.exitProcess(0)
    }
}

sealed class UpdateResult {
    object UpToDate : UpdateResult()
    data class Available(val release: AutoUpdater.GitHubRelease, val version: String) : UpdateResult()
    data class Error(val exception: Exception) : UpdateResult()
}
