package io.rudione.chatone.data.remote

import io.rudione.chatone.domain.model.IrcEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CommunityPointsParserTest {

    @Test
    fun claimAvailableCarriesChannelAndPoints() {
        val event = CommunityPointsParser.parse(
            """{"type":"claim-available","data":{"timestamp":"2026-11-15T10:00:00Z","claim":{"id":"c1","user_id":"7","channel_id":"42","point_gain":{"total_points":50,"reason_code":"CLAIM"}}}}"""
        )
        assertEquals(IrcEvent.PointsClaimAvailable("42", "c1", 50), event)
    }

    @Test
    fun claimAvailableWithoutGainDefaultsToZero() {
        val event = CommunityPointsParser.parse(
            """{"type":"claim-available","data":{"claim":{"id":"c1","channel_id":"42"}}}"""
        )
        assertEquals(IrcEvent.PointsClaimAvailable("42", "c1", 0), event)
    }

    @Test
    fun pointsEarnedReadsGainReasonAndBalance() {
        val event = CommunityPointsParser.parse(
            """{"type":"points-earned","data":{"timestamp":"t","channel_id":"42","point_gain":{"user_id":"7","channel_id":"42","total_points":10,"baseline_points":10,"reason_code":"WATCH","multipliers":[]},"balance":{"user_id":"7","channel_id":"42","balance":1290}}}"""
        )
        assertEquals(IrcEvent.PointsEarned("42", 10, "WATCH", 1290L), event)
    }

    @Test
    fun pointsEarnedFallsBackToBalanceChannel() {
        val event = CommunityPointsParser.parse(
            """{"type":"points-earned","data":{"point_gain":{"total_points":50,"reason_code":"CLAIM"},"balance":{"channel_id":"42","balance":"1340"}}}"""
        )
        assertEquals(IrcEvent.PointsEarned("42", 50, "CLAIM", 1340L), event)
    }

    @Test
    fun claimClaimedAndPointsSpent() {
        assertEquals(
            IrcEvent.PointsClaimed("42", "c1"),
            CommunityPointsParser.parse("""{"type":"claim-claimed","data":{"claim":{"id":"c1","channel_id":"42"}}}""")
        )
        assertEquals(
            IrcEvent.PointsBalanceChanged("42", 800L),
            CommunityPointsParser.parse("""{"type":"points-spent","data":{"balance":{"channel_id":"42","balance":800}}}""")
        )
    }

    @Test
    fun ignoresUnknownAndBrokenMessages() {
        assertNull(CommunityPointsParser.parse("""{"type":"reward-redeemed","data":{}}"""))
        assertNull(CommunityPointsParser.parse("""{"type":"points-earned","data":{"channel_id":"42"}}"""))
        assertNull(CommunityPointsParser.parse("not json"))
        assertNull(CommunityPointsParser.parse("""["array"]"""))
    }
}
