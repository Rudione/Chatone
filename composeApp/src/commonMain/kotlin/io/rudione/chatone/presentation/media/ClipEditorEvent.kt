package io.rudione.chatone.presentation.media

import io.rudione.chatone.base.UiEvent

sealed interface ClipEditorEvent : UiEvent {
    data object Retry : ClipEditorEvent
    data object BeginTrim : ClipEditorEvent
    data class SetRange(val start: Float, val end: Float, val previewAt: Float) : ClipEditorEvent
    data object EndTrim : ClipEditorEvent
    data class Seek(val seconds: Float) : ClipEditorEvent
    data class Preset(val seconds: Float) : ClipEditorEvent
    data class EditTitle(val value: String) : ClipEditorEvent
    data object Publish : ClipEditorEvent
}
