package io.rudione.chatone.presentation.auth

import io.rudione.chatone.base.UIEffect
import io.rudione.chatone.domain.model.TwitchAccount

sealed class AuthEffect : UIEffect {
    data class NavigateToHome(val account: TwitchAccount?) : AuthEffect()
    data class OpenAuthUrl(val url: String) : AuthEffect()
    data class OpenAuthTab(val url: String) : AuthEffect()
}
