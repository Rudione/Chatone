package io.rudione.chatone.presentation.profile.editor

import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import io.rudione.chatone.domain.profile.SocialLink
import io.rudione.chatone.domain.profile.StreamSettings
import io.rudione.chatone.domain.profile.StreamCategory
import io.rudione.chatone.domain.profile.VanityChanges
import io.rudione.chatone.domain.profile.VanityChange
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import io.rudione.chatone.base.BaseViewModel
import io.rudione.chatone.base.UIEffect
import io.rudione.chatone.base.UiEvent
import io.rudione.chatone.base.UiState
import io.rudione.chatone.domain.profile.ChatColorPresets
import io.rudione.chatone.domain.profile.ProfileVanityGateway
import io.rudione.chatone.domain.profile.SevenTvLink
import io.rudione.chatone.domain.profile.SevenTvVanity
import io.rudione.chatone.domain.profile.TwitchVanity
import io.rudione.chatone.domain.profile.VanityDraft
import io.rudione.chatone.domain.profile.VanityPart
import io.rudione.chatone.domain.profile.VanitySaveResult
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

enum class EditorLoad { LOADING, READY, SIGNED_OUT, FAILED }

@Immutable
data class ProfileEditorState(
    val load: EditorLoad = EditorLoad.LOADING,
    val twitch: TwitchVanity? = null,
    val sevenTv: SevenTvLink = SevenTvLink.NotConnected,
    val sevenTvBusy: Boolean = false,
    val draft: VanityDraft? = null,
    val saving: Boolean = false,
    val result: VanitySaveResult? = null,
    val pendingChanges: List<VanityChange>? = null,
    val categoryResults: List<StreamCategory> = emptyList()
) : UiState {
    val sevenTvVanity: SevenTvVanity? get() = (sevenTv as? SevenTvLink.Connected)?.vanity

    val dirty: Boolean
        get() {
            val base = twitch ?: return false
            val current = draft ?: return false
            return current != draftOf(base, sevenTvVanity)
        }
}

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

sealed interface ProfileEditorEffect : UIEffect {
    data class Saved(val result: VanitySaveResult) : ProfileEditorEffect
}

internal fun draftOf(twitch: TwitchVanity, sevenTv: SevenTvVanity?) = VanityDraft(
    colorHex = twitch.chatColorHex,
    globalBadge = twitch.selectedGlobal,
    channelBadge = twitch.selectedChannel,
    paintId = sevenTv?.activePaintId,
    sevenTvBadgeId = sevenTv?.activeBadgeId,
    displayName = twitch.displayName,
    bio = twitch.bio,
    accentHex = twitch.accentHex,
    links = twitch.links,
    stream = twitch.stream
)

class ProfileEditorViewModel(
    private val channelLogin: String?,
    private val gateway: ProfileVanityGateway
) : BaseViewModel<ProfileEditorState, ProfileEditorEvent, ProfileEditorEffect>(ProfileEditorState()) {

    init {
        subscribeToEvents()
        load()
    }

    override suspend fun onEvent(event: ProfileEditorEvent) {
        when (event) {
            ProfileEditorEvent.Retry -> load()
            is ProfileEditorEvent.PickColor -> ChatColorPresets.normalize(event.hex)?.let { hex -> edit { it.copy(colorHex = hex) } }
            is ProfileEditorEvent.PickGlobalBadge -> edit { it.copy(globalBadge = event.key) }
            is ProfileEditorEvent.PickChannelBadge -> edit { it.copy(channelBadge = event.key) }
            is ProfileEditorEvent.PickPaint -> edit { it.copy(paintId = event.id) }
            is ProfileEditorEvent.PickSevenTvBadge -> edit { it.copy(sevenTvBadgeId = event.id) }
            is ProfileEditorEvent.EditDisplayName -> edit { it.copy(displayName = event.value.take(MAX_NAME)) }
            is ProfileEditorEvent.EditBio -> edit { it.copy(bio = event.value.take(MAX_BIO)) }
            is ProfileEditorEvent.ConnectSevenTv -> connect(event.token)
            ProfileEditorEvent.DisconnectSevenTv -> {
                gateway.disconnectSevenTv()
                update { s -> s.copy(sevenTv = SevenTvLink.NotConnected, draft = s.draft?.copy(paintId = null, sevenTvBadgeId = null)) }
            }
            is ProfileEditorEvent.PickAccent -> ChatColorPresets.normalize(event.hex)?.let { hex -> edit { it.copy(accentHex = hex) } }
            is ProfileEditorEvent.AddLink -> addLink(event.title, event.url)
            is ProfileEditorEvent.RemoveLink -> edit { draft -> draft.copy(links = draft.links.filterNot { it.url == event.url }) }
            is ProfileEditorEvent.EditStreamTitle -> editStream { it.copy(title = event.value.take(MAX_STREAM_TITLE)) }
            is ProfileEditorEvent.SearchCategory -> searchCategory(event.query)
            is ProfileEditorEvent.PickCategory -> {
                editStream { it.copy(category = event.category) }
                update { it.copy(categoryResults = emptyList()) }
            }
            is ProfileEditorEvent.AddTag -> {
                val tag = event.tag.trim().filter { it.isLetterOrDigit() }.take(MAX_TAG)
                if (tag.isNotEmpty()) editStream { s -> if (s.tags.size >= MAX_TAGS || s.tags.any { it.equals(tag, true) }) s else s.copy(tags = s.tags + tag) }
            }
            is ProfileEditorEvent.RemoveTag -> editStream { s -> s.copy(tags = s.tags - event.tag) }
            is ProfileEditorEvent.SetLanguage -> editStream { it.copy(language = event.code.lowercase()) }
            ProfileEditorEvent.Discard -> update { s -> s.copy(draft = s.twitch?.let { draftOf(it, s.sevenTvVanity) }) }
            ProfileEditorEvent.ReviewChanges -> update { s ->
                val twitch = s.twitch
                val draft = s.draft
                if (twitch == null || draft == null) s else s.copy(pendingChanges = VanityChanges.of(twitch, s.sevenTvVanity, draft))
            }
            ProfileEditorEvent.CancelReview -> update { it.copy(pendingChanges = null) }
            ProfileEditorEvent.Save -> {
                update { it.copy(pendingChanges = null) }
                save()
            }
        }
    }

    private var searchJob: Job? = null

    private fun editStream(change: (StreamSettings) -> StreamSettings) = edit { draft ->
        draft.stream?.let { draft.copy(stream = change(it)) } ?: draft
    }

    private fun addLink(title: String, url: String) {
        val clean = url.trim()
        val valid = (clean.startsWith("https://") || clean.startsWith("http://")) && clean.length in 10..MAX_URL
        if (!valid) return
        edit { draft ->
            if (draft.links.size >= MAX_LINKS || draft.links.any { it.url == clean }) draft
            else draft.copy(links = draft.links + SocialLink(null, title.trim().take(MAX_LINK_TITLE), clean))
        }
    }

    private fun searchCategory(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            update { it.copy(categoryResults = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            val results = gateway.searchCategories(query)
            update { it.copy(categoryResults = results) }
        }
    }

    private fun edit(change: (VanityDraft) -> VanityDraft) {
        update { s -> s.draft?.let { s.copy(draft = change(it), result = null) } ?: s }
    }

    private fun load() {
        update { it.copy(load = EditorLoad.LOADING) }
        viewModelScope.launch {
            val sevenTvCall = async { gateway.loadSevenTv() }
            val twitch = gateway.loadTwitch(channelLogin)
            val sevenTv = sevenTvCall.await()
            if (twitch == null) {
                update { it.copy(load = if (sevenTv == SevenTvLink.NotConnected) EditorLoad.SIGNED_OUT else EditorLoad.FAILED) }
                return@launch
            }
            update {
                it.copy(
                    load = EditorLoad.READY,
                    twitch = twitch,
                    sevenTv = sevenTv,
                    draft = draftOf(twitch, (sevenTv as? SevenTvLink.Connected)?.vanity)
                )
            }
        }
    }

    private fun connect(token: String) {
        if (state.value.sevenTvBusy) return
        update { it.copy(sevenTvBusy = true) }
        viewModelScope.launch {
            val link = gateway.connectSevenTv(token)
            update { s ->
                val vanity = (link as? SevenTvLink.Connected)?.vanity
                s.copy(
                    sevenTvBusy = false,
                    sevenTv = link,
                    draft = s.draft?.copy(paintId = vanity?.activePaintId, sevenTvBadgeId = vanity?.activeBadgeId)
                )
            }
        }
    }

    private fun save() {
        val current = state.value
        val twitch = current.twitch ?: return
        val draft = current.draft ?: return
        if (current.saving || !current.dirty) return
        update { it.copy(saving = true, result = null) }
        viewModelScope.launch {
            val result = gateway.save(twitch, current.sevenTvVanity, draft)
            val saved = result.saved
            val confirmed = twitch.copy(
                chatColorHex = if (VanityPart.COLOR in saved) draft.colorHex else twitch.chatColorHex,
                selectedGlobal = if (VanityPart.BADGE in saved) draft.globalBadge else twitch.selectedGlobal,
                selectedChannel = if (VanityPart.BADGE in saved) draft.channelBadge else twitch.selectedChannel,
                displayName = if (VanityPart.PROFILE in saved) draft.displayName.trim() else twitch.displayName,
                bio = if (VanityPart.PROFILE in saved) draft.bio.trim() else twitch.bio,
                accentHex = if (VanityPart.ACCENT in saved) draft.accentHex else twitch.accentHex,
                links = if (VanityPart.LINKS in saved) draft.links else twitch.links,
                stream = if (VanityPart.STREAM in saved) draft.stream else twitch.stream
            )
            val sevenTv = current.sevenTvVanity
                ?.let { if (VanityPart.SEVEN_TV in saved) it.copy(activePaintId = draft.paintId, activeBadgeId = draft.sevenTvBadgeId) else it }
                ?.let { SevenTvLink.Connected(it) } ?: current.sevenTv
            update { it.copy(saving = false, result = result, twitch = confirmed, sevenTv = sevenTv) }
            sendEffect(ProfileEditorEffect.Saved(result))
        }
    }

    private companion object {
        const val MAX_NAME = 25
        const val MAX_BIO = 300
        const val MAX_STREAM_TITLE = 140
        const val MAX_TAG = 25
        const val MAX_TAGS = 10
        const val MAX_LINKS = 5
        const val MAX_LINK_TITLE = 25
        const val MAX_URL = 300
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
