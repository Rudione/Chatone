package io.rudione.chatone.util

object AppConfig {

    const val TWITCH_CLIENT_ID = "5ez3vtq4fbp8nvpgbkpxk15oga8o7l"


    const val REDIRECT_URI_DESKTOP = "http://localhost:3829/auth/callback"
    const val REDIRECT_URI_MOBILE = "chatone://auth/callback"

    const val OAUTH_CALLBACK_PORT = 3829


    val REQUIRED_SCOPES = listOf(
        "chat:read",
        "chat:edit",
        "channel:moderate",
        "channel:manage:broadcast",
        "channel:manage:moderators",
        "channel:manage:vips",
        "channel:manage:raids",
        "moderator:manage:banned_users",
        "moderator:manage:chat_messages",
        "moderator:manage:chat_settings",
        "moderator:manage:announcements",
        "moderator:manage:automod",
        "moderator:manage:shoutouts",
        "moderator:manage:warnings",
        "moderator:read:chatters",
        "moderator:read:followers",
        "moderator:read:moderators",
        "moderator:read:vips",
        "user:manage:blocked_users",
        "user:manage:chat_color",
        "user:manage:whispers",
        "user:read:blocked_users",
        "whispers:read",
        "whispers:edit"
    )


    fun getAuthUrl(redirectUri: String): String {
        val scopesString = REQUIRED_SCOPES.joinToString("%20")
        val encodedRedirect = redirectUri
            .replace(":", "%3A")
            .replace("/", "%2F")
        return "https://id.twitch.tv/oauth2/authorize" +
                "?client_id=$TWITCH_CLIENT_ID" +
                "&redirect_uri=$encodedRedirect" +
                "&response_type=token" +
                "&scope=$scopesString" +
                "&force_verify=true"
    }
}
