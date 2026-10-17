package io.rudione.chatone.util.media

import android.os.Build

internal actual val animatedWebpSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
