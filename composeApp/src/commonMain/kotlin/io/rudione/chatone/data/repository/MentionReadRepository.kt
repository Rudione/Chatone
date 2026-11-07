package io.rudione.chatone.data.repository

import com.russhwolf.settings.Settings
import io.rudione.chatone.domain.mention.MentionReadMark
import io.rudione.chatone.domain.mention.MentionReadStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.time.Clock

@Serializable
internal data class StoredMentionMark(
    val watermarkMs: Long,
    val readIds: List<String> = emptyList(),
    val updatedAtMs: Long = 0L
)

internal object MentionReadCodec {
    private const val MAX_CHANNELS = 150
    private const val MAX_ID_LENGTH = 64
    private val LOGIN_PATTERN = Regex("^[a-z0-9_]{1,25}$")
    private val ID_PATTERN = Regex("^[A-Za-z0-9_:.-]+$")
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = MapSerializer(String.serializer(), StoredMentionMark.serializer())

    fun channelKey(login: String): String? =
        login.trim().removePrefix("#").lowercase().takeIf { LOGIN_PATTERN.matches(it) }

    fun decode(raw: String?): Map<String, StoredMentionMark> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching { json.decodeFromString(serializer, raw) }
            .getOrDefault(emptyMap())
            .filterKeys { channelKey(it) == it }
    }

    fun encode(marks: Map<String, StoredMentionMark>): String {
        val bounded = marks.entries
            .sortedByDescending { it.value.updatedAtMs }
            .take(MAX_CHANNELS)
            .associate { it.key to it.value }
        return json.encodeToString(serializer, bounded)
    }

    fun toMark(stored: StoredMentionMark): MentionReadMark =
        MentionReadMark(stored.watermarkMs, stored.readIds.filterTo(LinkedHashSet(), ::isValidId))

    fun toStored(mark: MentionReadMark, nowMs: Long): StoredMentionMark = StoredMentionMark(
        watermarkMs = mark.watermarkMs,
        readIds = mark.readIds.filter(::isValidId).take(MentionReadMark.MAX_READ_IDS),
        updatedAtMs = nowMs
    )

    private fun isValidId(id: String): Boolean = id.length <= MAX_ID_LENGTH && ID_PATTERN.matches(id)
}

class MentionReadRepository(
    private val settings: Settings,
    private val clock: Clock = Clock.System
) : MentionReadStore {

    private val marks: MutableMap<String, StoredMentionMark> by lazy {
        MentionReadCodec.decode(settings.getStringOrNull(KEY)).toMutableMap()
    }

    override fun mark(channelLogin: String): MentionReadMark? {
        val key = MentionReadCodec.channelKey(channelLogin) ?: return null
        return marks[key]?.let(MentionReadCodec::toMark)
    }

    override fun save(channelLogin: String, mark: MentionReadMark) {
        val key = MentionReadCodec.channelKey(channelLogin) ?: return
        marks[key] = MentionReadCodec.toStored(mark, clock.now().toEpochMilliseconds())
        settings.putString(KEY, MentionReadCodec.encode(marks))
    }

    private companion object {
        const val KEY = "mention_read_marks_v1"
    }
}
