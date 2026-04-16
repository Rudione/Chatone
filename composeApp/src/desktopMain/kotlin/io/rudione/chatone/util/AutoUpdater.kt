package io.rudione.chatone.util

import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

object AutoUpdater {

    private const val GITHUB_API = "https://api.github.com"
    private const val REPO = "Rudione/Chatone"

    private val CURRENT_VERSION: String = BuildConfig.VERSION


    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        expectSuccess = true
    }

    @Serializable
    data class GitHubRelease(
        @SerialName("tag_name") val tagName: String,
        @SerialName("name") val name: String,
        @SerialName("body") val body: String,
        @SerialName("assets") val assets: List<Asset>
    ) {
        @Serializable
        data class Asset(
            @SerialName("name") val name: String,
            @SerialName("browser_download_url") val downloadUrl: String,
            @SerialName("size") val size: Long
        )
    }


    suspend fun checkForUpdates(showDialog: Boolean = true): UpdateResult {
        return withContext(Dispatchers.IO) {
            try {
                Napier.d("Checking for updates... current: $CURRENT_VERSION")

                val response = httpClient.get("$GITHUB_API/repos/$REPO/releases/latest")
                val release: GitHubRelease = response.body()
                val latestVersion = release.tagName.removePrefix("v")

                Napier.d("Latest version: $latestVersion")

                if (isVersionNewer(latestVersion, CURRENT_VERSION)) {
                    Napier.i("Update available: $CURRENT_VERSION → $latestVersion")

                    if (showDialog) {
                        withContext(Dispatchers.Main) {
                            showUpdateDialog(release, latestVersion)
                        }
                    }

                    return@withContext UpdateResult.Available(release, latestVersion)
                }

                Napier.d("No updates available")
                UpdateResult.UpToDate

            } catch (e: Exception) {
                Napier.e("Failed to check for updates: ${e.message}", e)
                UpdateResult.Error(e)
            }
        }
    }

    suspend fun downloadAndUpdate(release: GitHubRelease, latestVersion: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val asset = selectAssetForCurrentOs(release.assets)
                    ?: throw IllegalStateException("No suitable asset found for ${System.getProperty("os.name")}")

                Napier.i("Downloading update: ${asset.name} (${formatSize(asset.size)})")

                val downloadsDir = File(System.getProperty("user.home"), "Downloads").apply { mkdirs() }
                val updateFile = File(downloadsDir, asset.name)

                downloadFile(asset.downloadUrl, updateFile) { progress ->
                    Napier.d("Download progress: $progress%")
                }

                Napier.i("Downloaded to: ${updateFile.absolutePath}")

                launchInstallerAndExit(updateFile)
                true

            } catch (e: Exception) {
                Napier.e("Failed to download/update: ${e.message}", e)
                false
            }
        }
    }



    private fun isVersionNewer(latest: String, current: String): Boolean {
        return latest.split(".").map { it.toIntOrNull() ?: 0 }
            .zip(current.split(".").map { it.toIntOrNull() ?: 0 })
            .run {
                for ((l, c) in this) {
                    if (l > c) return true
                    if (l < c) return false
                }
                false
            }
    }

    private fun selectAssetForCurrentOs(assets: List<GitHubRelease.Asset>): GitHubRelease.Asset? {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") ->
                assets.find { it.name.lowercase().endsWith(".msi") }
                    ?: assets.find { it.name.lowercase().endsWith(".zip") }
            os.contains("mac") ->
                assets.find { it.name.lowercase().endsWith(".dmg") }
                    ?: assets.find { it.name.lowercase().endsWith(".zip") }
            else ->
                assets.find { it.name.lowercase().endsWith(".deb") }
        }
    }


    private suspend fun downloadFile(url: String, destination: File, onProgress: (Int) -> Unit = {}) {
        withContext(Dispatchers.IO) {
            httpClient.prepareGet(url).execute { response ->
                val totalBytes = response.headers["Content-Length"]?.toLong() ?: 0
                var downloadedBytes = 0L


                FileOutputStream(destination).use { output ->
                    val fileChannel: FileChannel = output.channel
                    val byteBuffer = ByteBuffer.allocate(8192)

                    response.bodyAsChannel().toInputStream().use { input ->
                        val readableChannel = Channels.newChannel(input)
                        var bytesRead: Int
                        while (readableChannel.read(byteBuffer).also { bytesRead = it } != -1) {
                            byteBuffer.flip()
                            fileChannel.write(byteBuffer)
                            byteBuffer.clear()

                            downloadedBytes += bytesRead
                            if (totalBytes > 0) {
                                val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                                onProgress(progress.coerceIn(0, 100))
                            }
                        }
                    }
                }
            }
        }
    }


    private fun launchInstallerAndExit(file: File) {
        val os = System.getProperty("os.name").lowercase()
        try {
            val command = when {
                os.contains("win") -> listOf("cmd", "/c", "start", "", file.absolutePath)
                os.contains("mac") -> listOf("open", file.absolutePath)
                else -> listOf("xdg-open", file.absolutePath)
            }
            Napier.i("Launching installer: ${command.joinToString(" ")}")
            ProcessBuilder(command).start()
        } catch (e: Exception) {
            Napier.e("Failed to launch installer, falling back to Desktop.open: ${e.message}", e)
            try { java.awt.Desktop.getDesktop().open(file) } catch (_: Exception) {}
        }

        SwingUtilities.invokeLater {
            JOptionPane.showMessageDialog(
                null,
                "Installer was opened.\nPlease close Chatone and complete the installation.\n\nFile: ${file.absolutePath}",
                "Update Ready",
                JOptionPane.INFORMATION_MESSAGE
            )
            System.exit(0)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun showUpdateDialog(release: GitHubRelease, latestVersion: String) {
        SwingUtilities.invokeLater {
            val message = """
                <html>
                <b>New version available!</b><br><br>
                Current: $CURRENT_VERSION<br>
                Latest: $latestVersion<br><br>
                <b>What's new:</b><br>
                ${release.body.take(300).replace("\n", "<br>")}...
                </html>
            """.trimIndent()

            val options = arrayOf("Update Now", "Remind Me Later", "Skip This Version")
            val choice = JOptionPane.showOptionDialog(
                null,
                message,
                "Update Available",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
            )

            when (choice) {
                0 -> {
                    GlobalScope.launch(Dispatchers.IO) {
                        val success = downloadAndUpdate(release, latestVersion)
                        if (!success) {
                            withContext(Dispatchers.Main) {
                                JOptionPane.showMessageDialog(
                                    null,
                                    "Failed to download update. Please try again later.",
                                    "Update Error",
                                    JOptionPane.ERROR_MESSAGE
                                )
                            }
                        }
                    }
                }
                1 -> Napier.d("User chose to remind later")
                2 -> Napier.d("User skipped version $latestVersion")
            }
        }
    }

    private fun formatSize(bytes: Long): String {
        val kb = 1024
        val mb = kb * 1024
        return when {
            bytes < kb -> "$bytes B"
            bytes < mb -> "%.1f KB".format(bytes / kb.toDouble())
            else -> "%.1f MB".format(bytes / mb.toDouble())
        }
    }
}

sealed class UpdateResult {
    object UpToDate : UpdateResult()
    data class Available(val release: AutoUpdater.GitHubRelease, val version: String) : UpdateResult()
    data class Error(val exception: Exception) : UpdateResult()
}