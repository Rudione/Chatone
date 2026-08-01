package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.rudione.chatone.domain.model.IrcEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class TwitchPubSubClient(
    private val httpClient: HttpClient,
    private val scope: CoroutineScope
) {
    private val TAG = "TwitchPubSub"
    private val PUBSUB_URL = "wss://pubsub-edge.twitch.tv/v1"
    private val json = Json { ignoreUnknownKeys = true }

    private val _events = MutableSharedFlow<IrcEvent>(
        extraBufferCapacity = 512,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<IrcEvent> = _events

    private var connectionJob: Job? = null
    private var session: WebSocketSession? = null
    private var pingJob: Job? = null

    private var currentToken: String = ""
    private var currentUserId: String = ""
    private var currentChannelId: String = ""
    private var currentIsMod: Boolean = false

    fun connect(accessToken: String, userId: String, channelId: String, isMod: Boolean = false) {
        if (accessToken.isEmpty() || userId.isEmpty() || channelId.isEmpty()) return
        if (accessToken == currentToken && userId == currentUserId && channelId == currentChannelId &&
            isMod == currentIsMod
        ) return

        currentToken = accessToken
        currentUserId = userId
        currentChannelId = channelId
        currentIsMod = isMod
        reconnect()
    }

    fun disconnect() {
        pingJob?.cancel()
        connectionJob?.cancel()
        session = null
        currentToken = ""
        currentUserId = ""
        currentChannelId = ""
        currentIsMod = false
    }

    private fun reconnect() {
        pingJob?.cancel()
        connectionJob?.cancel()
        connectionJob = scope.launch {
            var backoff = 2_000L
            while (isActive && currentToken.isNotEmpty()) {
                try {
                    Napier.d("Connecting to PubSub...", tag = TAG)
                    httpClient.webSocket(PUBSUB_URL) {
                        session = this

                        val pinTopic = "pinned-chat-updates-v1.$currentChannelId"
                        val pinListen =
                            """{"type":"LISTEN","nonce":"pin","data":{"topics":["$pinTopic"],"auth_token":"$currentToken"}}"""
                        send(Frame.Text(pinListen))
                        Napier.d("Subscribed to PubSub topic: $pinTopic", tag = TAG)

                        val pointsTopic = "community-points-user-v1.$currentUserId"
                        val pointsListen =
                            """{"type":"LISTEN","nonce":"points","data":{"topics":["$pointsTopic"],"auth_token":"$currentToken"}}"""
                        send(Frame.Text(pointsListen))
                        Napier.d("Subscribed to PubSub topic: $pointsTopic", tag = TAG)

                        val pollTopic = "polls.$currentChannelId"
                        send(
                            Frame.Text(
                                """{"type":"LISTEN","nonce":"polls","data":{"topics":["$pollTopic"],"auth_token":"$currentToken"}}"""
                            )
                        )
                        Napier.d("Subscribed to PubSub topic: $pollTopic", tag = TAG)

                        val predictionTopic = "predictions-channel-v1.$currentChannelId"
                        send(
                            Frame.Text(
                                """{"type":"LISTEN","nonce":"predictions","data":{"topics":["$predictionTopic"],"auth_token":"$currentToken"}}"""
                            )
                        )
                        Napier.d("Subscribed to PubSub topic: $predictionTopic", tag = TAG)

                        if (currentIsMod) {
                            val automodTopic = "automod-queue.$currentUserId.$currentChannelId"
                            val modActionsTopic =
                                "chat_moderator_actions.$currentUserId.$currentChannelId"

                            val automodListen =
                                """{"type":"LISTEN","nonce":"automod","data":{"topics":["$automodTopic"],"auth_token":"$currentToken"}}"""
                            send(Frame.Text(automodListen))
                            Napier.d("Subscribed to PubSub topic: $automodTopic", tag = TAG)

                            val modActionsListen =
                                """{"type":"LISTEN","nonce":"modactions","data":{"topics":["$modActionsTopic"],"auth_token":"$currentToken"}}"""
                            send(Frame.Text(modActionsListen))
                            Napier.d("Subscribed to PubSub topic: $modActionsTopic", tag = TAG)
                        }

                        pingJob = scope.launch {
                            while (isActive) {
                                delay(60_000L)
                                try {
                                    send(Frame.Text("{\"type\":\"PING\"}"))
                                } catch (_: Exception) {
                                }
                            }
                        }

                        backoff = 2_000L
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                handleMessage(frame.readText())
                            }
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Napier.e("PubSub error: ${e.message}", e, tag = TAG)
                }
                session = null
                pingJob?.cancel()
                if (isActive && currentToken.isNotEmpty()) {
                    Napier.d("PubSub reconnecting in ${backoff}ms", tag = TAG)
                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(120_000L)
                }
            }
        }
    }

    private fun handleMessage(raw: String) {
        try {
            val obj = json.parseToJsonElement(raw).jsonObject
            val type = obj["type"]?.jsonPrimitive?.content ?: return

            when (type) {
                "PONG" -> Napier.v("PubSub PONG", tag = TAG)
                "RECONNECT" -> {
                    Napier.d("PubSub server requested reconnect", tag = TAG)
                    reconnect()
                }

                "RESPONSE" -> {
                    val error = obj["error"]?.jsonPrimitive?.content
                    if (!error.isNullOrEmpty()) {
                        val nonce = obj["nonce"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        Napier.e("PubSub LISTEN error [$nonce]: $error", tag = TAG)
                    }
                }

                "MESSAGE" -> {
                    val dataObj = obj["data"]?.jsonObject ?: return
                    val topic = dataObj["topic"]?.jsonPrimitive?.content ?: return
                    val messageStr = dataObj["message"]?.jsonPrimitive?.content ?: return

                    when {
                        topic.startsWith("automod-queue.") -> handleAutoModMessage(messageStr)
                        topic.startsWith("chat_moderator_actions.") -> handleModActionMessage(
                            messageStr
                        )

                        topic.startsWith("pinned-chat-updates-v1.") -> handlePinnedChatUpdate(
                            messageStr
                        )

                        topic.startsWith("community-points-user-v1.") -> handleCommunityPoints(
                            messageStr
                        )

                        topic.startsWith("polls.") -> handlePollUpdate(messageStr)

                        topic.startsWith("predictions-channel-v1.") -> handlePredictionUpdate(
                            messageStr
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Napier.e("PubSub parse error: ${e.message}", tag = TAG)
        }
    }

    private fun handleCommunityPoints(messageStr: String) {
        try {
            val root = json.parseToJsonElement(messageStr).jsonObject
            if (root["type"]?.jsonPrimitive?.contentOrNull != "claim-available") return
            val claim = root["data"]?.jsonObject?.get("claim")?.jsonObject ?: return
            val claimId = claim["id"]?.jsonPrimitive?.contentOrNull ?: return
            val channelId = claim["channel_id"]?.jsonPrimitive?.contentOrNull ?: return
            scope.launch { _events.emit(IrcEvent.PointsClaimAvailable(channelId, claimId)) }
        } catch (e: Exception) {
            Napier.w("community-points parse failed: ${e.message}", tag = TAG)
        }
    }

    private fun handlePollUpdate(messageStr: String) {
        try {
            val root = json.parseToJsonElement(messageStr).jsonObject
            val type = root["type"]?.jsonPrimitive?.contentOrNull ?: return
            if (type.equals("POLL_ARCHIVE", ignoreCase = true)) {
                scope.launch { _events.emit(IrcEvent.PollUpdated(currentChannelId, null)) }
                return
            }
            val poll = root["data"]?.jsonObject?.get("poll")?.jsonObject ?: return
            val id = poll["poll_id"]?.jsonPrimitive?.contentOrNull ?: return
            val choices = poll["choices"]?.jsonArray?.mapNotNull { c ->
                val co = c.jsonObject
                val cid = co["choice_id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val votes = co["votes"]?.jsonObject
                io.rudione.chatone.data.remote.dto.PollChoice(
                    id = cid,
                    title = co["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    votes = votes?.get("base")?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                    channelPointsVotes = votes?.get("channel_points")?.jsonPrimitive
                        ?.contentOrNull?.toIntOrNull() ?: 0,
                    bitsVotes = votes?.get("bits")?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                )
            }.orEmpty()
            val data = io.rudione.chatone.data.remote.dto.PollData(
                id = id,
                broadcasterId = poll["owned_by"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                title = poll["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                choices = choices,
                status = poll["status"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                duration = poll["duration_seconds"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                startedAt = poll["started_at"]?.jsonPrimitive?.contentOrNull,
                endedAt = poll["ended_at"]?.jsonPrimitive?.contentOrNull,
                remainingMs = poll["remaining_duration_milliseconds"]?.jsonPrimitive
                    ?.contentOrNull?.toLongOrNull()
            )
            Napier.d("PubSub poll $type: ${data.title} (${data.status})", tag = TAG)
            scope.launch { _events.emit(IrcEvent.PollUpdated(currentChannelId, data)) }
        } catch (e: Exception) {
            Napier.w("PubSub poll parse failed: ${e.message}", tag = TAG)
        }
    }

    private fun handlePredictionUpdate(messageStr: String) {
        try {
            val root = json.parseToJsonElement(messageStr).jsonObject
            val event = root["data"]?.jsonObject?.get("event")?.jsonObject ?: return
            val id = event["id"]?.jsonPrimitive?.contentOrNull ?: return
            val outcomes = event["outcomes"]?.jsonArray?.mapNotNull { o ->
                val oo = o.jsonObject
                val oid = oo["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                io.rudione.chatone.data.remote.dto.PredictionOutcome(
                    id = oid,
                    title = oo["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    users = oo["total_users"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                    channelPoints = oo["total_points"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                    color = oo["color"]?.jsonPrimitive?.contentOrNull ?: "BLUE"
                )
            }.orEmpty()
            val data = io.rudione.chatone.data.remote.dto.PredictionData(
                id = id,
                broadcasterId = event["channel_id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                title = event["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                winningOutcomeId = event["winning_outcome_id"]?.jsonPrimitive?.contentOrNull,
                outcomes = outcomes,
                predictionWindow = event["prediction_window_seconds"]?.jsonPrimitive
                    ?.contentOrNull?.toIntOrNull() ?: 0,
                status = event["status"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                createdAt = event["created_at"]?.jsonPrimitive?.contentOrNull,
                endedAt = event["ended_at"]?.jsonPrimitive?.contentOrNull,
                lockedAt = event["locked_at"]?.jsonPrimitive?.contentOrNull
            )
            Napier.d("PubSub prediction: ${data.title} (${data.status})", tag = TAG)
            scope.launch { _events.emit(IrcEvent.PredictionUpdated(currentChannelId, data)) }
        } catch (e: Exception) {
            Napier.w("PubSub prediction parse failed: ${e.message}", tag = TAG)
        }
    }

    private fun handlePinnedChatUpdate(messageStr: String) {
        val channelId = currentChannelId
        try {
            val root = json.parseToJsonElement(messageStr).jsonObject
            val type = root["type"]?.jsonPrimitive?.contentOrNull ?: return
            val data = root["data"]?.jsonObject

            when (type) {
                "pin-message", "update-message" -> {
                    val msg = data?.get("message")?.jsonObject
                    val sender = msg?.get("sender")?.jsonObject
                    val pin = if (msg != null) {
                        val senderName =
                            sender?.get("display_name")?.jsonPrimitive?.contentOrNull.orEmpty()
                        IrcEvent.PinnedChatPayload(
                            pinId = data["id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                            messageId = msg["id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                            text = msg["content"]?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull.orEmpty(),
                            senderId = sender?.get("id")?.jsonPrimitive?.contentOrNull.orEmpty(),
                            senderName = senderName,
                            senderLogin = sender?.get("login")?.jsonPrimitive?.contentOrNull
                                ?.takeIf { it.isNotBlank() } ?: senderName.lowercase(),
                            senderColor = sender?.get("chat_color")?.jsonPrimitive?.contentOrNull,
                            endsAtEpochMs = msg["ends_at"]?.jsonPrimitive?.contentOrNull
                                ?.toLongOrNull()?.takeIf { it > 0 }?.times(1000),
                            pinnerName = data["pinned_by"]?.jsonObject
                                ?.get("display_name")?.jsonPrimitive?.contentOrNull.orEmpty(),
                            pinnerUserId = data["pinned_by"]?.jsonObject
                                ?.get("id")?.jsonPrimitive?.contentOrNull.orEmpty(),
                            pinnerLogin = data["pinned_by"]?.jsonObject
                                ?.get("login")?.jsonPrimitive?.contentOrNull.orEmpty()
                        )
                    } else null
                    scope.launch { _events.emit(IrcEvent.PinnedChatUpdated(channelId, pin = pin)) }
                }

                "unpin-message" -> {
                    val unpinnedBy = data?.get("unpinned_by")?.jsonObject
                        ?.get("display_name")?.jsonPrimitive?.contentOrNull
                    val unpinId = listOf("id", "pin_id", "pinned_chat_message_id")
                        .firstNotNullOfOrNull { key -> data?.get(key)?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } }
                    scope.launch {
                        _events.emit(
                            IrcEvent.PinnedChatUpdated(
                                channelId,
                                isUnpin = true,
                                unpinId = unpinId,
                                unpinnedBy = unpinnedBy
                            )
                        )
                    }
                }

                else -> scope.launch { _events.emit(IrcEvent.PinnedChatUpdated(channelId)) }
            }
        } catch (e: Exception) {
            Napier.w("PubSub pin payload parse failed: ${e.message}", tag = TAG)
            scope.launch { _events.emit(IrcEvent.PinnedChatUpdated(channelId)) }
        }
    }

    private fun handleModActionMessage(messageStr: String) {
        try {
            val outer = json.parseToJsonElement(messageStr).jsonObject
            val data = outer["data"]?.jsonObject ?: outer
            val rawAction = data["moderation_action"]?.jsonPrimitive?.content
                ?: data["type"]?.jsonPrimitive?.content
                ?: return
            val moderator = data["created_by"]?.jsonPrimitive?.content
                ?: data["moderator"]?.jsonObject?.get("login")?.jsonPrimitive?.content
                ?: return
            val moderatorUserId = data["created_by_user_id"]?.jsonPrimitive?.content
                ?: data["moderator"]?.jsonObject?.get("user_id")?.jsonPrimitive?.content
            val args = (data["args"] as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                ?: emptyList()
            val targetUserId = data["target_user_id"]?.jsonPrimitive?.content
                ?: data["target"]?.jsonObject?.get("user_id")?.jsonPrimitive?.content
            val target = args.firstOrNull()
                ?: data["target_user_login"]?.jsonPrimitive?.content
                ?: data["target"]?.jsonObject?.get("login")?.jsonPrimitive?.content

            val action = when (rawAction.lowercase()) {
                "ban" -> IrcEvent.ModeratorAction.ACTION_BAN
                "timeout" -> IrcEvent.ModeratorAction.ACTION_TIMEOUT
                "unban" -> IrcEvent.ModeratorAction.ACTION_UNBAN
                "untimeout" -> IrcEvent.ModeratorAction.ACTION_UNTIMEOUT
                "delete", "delete_notification" -> IrcEvent.ModeratorAction.ACTION_DELETE
                "clear", "clear_chat" -> IrcEvent.ModeratorAction.ACTION_CLEAR
                "mod" -> IrcEvent.ModeratorAction.ACTION_MOD
                "unmod" -> IrcEvent.ModeratorAction.ACTION_UNMOD
                "vip", "add_vip" -> IrcEvent.ModeratorAction.ACTION_VIP
                "unvip", "remove_vip" -> IrcEvent.ModeratorAction.ACTION_UNVIP
                else -> rawAction.lowercase()
            }

            val duration = if (action == IrcEvent.ModeratorAction.ACTION_TIMEOUT) {
                args.getOrNull(1)?.toIntOrNull()
            } else null

            val reason = when (action) {
                IrcEvent.ModeratorAction.ACTION_BAN,
                IrcEvent.ModeratorAction.ACTION_UNBAN -> args.getOrNull(1)

                IrcEvent.ModeratorAction.ACTION_TIMEOUT -> args.getOrNull(2)
                else -> null
            }

            val targetMessageId = if (action == IrcEvent.ModeratorAction.ACTION_DELETE) {
                args.getOrNull(2) ?: args.getOrNull(1)
            } else null

            Napier.d(
                "ModAction: $action by $moderator on ${target ?: "-"} dur=$duration", tag = TAG
            )

            scope.launch {
                _events.emit(
                    IrcEvent.ModeratorAction(
                        channel = currentChannelId,
                        action = action,
                        moderator = moderator,
                        moderatorUserId = moderatorUserId,
                        target = target,
                        targetUserId = targetUserId,
                        duration = duration,
                        reason = reason,
                        targetMessageId = targetMessageId
                    )
                )
            }
        } catch (e: Exception) {
            Napier.e("ModAction parse error: ${e.message}", e, tag = TAG)
        }
    }

    private fun handleAutoModMessage(messageStr: String) {
        try {
            val msgObj = json.parseToJsonElement(messageStr).jsonObject
            val msgType = msgObj["type"]?.jsonPrimitive?.content ?: return

            if (msgType != "automod_caught_message") return

            val data = msgObj["data"]?.jsonObject ?: return

            val status = data["status"]?.jsonPrimitive?.content
            if (status == "ALLOWED" || status == "DENIED") {
                val msgId = data["message"]?.jsonObject?.get("id")?.jsonPrimitive?.content
                    ?: data["message_id"]?.jsonPrimitive?.content
                    ?: return
                val resolvedBy = data["resolver_login"]?.jsonPrimitive?.content
                    ?: data["moderator"]?.jsonObject?.get("login")?.jsonPrimitive?.content
                    ?: "a moderator"
                Napier.d("AutoMod resolved by $resolvedBy: msgId=$msgId status=$status", tag = TAG)
                scope.launch {
                    _events.emit(
                        IrcEvent.AutoModResolved(
                            channel = currentChannelId,
                            msgId = msgId,
                            resolvedBy = resolvedBy,
                            action = status
                        )
                    )
                }
                return
            }

            val messageObj = data["message"]?.jsonObject

            val msgId = messageObj?.get("id")?.jsonPrimitive?.content
                ?: data["message_id"]?.jsonPrimitive?.content
                ?: return

            val contentObj = messageObj?.get("content")?.jsonObject
            val text = contentObj?.get("text")?.jsonPrimitive?.content
                ?: messageObj?.get("text")?.jsonPrimitive?.content
                ?: data["message"]?.jsonPrimitive?.content
                ?: ""

            val senderObj = messageObj?.get("sender")?.jsonObject
                ?: data["sender"]?.jsonObject
            val userId = senderObj?.get("user_id")?.jsonPrimitive?.content ?: ""
            val login = senderObj?.get("login")?.jsonPrimitive?.content ?: ""
            val displayName = senderObj?.get("display_name")?.jsonPrimitive?.content ?: login
            val color = senderObj?.get("chat_color")?.jsonPrimitive?.content

            var reasonCategory: String? = data["content_category"]?.jsonPrimitive?.content
            var reasonLevel: Int? = data["content_level"]?.jsonPrimitive?.content?.toIntOrNull()

            if (reasonCategory == null) {
                try {
                    val classArr = data["content_classification"]?.jsonArray
                    if (classArr != null && classArr.isNotEmpty()) {
                        var maxLevel = -1
                        classArr.forEach { item ->
                            try {
                                val itemObj = item.jsonObject
                                val cat = itemObj["category"]?.jsonPrimitive?.content
                                val lvl =
                                    itemObj["level"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                                if (cat != null && lvl > maxLevel) {
                                    maxLevel = lvl
                                    reasonCategory = cat
                                    reasonLevel = lvl
                                }
                            } catch (_: Exception) {
                            }
                        }
                    }
                } catch (_: Exception) {
                }
            }

            if (reasonCategory == null) {
                reasonCategory = data["reason"]?.jsonPrimitive?.content
                    ?: data["reason_code"]?.jsonPrimitive?.content
            }

            val channel = currentChannelId

            Napier.d(
                "AutoMod held: msgId=$msgId user=$login text='$text' reason=$reasonCategory/$reasonLevel",
                tag = TAG
            )

            scope.launch {
                _events.emit(
                    IrcEvent.AutoModHeld(
                        channel = channel,
                        msgId = msgId,
                        userId = userId,
                        username = login,
                        displayName = displayName,
                        message = text,
                        color = color,
                        reasonCategory = reasonCategory,
                        reasonLevel = reasonLevel
                    )
                )
            }
        } catch (e: Exception) {
            Napier.e("AutoMod message parse error: ${e.message}", e, tag = TAG)
        }
    }
}
