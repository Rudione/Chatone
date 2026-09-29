package io.rudione.chatone.data.remote.stream

import io.rudione.chatone.domain.stream.StreamAdBreakSource
import io.rudione.chatone.domain.stream.StreamManifestResult
import io.rudione.chatone.domain.stream.StreamVariant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

class TwitchAdBreakResolver internal constructor(
    private val client: TwitchPlaybackClient,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) : StreamAdBreakSource {

    private class BackupSession(val variants: List<StreamVariant>, val createdAtMs: Long)

    private val mutex = Mutex()
    private val sessions = HashMap<String, BackupSession>()
    private val preferredPlayerType = HashMap<String, String>()

    override suspend fun adFreeMediaPlaylist(
        channelLogin: String,
        variant: StreamVariant,
        supportedCodecs: String
    ): String? {
        val login = channelLogin.trim().lowercase()
        for (playerType in playerTypesFor(login)) {
            val backup = backupVariant(login, playerType, supportedCodecs, variant) ?: continue
            val playlist = client.fetchMediaPlaylist(backup.url)
            if (playlist == null) {
                forget(login, playerType)
                continue
            }
            if (TwitchAdSegments.hasAds(playlist) || !TwitchAdSegments.hasOnlyAbsoluteHttpsSegments(playlist)) continue
            mutex.withLock { preferredPlayerType[login] = playerType }
            return playlist
        }
        return null
    }

    private suspend fun playerTypesFor(login: String): List<String> {
        val preferred = mutex.withLock { preferredPlayerType[login] } ?: return BACKUP_PLAYER_TYPES
        return listOf(preferred) + BACKUP_PLAYER_TYPES.filter { it != preferred }
    }

    private suspend fun backupVariant(
        login: String,
        playerType: String,
        codecs: String,
        target: StreamVariant
    ): StreamVariant? {
        val key = sessionKey(login, playerType)
        val cached = mutex.withLock {
            sessions[key]?.takeIf { nowMs() - it.createdAtMs < SESSION_TTL_MS }
        }
        val variants = cached?.variants ?: run {
            val result = client.fetchManifest(login, codecs, playerType)
            val fresh = (result as? StreamManifestResult.Ready)?.manifest?.variants ?: return null
            mutex.withLock { sessions[key] = BackupSession(fresh, nowMs()) }
            fresh
        }
        return matchVariant(target, variants)
    }

    private suspend fun forget(login: String, playerType: String) {
        mutex.withLock {
            sessions.remove(sessionKey(login, playerType))
            if (preferredPlayerType[login] == playerType) preferredPlayerType.remove(login)
        }
    }

    private fun sessionKey(login: String, playerType: String) = "$login/$playerType"

    internal companion object {
        const val SESSION_TTL_MS = 5 * 60_000L
        val BACKUP_PLAYER_TYPES = listOf(
            TwitchPlayerType.POPOUT,
            TwitchPlayerType.EMBED,
            TwitchPlayerType.AUTOPLAY,
            TwitchPlayerType.PICTURE_BY_PICTURE
        )

        fun matchVariant(target: StreamVariant, candidates: List<StreamVariant>): StreamVariant? {
            if (target.isAudioOnly) return candidates.firstOrNull { it.isAudioOnly }
            candidates.firstOrNull { it.groupId == target.groupId }?.let { return it }
            val video = candidates.filterNot { it.isAudioOnly }
            val ceiling = if (target.height > 0) target.height else Int.MAX_VALUE
            return video
                .filter { it.height in 1..ceiling }
                .maxWithOrNull(compareBy<StreamVariant> { it.height }.thenBy { it.frameRate })
                ?: video.minByOrNull { it.height }
        }
    }
}
