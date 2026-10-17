package io.rudione.chatone.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ModLogSummaryParserTest {

    private fun parse(raw: String) = ModLogSummaryParser.parse(Json.parseToJsonElement(raw).jsonObject, targetId = "42")

    @Test
    fun readsCountsLabelsAndModerator() {
        val summary = parse(
            """
            {
              "messages": {"count": 318},
              "bans": {"count": 1, "edges": [{"node": {"id": "b1", "type": "BAN", "timestamp": "2026-10-01T10:00:00Z",
                "localizedLabel": {"fallbackString": "rudionee banned spammer. Reason: links",
                  "localizedStringFragments": [{"token": {"id": "1", "login": "rudionee"}}, {"token": {"text": " banned "}}, {"token": {"id": "42", "login": "spammer"}}]}}}]},
              "timeouts": {"count": 120, "edges": [{"node": {"id": "t1", "type": "TIMEOUT", "timestamp": "2026-10-02T10:00:00Z",
                "localizedLabel": {"fallbackString": "mod_one timed out spammer for 600 seconds", "localizedStringFragments": []}}}]},
              "unbans": {"count": 0, "edges": []},
              "untimeouts": {"code": "FORBIDDEN"},
              "warnings": {"count": 0, "edges": []}
            }
            """
        )!!
        assertEquals(318, summary.messageCount)
        assertEquals(1, summary.counts[ModLogActionType.BAN])
        assertEquals(120, summary.counts[ModLogActionType.TIMEOUT])
        assertNull(summary.counts[ModLogActionType.UNTIMEOUT])
        assertEquals(listOf("t1", "b1"), summary.actions.map { it.id })
        assertEquals("rudionee", summary.actions.last().moderatorLogin)
        assertEquals("rudionee banned spammer. Reason: links", summary.actions.last().label)
        assertNull(summary.actions.first().moderatorLogin)
    }

    @Test
    fun deniedAccessGivesNothing() {
        assertNull(parse("""{"bans": {"code": "FORBIDDEN"}, "timeouts": {"code": "FORBIDDEN"}}"""))
    }
}
