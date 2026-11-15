package io.rudione.chatone.presentation.nativecontext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo

class NativeMessageAction(val title: String, val symbol: String, val destructive: Boolean = false, private val action: () -> Unit) {
    fun perform() = action()
}

class NativeMessageMenu(val title: String, val body: String, val actions: List<NativeMessageAction>)

class NativeMessageMenuHit(val menu: NativeMessageMenu, val left: Double, val top: Double, val width: Double, val height: Double)

object NativeMessageMenus {
    var enabled = false
    private var modalCount = 0
    val isAvailable: Boolean get() = enabled && modalCount == 0
    var onAvailabilityChanged: ((Boolean) -> Unit)? = null
    internal fun changeModalCount(delta: Int) {
        modalCount += delta
        onAvailabilityChanged?.invoke(isAvailable)
    }
    private val nodes = LinkedHashSet<MessageMenuNode>()
    internal fun register(node: MessageMenuNode) { nodes.add(node) }
    internal fun remove(node: MessageMenuNode) { nodes.remove(node) }
    fun menuAt(x: Double, y: Double): NativeMessageMenuHit? {
        if (!isAvailable) return null
        val position = Offset(x.toFloat(), y.toFloat())
        return nodes.asSequence().mapNotNull { it.hit(position) }.lastOrNull()
    }
}

fun Modifier.nativeMessageMenu(menu: NativeMessageMenu, enabled: Boolean, accepts: (Offset) -> Boolean): Modifier =
    if (NativeMessageMenus.enabled && enabled) then(MessageMenuElement(menu, accepts)) else this

private data class MessageMenuElement(val menu: NativeMessageMenu, val accepts: (Offset) -> Boolean) : ModifierNodeElement<MessageMenuNode>() {
    override fun create() = MessageMenuNode(menu, accepts)
    override fun update(node: MessageMenuNode) { node.menu = menu; node.accepts = accepts }
    override fun InspectorInfo.inspectableProperties() { name = "nativeMessageMenu" }
}

internal class MessageMenuNode(var menu: NativeMessageMenu, var accepts: (Offset) -> Boolean) : Modifier.Node() {
    override fun onAttach() = NativeMessageMenus.register(this)
    override fun onDetach() = NativeMessageMenus.remove(this)
    fun hit(point: Offset): NativeMessageMenuHit? {
        if (!isAttached) return null
        val coordinates = requireLayoutCoordinates()
        if (!coordinates.isAttached) return null
        val bounds = coordinates.boundsInWindow()
        if (bounds.isEmpty || !bounds.contains(point)) return null
        val local = coordinates.windowToLocal(point)
        if (!accepts(local)) return null
        return NativeMessageMenuHit(menu, bounds.left.toDouble(), bounds.top.toDouble(), bounds.width.toDouble(), bounds.height.toDouble())
    }
}

@Composable
fun SuspendNativeMessageMenus() {
    if (!NativeMessageMenus.enabled) return
    DisposableEffect(Unit) {
        NativeMessageMenus.changeModalCount(1)
        onDispose { NativeMessageMenus.changeModalCount(-1) }
    }
}
