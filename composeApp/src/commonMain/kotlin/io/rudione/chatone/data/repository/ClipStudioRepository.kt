package io.rudione.chatone.data.repository

import io.github.aakira.napier.Napier
import io.rudione.chatone.data.remote.GqlIntegrityRejectedException
import io.rudione.chatone.data.remote.GqlReply
import io.rudione.chatone.data.remote.GqlServerException
import io.rudione.chatone.data.remote.GqlUnauthorizedException
import io.rudione.chatone.data.remote.TwitchGqlClient
import io.rudione.chatone.data.remote.TwitchMediaClient
import io.rudione.chatone.data.remote.ViewerGqlTransport
import io.rudione.chatone.data.remote.obj
import io.rudione.chatone.data.remote.str
import io.rudione.chatone.domain.media.ClipDraft
import io.rudione.chatone.domain.media.ClipFailure
import io.rudione.chatone.domain.media.ClipOutcome
import io.rudione.chatone.domain.media.ClipStudio
import io.rudione.chatone.util.Result
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class ClipStudioRepository(
    private val gql: TwitchGqlClient,
    private val transport: ViewerGqlTransport,
    private val media: TwitchMediaClient,
    private val sessions: ViewerSessionProvider
) : ClipStudio {

    override suspend fun createDraft(broadcasterId: String, broadcastId: String?, offsetSeconds: Float): ClipOutcome<String> =
        create(broadcasterId, offsetSeconds) { broadcastId?.let { put("broadcastID", it) } }

    override suspend fun createVideoDraft(broadcasterId: String, videoId: String, offsetSeconds: Float): ClipOutcome<String> =
        create(broadcasterId, offsetSeconds) { put("videoID", videoId) }

    private suspend fun create(
        broadcasterId: String,
        offsetSeconds: Float,
        anchor: JsonObjectBuilder.() -> Unit
    ): ClipOutcome<String> {
        val session = sessions.current() ?: return ClipOutcome.Failed(ClipFailure.SIGNED_OUT)
        val variables = buildJsonObject {
            putJsonObject("input") {
                put("broadcasterID", broadcasterId)
                anchor()
                put("offsetSeconds", offsetSeconds.coerceAtLeast(0f))
            }
        }
        return call(CREATE_MUTATION, variables, session.gqlToken, "createClip") { payload ->
            payload.obj("error")?.str("code")?.let { code ->
                Napier.w("createClip refused: $code", tag = TAG)
                return@call ClipOutcome.Failed(ClipFailure.fromTwitchCode(code))
            }
            payload.obj("clip")?.str("slug")?.let { ClipOutcome.Done(it) } ?: ClipOutcome.Failed(ClipFailure.FAILED)
        }
    }

    override suspend fun draft(slug: String): ClipDraft? {
        val session = sessions.current() ?: return null
        val reply = transport.signedIn(session.gqlToken, DRAFT_QUERY, buildJsonObject { put("slug", slug) }) as? GqlReply.Data
            ?: return null
        val clip = reply.data.obj("clip") ?: return null
        val edited = media.clipSources(clip)
        val raw = media.clipSources(clip, TwitchMediaClient.RAW_VIDEO_QUALITIES)
        if (raw.isEmpty() && edited.isEmpty()) return null
        val duration = (clip["durationSeconds"] as? JsonPrimitive)?.content?.toFloatOrNull() ?: return null
        return ClipDraft(
            slug = slug,
            durationSeconds = duration,
            sources = raw.ifEmpty { edited },
            thumbnailUrl = clip.str("thumbnailURL").orEmpty(),
            fallbackSources = edited.takeIf { raw.isNotEmpty() }.orEmpty(),
            ready = clip.str("creationState") != CREATING_STATE
        )
    }

    override suspend fun publish(slug: String, startSeconds: Float, lengthSeconds: Float, title: String): ClipOutcome<String> {
        val session = sessions.current() ?: return ClipOutcome.Failed(ClipFailure.SIGNED_OUT)
        val variables = buildJsonObject {
            putJsonObject("input") {
                put("slug", slug)
                put("title", title.trim().take(MAX_TITLE))
                putJsonArray("segments") {
                    addJsonObject {
                        put("offsetSeconds", startSeconds.coerceAtLeast(0f))
                        put("durationSeconds", lengthSeconds)
                        put("speed", 1.0)
                    }
                }
            }
        }
        return call(PUBLISH_MUTATION, variables, session.gqlToken, "publishClip") { payload ->
            val error = payload.obj("error")
            if (error != null) {
                Napier.w("publishClip refused: ${error.str("__typename")} ${error.str("message").orEmpty()}", tag = TAG)
                ClipOutcome.Failed(ClipFailure.FAILED)
            } else {
                ClipOutcome.Done(payload.obj("clip")?.str("slug") ?: slug)
            }
        }
    }

    override suspend fun delete(slug: String): ClipOutcome<Unit> {
        val session = sessions.current() ?: return ClipOutcome.Failed(ClipFailure.SIGNED_OUT)
        val variables = buildJsonObject { putJsonObject("input") { putJsonArray("slugs") { add(slug) } } }
        return call(DELETE_MUTATION, variables, session.gqlToken, "deleteClips") { ClipOutcome.Done(Unit) }
    }

    private suspend fun <T> call(
        query: String,
        variables: JsonObject,
        token: String,
        key: String,
        map: (JsonObject) -> ClipOutcome<T>
    ): ClipOutcome<T> = when (val r = gql.firstPartyCall(query, variables, token, key)) {
        is Result.Success -> map(r.data)
        is Result.Error -> ClipOutcome.Failed(
            when (r.exception) {
                is GqlIntegrityRejectedException -> ClipFailure.BLOCKED
                is GqlUnauthorizedException -> ClipFailure.NEEDS_RIGHTS
                is GqlServerException -> ClipFailure.TWITCH_ERROR
                else -> ClipFailure.FAILED
            }
        )
        else -> ClipOutcome.Failed(ClipFailure.FAILED)
    }

    private companion object {
        const val TAG = "ClipStudio"
        const val MAX_TITLE = 100
        const val CREATING_STATE = "CREATING"
        const val CREATE_MUTATION =
            "mutation ChatoneCreateClip(\$input: CreateClipInput!) { createClip(input: \$input) { clip { slug } error { code } } }"
        const val PUBLISH_MUTATION =
            "mutation ChatonePublishClip(\$input: PublishClipInput!) { publishClip(input: \$input) { clip { slug } error { __typename message } } }"
        const val DELETE_MUTATION =
            "mutation ChatoneDeleteClips(\$input: DeleteClipsInput!) { deleteClips(input: \$input) { clips { slug } } }"
        const val DRAFT_QUERY =
            "query ChatoneClipDraft(\$slug: ID!) { clip(slug: \$slug) { slug creationState durationSeconds thumbnailURL(width: 480, height: 272) " +
                    "${TwitchMediaClient.SOURCES_FIELDS} } }"
    }
}
