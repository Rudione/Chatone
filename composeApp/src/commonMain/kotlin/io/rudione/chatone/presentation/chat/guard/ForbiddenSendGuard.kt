package io.rudione.chatone.presentation.chat.guard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.rudione.chatone.data.repository.ForbiddenWordsRepository
import io.rudione.chatone.domain.moderation.ForbiddenHit
import io.rudione.chatone.domain.moderation.ForbiddenWordScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.compose.koinInject

@Immutable
class ForbiddenSendPrompt internal constructor(
    val text: String,
    val hits: List<ForbiddenHit>,
    internal val send: () -> Unit
)

@Stable
class ForbiddenSendGuard internal constructor(
    private val scanner: StateFlow<ForbiddenWordScanner?>,
    private val scope: CoroutineScope
) {

    var prompt: ForbiddenSendPrompt? by mutableStateOf(null)
        private set

    private var waitingForDictionary = false

    fun submit(text: String, send: () -> Unit) {
        if (prompt != null || waitingForDictionary) return
        val ready = scanner.value
        if (ready != null) {
            decide(ready, text, send)
            return
        }
        waitingForDictionary = true
        scope.launch {
            val loaded = withTimeoutOrNull(DICTIONARY_WAIT_MS) { scanner.filterNotNull().first() }
            waitingForDictionary = false
            if (loaded == null) send() else decide(loaded, text, send)
        }
    }

    fun confirm() {
        val pending = prompt ?: return
        prompt = null
        pending.send()
    }

    fun dismiss() {
        prompt = null
    }

    private fun decide(scanner: ForbiddenWordScanner, text: String, send: () -> Unit) {
        val hits = scanner.scanChatMessage(text)
        if (hits.isEmpty()) send() else prompt = ForbiddenSendPrompt(text, hits, send)
    }

    private companion object {
        const val DICTIONARY_WAIT_MS = 1_500L
    }
}

@Composable
fun rememberForbiddenSendGuard(): ForbiddenSendGuard {
    val repository: ForbiddenWordsRepository = koinInject()
    val scope = rememberCoroutineScope()
    return remember(repository, scope) { ForbiddenSendGuard(repository.scanner, scope) }
}
