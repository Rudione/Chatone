package io.rudione.chatone.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.rudione.chatone.domain.model.IrcEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RecentMessagesClientTest {

    private val lines = listOf(
        "@id=a1;tmi-sent-ts=1000;user-id=5;display-name=Alice;rm-deleted=1;historical=1 :alice!alice@alice.tmi.twitch.tv PRIVMSG #chan :bad words",
        "@id=a2;tmi-sent-ts=2000;user-id=6;display-name=Bob;historical=1 :bob!bob@bob.tmi.twitch.tv PRIVMSG #chan :hi",
        "@ban-duration=600;target-user-id=5;tmi-sent-ts=3000;historical=1 :tmi.twitch.tv CLEARCHAT #chan :alice",
        "@target-user-id=9;tmi-sent-ts=4000;historical=1 :tmi.twitch.tv CLEARCHAT #chan :carl",
        "@login=bob;target-msg-id=a2;tmi-sent-ts=5000;historical=1 :tmi.twitch.tv CLEARMSG #chan :hi",
        "@id=n1;msg-id=resub;system-msg=Dan\\ssubscribed;tmi-sent-ts=6000;user-id=7;display-name=Dan;historical=1 :tmi.twitch.tv USERNOTICE #chan :still here",
        "@tmi-sent-ts=7000 :tmi.twitch.tv ROOMSTATE #chan"
    )

    private fun client(): RecentMessagesClient {
        val body = buildJsonObject {
            put("messages", buildJsonArray { lines.forEach { add(it) } })
        }.toString()
        val engine = MockEngine { respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json")) }
        return RecentMessagesClient(HttpClient(engine) {
            install(HttpTimeout)
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        })
    }

    @Test
    fun historyKeepsDeletionsAndModerationEvents() = runTest {
        val result = withContext(Dispatchers.Default) { client().getRecentMessages("chan") }
        assertIs<RecentMessagesResult.Success>(result)
        assertEquals(listOf("a1", "a2"), result.messages.map { it.id })
        assertEquals(setOf("a1", "a2"), result.deletedMessageIds)
        assertEquals(listOf(3000L, 4000L, 5000L, 6000L), result.events.map { it.timestamp })

        val timeout = assertIs<IrcEvent.ClearChat>(result.events[0].event)
        assertEquals("alice", timeout.targetUser)
        assertEquals(600, timeout.banDuration)

        val ban = assertIs<IrcEvent.ClearChat>(result.events[1].event)
        assertEquals("carl", ban.targetUser)
        assertEquals(null, ban.banDuration)

        assertIs<IrcEvent.ClearMsg>(result.events[2].event)

        val notice = assertIs<IrcEvent.UserNotice>(result.events[3].event)
        assertEquals("resub", notice.msgId)
        assertEquals("still here", notice.message?.message)
    }
}
