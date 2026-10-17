package io.rudione.chatone.util.media

import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import io.rudione.chatone.data.repository.AccountManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class AppImageLoaderInstaller(
    private val accountManager: AccountManager,
    private val scope: CoroutineScope
) {

    private val factory = SingletonImageLoader.Factory { context ->
        createAnimatedImageLoader(context, accountManager.activeProxy.value)
    }

    @OptIn(DelicateCoilApi::class)
    fun install() {
        SingletonImageLoader.setUnsafe(factory)
        scope.launch {
            accountManager.activeProxy.drop(1).collect {
                SingletonImageLoader.setUnsafe(factory)
            }
        }
    }
}
