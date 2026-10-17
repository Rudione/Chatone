package io.rudione.chatone.util.system

import com.sun.jna.Callback
import com.sun.jna.Function
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Timer
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.schedule

internal object MacSpaces {

    enum class StackLevel(val value: Long) { NORMAL(NORMAL_LEVEL), FLOATING(FLOATING_LEVEL), ABOVE_SYSTEM(STATUS_LEVEL) }

    data class FrontmostApp(val pid: Long, val bundleId: String?)

    fun pinToAllSpaces(
        windowHandle: Long,
        windowTitle: String,
        pinned: Boolean,
        aboveFullScreen: Boolean,
        nonActivating: Boolean
    ) = onMainThread {
        val window = nsWindow(windowHandle) ?: windowTitled(windowTitle) ?: return@onMainThread
        val behavior = msgSend.invokeLong(arrayOf(window, selector("collectionBehavior")))
        val updated = if (pinned) (behavior or JOINING) and CONFLICTING.inv() else behavior and JOINING.inv()
        if (updated != behavior) msgSend.invokeVoid(arrayOf(window, selector("setCollectionBehavior:"), updated))
        if (respondsTo(window, PREVENTS_ACTIVATION)) {
            msgSend.invokeVoid(arrayOf(window, selector(PREVENTS_ACTIVATION), if (pinned && nonActivating) 1L else 0L))
        }
        if (!pinned) return@onMainThread
        msgSend.invokeVoid(arrayOf(window, selector("setLevel:"), if (aboveFullScreen) STATUS_LEVEL else FLOATING_LEVEL))
        if (isTrue(window, "isVisible")) msgSend.invokeVoid(arrayOf(window, selector("orderFrontRegardless")))
    }

    fun applyStacking(windowHandle: Long, windowTitle: String, level: StackLevel) = onMainThread {
        val window = nsWindow(windowHandle) ?: windowTitled(windowTitle) ?: return@onMainThread
        val key = Pointer.nativeValue(window)
        val behavior = msgSend.invokeLong(arrayOf(window, selector("collectionBehavior")))
        val updated = if (level == StackLevel.ABOVE_SYSTEM) {
            stackedBehaviors.putIfAbsent(key, behavior)
            (behavior or JOINING) and CONFLICTING.inv()
        } else {
            stackedBehaviors.remove(key) ?: behavior
        }
        if (updated != behavior) msgSend.invokeVoid(arrayOf(window, selector("setCollectionBehavior:"), updated))
        msgSend.invokeVoid(arrayOf(window, selector("setLevel:"), level.value))
        if (level == StackLevel.ABOVE_SYSTEM && isTrue(window, "isVisible")) {
            msgSend.invokeVoid(arrayOf(window, selector("orderFrontRegardless")))
        }
    }

    suspend fun frontmostApplication(): FrontmostApp? = mainThreadResult {
        val app = sharedWorkspace()?.let { msgSend.invokePointer(arrayOf(it, selector("frontmostApplication"))) }
            ?: return@mainThreadResult null
        FrontmostApp(
            pid = msgSend.invokeInt(arrayOf(app, selector("processIdentifier"))).toLong(),
            bundleId = utf8(msgSend.invokePointer(arrayOf(app, selector("bundleIdentifier"))))
        )
    }

    suspend fun webBrowserBundleIds(): Set<String>? = mainThreadResult {
        val workspace = sharedWorkspace() ?: return@mainThreadResult null
        if (!respondsTo(workspace, OPEN_URL_HANDLERS)) return@mainThreadResult null
        val address = msgSend.invokePointer(arrayOf(objcClass("NSString"), selector("stringWithUTF8String:"), WEB_PROBE_URL))
        val url = msgSend.invokePointer(arrayOf(objcClass("NSURL"), selector("URLWithString:"), address))
            ?: return@mainThreadResult null
        val handlers = msgSend.invokePointer(arrayOf(workspace, selector(OPEN_URL_HANDLERS), url))
            ?: return@mainThreadResult null
        val count = msgSend.invokeLong(arrayOf(handlers, selector("count")))
        (0 until count).mapNotNullTo(mutableSetOf()) { index ->
            msgSend.invokePointer(arrayOf(handlers, selector("objectAtIndex:"), index))
                ?.let { msgSend.invokePointer(arrayOf(objcClass("NSBundle"), selector("bundleWithURL:"), it)) }
                ?.let { utf8(msgSend.invokePointer(arrayOf(it, selector("bundleIdentifier")))) }
                ?.lowercase()
        }
    }

    private suspend fun <T> mainThreadResult(block: () -> T?): T? {
        val result = CompletableDeferred<T?>()
        onMainThread {
            try {
                result.complete(block())
            } finally {
                result.complete(null)
            }
        }
        return withTimeoutOrNull(MAIN_RESULT_TIMEOUT_MS) { result.await() }
    }

    private fun sharedWorkspace(): Pointer? =
        msgSend.invokePointer(arrayOf(objcClass("NSWorkspace"), selector("sharedWorkspace")))

    private fun utf8(nsString: Pointer?): String? =
        nsString?.let { msgSend.invokePointer(arrayOf(it, selector("UTF8String"))) }?.getString(0, "UTF-8")

    fun setDockIconHidden(hidden: Boolean) {
        onMainThread {
            val app = application() ?: return@onMainThread
            forEachWindow(app) { msgSend.invokeVoid(arrayOf(it, selector("setCanHide:"), 0L)) }
            msgSend.invokeLong(arrayOf(app, selector("setActivationPolicy:"), if (hidden) POLICY_ACCESSORY else POLICY_REGULAR))
            msgSend.invokeVoid(arrayOf(app, selector("unhideWithoutActivation")))
        }
        timer.schedule(POLICY_SETTLE_MS) {
            onMainThread {
                val app = application() ?: return@onMainThread
                if (isTrue(app, "isHidden")) msgSend.invokeVoid(arrayOf(app, selector("unhideWithoutActivation")))
                forEachWindow(app) { msgSend.invokeVoid(arrayOf(it, selector("setCanHide:"), 1L)) }
            }
        }
    }

    private fun nsWindow(handle: Long): Pointer? {
        if (handle == 0L) return null
        val candidate = Pointer(handle)
        return when {
            isKindOf(candidate, "NSWindow") -> candidate
            isKindOf(candidate, "NSView") -> msgSend.invokePointer(arrayOf(candidate, selector("window")))
            else -> null
        }
    }

    private fun windowTitled(title: String): Pointer? {
        val app = application() ?: return null
        var found: Pointer? = null
        forEachWindow(app) { window ->
            val name = utf8(msgSend.invokePointer(arrayOf(window, selector("title"))))
            if (found == null && name == title) found = window
        }
        return found
    }

    private fun forEachWindow(app: Pointer, action: (Pointer) -> Unit) {
        val windows = msgSend.invokePointer(arrayOf(app, selector("windows"))) ?: return
        val count = msgSend.invokeLong(arrayOf(windows, selector("count")))
        for (index in 0 until count) {
            msgSend.invokePointer(arrayOf(windows, selector("objectAtIndex:"), index))?.let(action)
        }
    }

    private fun application(): Pointer? =
        msgSend.invokePointer(arrayOf(objcClass("NSApplication"), selector("sharedApplication")))

    private fun isKindOf(target: Pointer, className: String): Boolean =
        msgSend.invokeLong(arrayOf(target, selector("isKindOfClass:"), objcClass(className))) and 0xFF != 0L

    private fun respondsTo(target: Pointer, name: String): Boolean =
        msgSend.invokeLong(arrayOf(target, selector("respondsToSelector:"), selector(name))) and 0xFF != 0L

    private fun isTrue(target: Pointer, getter: String): Boolean =
        msgSend.invokeLong(arrayOf(target, selector(getter))) and 0xFF != 0L

    private fun onMainThread(block: () -> Unit) {
        if (!isMac) return
        runCatching {
            val task = MainThreadTask(block)
            pending += task
            dispatchAsync.invokeVoid(arrayOf(mainQueue, null, task))
        }.onFailure { Napier.w("Main thread dispatch failed: ${it.message}", tag = TAG) }
    }

    internal class MainThreadTask(private val block: () -> Unit) : DispatchFunction {
        override fun invoke(context: Pointer?) {
            pending -= this
            runCatching(block).onFailure { Napier.w("Window space update failed: ${it.message}", tag = TAG) }
        }
    }

    internal fun interface DispatchFunction : Callback {
        fun invoke(context: Pointer?)
    }

    private fun selector(name: String): Pointer = registerSelector.invokePointer(arrayOf(name))

    private fun objcClass(name: String): Pointer = getClass.invokePointer(arrayOf(name))

    private val isMac = System.getProperty("os.name").orEmpty().lowercase().contains("mac")
    private val macMajorVersion = System.getProperty("os.version").orEmpty().substringBefore('.').toIntOrNull() ?: 0
    private val pending = ConcurrentHashMap.newKeySet<MainThreadTask>()
    private val stackedBehaviors = HashMap<Long, Long>()
    private val timer by lazy { Timer("mac-spaces", true) }
    private val runtime by lazy { NativeLibrary.getInstance("objc") }
    private val system by lazy { NativeLibrary.getInstance("System") }
    private val msgSend: Function by lazy { runtime.getFunction("objc_msgSend") }
    private val registerSelector: Function by lazy { runtime.getFunction("sel_registerName") }
    private val getClass: Function by lazy { runtime.getFunction("objc_getClass") }
    private val dispatchAsync: Function by lazy { system.getFunction("dispatch_async_f") }
    private val mainQueue: Pointer by lazy { system.getGlobalVariableAddress("_dispatch_main_q") }

    private const val JOIN_ALL_SPACES = 1L shl 0
    private const val MOVE_TO_ACTIVE_SPACE = 1L shl 1
    private const val FULL_SCREEN_PRIMARY = 1L shl 7
    private const val FULL_SCREEN_AUXILIARY = 1L shl 8
    private const val FULL_SCREEN_NONE = 1L shl 9
    private const val PRIMARY = 1L shl 16
    private const val AUXILIARY = 1L shl 17
    private const val JOIN_ALL_APPLICATIONS = 1L shl 18
    private const val MODERN_SPACES_VERSION = 13
    private val JOINING = JOIN_ALL_SPACES or FULL_SCREEN_AUXILIARY or
        (if (macMajorVersion >= MODERN_SPACES_VERSION) AUXILIARY or JOIN_ALL_APPLICATIONS else 0L)
    private val CONFLICTING = MOVE_TO_ACTIVE_SPACE or FULL_SCREEN_PRIMARY or FULL_SCREEN_NONE or
        (if (macMajorVersion >= MODERN_SPACES_VERSION) PRIMARY else 0L)
    private const val NORMAL_LEVEL = 0L
    private const val FLOATING_LEVEL = 3L
    private const val STATUS_LEVEL = 25L
    private const val POLICY_REGULAR = 0L
    private const val POLICY_ACCESSORY = 1L
    private const val POLICY_SETTLE_MS = 1_500L
    private const val MAIN_RESULT_TIMEOUT_MS = 1_000L
    private const val OPEN_URL_HANDLERS = "URLsForApplicationsToOpenURL:"
    private const val WEB_PROBE_URL = "https://example.com"
    private const val PREVENTS_ACTIVATION = "_setPreventsActivation:"
    private const val TAG = "MacSpaces"
}
