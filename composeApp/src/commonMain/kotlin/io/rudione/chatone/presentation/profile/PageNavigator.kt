package io.rudione.chatone.presentation.profile

import io.rudione.chatone.domain.media.ClipOrigin
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Immutable
sealed interface PageDestination {
    val key: String

    data class Channel(val login: String) : PageDestination {
        override val key: String get() = "channel:$login"
    }

    data class Editor(val channelLogin: String?) : PageDestination {
        override val key: String get() = "editor:${channelLogin.orEmpty()}"
    }

    data class Clip(val slug: String) : PageDestination {
        override val key: String get() = "clip:$slug"
    }

    data class Video(val id: String, val startSeconds: Int) : PageDestination {
        override val key: String get() = "video:$id"
    }

    data class ClipCreate(val login: String, val origin: ClipOrigin) : PageDestination {
        override val key: String get() = "clip-create:$login"
    }
}

class PageNavigator(val isSupported: Boolean) {
    private val _stack = MutableStateFlow<List<PageDestination>>(emptyList())
    val stack: StateFlow<List<PageDestination>> = _stack.asStateFlow()

    fun openChannel(login: String) {
        val normalized = login.trim().removePrefix("#").lowercase()
        if (LOGIN.matches(normalized)) push(PageDestination.Channel(normalized))
    }

    fun openEditor(channelLogin: String?) = push(PageDestination.Editor(channelLogin?.lowercase()))

    fun openClip(slug: String) {
        if (SLUG.matches(slug)) push(PageDestination.Clip(slug))
    }

    fun openVideo(id: String, startSeconds: Int = 0) {
        if (VIDEO_ID.matches(id)) push(PageDestination.Video(id, startSeconds.coerceAtLeast(0)))
    }

    fun createClip(login: String, delaySeconds: Float? = null) {
        val normalized = login.trim().removePrefix("#").lowercase()
        if (LOGIN.matches(normalized)) push(PageDestination.ClipCreate(normalized, ClipOrigin.Live(delaySeconds)))
    }

    fun createVideoClip(login: String, videoId: String, broadcasterId: String, offsetSeconds: Float) {
        val normalized = login.trim().removePrefix("#").lowercase()
        if (!LOGIN.matches(normalized) || !VIDEO_ID.matches(videoId) || broadcasterId.isBlank()) return
        push(PageDestination.ClipCreate(normalized, ClipOrigin.Video(videoId, broadcasterId, offsetSeconds.coerceAtLeast(0f))))
    }

    fun back() = _stack.update { it.dropLast(1) }

    fun close() {
        _stack.value = emptyList()
    }

    private fun push(destination: PageDestination) {
        if (!isSupported) return
        _stack.update { current ->
            if (current.lastOrNull() == destination) current
            else (current.filterNot { it.key == destination.key } + destination).takeLast(MAX_DEPTH)
        }
    }

    private companion object {
        const val MAX_DEPTH = 6
        val LOGIN = Regex("^[a-z0-9_]{1,25}$")
        val SLUG = Regex("^[A-Za-z0-9_-]{3,100}$")
        val VIDEO_ID = Regex("^[0-9]{1,20}$")
    }
}
