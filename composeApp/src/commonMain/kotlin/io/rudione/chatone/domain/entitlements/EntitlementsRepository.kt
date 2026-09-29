package io.rudione.chatone.domain.entitlements

import kotlinx.coroutines.flow.StateFlow

interface EntitlementsRepository {

    val manifest: StateFlow<EntitlementsManifest?>

    suspend fun refresh(): Boolean

    fun entitlementFor(twitchUserId: String): ChatoneUserEntitlement?
}
