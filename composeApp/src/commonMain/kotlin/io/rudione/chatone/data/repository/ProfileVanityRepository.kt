package io.rudione.chatone.data.repository

import io.rudione.chatone.domain.profile.StreamCategory
import io.rudione.chatone.data.remote.TwitchChannelSettingsClient
import com.russhwolf.settings.Settings
import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.data.remote.TwitchVanityClient
import io.rudione.chatone.data.remote.VanityWrite
import io.rudione.chatone.data.remote.emote.SevenTvCosmeticsClient
import io.rudione.chatone.data.remote.emote.SevenTvInventoryReply
import io.rudione.chatone.data.remote.emote.SevenTvVanityClient
import io.rudione.chatone.domain.profile.ChatColorPresets
import io.rudione.chatone.domain.profile.ProfileVanityGateway
import io.rudione.chatone.domain.profile.SevenTvLink
import io.rudione.chatone.domain.profile.SevenTvVanity
import io.rudione.chatone.domain.profile.TwitchVanity
import io.rudione.chatone.domain.profile.VanityDraft
import io.rudione.chatone.domain.profile.VanityPart
import io.rudione.chatone.domain.profile.VanitySaveResult
import io.rudione.chatone.util.Result
import io.rudione.chatone.util.security.getSecret
import io.rudione.chatone.util.security.putSecret

class ProfileVanityRepository(
    private val sessions: ViewerSessionProvider,
    private val twitch: TwitchVanityClient,
    private val helix: TwitchApiClient,
    private val sevenTv: SevenTvVanityClient,
    private val cosmetics: SevenTvCosmeticsClient,
    private val settings: Settings,
    private val channel: TwitchChannelSettingsClient
) : ProfileVanityGateway {

    override suspend fun loadTwitch(channelLogin: String?): TwitchVanity? {
        val session = sessions.current() ?: return null
        val vanity = twitch.load(session.gqlToken, channelLogin) ?: return null
        val stream = channel.load(session.helixToken, session.userId)
        return vanity.copy(stream = stream)
    }

    override suspend fun searchCategories(query: String): List<StreamCategory> {
        val session = sessions.current() ?: return emptyList()
        return channel.searchCategories(session.helixToken, query)
    }

    override suspend fun loadSevenTv(): SevenTvLink {
        val session = sessions.current() ?: return SevenTvLink.NotConnected
        val token = settings.getSecret(tokenKey(session.userId)).takeIf { it.isNotBlank() }
            ?: return SevenTvLink.NotConnected
        return resolve(token, session.userId)
    }

    override suspend fun connectSevenTv(token: String): SevenTvLink {
        val session = sessions.current() ?: return SevenTvLink.NotConnected
        val clean = token.trim().removePrefix("Bearer ").trim().trim('"')
        if (clean.isEmpty() || clean.length > MAX_TOKEN) return SevenTvLink.Rejected
        val link = resolve(clean, session.userId)
        if (link is SevenTvLink.Connected) settings.putSecret(tokenKey(session.userId), clean)
        return link
    }

    override fun disconnectSevenTv() {
        val userId = sessions.activeAccountId.value
        if (userId.isNotBlank()) settings.putSecret(tokenKey(userId), "")
    }

    private suspend fun resolve(token: String, twitchUserId: String): SevenTvLink =
        when (val reply = sevenTv.inventory(token)) {
            is SevenTvInventoryReply.Loaded ->
                if (reply.twitchId != null && reply.twitchId != twitchUserId) SevenTvLink.OtherAccount
                else SevenTvLink.Connected(reply.vanity)
            SevenTvInventoryReply.Rejected -> SevenTvLink.Rejected
            SevenTvInventoryReply.Unreachable -> SevenTvLink.Unreachable
        }

    override suspend fun save(twitch: TwitchVanity, sevenTv: SevenTvVanity?, draft: VanityDraft): VanitySaveResult {
        val session = sessions.current() ?: return VanitySaveResult(emptySet(), VanityPart.entries.toSet(), emptySet())
        val outcomes = mutableMapOf<VanityPart, VanityWrite>()

        val color = ChatColorPresets.normalize(draft.colorHex)
        if (color != null && color != twitch.chatColorHex) {
            outcomes[VanityPart.COLOR] = saveColor(session, color)
        }

        val badgeWrites = buildList {
            if (draft.globalBadge != twitch.selectedGlobal) {
                add(this@ProfileVanityRepository.twitch.selectGlobalBadge(
                    session.gqlToken,
                    twitch.globalBadges.firstOrNull { it.key == draft.globalBadge }
                ))
            }
            val channelId = twitch.channelId
            val channelBadge = twitch.channelBadges.firstOrNull { it.key == draft.channelBadge }
            if (channelId != null && channelBadge != null && draft.channelBadge != twitch.selectedChannel) {
                add(this@ProfileVanityRepository.twitch.selectChannelBadge(session.gqlToken, channelId, channelBadge))
            }
        }
        if (badgeWrites.isNotEmpty()) outcomes[VanityPart.BADGE] = badgeWrites.worst()

        if (sevenTv != null) {
            val token = settings.getSecret(tokenKey(session.userId))
            val sevenTvWrites = buildList {
                if (draft.paintId != sevenTv.activePaintId) add(this@ProfileVanityRepository.sevenTv.setPaint(token, sevenTv.userId, draft.paintId))
                if (draft.sevenTvBadgeId != sevenTv.activeBadgeId) add(this@ProfileVanityRepository.sevenTv.setBadge(token, sevenTv.userId, draft.sevenTvBadgeId))
            }
            if (sevenTvWrites.isNotEmpty()) {
                val ok = sevenTvWrites.all { it }
                outcomes[VanityPart.SEVEN_TV] = if (ok) VanityWrite.DONE else VanityWrite.FAILED
                if (ok) {
                    cosmetics.applyLocally(
                        twitchUserId = session.userId,
                        sevenTvId = sevenTv.userId,
                        paint = sevenTv.paints.firstOrNull { it.id == draft.paintId },
                        badge = sevenTv.badges.firstOrNull { it.id == draft.sevenTvBadgeId }
                    )
                }
            }
        }

        val accent = ChatColorPresets.normalize(draft.accentHex)
        if (accent != null && accent != twitch.accentHex) {
            outcomes[VanityPart.ACCENT] = this.twitch.updateAccent(session.gqlToken, accent)
        }

        val currentUrls = twitch.links.map { it.url }.toSet()
        val draftUrls = draft.links.map { it.url }.toSet()
        val linkWrites = buildList {
            twitch.links.filter { it.url !in draftUrls }.forEach { removed ->
                removed.id?.let { add(this@ProfileVanityRepository.twitch.removeLink(session.gqlToken, session.userId, it)) }
            }
            draft.links.filter { it.url !in currentUrls }.forEach { added ->
                add(this@ProfileVanityRepository.twitch.addLink(session.gqlToken, session.userId, added))
            }
        }
        if (linkWrites.isNotEmpty()) outcomes[VanityPart.LINKS] = linkWrites.worst()

        val nextStream = draft.stream
        if (nextStream != null && nextStream != twitch.stream) {
            outcomes[VanityPart.STREAM] =
                if (channel.update(session.helixToken, session.userId, nextStream)) VanityWrite.DONE else VanityWrite.FAILED
        }

        val name = draft.displayName.trim().takeIf { it != twitch.displayName && it.equals(twitch.login, ignoreCase = true) }
        val bio = draft.bio.trim().take(MAX_BIO).takeIf { it != twitch.bio.trim() }
        if (name != null || bio != null) {
            outcomes[VanityPart.PROFILE] = this.twitch.updateProfile(session.gqlToken, session.userId, name, bio)
        }

        return VanitySaveResult(
            saved = outcomes.filterValues { it == VanityWrite.DONE }.keys,
            failed = outcomes.filterValues { it == VanityWrite.FAILED }.keys,
            blocked = outcomes.filterValues { it == VanityWrite.BLOCKED }.keys
        )
    }

    private suspend fun saveColor(session: ViewerSession, hex: String): VanityWrite {
        if (session.hasScope(CHAT_COLOR_SCOPE)) {
            val value = ChatColorPresets.presetName(hex) ?: "#$hex"
            if (helix.updateUserChatColor(session.helixToken, session.userId, value) is Result.Success) return VanityWrite.DONE
        }
        return twitch.updateChatColor(session.gqlToken, hex)
    }

    private fun List<VanityWrite>.worst(): VanityWrite = when {
        any { it == VanityWrite.FAILED } -> VanityWrite.FAILED
        any { it == VanityWrite.BLOCKED } -> VanityWrite.BLOCKED
        else -> VanityWrite.DONE
    }

    private fun tokenKey(userId: String) = "$TOKEN_KEY_PREFIX$userId"

    private companion object {
        const val TOKEN_KEY_PREFIX = "seventv_vanity_token_"
        const val CHAT_COLOR_SCOPE = "user:manage:chat_color"
        const val MAX_TOKEN = 4096
        const val MAX_BIO = 300
    }
}
