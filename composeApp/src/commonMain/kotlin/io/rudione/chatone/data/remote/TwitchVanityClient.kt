package io.rudione.chatone.data.remote

import io.rudione.chatone.domain.profile.SocialLink
import io.rudione.chatone.domain.profile.TwitchVanity
import io.rudione.chatone.domain.profile.VanityBadge
import io.rudione.chatone.util.Result
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

enum class VanityWrite { DONE, BLOCKED, FAILED }

class TwitchVanityClient(
    private val transport: ViewerGqlTransport,
    private val gql: TwitchGqlClient
) {

    suspend fun load(token: String, channelLogin: String?): TwitchVanity? {
        val channel = channelLogin?.trim()?.lowercase()?.takeIf { LOGIN.matches(it) }
        val reply = transport.signedIn(
            token,
            if (channel != null) STATE_WITH_CHANNEL_QUERY else STATE_QUERY,
            buildJsonObject { if (channel != null) put("channelLogin", channel) }
        ) as? GqlReply.Data ?: return null
        val me = reply.data.obj("currentUser") ?: return null
        val userId = me.str("id")?.takeIf { it.isNotBlank() } ?: return null
        val channelNode = if (channel != null) reply.data.obj("user") else null
        val channelSelf = channelNode?.obj("self")
        return TwitchVanity(
            userId = userId,
            login = me.str("login").orEmpty(),
            displayName = me.str("displayName").orEmpty(),
            bio = me.str("description").orEmpty(),
            chatColorHex = me.str("chatColor")?.removePrefix("#")?.uppercase(),
            accentHex = me.str("primaryColorHex")?.uppercase(),
            links = me.obj("channel")?.arr("socialMedias")?.objects().orEmpty().mapNotNull { link ->
                val url =
                    link.str("url")?.takeIf { it.startsWith("http") } ?: return@mapNotNull null
                SocialLink(link.str("id"), link.str("title").orEmpty(), url)
            },
            globalBadges = me.arr("availableBadges").badges(),
            selectedGlobal = me.obj("selectedBadge")?.badge()?.key,
            channelId = channelNode?.str("id"),
            channelName = channelNode?.str("displayName"),
            channelBadges = channelSelf?.arr("availableBadges")?.badges().orEmpty(),
            selectedChannel = channelSelf?.obj("selectedBadge")?.badge()?.key
        )
    }

    suspend fun selectGlobalBadge(token: String, badge: VanityBadge?): VanityWrite =
        if (badge == null) {
            write(DESELECT_GLOBAL_MUTATION, JsonObject(emptyMap()), token, "deselectGlobalBadge")
        } else {
            write(SELECT_GLOBAL_MUTATION, buildJsonObject {
                putJsonObject("input") {
                    put("badgeSetID", badge.setId)
                    put("badgeSetVersion", badge.version)
                }
            }, token, "selectGlobalBadge")
        }

    suspend fun selectChannelBadge(
        token: String,
        channelId: String,
        badge: VanityBadge
    ): VanityWrite =
        write(SELECT_CHANNEL_MUTATION, buildJsonObject {
            putJsonObject("input") {
                put("badgeSetID", badge.setId)
                put("badgeSetVersion", badge.version)
                put("channelID", channelId)
            }
        }, token, "selectChannelBadge")

    suspend fun updateChatColor(token: String, hex: String): VanityWrite =
        write(CHAT_COLOR_MUTATION, buildJsonObject {
            putJsonObject("input") { put("color", "#$hex") }
        }, token, "updateChatColor")

    suspend fun updateAccent(token: String, hex: String): VanityWrite =
        write(
            ACCENT_MUTATION,
            buildJsonObject { putJsonObject("input") { put("primaryColorHex", hex) } },
            token,
            "updateUserColors"
        )

    suspend fun addLink(token: String, channelId: String, link: SocialLink): VanityWrite =
        write(ADD_LINK_MUTATION, buildJsonObject {
            putJsonObject("input") {
                put("channelID", channelId)
                put("title", link.title.ifBlank { link.url }.take(MAX_LINK_TITLE))
                put("url", link.url)
            }
        }, token, "createSocialMedia")

    suspend fun removeLink(token: String, channelId: String, id: String): VanityWrite =
        write(REMOVE_LINK_MUTATION, buildJsonObject {
            putJsonObject("input") {
                put("channelID", channelId)
                put("id", id)
            }
        }, token, "deleteSocialMedia")

    suspend fun updateProfile(
        token: String,
        userId: String,
        displayName: String?,
        bio: String?
    ): VanityWrite =
        write(UPDATE_USER_MUTATION, buildJsonObject {
            putJsonObject("input") {
                put("userID", userId)
                displayName?.let { put("displayName", it) }
                bio?.let { put("description", it) }
            }
        }, token, "updateUser")

    private suspend fun write(
        query: String,
        variables: JsonObject,
        token: String,
        key: String
    ): VanityWrite =
        when (val r = gql.firstPartyCall(query, variables, token, key)) {
            is Result.Success -> if (r.data.obj("error") != null || r.data.str("error") != null) VanityWrite.FAILED else VanityWrite.DONE
            is Result.Error -> if (r.exception is GqlIntegrityRejectedException) VanityWrite.BLOCKED else VanityWrite.FAILED
            else -> VanityWrite.FAILED
        }

    private fun kotlinx.serialization.json.JsonArray.badges(): List<VanityBadge> =
        objects().mapNotNull { it.badge() }.filter { it.setId.lowercase() !in RESERVED_SETS }
            .distinctBy { it.key }

    private fun JsonObject.badge(): VanityBadge? {
        val setId = str("setID")?.takeIf { it.isNotBlank() } ?: return null
        val image = str("imageURL")?.takeIf { it.startsWith("https://") } ?: return null
        return VanityBadge(
            setId,
            str("version").orEmpty(),
            str("title")?.takeIf { it.isNotBlank() } ?: setId,
            image)
    }

    private companion object {
        val LOGIN = Regex("^[a-z0-9_]{1,25}$")
        val RESERVED_SETS = setOf(
            "staff", "admin", "global_mod", "lead_moderator", "moderator", "vip", "broadcaster",
            "subscriber", "founder", "predictions"
        )
        const val BADGE_FIELDS = "setID version title imageURL(size: QUADRUPLE)"
        const val MAX_LINK_TITLE = 25
        const val ME_FIELDS =
            "currentUser { id login displayName description chatColor primaryColorHex " +
                    "channel { socialMedias { id title url } } " +
                    "selectedBadge { $BADGE_FIELDS } availableBadges { $BADGE_FIELDS } }"
        const val STATE_QUERY = "query ChatoneVanityState { $ME_FIELDS }"
        const val STATE_WITH_CHANNEL_QUERY =
            "query ChatoneVanityChannelState(\$channelLogin: String!) { $ME_FIELDS " +
                    "user(login: \$channelLogin) { id displayName self { " +
                    "selectedBadge { $BADGE_FIELDS } availableBadges { $BADGE_FIELDS } } } }"
        const val SELECT_GLOBAL_MUTATION =
            "mutation ChatoneSelectGlobalBadge(\$input: SelectGlobalBadgeInput!) { selectGlobalBadge(input: \$input) { user { id } } }"
        const val DESELECT_GLOBAL_MUTATION =
            "mutation ChatoneDeselectGlobalBadge { deselectGlobalBadge { user { id } } }"
        const val SELECT_CHANNEL_MUTATION =
            "mutation ChatoneSelectChannelBadge(\$input: SelectChannelBadgeInput!) { selectChannelBadge(input: \$input) { isSuccessful user { id } } }"
        const val CHAT_COLOR_MUTATION =
            "mutation ChatoneUpdateChatColor(\$input: UpdateChatColorInput!) { updateChatColor(input: \$input) { user { id chatColor } } }"
        const val ACCENT_MUTATION =
            "mutation ChatoneUpdateColors(\$input: UpdateUserColorsInput!) { updateUserColors(input: \$input) { user { id } } }"
        const val ADD_LINK_MUTATION =
            "mutation ChatoneAddLink(\$input: CreateSocialMediaInput!) { createSocialMedia(input: \$input) { error } }"
        const val REMOVE_LINK_MUTATION =
            "mutation ChatoneRemoveLink(\$input: DeleteSocialMediaInput!) { deleteSocialMedia(input: \$input) { __typename } }"
        const val UPDATE_USER_MUTATION =
            "mutation ChatoneUpdateUser(\$input: UpdateUserInput!) { updateUser(input: \$input) { user { id } error { code } } }"
    }
}
