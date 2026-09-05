package io.rudione.chatone.util.network

private val TYPE_MARKERS = listOf(
    "ioexception",
    "timeout",
    "unresolvedaddress",
    "unknownhost",
    "connectexception",
    "socketexception",
    "sslexception",
    "nsurlerror",
    "httprequesttimeout"
)

private val MESSAGE_MARKERS = listOf(
    "unable to resolve host",
    "no address associated",
    "nodename nor servname",
    "network is unreachable",
    "connection refused",
    "connection reset",
    "connection closed",
    "connection abort",
    "failed to connect",
    "timed out",
    "timeout",
    "offline",
    "econnrefused",
    "econnreset",
    "enotfound",
    "etimedout",
    "enetunreach",
    "eai_nodata",
    "no internet",
    "handshake"
)

private val TRANSIENT_STATUS = Regex("\\b(408|425|429|500|502|503|504)\\b")

fun isRetryableMessage(message: String?): Boolean {
    val text = message.orEmpty().lowercase()
    if (text.isEmpty()) return false
    return MESSAGE_MARKERS.any { text.contains(it) } || TRANSIENT_STATUS.containsMatchIn(text)
}

fun isNetworkFailure(error: Throwable?): Boolean {
    var current = error
    var depth = 0
    while (current != null && depth < 6) {
        val type = current::class.simpleName.orEmpty().lowercase()
        if (TYPE_MARKERS.any { type.contains(it) }) return true
        val message = current.message.orEmpty().lowercase()
        if (MESSAGE_MARKERS.any { message.contains(it) }) return true
        val cause = current.cause
        current = if (cause === current) null else cause
        depth++
    }
    return false
}

fun isTransientServerFailure(error: Throwable?): Boolean {
    var current = error
    var depth = 0
    while (current != null && depth < 6) {
        if (TRANSIENT_STATUS.containsMatchIn(current.message.orEmpty())) return true
        val cause = current.cause
        current = if (cause === current) null else cause
        depth++
    }
    return false
}

fun isRetryableFailure(error: Throwable?): Boolean =
    isNetworkFailure(error) || isTransientServerFailure(error)
