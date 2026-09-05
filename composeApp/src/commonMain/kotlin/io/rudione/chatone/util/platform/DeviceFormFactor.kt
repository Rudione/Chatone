package io.rudione.chatone.util.platform

enum class DeviceFormFactor {
    DESKTOP,
    PHONE,
    TABLET,
    TV,
    XR,
    WATCH;

    val isLeanBack: Boolean get() = this == TV

    val hasPointer: Boolean get() = this == DESKTOP || this == XR

    val prefersLargeTargets: Boolean get() = this == TV || this == XR || this == WATCH
}

expect fun currentFormFactor(): DeviceFormFactor
