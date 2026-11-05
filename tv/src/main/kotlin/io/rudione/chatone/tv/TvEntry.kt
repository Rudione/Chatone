package io.rudione.chatone.tv

import io.rudione.chatone.AppScreens
import io.rudione.chatone.tv.auth.TvAuthScreen
import io.rudione.chatone.tv.home.TvMainScreen

val TvAppScreens = AppScreens(
    auth = { onAuthenticated -> TvAuthScreen(onAuthenticated) },
    main = { onSignedOut -> TvMainScreen(onSignedOut) }
)
