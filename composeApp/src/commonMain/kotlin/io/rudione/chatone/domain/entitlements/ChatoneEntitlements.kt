package io.rudione.chatone.domain.entitlements

import kotlinx.serialization.Serializable

@Serializable
enum class ChatoneRole {
    DEVELOPER,
    SUPPORTER,
    SUBSCRIBER,
    TRANSLATOR,
    EARLY_ADOPTER,
    MODERATOR_TEAM
}

@Serializable
data class ChatoneBadgeDef(
    val id: String,
    val title: String,
    val imageUrl1x: String,
    val imageUrl2x: String = "",
    val colorHex: String? = null,
    val priority: Int = 0
)

@Serializable
data class ChatoneFeatureFlag(
    val id: String,
    val title: String = ""
)

@Serializable
data class ChatoneUserEntitlement(
    val twitchUserId: String,
    val roles: List<ChatoneRole> = emptyList(),
    val badgeIds: List<String> = emptyList(),
    val featureIds: List<String> = emptyList(),
    val nicknameColorHex: String? = null,
    val expiresAtEpochMs: Long? = null
)

@Serializable
data class EntitlementsManifest(
    val version: Int = 1,
    val updatedAtEpochMs: Long = 0,
    val badges: List<ChatoneBadgeDef> = emptyList(),
    val features: List<ChatoneFeatureFlag> = emptyList(),
    val users: List<ChatoneUserEntitlement> = emptyList()
)

data class ResolvedUserPerks(
    val roles: List<ChatoneRole>,
    val badges: List<ChatoneBadgeDef>,
    val features: Set<String>,
    val nicknameColorHex: String?
) {
    val isEmpty: Boolean get() = roles.isEmpty() && badges.isEmpty() && features.isEmpty()

    companion object {
        val EMPTY = ResolvedUserPerks(emptyList(), emptyList(), emptySet(), null)
    }
}
