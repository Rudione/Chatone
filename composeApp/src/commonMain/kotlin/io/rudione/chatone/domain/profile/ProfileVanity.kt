package io.rudione.chatone.domain.profile

import androidx.compose.runtime.Immutable
import io.rudione.chatone.domain.model.SevenTvCosmetics

@Immutable
data class VanityBadge(val setId: String, val version: String, val title: String, val imageUrl: String) {
    val key: String get() = "$setId/$version"
}

@Immutable
data class StreamCategory(val id: String, val name: String, val artUrl: String)

@Immutable
data class StreamSettings(val title: String, val category: StreamCategory?, val tags: List<String>, val language: String)

@Immutable
data class SocialLink(val id: String?, val title: String, val url: String)

@Immutable
data class TwitchVanity(
    val userId: String,
    val login: String,
    val displayName: String,
    val bio: String,
    val chatColorHex: String?,
    val accentHex: String? = null,
    val links: List<SocialLink> = emptyList(),
    val stream: StreamSettings? = null,
    val globalBadges: List<VanityBadge>,
    val selectedGlobal: String?,
    val channelId: String?,
    val channelName: String?,
    val channelBadges: List<VanityBadge>,
    val selectedChannel: String?
)

@Immutable
data class SevenTvVanity(
    val userId: String,
    val displayName: String,
    val paints: List<SevenTvCosmetics.Paint>,
    val badges: List<SevenTvCosmetics.Badge>,
    val activePaintId: String?,
    val activeBadgeId: String?
)

sealed interface SevenTvLink {
    data object NotConnected : SevenTvLink
    data class Connected(val vanity: SevenTvVanity) : SevenTvLink
    data object Rejected : SevenTvLink
    data object OtherAccount : SevenTvLink
    data object Unreachable : SevenTvLink
}

@Immutable
data class VanityDraft(
    val colorHex: String?,
    val globalBadge: String?,
    val channelBadge: String?,
    val paintId: String?,
    val sevenTvBadgeId: String?,
    val displayName: String,
    val bio: String,
    val accentHex: String? = null,
    val links: List<SocialLink> = emptyList(),
    val stream: StreamSettings? = null
)

enum class VanityPart { COLOR, BADGE, SEVEN_TV, PROFILE, ACCENT, LINKS, STREAM }

enum class VanityField { NAME_COLOR, GLOBAL_BADGE, CHANNEL_BADGE, PAINT, SEVEN_TV_BADGE, DISPLAY_NAME, BIO, ACCENT, LINK_ADDED, LINK_REMOVED, STREAM_TITLE, CATEGORY, TAGS, LANGUAGE }

@Immutable
data class VanityChange(val field: VanityField, val before: String?, val after: String?)

object VanityChanges {
    fun of(twitch: TwitchVanity, sevenTv: SevenTvVanity?, draft: VanityDraft): List<VanityChange> = buildList {
        fun add(field: VanityField, before: String?, after: String?) {
            if (before.orEmpty() != after.orEmpty()) add(VanityChange(field, before?.takeIf { it.isNotEmpty() }, after?.takeIf { it.isNotEmpty() }))
        }
        add(VanityField.NAME_COLOR, twitch.chatColorHex?.let { "#$it" }, draft.colorHex?.let { "#$it" })
        add(VanityField.GLOBAL_BADGE, badgeTitle(twitch.globalBadges, twitch.selectedGlobal), badgeTitle(twitch.globalBadges, draft.globalBadge))
        add(VanityField.CHANNEL_BADGE, badgeTitle(twitch.channelBadges, twitch.selectedChannel), badgeTitle(twitch.channelBadges, draft.channelBadge))
        if (sevenTv != null) {
            add(VanityField.PAINT, sevenTv.paints.firstOrNull { it.id == sevenTv.activePaintId }?.name, sevenTv.paints.firstOrNull { it.id == draft.paintId }?.name)
            add(VanityField.SEVEN_TV_BADGE, sevenTv.badges.firstOrNull { it.id == sevenTv.activeBadgeId }?.name, sevenTv.badges.firstOrNull { it.id == draft.sevenTvBadgeId }?.name)
        }
        add(VanityField.DISPLAY_NAME, twitch.displayName, draft.displayName.trim())
        add(VanityField.BIO, twitch.bio.trim(), draft.bio.trim())
        add(VanityField.ACCENT, twitch.accentHex?.let { "#$it" }, draft.accentHex?.let { "#$it" })
        val beforeLinks = twitch.links.map { it.url }.toSet()
        val afterLinks = draft.links.map { it.url }.toSet()
        draft.links.filter { it.url !in beforeLinks }.forEach { add(VanityChange(VanityField.LINK_ADDED, null, it.title.ifBlank { it.url })) }
        twitch.links.filter { it.url !in afterLinks }.forEach { add(VanityChange(VanityField.LINK_REMOVED, it.title.ifBlank { it.url }, null)) }
        val stream = twitch.stream
        val next = draft.stream
        if (stream != null && next != null) {
            add(VanityField.STREAM_TITLE, stream.title.trim(), next.title.trim())
            add(VanityField.CATEGORY, stream.category?.name, next.category?.name)
            add(VanityField.TAGS, stream.tags.joinToString(", "), next.tags.joinToString(", "))
            add(VanityField.LANGUAGE, stream.language.uppercase(), next.language.uppercase())
        }
    }

    fun parts(changes: List<VanityChange>): Set<VanityPart> = changes.mapTo(mutableSetOf()) { change ->
        when (change.field) {
            VanityField.NAME_COLOR -> VanityPart.COLOR
            VanityField.GLOBAL_BADGE, VanityField.CHANNEL_BADGE -> VanityPart.BADGE
            VanityField.PAINT, VanityField.SEVEN_TV_BADGE -> VanityPart.SEVEN_TV
            VanityField.DISPLAY_NAME, VanityField.BIO -> VanityPart.PROFILE
            VanityField.ACCENT -> VanityPart.ACCENT
            VanityField.LINK_ADDED, VanityField.LINK_REMOVED -> VanityPart.LINKS
            VanityField.STREAM_TITLE, VanityField.CATEGORY, VanityField.TAGS, VanityField.LANGUAGE -> VanityPart.STREAM
        }
    }

    private fun badgeTitle(badges: List<VanityBadge>, key: String?): String? = key?.let { k -> badges.firstOrNull { it.key == k }?.title ?: k }
}

@Immutable
data class VanitySaveResult(val saved: Set<VanityPart>, val failed: Set<VanityPart>, val blocked: Set<VanityPart>) {
    val isClean: Boolean get() = failed.isEmpty() && blocked.isEmpty()
}

object ChatColorPresets {
    val all: List<Pair<String, String>> = listOf(
        "blue" to "0000FF",
        "blue_violet" to "8A2BE2",
        "cadet_blue" to "5F9EA0",
        "chocolate" to "D2691E",
        "coral" to "FF7F50",
        "dodger_blue" to "1E90FF",
        "firebrick" to "B22222",
        "golden_rod" to "DAA520",
        "green" to "008000",
        "hot_pink" to "FF69B4",
        "orange_red" to "FF4500",
        "red" to "FF0000",
        "sea_green" to "2E8B57",
        "spring_green" to "00FF7F",
        "yellow_green" to "9ACD32"
    )

    private val HEX = Regex("^#?[0-9A-Fa-f]{6}$")

    fun normalize(raw: String?): String? = raw?.trim()?.takeIf { HEX.matches(it) }?.removePrefix("#")?.uppercase()

    fun presetName(hex: String): String? = all.firstOrNull { it.second.equals(hex, ignoreCase = true) }?.first
}

interface ProfileVanityGateway {
    suspend fun loadTwitch(channelLogin: String?): TwitchVanity?
    suspend fun loadSevenTv(): SevenTvLink
    suspend fun connectSevenTv(token: String): SevenTvLink
    fun disconnectSevenTv()
    suspend fun save(twitch: TwitchVanity, sevenTv: SevenTvVanity?, draft: VanityDraft): VanitySaveResult
    suspend fun searchCategories(query: String): List<StreamCategory>
}
