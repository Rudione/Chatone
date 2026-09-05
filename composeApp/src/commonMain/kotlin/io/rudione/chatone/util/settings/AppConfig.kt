package io.rudione.chatone.util.settings

object AppConfig {

    const val TWITCH_CLIENT_ID = "5ez3vtq4fbp8nvpgbkpxk15oga8o7l"

    const val SITE_LOGIN_URL = "https://app.chatone.im/auth/"

    const val LOGIN_PAYLOAD_VERSION = 1

    val REQUIRED_SCOPES = listOf(
        "chat:read",
        "chat:edit",
        "user:write:chat",
        "channel:moderate",
        "channel:manage:broadcast",
        "channel:manage:moderators",
        "channel:manage:vips",
        "channel:manage:raids",
        "channel:read:redemptions",
        "channel:manage:redemptions",
        "channel:read:polls",
        "channel:manage:polls",
        "channel:read:predictions",
        "channel:manage:predictions",
        "moderator:manage:banned_users",
        "moderator:manage:chat_messages",
        "moderator:read:automod_settings",
        "moderator:manage:automod_settings",
        "moderator:manage:chat_settings",
        "moderator:manage:announcements",
        "moderator:manage:automod",
        "moderator:manage:shoutouts",
        "moderator:manage:warnings",
        "moderator:read:chatters",
        "moderator:read:followers",
        "moderator:read:moderators",
        "moderator:read:vips",
        "moderator:read:blocked_terms",
        "moderator:read:unban_requests",
        "moderator:read:suspicious_users",
        "user:read:moderated_channels",
        "user:read:emotes",
        "user:manage:blocked_users",
        "user:manage:chat_color",
        "user:manage:whispers",
        "user:read:blocked_users",
        "whispers:read",
        "whispers:edit"
    )
}
