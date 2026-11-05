package io.rudione.chatone.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AutoModProfileParserTest {

    @Test
    fun liveShapeWithoutRelationshipIsParsed() {
        val user = Json.parseToJsonElement(
            """
            {"chatColor":"#D2691E","createdAt":"2017-01-03T13:10:42.500226Z",
             "displayBadges":[{"setID":"pogchamps-6-7","version":"1",
               "imageURL":"https://static-cdn.jtvnw.net/badges/v1/60ca34ba/1","title":"PogChamps 6 7"}],
             "relationship":null}
            """.trimIndent()
        ).jsonObject
        val profile = AutoModProfileParser.parse(user)
        assertEquals("#D2691E", profile.chatColor)
        assertEquals("pogchamps-6-7", profile.badges.single().id)
        assertEquals("https://static-cdn.jtvnw.net/badges/v1/60ca34ba/2", profile.badges.single().imageUrl)
        assertEquals(1483449042500L, profile.createdAtMs)
        assertFalse(profile.followKnown)
        assertNull(profile.followedAtMs)
    }

    @Test
    fun relationshipWithoutFollowMeansNotFollowing() {
        val user = Json.parseToJsonElement(
            """{"chatColor":null,"createdAt":null,"displayBadges":[],"relationship":{"followedAt":null,"subscriptionBenefit":{"tier":"1000"}}}"""
        ).jsonObject
        val profile = AutoModProfileParser.parse(user)
        assertTrue(profile.followKnown)
        assertNull(profile.followedAtMs)
        assertEquals("1000", profile.subscriptionTier)
        assertNull(profile.chatColor)
    }
}
