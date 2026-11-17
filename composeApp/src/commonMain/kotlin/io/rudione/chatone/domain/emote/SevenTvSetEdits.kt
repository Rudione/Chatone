package io.rudione.chatone.domain.emote

import io.rudione.chatone.domain.model.GenericEmote

data class SevenTvRemovedEmote(val id: String, val name: String)

data class SevenTvRenamedEmote(val id: String, val newName: String)

data class SevenTvSetEdits(
    val removed: List<SevenTvRemovedEmote> = emptyList(),
    val renamed: List<SevenTvRenamedEmote> = emptyList(),
    val added: List<GenericEmote> = emptyList()
) {
    val isEmpty: Boolean get() = removed.isEmpty() && renamed.isEmpty() && added.isEmpty()
}

data class SevenTvEditResult(val emotes: List<GenericEmote>, val touchedCodes: Set<String>)

object SevenTvSetEditor {

    fun apply(current: List<GenericEmote>, edits: SevenTvSetEdits): SevenTvEditResult {
        if (edits.isEmpty) return SevenTvEditResult(current, emptySet())
        val touched = HashSet<String>()
        val removedIds = HashSet<String>(edits.removed.size * 2)
        val removedNames = HashSet<String>(edits.removed.size * 2)
        for (removal in edits.removed) {
            removedIds += removal.id
            removedNames += removal.name
            touched += removal.name
        }
        val renames = HashMap<String, String>(edits.renamed.size * 2)
        for (rename in edits.renamed) {
            renames[rename.id] = rename.newName
            touched += rename.newName
        }
        val added = LinkedHashMap<String, GenericEmote>(edits.added.size * 2)
        for (emote in edits.added) {
            added.remove(emote.id)
            added[emote.id] = emote
            touched += emote.code
        }

        val result = ArrayList<GenericEmote>(current.size + added.size)
        for (emote in current) {
            if (emote.id in removedIds || emote.code in removedNames || emote.id in added) {
                touched += emote.code
                continue
            }
            val newName = renames[emote.id]
            if (newName == null || newName == emote.code) {
                result += emote
            } else {
                touched += emote.code
                result += emote.copy(code = newName)
            }
        }
        result.addAll(added.values)
        return SevenTvEditResult(result, touched)
    }
}
