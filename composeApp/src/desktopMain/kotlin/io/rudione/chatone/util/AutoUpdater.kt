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
import kotlinx.coroutines.Dispatchers
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

    private const val CURRENT_VERSION = "1.0.8"


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

                val tempDir = File(System.getProperty("java.io.tmpdir"), "chatone-update-${System.currentTimeMillis()}")
                tempDir.mkdirs()
                val updateFile = File(tempDir, asset.name)

                downloadFile(asset.downloadUrl, updateFile) { progress ->
                    Napier.d("Download progress: $progress%")
                }

                Napier.i("Downloaded to: ${updateFile.absolutePath}")

                when {
                    asset.name.endsWith(".msi", ignoreCase = true) -> {
                        installMsi(updateFile)
                    }
                    asset.name.endsWith(".zip", ignoreCase = true) -> {

                        extractZip(updateFile, tempDir)
                        restartFromZip(tempDir)
                        true
                    }
                    else -> {
                        Napier.e("Unknown update format: ${asset.name}")
                        false
                    }
                }

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
        return assets.find { asset ->
            val name = asset.name.lowercase()
            when {
                os.contains("win") && name.endsWith(".msi") -> true
                os.contains("win") && name.endsWith(".zip") -> true
                os.contains("mac") && (name.endsWith(".dmg") || name.endsWith(".zip")) -> true
                os.contains("linux") && name.endsWith(".deb") -> true
                else -> false
            }
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


    private fun installMsi(file: File): Boolean {
        return try {
            val command = listOf("msiexec", "/i", file.absolutePath, "/quiet", "/norestart")
            Napier.d("Running installer: ${command.joinToString(" ")}")

            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()
            Napier.d("Installer exited with code: $exitCode")

            if (exitCode == 0) {
                restartApp()
                true
            } else false

        } catch (e: Exception) {
            Napier.e("Failed to run installer: ${e.message}", e)
            false
        }
    }


    private fun extractZip(zipFile: File, destDir: File) {
        java.util.zip.ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val dest = File(destDir, entry.name)
                if (entry.isDirectory) {
                    dest.mkdirs()
                } else {
                    dest.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(dest).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }

    private fun restartFromZip(extractDir: File) {
        try {

            val launcher = File(extractDir, "Chatone.exe")
                .takeIf { it.exists() }
                ?: File(extractDir, "Chatone")
                    .takeIf { it.exists() }
                ?: throw IllegalStateException("Launcher not found in extracted update")

            Napier.d("Restarting from: ${launcher.absolutePath}")


            ProcessBuilder(launcher.absolutePath).start()
            System.exit(0)

        } catch (e: Exception) {
            Napier.e("Failed to restart from ZIP: ${e.message}", e)
        }
    }


    private fun restartApp() {
        try {


            val javaHome = System.getProperty("java.home")
            val javaBin = "$javaHome/bin/java"
            val classpath = System.getProperty("java.class.path")
            val mainClass = "io.rudione.chatone.MainKt"

            ProcessBuilder(javaBin, "-cp", classpath, mainClass)
                .start()
            System.exit(0)
        } catch (e: Exception) {
            Napier.e("Failed to restart app: ${e.message}", e)
        }
    }

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
                    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
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