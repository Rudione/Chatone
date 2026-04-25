package io.rudione.chatone.util.emote

import io.rudione.chatone.domain.model.GenericEmote

class EmoteSearchIndex {
    private data class TrieNode(
        val children: MutableMap<Char, TrieNode> = mutableMapOf(),
        val emotes: MutableList<GenericEmote> = mutableListOf()
    )

    private val root = TrieNode()
    private val reverseRoot = TrieNode()

    fun build(emotes: List<GenericEmote>) {
        root.children.clear()
        reverseRoot.children.clear()
        for (emote in emotes) {
            val code = emote.code.lowercase()
            insert(root, code, emote)
            insert(reverseRoot, code.reversed(), emote)
        }
    }

    private fun insert(root: TrieNode, code: String, emote: GenericEmote) {
        var node = root
        for (char in code) {
            node = node.children.getOrPut(char) { TrieNode() }
        }
        if (!node.emotes.any { it.id == emote.id }) {
            node.emotes.add(emote)
        }
    }

    fun search(query: String, limit: Int = Int.MAX_VALUE): List<GenericEmote> {
        if (query.isBlank()) return emptyList()
        val q = query.lowercase()
        val results = mutableSetOf<GenericEmote>()
        collectMatches(root, q, results)
        collectMatches(reverseRoot, q.reversed(), results)
        return results.sortedWith(
            compareByDescending<GenericEmote> { emote ->
                val code = emote.code.lowercase()
                when {
                    code == q -> 3
                    code.startsWith(q) -> 2
                    code.endsWith(q) -> 1
                    else -> 0
                }
            }
        ).take(limit)
    }

    private fun collectMatches(root: TrieNode, query: String, results: MutableSet<GenericEmote>) {
        var node = root
        for (char in query) {
            node = node.children[char] ?: return
        }
        collectFromNode(node, results)
    }

    private fun collectFromNode(node: TrieNode, results: MutableSet<GenericEmote>) {
        results.addAll(node.emotes)
        for (child in node.children.values) {
            collectFromNode(child, results)
        }
    }

    fun clear() {
        root.children.clear()
        reverseRoot.children.clear()
    }
}