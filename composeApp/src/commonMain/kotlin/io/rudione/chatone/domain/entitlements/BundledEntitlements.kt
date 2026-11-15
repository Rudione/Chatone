package io.rudione.chatone.domain.entitlements

internal val BundledEntitlements = EntitlementsManifest(
    badges = listOf(ChatoneBadgeDef(id = "contributor", title = "Chatone Contributor", imageUrl1x = "")),
    users = listOf(
        ChatoneUserEntitlement(
            twitchUserId = "814427398",
            badgeIds = listOf("contributor"),
            equippedBadgeId = "contributor"
        )
    )
)
