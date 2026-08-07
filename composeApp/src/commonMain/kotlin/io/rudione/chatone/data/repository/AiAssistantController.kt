package io.rudione.chatone.data.repository

import com.russhwolf.settings.Settings
import io.rudione.chatone.data.remote.AiAssistantClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class AiChatLine(
    val channel: String,
    val author: String,
    val text: String,
    val timestamp: Long,
    val authorLogin: String = ""
)

data class AiChatSnapshot(
    val activeChannel: String = "",
    val openChannels: List<String> = emptyList(),
    val recentMessages: List<AiChatLine> = emptyList(),
    val mentions: List<AiChatLine> = emptyList()
)

@Serializable
data class AiPersistMessage(val role: String, val content: String)

@Serializable
data class AiThread(
    val id: String,
    val title: String,
    val createdAt: Long,
    val messages: List<AiPersistMessage> = emptyList()
)

class AiAssistantController(private val settings: Settings) {

    data class Config(
        val baseUrl: String = AiAssistantClient.DEFAULT_BASE_URL,
        val model: String = AiAssistantClient.DEFAULT_MODEL,
        val apiKey: String = "",
        val temperature: Double = 0.7
    )

    data class AutoModConfig(
        val enabled: Boolean = false,
        val highlightOnly: Boolean = true,
        val sensitivity: Int = 2,
        val activeHours: Set<Int> = emptySet(),
        val extraInstructions: String = ""
    )

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _isOpen = MutableStateFlow(false)
    val isOpen: StateFlow<Boolean> = _isOpen.asStateFlow()
    fun open() { _isOpen.value = true }
    fun close() { _isOpen.value = false }
    fun toggle() { _isOpen.value = !_isOpen.value }

    private val _snapshot = MutableStateFlow(AiChatSnapshot())
    val snapshot: StateFlow<AiChatSnapshot> = _snapshot.asStateFlow()
    fun publishSnapshot(snapshot: AiChatSnapshot) { _snapshot.value = snapshot }

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<Config> = _config.asStateFlow()

    fun updateConfig(update: (Config) -> Config) {
        val c = update(_config.value)
        settings.putString(KEY_BASE_URL, c.baseUrl)
        settings.putString(KEY_MODEL, c.model)
        settings.putString(KEY_API_KEY, c.apiKey)
        settings.putDouble(KEY_TEMP, c.temperature)
        _config.value = c
    }

    private val _enabled = MutableStateFlow(settings.getBoolean(KEY_ENABLED, true))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()
    fun setEnabled(value: Boolean) {
        settings.putBoolean(KEY_ENABLED, value)
        _enabled.value = value
    }

    fun onboardingSeen(): Boolean = settings.getBoolean(KEY_ONBOARDING_SEEN, false)
    fun markOnboardingSeen() { settings.putBoolean(KEY_ONBOARDING_SEEN, true) }

    private val _autoMod = MutableStateFlow(loadAutoMod())
    val autoMod: StateFlow<AutoModConfig> = _autoMod.asStateFlow()

    fun updateAutoMod(update: (AutoModConfig) -> AutoModConfig) {
        val a = update(_autoMod.value)
        settings.putBoolean(KEY_AM_ENABLED, a.enabled)
        settings.putBoolean(KEY_AM_HIGHLIGHT, a.highlightOnly)
        settings.putInt(KEY_AM_SENS, a.sensitivity)
        settings.putString(KEY_AM_HOURS, a.activeHours.sorted().joinToString(","))
        settings.putString(KEY_AM_EXTRA, a.extraInstructions)
        _autoMod.value = a
    }

    fun autoModActiveAt(hour: Int): Boolean {
        val a = _autoMod.value
        return a.enabled && (a.activeHours.isEmpty() || hour in a.activeHours)
    }

    fun loadThreads(): List<AiThread> {
        val raw = settings.getStringOrNull(KEY_THREADS)?.takeIf { it.isNotBlank() } ?: return emptyList()
        return runCatching { json.decodeFromString<List<AiThread>>(raw) }.getOrDefault(emptyList())
    }

    fun saveThreads(threads: List<AiThread>) {
        settings.putString(KEY_THREADS, json.encodeToString(threads.take(MAX_THREADS)))
    }

    private fun loadConfig(): Config {
        var baseUrl = settings.getStringOrNull(KEY_BASE_URL) ?: AiAssistantClient.DEFAULT_BASE_URL
        var model = settings.getStringOrNull(KEY_MODEL) ?: AiAssistantClient.DEFAULT_MODEL

        val legacyG4f = baseUrl == "http://localhost:1337/v1" && model == "gpt-4o-mini"
        val legacyPollinations = baseUrl.contains("pollinations", ignoreCase = true)
        if (legacyG4f || legacyPollinations) {
            baseUrl = AiAssistantClient.DEFAULT_BASE_URL
            model = AiAssistantClient.DEFAULT_MODEL
            settings.putString(KEY_BASE_URL, baseUrl)
            settings.putString(KEY_MODEL, model)
        }
        return Config(
            baseUrl = baseUrl,
            model = model,
            apiKey = settings.getStringOrNull(KEY_API_KEY).orEmpty(),
            temperature = settings.getDouble(KEY_TEMP, 0.7)
        )
    }

    private fun loadAutoMod() = AutoModConfig(
        enabled = settings.getBoolean(KEY_AM_ENABLED, false),
        highlightOnly = settings.getBoolean(KEY_AM_HIGHLIGHT, true),
        sensitivity = settings.getInt(KEY_AM_SENS, 2),
        activeHours = settings.getStringOrNull(KEY_AM_HOURS)?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }?.toSet().orEmpty(),
        extraInstructions = settings.getStringOrNull(KEY_AM_EXTRA).orEmpty()
    )

    companion object {
        private const val KEY_BASE_URL = "ai_base_url"
        private const val KEY_MODEL = "ai_model"
        private const val KEY_API_KEY = "ai_api_key"
        private const val KEY_TEMP = "ai_temperature"
        private const val KEY_ENABLED = "ai_enabled"
        private const val KEY_ONBOARDING_SEEN = "ai_onboarding_seen"
        private const val KEY_THREADS = "ai_threads_json"
        private const val KEY_AM_ENABLED = "ai_automod_enabled"
        private const val KEY_AM_HIGHLIGHT = "ai_automod_highlight_only"
        private const val KEY_AM_SENS = "ai_automod_sensitivity"
        private const val KEY_AM_HOURS = "ai_automod_hours"
        private const val KEY_AM_EXTRA = "ai_automod_extra"
        private const val MAX_THREADS = 40
    }
}
