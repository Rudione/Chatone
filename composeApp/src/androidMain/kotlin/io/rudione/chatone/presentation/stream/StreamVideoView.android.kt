package io.rudione.chatone.presentation.stream

import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import io.rudione.chatone.domain.stream.StreamPlaybackEngine

@OptIn(UnstableApi::class)
@Composable
actual fun PlatformVideoView(engine: StreamPlaybackEngine?, keepScreenOn: Boolean, modifier: Modifier) {
    val androidEngine = engine as? AndroidStreamPlaybackEngine
    var surfaceView by remember { mutableStateOf<SurfaceView?>(null) }

    AndroidView(
        factory = { context -> SurfaceView(context).also { surfaceView = it } },
        update = { view -> view.keepScreenOn = keepScreenOn },
        onRelease = { view -> view.keepScreenOn = false },
        modifier = modifier
    )

    val view = surfaceView
    DisposableEffect(androidEngine, view) {
        if (androidEngine != null && view != null) androidEngine.attachSurface(view)
        onDispose {
            if (androidEngine != null && view != null) androidEngine.detachSurface(view)
        }
    }
}
