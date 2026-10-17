package io.rudione.chatone.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalComposeUiApi::class)
class SecondaryClickTest {

    private var macroEdits = 0
    private val barMenus = mutableListOf<Offset>()

    private val scene = ImageComposeScene(width = 300, height = 40, density = Density(1f)) {
        Row(Modifier.fillMaxSize().onUnclaimedSecondaryClick { barMenus += it }) {
            Box(Modifier.size(40.dp).onSecondaryClick { macroEdits++ })
        }
    }

    @AfterTest
    fun close() = scene.close()

    private fun rightClick(at: Offset) {
        scene.sendPointerEvent(
            PointerEventType.Press, at, type = PointerType.Mouse,
            buttons = PointerButtons(isSecondaryPressed = true), button = PointerButton.Secondary
        )
        scene.sendPointerEvent(
            PointerEventType.Release, at, type = PointerType.Mouse,
            buttons = PointerButtons(), button = PointerButton.Secondary
        )
        scene.render()
    }

    @Test
    fun rightClickOnAMacroOpensOnlyItsEditor() {
        scene.render()
        rightClick(Offset(20f, 20f))
        assertEquals(1, macroEdits)
        assertEquals(emptyList(), barMenus)
    }

    @Test
    fun rightClickOnAnEmptyPartOfTheBarOpensTheChannelMenu() {
        scene.render()
        rightClick(Offset(200f, 20f))
        assertEquals(0, macroEdits)
        assertEquals(listOf(Offset(200f, 20f)), barMenus)
    }

    @Test
    fun aLeftClickOpensNothing() {
        scene.render()
        scene.sendPointerEvent(
            PointerEventType.Press, Offset(200f, 20f), type = PointerType.Mouse,
            buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary
        )
        scene.render()
        assertEquals(emptyList(), barMenus)
    }
}
