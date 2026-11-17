package io.rudione.chatone.domain.emote

import io.rudione.chatone.domain.model.EmoteProvider
import io.rudione.chatone.domain.model.GenericEmote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SevenTvSetEditorTest {

    private fun emote(id: String, code: String) = GenericEmote(id, code, "u1", "u2", "u3", EmoteProvider.SEVEN_TV)

    private val base = listOf(emote("a", "KEKW"), emote("b", "PogU"), emote("c", "catJAM"))

    @Test
    fun noEditsKeepTheSameList() {
        val result = SevenTvSetEditor.apply(base, SevenTvSetEdits())
        assertSame(base, result.emotes)
        assertTrue(result.touchedCodes.isEmpty())
    }

    @Test
    fun addAppendsAndReplacesTheSameId() {
        val result = SevenTvSetEditor.apply(base, SevenTvSetEdits(added = listOf(emote("d", "Wave"), emote("b", "PogU"))))
        assertEquals(listOf("KEKW", "catJAM", "Wave", "PogU"), result.emotes.map { it.code })
        assertEquals(setOf("Wave", "PogU"), result.touchedCodes)
    }

    @Test
    fun removalMatchesIdOrName() {
        val edits = SevenTvSetEdits(
            removed = listOf(SevenTvRemovedEmote("a", "KEKW"), SevenTvRemovedEmote("zz", "catJAM"))
        )
        val result = SevenTvSetEditor.apply(base, edits)
        assertEquals(listOf("PogU"), result.emotes.map { it.code })
        assertEquals(setOf("KEKW", "catJAM"), result.touchedCodes)
    }

    @Test
    fun renameTouchesBothNames() {
        val result = SevenTvSetEditor.apply(base, SevenTvSetEdits(renamed = listOf(SevenTvRenamedEmote("b", "PogChamp"))))
        assertEquals(listOf("KEKW", "PogChamp", "catJAM"), result.emotes.map { it.code })
        assertEquals(setOf("PogU", "PogChamp"), result.touchedCodes)
    }

    @Test
    fun replacingAnEmoteUnderTheSameNameKeepsTheNewOne() {
        val edits = SevenTvSetEdits(
            removed = listOf(SevenTvRemovedEmote("b", "PogU")),
            added = listOf(emote("b2", "PogU"))
        )
        val result = SevenTvSetEditor.apply(base, edits)
        assertEquals(listOf("a", "c", "b2"), result.emotes.map { it.id })
        assertEquals(setOf("PogU"), result.touchedCodes)
    }

    @Test
    fun batchMatchesApplyingEditsOneByOne() {
        val edits = SevenTvSetEdits(
            removed = listOf(SevenTvRemovedEmote("a", "KEKW")),
            renamed = listOf(SevenTvRenamedEmote("c", "jam")),
            added = listOf(emote("d", "Wave"), emote("a", "KEKW"))
        )
        val oneByOne = listOf(
            SevenTvSetEdits(removed = edits.removed),
            SevenTvSetEdits(renamed = edits.renamed),
            SevenTvSetEdits(added = listOf(edits.added[0])),
            SevenTvSetEdits(added = listOf(edits.added[1]))
        ).fold(base) { acc, step -> SevenTvSetEditor.apply(acc, step).emotes }
        assertEquals(oneByOne, SevenTvSetEditor.apply(base, edits).emotes)
    }

    @Test
    fun largeSetsStayLinear() {
        val big = (0 until 1_000).map { emote("id$it", "code$it") }
        val edits = SevenTvSetEdits(
            removed = (0 until 500).map { SevenTvRemovedEmote("id$it", "code$it") },
            added = (1_000 until 1_500).map { emote("id$it", "code$it") }
        )
        val result = SevenTvSetEditor.apply(big, edits)
        assertEquals(1_000, result.emotes.size)
        assertEquals("code500", result.emotes.first().code)
        assertEquals("code1499", result.emotes.last().code)
    }
}
