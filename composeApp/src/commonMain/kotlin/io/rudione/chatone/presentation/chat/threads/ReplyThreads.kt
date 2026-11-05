package io.rudione.chatone.presentation.chat.threads

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import io.rudione.chatone.domain.model.DisplayMessage

@Immutable
data class ThreadOrigin(val login: String?, val displayName: String, val body: String)

@Immutable
data class ReplyThread(
    val rootId: String,
    val root: DisplayMessage.PrivMsg?,
    val origin: ThreadOrigin?,
    val replies: List<DisplayMessage.PrivMsg>
) {
    val starterLogin: String? get() = root?.username ?: origin?.login
    val starterName: String get() = root?.displayName ?: origin?.displayName.orEmpty()
    val replyTarget: DisplayMessage.PrivMsg? get() = root ?: replies.lastOrNull()
}

fun DisplayMessage.PrivMsg.threadRootId(): String? =
    rawMessage?.let { it.replyThreadParentMsgId ?: it.replyParentMsgId }

fun replyThreadOf(messages: List<DisplayMessage>, rootId: String): ReplyThread {
    var root: DisplayMessage.PrivMsg? = null
    val replies = ArrayList<DisplayMessage.PrivMsg>()
    for (message in messages) {
        if (message !is DisplayMessage.PrivMsg) continue
        when {
            message.id == rootId -> root = message
            message.threadRootId() == rootId -> replies += message
        }
    }
    val origin = if (root == null) {
        replies.firstOrNull { it.rawMessage?.replyParentMsgId == rootId }?.rawMessage?.let { raw ->
            raw.replyParentMsgBody?.let { body ->
                ThreadOrigin(raw.replyParentUserLogin, raw.replyParentDisplayName ?: raw.replyParentUserLogin.orEmpty(), body)
            }
        }
    } else {
        null
    }
    return ReplyThread(rootId, root, origin, replies)
}

class ReplyThreadOpener(val open: (rootId: String) -> Unit)

val LocalReplyThreadOpener = staticCompositionLocalOf<ReplyThreadOpener?> { null }
