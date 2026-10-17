package io.rudione.chatone.util.system

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinReg
import com.sun.jna.ptr.IntByReference
import java.io.File

internal enum class ForegroundApp { SELF, BROWSER, OTHER, UNKNOWN }

internal object ForegroundAppProbe {

    private val osName = System.getProperty("os.name").orEmpty().lowercase()
    private val isWindows = osName.contains("win")
    private val isMac = osName.contains("mac")
    private val ownPid = ProcessHandle.current().pid()

    @Volatile
    private var cachedExecutable: Pair<Long, String?>? = null

    @Volatile
    private var discoveredBrowsers: Set<String> = emptySet()

    @Volatile
    private var browsersDiscoveredAtNs = 0L

    val isSupported: Boolean = isWindows || isMac

    suspend fun current(): ForegroundApp = when {
        isWindows -> windowsForeground()
        isMac -> macForeground()
        else -> ForegroundApp.UNKNOWN
    }

    private suspend fun windowsForeground(): ForegroundApp {
        val pid = runCatching {
            val hwnd = User32.INSTANCE.GetForegroundWindow() ?: return ForegroundApp.UNKNOWN
            IntByReference().also { User32.INSTANCE.GetWindowThreadProcessId(hwnd, it) }.value.toLong()
        }.getOrNull() ?: return ForegroundApp.UNKNOWN
        val executable = runCatching { executableName(pid) }.getOrNull()
        return when {
            pid <= 0L -> ForegroundApp.UNKNOWN
            pid == ownPid -> ForegroundApp.SELF
            executable != null && (executable in WINDOWS_BROWSERS || executable in browsers()) -> ForegroundApp.BROWSER
            else -> ForegroundApp.OTHER
        }
    }

    private suspend fun macForeground(): ForegroundApp {
        val app = MacSpaces.frontmostApplication() ?: return ForegroundApp.UNKNOWN
        val bundleId = app.bundleId?.lowercase().orEmpty()
        return when {
            app.pid == ownPid -> ForegroundApp.SELF
            bundleId.isEmpty() -> ForegroundApp.OTHER
            MAC_BROWSER_PREFIXES.any(bundleId::startsWith) || bundleId in browsers() -> ForegroundApp.BROWSER
            else -> ForegroundApp.OTHER
        }
    }

    private suspend fun browsers(): Set<String> {
        val now = System.nanoTime()
        if (browsersDiscoveredAtNs != 0L && now - browsersDiscoveredAtNs < BROWSER_REFRESH_NS) return discoveredBrowsers
        browsersDiscoveredAtNs = now
        val found = if (isMac) MacSpaces.webBrowserBundleIds() else windowsRegisteredBrowsers()
        if (!found.isNullOrEmpty()) discoveredBrowsers = found
        return discoveredBrowsers
    }

    private fun windowsRegisteredBrowsers(): Set<String> =
        listOf(WinReg.HKEY_CURRENT_USER, WinReg.HKEY_LOCAL_MACHINE).flatMapTo(mutableSetOf()) { root ->
            WINDOWS_CLIENT_KEYS.flatMap { clientsKey ->
                runCatching { Advapi32Util.registryGetKeys(root, clientsKey).toList() }.getOrDefault(emptyList())
                    .mapNotNull { client ->
                        runCatching {
                            Advapi32Util.registryGetValue(root, "$clientsKey\\$client\\shell\\open\\command", "")
                        }.getOrNull()?.toString()?.let(::executableOfCommand)
                    }
            }
        }

    private fun executableOfCommand(command: String): String? {
        val trimmed = command.trim()
        val path = if (trimmed.startsWith('"')) trimmed.drop(1).substringBefore('"')
        else trimmed.substringBefore(".exe", missingDelimiterValue = "").takeIf { it.isNotEmpty() }?.plus(".exe")
        return path?.takeIf { it.isNotBlank() }?.let { File(it).name.lowercase() }
    }

    private fun executableName(pid: Long): String? {
        cachedExecutable?.takeIf { it.first == pid }?.let { return it.second }
        val name = ProcessHandle.of(pid)
            .flatMap { it.info().command() }
            .map { File(it).name.lowercase() }
            .orElse(null)
        cachedExecutable = pid to name
        return name
    }

    private const val BROWSER_REFRESH_NS = 10L * 60 * 1_000_000_000

    private val WINDOWS_CLIENT_KEYS = listOf(
        "SOFTWARE\\Clients\\StartMenuInternet",
        "SOFTWARE\\WOW6432Node\\Clients\\StartMenuInternet"
    )

    private val WINDOWS_BROWSERS = setOf(
        "chrome.exe", "msedge.exe", "firefox.exe", "opera.exe", "brave.exe", "browser.exe",
        "vivaldi.exe", "arc.exe", "zen.exe", "librewolf.exe", "waterfox.exe", "floorp.exe",
        "chromium.exe", "thorium.exe", "comet.exe", "iexplore.exe"
    )

    private val MAC_BROWSER_PREFIXES = listOf(
        "com.google.chrome", "com.apple.safari", "org.mozilla.firefox", "org.mozilla.nightly",
        "com.microsoft.edgemac", "com.operasoftware.", "com.brave.browser", "ru.yandex.desktop.yandex-browser",
        "com.vivaldi.vivaldi", "company.thebrowser.", "app.zen-browser.", "org.chromium.chromium",
        "io.gitlab.librewolf", "net.waterfox.", "one.ablaze.floorp", "ai.perplexity.comet", "com.kagi.kagimacos"
    )
}
