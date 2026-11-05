package io.rudione.chatone.domain.moderation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AutoModInsightsTest {

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_000 * day

    @Test
    fun riskySignalsComeFirst() {
        val insights = AutoModInsights.of(
            AutoModContext(accountCreatedAtMs = now - 2 * day, followKnown = true, timeouts = 3, bans = 1, messages = 0),
            now
        )
        assertEquals(
            listOf(
                AutoModInsightKind.BANS,
                AutoModInsightKind.TIMEOUTS,
                AutoModInsightKind.NEW_ACCOUNT,
                AutoModInsightKind.NOT_FOLLOWING,
                AutoModInsightKind.FIRST_MESSAGE
            ),
            insights.map { it.kind }
        )
        assertEquals(InsightTone.DANGER, insights.first().tone)
    }

    @Test
    fun regularsLookCalm() {
        val insights = AutoModInsights.of(
            AutoModContext(
                accountCreatedAtMs = now - 900 * day,
                followedAtMs = now - 60 * day,
                followKnown = true,
                subscriptionTier = 1,
                timeouts = 0,
                bans = 0,
                messages = 420
            ),
            now
        )
        assertEquals(
            listOf(AutoModInsightKind.ACCOUNT_AGE, AutoModInsightKind.FOLLOWING, AutoModInsightKind.SUBSCRIBER, AutoModInsightKind.MESSAGES),
            insights.map { it.kind }
        )
        assertTrue(insights.none { it.tone == InsightTone.WARNING || it.tone == InsightTone.DANGER })
        assertEquals(InsightTone.POSITIVE, insights[1].tone)
    }

    @Test
    fun unknownFollowStateIsNotReportedAsMissing() {
        val insights = AutoModInsights.of(AutoModContext(followKnown = false), now)
        assertTrue(insights.isEmpty())
    }

    @Test
    fun tiersAreNormalised() {
        assertEquals(1, AutoModInsights.tierOf("1000"))
        assertEquals(3, AutoModInsights.tierOf("TIER_3"))
        assertEquals(null, AutoModInsights.tierOf(null))
    }
}
