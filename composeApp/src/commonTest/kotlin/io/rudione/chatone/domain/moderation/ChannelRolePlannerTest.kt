package io.rudione.chatone.domain.moderation

import kotlin.test.Test
import kotlin.test.assertEquals

class ChannelRolePlannerTest {

    private val viewer = UserRoles()
    private val vip = UserRoles(vip = true)
    private val mod = UserRoles(moderator = true)

    @Test
    fun grantingAMissingRoleIsOneStep() {
        assertEquals(listOf(RoleChange(ChannelRole.VIP, true)), ChannelRolePlanner.plan(viewer, ChannelRole.VIP, true))
        assertEquals(listOf(RoleChange(ChannelRole.MODERATOR, true)), ChannelRolePlanner.plan(viewer, ChannelRole.MODERATOR, true))
    }

    @Test
    fun revokingIsOneStep() {
        assertEquals(listOf(RoleChange(ChannelRole.VIP, false)), ChannelRolePlanner.plan(vip, ChannelRole.VIP, false))
        assertEquals(listOf(RoleChange(ChannelRole.MODERATOR, false)), ChannelRolePlanner.plan(mod, ChannelRole.MODERATOR, false))
    }

    @Test
    fun switchingRolesRevokesTheOtherFirst() {
        assertEquals(
            listOf(RoleChange(ChannelRole.VIP, false), RoleChange(ChannelRole.MODERATOR, true)),
            ChannelRolePlanner.plan(vip, ChannelRole.MODERATOR, true)
        )
        assertEquals(
            listOf(RoleChange(ChannelRole.MODERATOR, false), RoleChange(ChannelRole.VIP, true)),
            ChannelRolePlanner.plan(mod, ChannelRole.VIP, true)
        )
    }

    @Test
    fun nothingToDoWhenTheRoleAlreadyMatches() {
        assertEquals(emptyList(), ChannelRolePlanner.plan(vip, ChannelRole.VIP, true))
        assertEquals(emptyList(), ChannelRolePlanner.plan(viewer, ChannelRole.MODERATOR, false))
    }

    @Test
    fun unknownRolesSendTheRequestAsIs() {
        assertEquals(listOf(RoleChange(ChannelRole.VIP, false)), ChannelRolePlanner.plan(null, ChannelRole.VIP, false))
    }

    @Test
    fun rolesAfterChangesAreExclusive() {
        assertEquals(mod, vip.after(ChannelRolePlanner.plan(vip, ChannelRole.MODERATOR, true)))
        assertEquals(viewer, vip.with(ChannelRole.VIP, false))
    }
}
