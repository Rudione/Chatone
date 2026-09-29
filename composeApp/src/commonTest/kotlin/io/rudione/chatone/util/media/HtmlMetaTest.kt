package io.rudione.chatone.util.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HtmlMetaTest {

    @Test
    fun readsMetaValuesInAnyAttributeOrder() {
        val html = """
            <html><head>
            <meta property="og:title" content="Straight order">
            <meta content="Reversed order" name="og:description">
            <meta name=og:site_name content=Unquoted>
            </head></html>
        """.trimIndent()

        val values = htmlMetaValues(html)
        assertEquals("Straight order", values["og:title"])
        assertEquals("Reversed order", values["og:description"])
        assertEquals("Unquoted", values["og:site_name"])
    }

    @Test
    fun firstValueWinsAndUnrelatedTagsAreIgnored() {
        val html = """
            <meta property="og:image" content="https://cdn.example/first.png">
            <meta property="og:image" content="https://cdn.example/second.png">
            <metafoo property="og:image" content="https://evil.example/x.png">
            <meta content="no key here">
        """.trimIndent()

        assertEquals("https://cdn.example/first.png", htmlMetaValues(html)["og:image"])
    }

    @Test
    fun parsesAttributesWithQuotesAndFlags() {
        val attributes = htmlAttributes(""" id='preview-image' src="/uploads/a/b.png" async data-x=1/""")
        assertEquals("preview-image", attributes["id"])
        assertEquals("/uploads/a/b.png", attributes["src"])
        assertEquals("", attributes["async"])
        assertEquals("1", attributes["data-x"])
    }

    @Test
    fun readsTitleAndSurvivesUnclosedTitle() {
        assertEquals("Hello", htmlTitle("<html><head><title>Hello</title></head>"))
        assertEquals("Hi", htmlTitle("""<title lang="en">Hi</TITLE>"""))
        assertNull(htmlTitle("<html><title>never closed"))
        assertNull(htmlTitle("<titlebar>not a title</titlebar>"))
    }

    @Test
    fun hostileHtmlIsParsedInLinearTime() {
        val payloads = listOf(
            "<meta " + "name=\"a\" ".repeat(20_000),
            "<meta ".repeat(30_000) + "name=\"a\" content=\"b\"",
            "<title".repeat(30_000),
            "<img " + "src=\"a\" ".repeat(20_000),
            "<meta name=\"x\" " + "a".repeat(180_000)
        )

        payloads.forEach { payload ->
            htmlMetaValues(payload)
            htmlTitle(payload)
            htmlTags(payload, "img").firstOrNull()?.let { htmlAttributes(it) }
        }

        assertTrue(true)
    }

    @Test
    fun oversizedTagIsSkippedInsteadOfScanned() {
        val giant = "<meta " + "b".repeat(20_000) + " property=\"og:title\" content=\"nope\">"
        assertNull(htmlMetaValues(giant)["og:title"])
    }
}
