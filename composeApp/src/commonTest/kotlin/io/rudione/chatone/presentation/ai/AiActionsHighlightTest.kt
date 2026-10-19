package io.rudione.chatone.presentation.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AiActionsHighlightTest {

    private val reply = """
        Подсвечу опасные сообщения.
        ```chatone-action
        {"tool":"highlight_messages","criteria":"threats, slurs or scam links","label":"Опасные"}
        ```
    """.trimIndent()

    @Test
    fun highlightBlockIsParsedAndStripped() {
        val proposal = AiActions.parseHighlight(reply)

        assertEquals("threats, slurs or scam links", proposal?.criteria)
        assertEquals("Опасные", proposal?.label)
        assertEquals("Подсвечу опасные сообщения.", AiActions.strip(reply))
        assertNull(AiActions.parse(reply))
    }

    @Test
    fun ruleBlockIsNotAHighlight() {
        val rule = "```chatone-action\n{\"tool\":\"add_automod_rule\",\"type\":\"LINKS\",\"action\":\"DELETE\"}\n```"

        assertNull(AiActions.parseHighlight(rule))
        assertEquals("LINKS", AiActions.parse(rule)?.type)
    }

    @Test
    fun emptyCriteriaIsIgnored() {
        assertNull(AiActions.parseHighlight("```chatone-action\n{\"tool\":\"highlight_messages\",\"criteria\":\" \"}\n```"))
    }
}
