package io.rudione.chatone.presentation.profile.editor

import io.rudione.chatone.domain.profile.StreamCategory
import io.rudione.chatone.base.UiEvent

sealed interface ProfileEditorEvent : UiEvent {
    data object Retry : ProfileEditorEvent
    data class PickColor(val hex: String) : ProfileEditorEvent
    data class PickGlobalBadge(val key: String?) : ProfileEditorEvent
    data class PickChannelBadge(val key: String) : ProfileEditorEvent
    data class PickPaint(val id: String?) : ProfileEditorEvent
    data class PickSevenTvBadge(val id: String?) : ProfileEditorEvent
    data class EditDisplayName(val value: String) : ProfileEditorEvent
    data class EditBio(val value: String) : ProfileEditorEvent
    data class ConnectSevenTv(val token: String) : ProfileEditorEvent
    data object DisconnectSevenTv : ProfileEditorEvent
    data class PickAccent(val hex: String) : ProfileEditorEvent
    data class AddLink(val title: String, val url: String) : ProfileEditorEvent
    data class RemoveLink(val url: String) : ProfileEditorEvent
    data class EditStreamTitle(val value: String) : ProfileEditorEvent
    data class SearchCategory(val query: String) : ProfileEditorEvent
    data class PickCategory(val category: StreamCategory) : ProfileEditorEvent
    data class AddTag(val tag: String) : ProfileEditorEvent
    data class RemoveTag(val tag: String) : ProfileEditorEvent
    data class SetLanguage(val code: String) : ProfileEditorEvent
    data object Discard : ProfileEditorEvent
    data object ReviewChanges : ProfileEditorEvent
    data object CancelReview : ProfileEditorEvent
    data object Save : ProfileEditorEvent
}
