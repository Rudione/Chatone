package io.rudione.chatone.data.remote

private const val BROWSER_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"

private const val TV_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 7.1; Smart Box C1) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"

private const val TV_ORIGIN = "https://android.tv.twitch.tv"

enum class TwitchFirstPartyClient(
    val clientId: String,
    val userAgent: String,
    val origin: String?,
    val referer: String?
) {
    WEB(
        clientId = "kimne78kx3ncx6brgo4mv6wki5h1ko",
        userAgent = BROWSER_USER_AGENT,
        origin = null,
        referer = null
    ),
    TV(
        clientId = "ue6666qo983tsx6so1t0vnawi233wa",
        userAgent = TV_USER_AGENT,
        origin = TV_ORIGIN,
        referer = "$TV_ORIGIN/"
    );

    companion object {
        val deviceFlowOrder: List<TwitchFirstPartyClient> = listOf(TV, WEB)

        fun fromClientId(clientId: String?): TwitchFirstPartyClient? =
            entries.firstOrNull { it.clientId == clientId }
    }
}
