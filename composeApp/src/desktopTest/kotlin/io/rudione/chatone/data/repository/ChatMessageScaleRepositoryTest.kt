package io.rudione.chatone.data.repository

import com.russhwolf.settings.PreferencesSettings
import io.rudione.chatone.domain.model.ChatMessageScale
import java.util.prefs.Preferences
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatMessageScaleRepositoryTest {

    private val node = Preferences.userRoot().node("chatone-test-chat-scale-${System.nanoTime()}")
    private val settings = PreferencesSettings(node)

    @AfterTest
    fun removeNode() {
        node.removeNode()
    }

    @Test
    fun startsAtDefaultScale() {
        assertEquals(ChatMessageScale.Default, ChatMessageScaleRepository(settings).scale.value)
    }

    @Test
    fun stepsArePersistedAcrossInstances() {
        val repository = ChatMessageScaleRepository(settings)
        repository.enlarge()
        repository.enlarge()
        repository.shrink()

        assertEquals(110, ChatMessageScaleRepository(settings).scale.value.percent)
    }

    @Test
    fun resetReturnsToOneHundredPercent() {
        val repository = ChatMessageScaleRepository(settings)
        repository.set(ChatMessageScale.of(180))
        repository.reset()

        assertEquals(100, ChatMessageScaleRepository(settings).scale.value.percent)
    }

    @Test
    fun corruptedStoredValueIsClamped() {
        settings.putInt("chat_message_scale_percent", 999)

        assertEquals(200, ChatMessageScaleRepository(settings).scale.value.percent)
    }
}
