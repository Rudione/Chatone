package io.rudione.chatone.domain.moderation

import io.rudione.chatone.domain.model.ModActionButton
import io.rudione.chatone.domain.model.ModSliderSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModSliderTest {

    private val metrics = ModSliderMetrics(
        deadZone = 14f,
        minTravel = 160f,
        backwardReach = 64f,
        hysteresis = 4f,
        overshoot = 40f
    )

    private fun equalWeights(count: Int) = FloatArray(count) { 1f }

    @Test
    fun twitchScaleGoesThroughSecondsMinutesHoursAndDays() {
        val scale = ModSliderDurations.TWITCH_SCALE
        assertEquals(59 + 59 + 23 + 14, scale.size)
        assertEquals(1, scale.first())
        assertEquals(listOf(59, 60), scale.subList(58, 60))
        assertEquals(listOf(59 * 60, 3_600), scale.subList(117, 119))
        assertEquals(listOf(23 * 3_600, 86_400), scale.subList(140, 142))
        assertEquals(14 * 86_400, scale.last())
        assertEquals(ModActionButton.MAX_TIMEOUT_SECONDS, scale.last())
    }

    @Test
    fun durationsAreLabelledInTheirOwnUnit() {
        assertEquals("37s", ModSliderDurations.format(37))
        assertEquals("5m", ModSliderDurations.format(300))
        assertEquals("2h", ModSliderDurations.format(7_200))
        assertEquals("14d", ModSliderDurations.format(14 * 86_400))
        assertEquals("90s", ModSliderDurations.format(90))
    }

    @Test
    fun theSliderIsOnWithTheTwitchScaleByDefault() {
        val settings = ModSliderSettings()
        assertTrue(settings.enabled)
        assertEquals(ModSliderDurations.TWITCH_SCALE, settings.durations(emptyList(), defaultTimeoutSeconds = 600))
    }

    @Test
    fun ownStepsStartFromTheTimeoutButtons() {
        val buttons = listOf(
            ModActionButton.DEFAULT_DELETE,
            ModActionButton.DEFAULT_TIMEOUT,
            ModActionButton.DEFAULT_BAN,
            ModActionButton("custom_1", 60),
            ModActionButton("custom_2", 86_400, enabled = false),
            ModActionButton("custom_3", 3_600)
        )
        val settings = ModSliderSettings(twitchScale = false)
        assertEquals(listOf(60, 300, 3_600), settings.durations(buttons, defaultTimeoutSeconds = 300))
    }

    @Test
    fun ownStepsAreSortedAndDeduplicated() {
        val settings = ModSliderSettings(twitchScale = false).withDurations(listOf(600, 0, 60, 600, 2_000_000))
        assertEquals(listOf(60, 600), settings.durations(emptyList(), defaultTimeoutSeconds = 300))
        assertTrue(settings.hasCustomDurations)
    }

    @Test
    fun travelPercentIsKeptInRange() {
        assertEquals(ModSliderSettings.MAX_TRAVEL_PERCENT, ModSliderSettings(travelPercent = 400).sanitized().travelPercent)
        assertEquals(ModSliderSettings.MIN_TRAVEL_PERCENT, ModSliderSettings(travelPercent = 1).sanitized().travelPercent)
    }

    @Test
    fun shortTracksGiveEveryStopTheSameRoom() {
        val track = ModSliderTrack.build(true, listOf(60, 600), true, canDelete = true, canPunish = true)
        assertEquals(
            listOf(ModSliderAction.Delete, ModSliderAction.Timeout(60), ModSliderAction.Timeout(600), ModSliderAction.Ban),
            track.forward
        )
        assertEquals(ModSliderAction.Unban, track.backward)
        assertTrue(track.weights.all { it == 1f })
        assertEquals(listOf(0, 1, 2, 3), track.milestones)
    }

    @Test
    fun twitchTrackGivesEachUnitTheSameShareAndMarksUnitStarts() {
        val track = ModSliderTrack.build(true, ModSliderDurations.TWITCH_SCALE, true, canDelete = true, canPunish = true)
        assertEquals(157, track.forward.size)
        assertEquals(listOf(0, 1, 60, 119, 142, 156), track.milestones)
        val seconds = track.weights.slice(1..59).sum()
        val days = track.weights.slice(142..155).sum()
        assertEquals(1f, seconds, 0.0001f)
        assertEquals(1f, days, 0.0001f)
    }

    @Test
    fun ownMessagesOnlyAllowDeleting() {
        val track = ModSliderTrack.build(true, listOf(60), true, canDelete = true, canPunish = false)
        assertEquals(listOf<ModSliderAction>(ModSliderAction.Delete), track.forward)
        assertEquals(null, track.backward)
    }

    @Test
    fun untouchableUsersGetNoSlider() {
        assertTrue(ModSliderTrack.build(true, listOf(60), true, canDelete = false, canPunish = false).isEmpty)
    }

    @Test
    fun travelIsAShareOfTheRowWidth() {
        val geometry = ModSliderGeometry.resolve(metrics, travelFraction = 0.6f, availableWidth = 1000f, weights = equalWeights(4), hasBackward = true)
        assertEquals(600f, geometry.travel)
    }

    @Test
    fun narrowRowsStillGetAUsableTravel() {
        val geometry = ModSliderGeometry.resolve(metrics, travelFraction = 0.3f, availableWidth = 300f, weights = equalWeights(4), hasBackward = true)
        assertEquals(160f, geometry.travel)
        val tiny = ModSliderGeometry.resolve(metrics, travelFraction = 0.3f, availableWidth = 100f, weights = equalWeights(4), hasBackward = true)
        assertEquals(95f, tiny.travel)
    }

    @Test
    fun offsetsPickStopsAlongTheTrack() {
        val geometry = ModSliderGeometry(metrics, travel = 214f, weights = equalWeights(4), hasBackward = true)
        assertEquals(ModSliderGeometry.NONE, geometry.forwardIndex(10f, ModSliderGeometry.NONE))
        assertEquals(0, geometry.forwardIndex(20f, ModSliderGeometry.NONE))
        assertEquals(1, geometry.forwardIndex(70f, ModSliderGeometry.NONE))
        assertEquals(3, geometry.forwardIndex(500f, ModSliderGeometry.NONE))
    }

    @Test
    fun boundariesDoNotFlickerThanksToHysteresis() {
        val geometry = ModSliderGeometry(metrics, travel = 214f, weights = equalWeights(4), hasBackward = true)
        assertEquals(0, geometry.forwardIndex(66f, previous = 0))
        assertEquals(1, geometry.forwardIndex(62f, previous = 1))
        assertEquals(0, geometry.forwardIndex(58f, previous = 1))
    }

    @Test
    fun tinyStepsDoNotStickToThePreviousStop() {
        val track = ModSliderTrack.build(false, ModSliderDurations.TWITCH_SCALE, false, canDelete = true, canPunish = true)
        val geometry = ModSliderGeometry(metrics, travel = 600f, weights = track.weights, hasBackward = true)
        val first = geometry.forwardIndex(geometry.threshold(10) + 0.1f, ModSliderGeometry.NONE)
        val next = geometry.forwardIndex(geometry.threshold(12) + 0.1f, first)
        assertEquals(10, first)
        assertEquals(12, next)
    }

    @Test
    fun eachUnitTakesTheSameDistanceOnTheTwitchScale() {
        val track = ModSliderTrack.build(false, ModSliderDurations.TWITCH_SCALE, false, canDelete = true, canPunish = true)
        val geometry = ModSliderGeometry(metrics, travel = 414f, weights = track.weights, hasBackward = true)
        val secondsWidth = geometry.threshold(59) - geometry.threshold(0)
        val minutesWidth = geometry.threshold(118) - geometry.threshold(59)
        assertEquals(secondsWidth, minutesWidth, 0.01f)
    }

    @Test
    fun draggingLeftFarEnoughArmsUnban() {
        val geometry = ModSliderGeometry(metrics, travel = 214f, weights = equalWeights(4), hasBackward = true)
        assertFalse(geometry.backwardArmed(-40f))
        assertTrue(geometry.backwardArmed(-70f))
        assertFalse(ModSliderGeometry(metrics, 214f, equalWeights(4), hasBackward = false).backwardArmed(-200f))
    }

    @Test
    fun pullingPastTheEndResistsInsteadOfFollowing() {
        val geometry = ModSliderGeometry(metrics, travel = 214f, weights = equalWeights(4), hasBackward = false)
        assertEquals(100f, geometry.displayOffset(100f))
        val far = geometry.displayOffset(2_000f)
        assertTrue(far > 214f && far < 254f)
        assertTrue(geometry.displayOffset(-300f) > -54f)
    }
}
