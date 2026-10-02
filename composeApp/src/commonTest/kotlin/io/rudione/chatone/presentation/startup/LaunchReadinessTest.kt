package io.rudione.chatone.presentation.startup

import io.rudione.chatone.presentation.chat.ChatWarmup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LaunchReadinessTest {

    @Test
    fun startsHoldingTheSplash() {
        val readiness = LaunchReadiness()
        assertEquals(LaunchStage.Starting, readiness.stage.value)
        assertFalse(readiness.isReady)
    }

    @Test
    fun stagesOnlyMoveForward() {
        val readiness = LaunchReadiness()
        readiness.advance(LaunchStage.History)
        readiness.advance(LaunchStage.Emotes)
        readiness.advance(LaunchStage.Connecting)
        assertEquals(LaunchStage.History, readiness.stage.value)
    }

    @Test
    fun finishIsFinal() {
        val readiness = LaunchReadiness()
        readiness.finish()
        readiness.advance(LaunchStage.Emotes)
        assertTrue(readiness.isReady)
        assertEquals(LaunchStage.Ready, readiness.stage.value)
    }

    @Test
    fun chatWaitsForAssetsBeforeHistory() {
        assertEquals(LaunchStage.Emotes, ChatWarmup().launchStage())
        assertEquals(LaunchStage.Emotes, ChatWarmup(globalEmotes = true, history = true).launchStage())
        assertEquals(
            LaunchStage.History,
            ChatWarmup(globalEmotes = true, globalBadges = true, channelAssets = true).launchStage()
        )
    }

    @Test
    fun completeWarmupIsReady() {
        assertTrue(ChatWarmup.Complete.isComplete)
        assertEquals(LaunchStage.Ready, ChatWarmup.Complete.launchStage())
    }
}
