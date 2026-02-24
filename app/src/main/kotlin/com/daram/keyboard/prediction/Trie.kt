package com.daram.keyboard.prediction

class TrieNode {
    val children = HashMap<Char, TrieNode>(4)
    var isEnd = false
    var frequency = 0
}

class Trie {
    val root = TrieNode()

    fun insert(word: String, frequency: Int = 1) {
        var node = root
        for (ch in word) {
            node = node.children.getOrPut(ch) { TrieNode() }
        }
        node.isEnd = true
        node.frequency += frequency
    }

    /**
     * prefix로 시작하는 단어를 빈도 내림차순으로 최대 maxResults개 반환.
     */
    fun search(prefix: String, maxResults: Int = 5): List<Pair<String, Int>> {
        var node = root
        for (ch in prefix) {
            node = node.children[ch] ?: return emptyList()
        }
        val results = mutableListOf<Pair<String, Int>>()
        collectWords(node, prefix, results, maxResults)
        results.sortByDescending { it.second }
        return results.take(maxResults)
    }

    private fun collectWords(
        node: TrieNode,
        current: String,
        results: MutableList<Pair<String, Int>>,
        maxResults: Int
    ) {
        if (results.size >= maxResults * 4) return  // 조기 종료 (정렬 후 잘라냄)
        if (node.isEnd) results.add(Pair(current, node.frequency))
        for ((ch, child) in node.children) {
            collectWords(child, current + ch, results, maxResults)
        }
    }

    fun isEmpty(): Boolean = root.children.isEmpty()
}
