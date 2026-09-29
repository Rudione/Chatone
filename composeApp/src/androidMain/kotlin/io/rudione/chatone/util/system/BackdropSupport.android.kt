package io.rudione.chatone.util.system

import android.os.Build

actual val isBackdropBlurSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
