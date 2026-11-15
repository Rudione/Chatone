package io.rudione.chatone.domain.ai

import com.russhwolf.settings.Settings
import io.ktor.http.Url
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

interface AiDataConsent {
    fun isGranted(endpoint: String): Boolean
    suspend fun authorize(endpoint: String): Boolean
}

object DeniedAiDataConsent : AiDataConsent {
    override fun isGranted(endpoint: String) = false
    override suspend fun authorize(endpoint: String) = false
}

fun aiRecipient(endpoint: String): String? = runCatching {
    val url = Url(endpoint.trim())
    require(url.protocol.name in setOf("https", "http") && url.host.isNotBlank())
    require(url.user.isNullOrEmpty() && url.password.isNullOrEmpty())
    require(url.parameters.isEmpty() && url.fragment.isEmpty())
    url.toString().trimEnd('/')
}.getOrNull()

class AiConsentRequest internal constructor(val recipient: String) {
    internal val decision = CompletableDeferred<Boolean>()
}

class AiConsentStore(private val settings: Settings) : AiDataConsent {
    private val key = "ai_data_consent_v1"
    private val mutex = Mutex()
    private var revision = 0L
    private val _granted = MutableStateFlow(
        runCatching { Json.decodeFromString<Set<String>>(settings.getString(key, "[]")) }.getOrDefault(emptySet())
    )
    val granted = _granted.asStateFlow()
    private val _pending = MutableStateFlow<AiConsentRequest?>(null)
    val pending = _pending.asStateFlow()

    override fun isGranted(endpoint: String): Boolean = aiRecipient(endpoint)?.let { it in _granted.value } == true

    override suspend fun authorize(endpoint: String): Boolean {
        val recipient = aiRecipient(endpoint) ?: return false
        return mutex.withLock {
            if (isGranted(recipient)) return@withLock true
            val requestRevision = revision
            val request = AiConsentRequest(recipient)
            _pending.value = request
            try {
                val accepted = request.decision.await() && requestRevision == revision
                if (accepted) save((_granted.value + recipient).toList().takeLast(32).toSet())
                accepted
            } finally {
                _pending.compareAndSet(request, null)
            }
        }
    }

    fun respond(request: AiConsentRequest, accepted: Boolean) {
        if (_pending.value === request) request.decision.complete(accepted)
    }

    fun revokeAll() {
        revision++
        _pending.value?.decision?.complete(false)
        save(emptySet())
    }

    private fun save(values: Set<String>) {
        settings.putString(key, Json.encodeToString(values))
        _granted.value = values
    }
}

object AiDataRedactor {
    private val secrets = listOf(
        Regex("(?i)\\b(?:oauth:|Bearer\\s+)[A-Za-z0-9._~+/=-]{8,}"),
        Regex("\\b(?:sk-|gsk_|xai-|ghp_|github_pat_|AIza)[A-Za-z0-9_-]{12,}"),
        Regex("\\beyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"),
        Regex("(?i)\\b(?:api[_-]?key|access[_-]?token|refresh[_-]?token|password|пароль)[\\\"']?\\s*[=:]\\s*[\\\"']?[^\\s\\\"'&,;]{4,}"),
        Regex("(?i)([?&](?:token|key|secret|auth|code|signature|session)[^=\\s]*=)[^&\\s#]+"),
        Regex("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b"),
        Regex("(?<![\\w])\\+\\d[\\d ()-]{8,}\\d(?![\\w])")
    )

    fun redact(text: String): String = secrets.fold(text) { value, pattern -> pattern.replace(value, "[REDACTED]") }
}
