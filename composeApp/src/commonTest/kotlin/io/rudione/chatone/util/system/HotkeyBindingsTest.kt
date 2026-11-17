package io.rudione.chatone.util.system

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HotkeyBindingsTest {

    @Test
    fun normalizeSortsModifiersAndLowercases() {
        assertEquals("ctrl+shift+k", HotkeyBindings.normalize("Shift + CTRL + K"))
        assertEquals("ctrl+alt+1", HotkeyBindings.normalize("alt+ctrl+1"))
        assertEquals("f5", HotkeyBindings.normalize("F5"))
        assertEquals("", HotkeyBindings.normalize("  "))
    }

    @Test
    fun safeTriggerNeedsModifierOrFunctionKey() {
        assertTrue(HotkeyBindings.isSafeTrigger("ctrl+1"))
        assertTrue(HotkeyBindings.isSafeTrigger("alt+shift+m"))
        assertTrue(HotkeyBindings.isSafeTrigger("f8"))
        assertFalse(HotkeyBindings.isSafeTrigger("k"))
        assertFalse(HotkeyBindings.isSafeTrigger("ctrl"))
        assertFalse(HotkeyBindings.isSafeTrigger("mouse4"))
        assertFalse(HotkeyBindings.isSafeTrigger(""))
    }

    @Test
    fun conflictsListTheOtherOwners() {
        val sidebar = HotkeyBinding(HotkeyOwner.ACTION, "toggle_sidebar", "ctrl+b", "Sidebar")
        val macro = HotkeyBinding(HotkeyOwner.MACRO, "m1", "CTRL+B", "Ban wave")
        val command = HotkeyBinding(HotkeyOwner.COMMAND, "c1", "ctrl+shift+b", "!bonk")
        val unset = HotkeyBinding(HotkeyOwner.MACRO, "m2", "", "Empty")
        val conflicts = HotkeyBindings.conflicts(listOf(sidebar, macro, command, unset))
        assertEquals(listOf(macro), conflicts[sidebar.key])
        assertEquals(listOf(sidebar), conflicts[macro.key])
        assertEquals(null, conflicts[command.key])
        assertEquals(null, conflicts[unset.key])
    }
}
