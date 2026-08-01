package io.rudione.chatone.util.settings

object AppConfig {

    const val TWITCH_CLIENT_ID_DESKTOP = "5ez3vtq4fbp8nvpgbkpxk15oga8o7l"
    const val TWITCH_CLIENT_ID_MOBILE = "8hw5ezro8y4opnuqv5myh11up48hwz"

    const val REDIRECT_URI_DESKTOP = "http://localhost:3829/auth/callback"
    const val REDIRECT_URI_MOBILE = "chatone://auth/callback"

    const val OAUTH_CALLBACK_PORT = 3829

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

    fun getAuthUrl(clientId: String, redirectUri: String, state: String = ""): String {
        val scopesString = REQUIRED_SCOPES.joinToString("%20")
        val encodedRedirect = redirectUri
            .replace(":", "%3A")
            .replace("/", "%2F")
        val stateParam = if (state.isNotEmpty()) "&state=$state" else ""
        return "https://id.twitch.tv/oauth2/authorize" +
                "?client_id=$clientId" +
                "&redirect_uri=$encodedRedirect" +
                "&response_type=token" +
                "&scope=$scopesString" +
                stateParam +
                "&force_verify=true"
    }
}
