package io.rudione.chatone.presentation.auth

import io.rudione.chatone.data.repository.LoginFailure
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthStepTest {

    @Test
    fun freshScreenStartsAtSignIn() {
        assertEquals(AuthStep.SIGN_IN, AuthState(isCheckingToken = false).currentStep())
        assertEquals(AuthStep.SIGN_IN, AuthState(isPreparing = true).currentStep())
    }

    @Test
    fun waitingForTheLineIsThePasteStep() {
        assertEquals(AuthStep.PASTE, AuthState(awaitingPaste = true).currentStep())
        assertEquals(AuthStep.PASTE, AuthState(awaitingPaste = true, isVerifying = true).currentStep())
        assertEquals(
            AuthStep.PASTE,
            AuthState(awaitingPaste = true, failure = LoginFailure.Malformed).currentStep()
        )
    }

    @Test
    fun rightsWinOverEverythingElse() {
        assertEquals(AuthStep.RIGHTS, AuthState(awaitingRights = true).currentStep())
        assertEquals(AuthStep.RIGHTS, AuthState(failure = LoginFailure.RightsNotGranted).currentStep())
    }

    @Test
    fun brokenLoginUrlStaysOnSignIn() {
        assertEquals(
            AuthStep.SIGN_IN,
            AuthState(awaitingPaste = true, failure = LoginFailure.UnsafeLoginUrl).currentStep()
        )
    }

    @Test
    fun phoneShowsThreeStepsDesktopTwo() {
        assertEquals(AuthStep.entries, authStepSlots(stepByStep = true, current = AuthStep.PASTE))
        assertEquals(
            listOf(AuthStep.SIGN_IN, AuthStep.PASTE),
            authStepSlots(stepByStep = false, current = AuthStep.SIGN_IN)
        )
        assertEquals(
            listOf(AuthStep.SIGN_IN, AuthStep.RIGHTS),
            authStepSlots(stepByStep = false, current = AuthStep.RIGHTS)
        )
    }
}
