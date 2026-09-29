package io.rudione.chatone.presentation.window

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.PopupPositionProvider
import com.russhwolf.settings.Settings

internal interface FloatingPositionStore {
    fun load(): Offset?
    fun save(fraction: Offset)
}

internal class SettingsFloatingPositionStore(
    private val settings: Settings,
    private val key: String
) : FloatingPositionStore {

    override fun load(): Offset? {
        if (!settings.hasKey("$key.x") || !settings.hasKey("$key.y")) return null
        return Offset(
            settings.getFloat("$key.x", 0f).coerceIn(0f, 1f),
            settings.getFloat("$key.y", 0f).coerceIn(0f, 1f)
        )
    }

    override fun save(fraction: Offset) {
        settings.putFloat("$key.x", fraction.x)
        settings.putFloat("$key.y", fraction.y)
    }
}

@Immutable
internal data class SafeInsets(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    companion object {
        val Zero = SafeInsets(0, 0, 0, 0)
    }
}

@Stable
internal class FloatingPopupPlacement(private val store: FloatingPositionStore) {

    var fraction: Offset? by mutableStateOf(store.load())
        private set

    private var exact = Offset.Zero
    private var origin = Offset.Zero
    private var span = Offset.Zero

    fun resolve(anchor: IntOffset, window: IntSize, popup: IntSize, safe: SafeInsets): IntOffset {
        origin = Offset(safe.left.toFloat(), safe.top.toFloat())
        span = Offset(
            (window.width - safe.right - popup.width - safe.left).coerceAtLeast(0).toFloat(),
            (window.height - safe.bottom - popup.height - safe.top).coerceAtLeast(0).toFloat()
        )
        val requested = fraction?.let { origin + Offset(it.x * span.x, it.y * span.y) }
            ?: Offset(anchor.x.toFloat(), anchor.y.toFloat())
        exact = clamp(requested)
        return exact.round()
    }

    fun onDrag(delta: Offset) {
        exact = clamp(exact + delta)
        fraction = Offset(
            if (span.x > 0f) (exact.x - origin.x) / span.x else 0f,
            if (span.y > 0f) (exact.y - origin.y) / span.y else 0f
        )
    }

    fun onDragEnd() {
        fraction?.let(store::save)
    }

    private fun clamp(position: Offset) = Offset(
        position.x.coerceIn(origin.x, origin.x + span.x),
        position.y.coerceIn(origin.y, origin.y + span.y)
    )
}

internal class FloatingPopupPositionProvider(
    private val placement: FloatingPopupPlacement,
    private val safe: SafeInsets,
    private val container: IntSize
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val bounds = if (container.width > 0 && container.height > 0) container else windowSize
        return placement.resolve(anchorBounds.topLeft, bounds, popupContentSize, safe)
    }
}
