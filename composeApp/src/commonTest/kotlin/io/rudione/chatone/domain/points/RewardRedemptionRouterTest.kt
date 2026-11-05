package io.rudione.chatone.domain.points

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RewardRedemptionRouterTest {

    private fun redemption(id: String, input: String? = null, title: String = "Hydrate") = RewardRedemption(
        id = id,
        channelId = "42",
        rewardId = "rw-1",
        rewardTitle = title,
        cost = 500,
        userId = "77",
        login = "viewer",
        displayName = "Viewer",
        userInput = input,
        redeemedAtMs = 0L
    )

    @Test
    fun rewardsWithoutTextAreShownOnce() {
        val router = RewardRedemptionRouter()
        assertIs<RedemptionRoute.Show>(router.route(redemption("a")))
        assertEquals(RedemptionRoute.Skip, router.route(redemption("a")))
    }

    @Test
    fun rewardsWithTextOnlyTeachTheTitle() {
        val router = RewardRedemptionRouter()
        assertEquals(RedemptionRoute.TitleOnly("rw-1", "Hydrate"), router.route(redemption("b", input = "hi")))
        assertEquals(RedemptionRoute.Skip, router.route(redemption("c", input = "hi", title = "")))
    }

    @Test
    fun memoryIsBounded() {
        val router = RewardRedemptionRouter(memory = 2)
        router.route(redemption("1"))
        router.route(redemption("2"))
        router.route(redemption("3"))
        assertIs<RedemptionRoute.Show>(router.route(redemption("1")))
        assertEquals(RedemptionRoute.Skip, router.route(redemption("3")))
    }

    @Test
    fun blankIdsAreSkipped() {
        assertEquals(RedemptionRoute.Skip, RewardRedemptionRouter().route(redemption("")))
    }
}
