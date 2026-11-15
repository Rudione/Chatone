package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.chatone_contributor
import io.rudione.chatone.domain.entitlements.ChatoneBadgeDef
import io.rudione.chatone.domain.entitlements.ChatoneUserEntitlement
import org.jetbrains.compose.resources.DrawableResource
import kotlin.time.Clock

internal data class ChatoneBadgeAssignment(val user: ChatoneUserEntitlement, val badge: ChatoneBadgeDef)

internal val LocalChatoneBadges = staticCompositionLocalOf<Map<String, ChatoneBadgeAssignment>> { emptyMap() }

@Composable
internal fun rememberEquippedChatoneBadge(userId: String): ChatoneBadgeDef? {
    val assignments = LocalChatoneBadges.current
    return remember(assignments, userId) {
        assignments[userId]?.takeUnless {
            it.user.expiresAtEpochMs?.let { expiry -> expiry <= Clock.System.now().toEpochMilliseconds() } == true
        }?.badge
    }
}

internal fun ChatoneBadgeDef.bundledDrawable(): DrawableResource? =
    if (id == "contributor") Res.drawable.chatone_contributor else null
