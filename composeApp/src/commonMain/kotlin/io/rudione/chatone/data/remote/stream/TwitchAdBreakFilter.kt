package io.rudione.chatone.data.remote.stream

import io.rudione.chatone.domain.stream.StreamAdBreakSource
import io.rudione.chatone.domain.stream.StreamManifest
import io.rudione.chatone.domain.stream.StreamVariant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.concurrent.Volatile

internal class TwitchAdBreakFilter(
    private val manifest: StreamManifest,
    private val supportedCodecs: String,
    private val source: StreamAdBreakSource,
    private val backupTimeoutMs: Long = BACKUP_TIMEOUT_MS
) {
    private val mutex = Mutex()
    private val replacedPlaylists = HashSet<String>()

    @Volatile
    private var replacedView: Set<String> = emptySet()

    fun needsFilter(playlistUrl: String, playlist: String): Boolean {
        variantFor(playlistUrl) ?: return false
        return TwitchAdSegments.hasAds(playlist) || playlistUrl in replacedView
    }

    suspend fun filter(playlistUrl: String, playlist: String): String {
        val variant = variantFor(playlistUrl) ?: return playlist
        val hasAds = TwitchAdSegments.hasAds(playlist)
        val replaced = mutex.withLock { playlistUrl in replacedPlaylists }
        if (!hasAds && !replaced) return playlist

        val backup = withTimeoutOrNull(backupTimeoutMs) {
            source.adFreeMediaPlaylist(manifest.channelLogin, variant, supportedCodecs)
        }
        val chosen = backup?.takeIf { hasAds || !TwitchAdSegments.shareTimeline(playlist, it) }
        mutex.withLock {
            if (chosen != null) replacedPlaylists += playlistUrl else if (!hasAds) replacedPlaylists -= playlistUrl
            replacedView = replacedPlaylists.toSet()
        }
        return chosen ?: playlist
    }

    private fun variantFor(url: String): StreamVariant? =
        manifest.variants.firstOrNull { it.url == url }
            ?: url.substringBefore('?').let { path -> manifest.variants.firstOrNull { it.url.substringBefore('?') == path } }

    private companion object {
        const val BACKUP_TIMEOUT_MS = 6_000L
    }
}
