package io.rudione.chatone.presentation.auth

import io.rudione.chatone.data.repository.LoginFailure

enum class AuthStep { SIGN_IN, PASTE, RIGHTS }

fun AuthState.currentStep(): AuthStep = when {
    awaitingRights || failure == LoginFailure.RightsNotGranted -> AuthStep.RIGHTS
    failure == LoginFailure.UnsafeLoginUrl -> AuthStep.SIGN_IN
    awaitingPaste -> AuthStep.PASTE
    else -> AuthStep.SIGN_IN
}

fun authStepSlots(stepByStep: Boolean, current: AuthStep): List<AuthStep> =
    if (stepByStep) AuthStep.entries
    else listOf(AuthStep.SIGN_IN, if (current == AuthStep.RIGHTS) AuthStep.RIGHTS else AuthStep.PASTE)
