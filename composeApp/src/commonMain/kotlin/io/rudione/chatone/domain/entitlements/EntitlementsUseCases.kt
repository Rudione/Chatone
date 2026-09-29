package io.rudione.chatone.domain.entitlements

import kotlin.time.Clock

class RefreshEntitlementsUseCase(private val repository: EntitlementsRepository) {
    suspend operator fun invoke(): Boolean = repository.refresh()
}

class ResolveUserPerksUseCase(private val repository: EntitlementsRepository) {

    operator fun invoke(twitchUserId: String): ResolvedUserPerks {
        val manifest = repository.manifest.value ?: return ResolvedUserPerks.EMPTY
        val entitlement = repository.entitlementFor(twitchUserId) ?: return ResolvedUserPerks.EMPTY

        val expired = entitlement.expiresAtEpochMs
            ?.let { it < Clock.System.now().toEpochMilliseconds() } == true
        if (expired) return ResolvedUserPerks.EMPTY

        val badgesById = manifest.badges.associateBy { it.id }
        val badges = entitlement.badgeIds
            .mapNotNull { badgesById[it] }
            .sortedByDescending { it.priority }

        return ResolvedUserPerks(
            roles = entitlement.roles,
            badges = badges,
            features = entitlement.featureIds.toSet(),
            nicknameColorHex = entitlement.nicknameColorHex
        )
    }
}

class HasFeatureUseCase(private val resolvePerks: ResolveUserPerksUseCase) {
    operator fun invoke(twitchUserId: String, featureId: String): Boolean =
        featureId in resolvePerks(twitchUserId).features
}
