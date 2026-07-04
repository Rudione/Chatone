package io.rudione.chatone.util

/** Opens the stream in an external player via streamlink (must be on PATH).
 * Quality: "best", "720p", "480p", "audio_only"… Falls back to browser if missing. */
expect fun openInStreamlink(channelLogin: String, quality: String = "best")
