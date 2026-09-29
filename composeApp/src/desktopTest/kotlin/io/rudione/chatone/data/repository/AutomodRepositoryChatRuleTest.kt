package io.rudione.chatone.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.rudione.chatone.data.local.ChatoneDatabase
import io.rudione.chatone.data.local.SchemaHealer
import io.rudione.chatone.domain.model.AutomodScope
import io.rudione.chatone.domain.model.ChatRule
import io.rudione.chatone.domain.model.ChatRuleAction
import io.rudione.chatone.domain.model.ChatRuleType
import io.rudione.chatone.util.automod.AutomodTarget
import io.rudione.chatone.util.automod.ChatRuleEngine
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AutomodRepositoryChatRuleTest {

    private val viewer = AutomodTarget(userId = "42", username = "viewer")

    private val drivers = mutableListOf<JdbcSqliteDriver>()

    @AfterTest
    fun closeDrivers() {
        drivers.forEach { it.close() }
        ChatRuleEngine.invalidate()
    }

    private fun freshDatabase(): ChatoneDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also(drivers::add)
        ChatoneDatabase.Schema.create(driver)
        SchemaHealer.heal(driver)
        return ChatoneDatabase(driver)
    }

    private fun legacyDatabase(): ChatoneDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also(drivers::add)
        ChatoneDatabase.Schema.create(driver)
        driver.execute(null, "DROP TABLE AutomodRuleEntity", 0)
        driver.execute(null, LEGACY_AUTOMOD_TABLE, 0)
        SchemaHealer.heal(driver)
        return ChatoneDatabase(driver)
    }

    private fun AutomodRepository.editLikeRuleEditor(): ChatRule {
        val fresh = ChatRule(id = "rule-1", type = ChatRuleType.SPAM_RATE, createdAt = 1L)
        upsertChatRule(fresh)
        upsertChatRule(chatRules.value.single().copy(type = ChatRuleType.MESSAGE_LENGTH))
        listOf(2, 20).forEach { typed ->
            upsertChatRule(chatRules.value.single().copy(messageMaxLength = typed))
        }
        return chatRules.value.single()
    }

    private fun AutomodRepository.verdictFor(text: String) = ChatRuleEngine.evaluate(
        text = text,
        tokens = emptyList(),
        target = viewer,
        currentChannelLogin = "channel",
        rules = chatRulesForChannel("channel")
    )

    @Test
    fun lengthRuleSurvivesEditorRoundTrip() {
        val repository = AutomodRepository(freshDatabase())

        val saved = repository.editLikeRuleEditor()

        assertEquals(ChatRuleType.MESSAGE_LENGTH, saved.type)
        assertEquals(20, saved.messageMaxLength)
        assertNull(repository.verdictFor("a".repeat(20)))
        assertEquals(ChatRuleType.MESSAGE_LENGTH, assertNotNull(repository.verdictFor("a".repeat(21))).rule.type)
    }

    @Test
    fun lengthRuleWorksOnHealedLegacySchema() {
        val repository = AutomodRepository(legacyDatabase())

        val saved = repository.editLikeRuleEditor()

        assertEquals(20, saved.messageMaxLength)
        assertNotNull(repository.verdictFor("b".repeat(40)))
    }

    @Test
    fun lengthIsClampedToTwitchLimitOnSave() {
        val repository = AutomodRepository(freshDatabase())

        repository.upsertChatRule(ChatRule(id = "r", type = ChatRuleType.MESSAGE_LENGTH, messageMaxLength = 9_000))

        assertEquals(ChatRule.MAX_MESSAGE_LENGTH, repository.chatRules.value.single().messageMaxLength)
    }

    @Test
    fun localRuleOnlyAppliesToItsChannel() {
        val repository = AutomodRepository(freshDatabase())
        repository.upsertChatRule(
            ChatRule(
                id = "local",
                type = ChatRuleType.MESSAGE_LENGTH,
                scope = AutomodScope.LOCAL,
                channelLogin = "Channel",
                messageMaxLength = 5,
                action = ChatRuleAction.TIMEOUT
            )
        )

        assertEquals(1, repository.chatRulesForChannel("CHANNEL").size)
        assertEquals(0, repository.chatRulesForChannel("other").size)
    }

    @Test
    fun disabledRuleIsNotReturnedForChannel() {
        val repository = AutomodRepository(freshDatabase())
        repository.upsertChatRule(ChatRule(id = "off", type = ChatRuleType.MESSAGE_LENGTH, enabled = false))

        assertEquals(0, repository.chatRulesForChannel("channel").size)
        repository.setChatRuleEnabled("off", true)
        assertEquals(1, repository.chatRulesForChannel("channel").size)
    }

    private companion object {
        val LEGACY_AUTOMOD_TABLE = """
            CREATE TABLE AutomodRuleEntity (
                id TEXT NOT NULL PRIMARY KEY,
                scope TEXT NOT NULL,
                channelLogin TEXT,
                pattern TEXT NOT NULL DEFAULT '',
                alternates TEXT NOT NULL DEFAULT '',
                isRegex INTEGER NOT NULL DEFAULT 0,
                caseSensitive INTEGER NOT NULL DEFAULT 0,
                wholeWord INTEGER NOT NULL DEFAULT 0,
                action TEXT NOT NULL DEFAULT 'DELETE',
                timeoutMs INTEGER NOT NULL DEFAULT 60000,
                frequencyThreshold INTEGER NOT NULL DEFAULT 0,
                frequencyWindowMs INTEGER NOT NULL DEFAULT 60000,
                exemptMods INTEGER NOT NULL DEFAULT 1,
                exemptSubs INTEGER NOT NULL DEFAULT 0,
                exemptVips INTEGER NOT NULL DEFAULT 1,
                enabled INTEGER NOT NULL DEFAULT 1,
                note TEXT NOT NULL DEFAULT '',
                createdAt INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent()
    }
}
