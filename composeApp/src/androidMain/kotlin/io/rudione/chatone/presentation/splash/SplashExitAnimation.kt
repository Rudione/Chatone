package io.rudione.chatone.presentation.splash

import android.os.SystemClock
import android.view.animation.PathInterpolator
import androidx.core.splashscreen.SplashScreenViewProvider

private const val EXIT_DURATION_MS = 260L
private const val MAX_ICON_WAIT_MS = 420L
private const val ICON_EXIT_SCALE = 1.16f

internal fun SplashScreenViewProvider.playChatoneExit() {
    val easing = PathInterpolator(0.4f, 0f, 0.2f, 1f)
    val iconEndsAt = iconAnimationStartMillis + iconAnimationDurationMillis
    val wait = (iconEndsAt - SystemClock.uptimeMillis()).coerceIn(0L, MAX_ICON_WAIT_MS)
    runCatching { iconView }.getOrNull()?.animate()
        ?.setStartDelay(wait)
        ?.setDuration(EXIT_DURATION_MS)
        ?.setInterpolator(easing)
        ?.scaleX(ICON_EXIT_SCALE)
        ?.scaleY(ICON_EXIT_SCALE)
        ?.alpha(0f)
        ?.start()
    view.animate()
        .setStartDelay(wait)
        .setDuration(EXIT_DURATION_MS)
        .setInterpolator(easing)
        .alpha(0f)
        .withEndAction { remove() }
        .start()
}
