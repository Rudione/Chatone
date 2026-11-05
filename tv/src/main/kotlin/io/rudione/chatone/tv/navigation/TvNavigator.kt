package io.rudione.chatone.tv.navigation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import io.rudione.chatone.domain.browse.BrowseCategory
import io.rudione.chatone.domain.media.MediaKind

@Immutable
sealed interface TvRoute {
    data object Browse : TvRoute
    data class Category(val category: BrowseCategory) : TvRoute
    data class Channel(val login: String) : TvRoute
    data class Player(val login: String, val displayName: String = "", val avatarUrl: String = "") :
        TvRoute

    data class Media(val kind: MediaKind, val id: String) : TvRoute
    data class ClipEditor(val login: String, val delaySeconds: Float?) : TvRoute
}

@Stable
class TvNavigator {
    private val routes = mutableStateListOf<TvRoute>(TvRoute.Browse)

    val stack: List<TvRoute> get() = routes
    val top: TvRoute get() = routes.last()

    fun open(route: TvRoute) {
        if (routes.lastOrNull() == route) return
        if (route is TvRoute.Player) routes.removeAll { it is TvRoute.Player }
        routes.add(route)
    }

    fun back(): Boolean {
        if (routes.size <= 1) return false
        routes.removeAt(routes.lastIndex)
        return true
    }

    fun replaceTop(route: TvRoute) {
        if (routes.size > 1) routes.removeAt(routes.lastIndex)
        open(route)
    }
}
