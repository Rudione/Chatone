package io.rudione.chatone.util.media

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import coil3.SingletonImageLoader
import io.rudione.chatone.presentation.chat.emote.SharedEmoteDrawables

class ImageMemoryTrimmer(private val context: Context) : ComponentCallbacks2 {

    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) release()
    }

    override fun onConfigurationChanged(newConfig: Configuration) = Unit

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() = release()

    private fun release() {
        SharedEmoteDrawables.evictAll()
        SingletonImageLoader.get(context).memoryCache?.clear()
    }
}
