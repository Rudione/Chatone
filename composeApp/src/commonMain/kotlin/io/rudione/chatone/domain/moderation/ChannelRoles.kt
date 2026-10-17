package io.rudione.chatone.domain.moderation

enum class ChannelRole {
    MODERATOR,
    VIP;

    val exclusive: ChannelRole
        get() = when (this) {
            MODERATOR -> VIP
            VIP -> MODERATOR
        }
}

data class UserRoles(val moderator: Boolean = false, val vip: Boolean = false) {

    fun has(role: ChannelRole): Boolean = when (role) {
        ChannelRole.MODERATOR -> moderator
        ChannelRole.VIP -> vip
    }

    fun with(role: ChannelRole, granted: Boolean): UserRoles = when {
        !granted && role == ChannelRole.MODERATOR -> copy(moderator = false)
        !granted -> copy(vip = false)
        role == ChannelRole.MODERATOR -> UserRoles(moderator = true, vip = false)
        else -> UserRoles(moderator = false, vip = true)
    }

    fun after(changes: List<RoleChange>): UserRoles = changes.fold(this) { roles, change -> roles.with(change.role, change.granted) }
}

data class RoleChange(val role: ChannelRole, val granted: Boolean)

data class RoleTarget(val userId: String, val login: String)

object ChannelRolePlanner {

    fun plan(current: UserRoles?, role: ChannelRole, granted: Boolean): List<RoleChange> {
        val wanted = RoleChange(role, granted)
        if (current == null) return listOf(wanted)
        if (current.has(role) == granted) return emptyList()
        if (granted && current.has(role.exclusive)) return listOf(RoleChange(role.exclusive, false), wanted)
        return listOf(wanted)
    }
}
