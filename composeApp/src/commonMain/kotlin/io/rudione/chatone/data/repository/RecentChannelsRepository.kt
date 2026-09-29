package io.rudione.chatone.data.repository

import com.russhwolf.settings.Settings
import io.rudione.chatone.domain.model.RecentChannel
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class RecentChannelsRepository(
    private val settings: Settings,
    private val clock: Clock = Clock.System
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(RecentChannel.serializer())

    fun load(): List<RecentChannel> {
        val raw = settings.getStringOrNull(KEY_RECENT) ?: return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }
            .getOrDefault(emptyList())
            .mapNotNull { it.sanitized() }
            .distinctBy { it.login }
            .take(MAX_RECENT)
    }

    fun remember(login: String, displayName: String, avatarUrl: String): List<RecentChannel> {
        val entry = RecentChannel(
            login = login,
            displayName = displayName,
            avatarUrl = avatarUrl,
            openedAtMs = clock.now().toEpochMilliseconds()
        ).sanitized() ?: return load()
        val current = load()
        val previous = current.firstOrNull { it.login == entry.login }
        val merged = entry.copy(
            displayName = entry.displayName.takeUnless { it == entry.login } ?: previous?.displayName ?: entry.login,
            avatarUrl = entry.avatarUrl.ifEmpty { previous?.avatarUrl.orEmpty() }
        )
        return save(listOf(merged) + current.filter { it.login != merged.login })
    }

    fun forget(login: String): List<RecentChannel> {
        val key = login.normalizedLogin() ?: return load()
        return save(load().filter { it.login != key })
    }

    private fun save(channels: List<RecentChannel>): List<RecentChannel> {
        val trimmed = channels.take(MAX_RECENT)
        settings.putString(KEY_RECENT, json.encodeToString(serializer, trimmed))
        return trimmed
    }

    private fun RecentChannel.sanitized(): RecentChannel? {
        val key = login.normalizedLogin() ?: return null
        return copy(
            login = key,
            displayName = displayName.trim().take(MAX_NAME_LENGTH).ifEmpty { key },
            avatarUrl = avatarUrl.takeIf { it.startsWith("https://") }.orEmpty()
        )
    }

    private fun String.normalizedLogin(): String? =
        trim().removePrefix("#").lowercase().takeIf { LOGIN_PATTERN.matches(it) }

    companion object {
        const val MAX_RECENT = 10
        private const val MAX_NAME_LENGTH = 40
        private const val KEY_RECENT = "recent_channels_v1"
        private val LOGIN_PATTERN = Regex("^[a-z0-9_]{1,25}$")
    }
}
