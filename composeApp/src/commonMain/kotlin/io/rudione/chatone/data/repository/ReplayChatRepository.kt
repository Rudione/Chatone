package io.rudione.chatone.data.repository

import io.github.aakira.napier.Napier
import io.rudione.chatone.data.remote.emote.SevenTvCosmeticsClient
import io.rudione.chatone.domain.media.ReplayChannel
import io.rudione.chatone.domain.media.ReplayChatRenderer
import io.rudione.chatone.domain.media.ReplayLine
import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.domain.model.ChannelEmotes
import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.Emote
import io.rudione.chatone.domain.model.EmotePosition
import io.rudione.chatone.domain.model.hasGrandModBadge
import io.rudione.chatone.util.chat.MessageTokenizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReplayChatRepository(
    private val emoteRepository: EmoteRepository,
    private val badgeRepository: BadgeRepository,
    private val cosmetics: SevenTvCosmeticsClient,
    private val sessions: ViewerSessionProvider
) : ReplayChatRenderer {

    override suspend fun prepare(channel: ReplayChannel) {
        val token = sessions.current()?.helixToken.orEmpty()
        coroutineScope {
            launch { attempt("global emotes") { emoteRepository.loadGlobalEmotes() } }
            launch { attempt("channel emotes") { emoteRepository.loadChannelEmotes(channel.login, channel.id) } }
            launch {
                attempt("badges") {
                    badgeRepository.loadGlobalBadges(token)
                    badgeRepository.loadChannelBadges(channel.id, token)
                }
            }
        }
    }

    override suspend fun render(channel: ReplayChannel, lines: List<ReplayLine>): List<DisplayMessage.PrivMsg> =
        withContext(Dispatchers.Default) {
            val resolved = emoteRepository.getResolvedEmotes(channel.login)
            lines.map { line -> line.toMessage(channel, resolved) }
        }

    override suspend fun loadCosmetics(userIds: Collection<String>): Boolean = coroutineScope {
        userIds.asSequence()
            .filter { it.isNotBlank() && cosmetics.getCachedCosmetics(it) == null }
            .distinct()
            .take(MAX_COSMETIC_LOOKUPS)
            .map { id -> async { attempt("7tv cosmetics") { cosmetics.getUserCosmetics(id) } } }
            .toList()
            .awaitAll()
            .any { it != null }
    }

    private fun ReplayLine.toMessage(channel: ReplayChannel, resolved: ChannelEmotes): DisplayMessage.PrivMsg {
        val rawBadges = badges.map { Badge(id = it.setId, version = it.version, imageUrl = "") }
        val raw = ChatMessage(
            id = "replay_$id",
            channelId = channel.id,
            channelName = channel.login,
            userId = userId,
            username = login,
            displayName = displayName,
            message = text,
            timestamp = channel.startedAtMs + offsetSeconds * MS_PER_SECOND,
            color = colorHex,
            badges = rawBadges,
            emotes = emotes.map { emote ->
                Emote(
                    id = emote.id,
                    name = text.substring(emote.start, emote.end + 1),
                    positions = listOf(EmotePosition(emote.start, emote.end)),
                    imageUrl = "$TWITCH_EMOTE_CDN/${emote.id}/default/dark/2.0"
                )
            },
            isModerator = rawBadges.any { it.id == "moderator" },
            isSubscriber = rawBadges.any { it.id == "subscriber" || it.id == "founder" },
            isVip = rawBadges.any { it.id == "vip" },
            isBroadcaster = rawBadges.any { it.id == "broadcaster" },
            isGrandMod = rawBadges.hasGrandModBadge()
        )
        val cosmetic = cosmetics.getCachedCosmetics(userId)
        return DisplayMessage.PrivMsg(
            id = raw.id,
            timestamp = raw.timestamp,
            channel = raw.channelName,
            userId = userId,
            username = login,
            displayName = displayName,
            tokens = MessageTokenizer.tokenize(raw, resolved, personalEmotes = emoteRepository.getCachedPersonalEmotes(userId)),
            color = colorHex,
            badges = badgeRepository.resolveBadges(rawBadges, channel.id),
            isModerator = raw.isModerator,
            isSubscriber = raw.isSubscriber,
            isVip = raw.isVip,
            isBroadcaster = raw.isBroadcaster,
            isGrandMod = raw.isGrandMod,
            isMention = false,
            isAction = false,
            rawMessage = raw,
            sevenTvPaint = cosmetic?.paint,
            sevenTvBadge = cosmetic?.badge
        )
    }

    private suspend fun <T> attempt(what: String, block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Napier.w("Replay $what failed: ${e.message}", tag = TAG)
        null
    }

    private companion object {
        const val TAG = "ReplayChat"
        const val MS_PER_SECOND = 1_000L
        const val MAX_COSMETIC_LOOKUPS = 60
        const val TWITCH_EMOTE_CDN = "https://static-cdn.jtvnw.net/emoticons/v2"
    }
}
