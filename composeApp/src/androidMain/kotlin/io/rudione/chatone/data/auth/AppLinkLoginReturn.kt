package io.rudione.chatone.data.auth

import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import kotlin.concurrent.Volatile

class AppLinkLoginReturn(private val scope: CoroutineScope) : LoginReturnChannel {

    @Volatile
    private var handler: LoginReturnHandler? = null

    override suspend fun open(handler: LoginReturnHandler): LoginReturnTarget {
        this.handler = handler
        return LoginReturnTarget.AppLink
    }

    override fun close() {
        handler = null
    }

    fun deliver(line: String) {
        val current = handler ?: return
        scope.launch { current.accept(line) }
    }
}

object LoginReturnIntents {

    private const val SCHEME = "chatone"
    private const val HOST = "login"
    private const val LINE_PARAMETER = "line"
    private const val MAX_LINE_CHARS = 8_192

    fun consume(intent: Intent?): Boolean {
        val data = intent?.data ?: return false
        if (intent.action != Intent.ACTION_VIEW || data.scheme != SCHEME || data.host != HOST) return false
        val line = runCatching { data.getQueryParameter(LINE_PARAMETER) }.getOrNull()
        intent.data = null
        if (line.isNullOrBlank() || line.length > MAX_LINE_CHARS) return true
        KoinPlatform.getKoinOrNull()?.getOrNull<AppLinkLoginReturn>()?.deliver(line)
        return true
    }
}
