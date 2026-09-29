package io.rudione.chatone.presentation.settings.components

import io.rudione.chatone.domain.model.MacroStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MacroStepFormTest {

    @Test
    fun everyStepSurvivesTheFormRoundTrip() {
        listOf(
            MacroStep.SendMessage("hello", 3),
            MacroStep.InsertText("draft", 2),
            MacroStep.SubMode(true),
            MacroStep.EmoteMode(false),
            MacroStep.SlowMode(true, 45),
            MacroStep.FollowerMode(true, 30),
            MacroStep.R9KMode(false),
            MacroStep.StartRaid("somechannel"),
            MacroStep.PinMessage("pinned text"),
            MacroStep.Delay(7, 2),
            MacroStep.ClearChat()
        ).forEach { step ->
            assertEquals(step, MacroStepForm.from(step).toStep(MacroStepKind.of(step)))
        }
    }

    @Test
    fun blankInputsProduceNoStep() {
        assertNull(MacroStepForm(text = "   ").toStep(MacroStepKind.SEND))
        assertNull(MacroStepForm(raidTarget = "  ").toStep(MacroStepKind.RAID))
        assertNull(MacroStepForm(pinMessage = "").toStep(MacroStepKind.PIN))
        assertNull(MacroStepForm(delaySeconds = 0).toStep(MacroStepKind.DELAY))
    }

    @Test
    fun raidTargetIsTrimmed() {
        assertEquals(MacroStep.StartRaid("target"), MacroStepForm(raidTarget = "  target ").toStep(MacroStepKind.RAID))
    }
}
