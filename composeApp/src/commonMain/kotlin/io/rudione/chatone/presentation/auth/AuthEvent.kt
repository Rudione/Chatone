package io.rudione.chatone.presentation.auth

import io.rudione.chatone.base.UiEvent

sealed class AuthEvent : UiEvent {
    data object OnStartLogin : AuthEvent()
    data object OnReopenBrowser : AuthEvent()
    data object OnOpenMirror : AuthEvent()
    data class OnPastePayload(val payload: String, val auto: Boolean = false) : AuthEvent()
    data object OnRetryPayload : AuthEvent()
    data object OnGuestClicked : AuthEvent()
    data object OnCancel : AuthEvent()
    data object OnDismissFailure : AuthEvent()
    data object OnContinueWithoutRights : AuthEvent()
    data object OnRetryRights : AuthEvent()
    data object OnOpenActivation : AuthEvent()
}
