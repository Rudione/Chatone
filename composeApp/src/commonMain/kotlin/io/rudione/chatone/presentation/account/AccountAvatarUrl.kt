package io.rudione.chatone.presentation.account

import io.rudione.chatone.domain.model.TwitchAccount


object AccountAvatarUrl {

    fun resolve(account: TwitchAccount): String? {
        return account.profileImageUrl.takeIf { it.isNotBlank() && it.startsWith("http") }
    }


    fun fallbackInitial(account: TwitchAccount): Char {
        val src = account.displayName.takeIf { it.isNotBlank() } ?: account.login
        return src.firstOrNull()?.uppercaseChar() ?: '?'
    }
}
