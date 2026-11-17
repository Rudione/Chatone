package io.rudione.chatone.util.system

enum class HotkeyOwner { ACTION, PAUSE, MACRO, COMMAND }

data class HotkeyBinding(
    val owner: HotkeyOwner,
    val id: String,
    val combo: String,
    val label: String
) {
    val key: String get() = "${owner.name}:$id"
}

object HotkeyBindings {
    private val modifierOrder = listOf("ctrl", "alt", "shift")
    private val functionKeys = (1..12).map { "f$it" }.toSet()

    fun normalize(combo: String): String {
        val parts = combo.lowercase().split("+").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return ""
        val modifiers = parts.dropLast(1).filter { it in modifierOrder }.distinct()
            .sortedBy { modifierOrder.indexOf(it) }
        return (modifiers + parts.last()).joinToString("+")
    }

    fun isSafeTrigger(combo: String): Boolean {
        val parts = normalize(combo).split("+").filter { it.isNotEmpty() }
        if (parts.isEmpty()) return false
        val main = parts.last()
        if (main.startsWith("mouse")) return false
        if (main in functionKeys) return true
        if (main in modifierOrder) return false
        return parts.size > 1
    }

    fun conflicts(bindings: List<HotkeyBinding>): Map<String, List<HotkeyBinding>> {
        val byCombo = bindings
            .filter { it.combo.isNotBlank() }
            .groupBy { normalize(it.combo) }
            .filterValues { it.size > 1 }
        return buildMap {
            byCombo.values.forEach { group ->
                group.forEach { binding -> put(binding.key, group.filter { it.key != binding.key }) }
            }
        }
    }
}
