package com.ccread.reader.ui.reader

import com.ccread.reader.domain.model.Chapter
import com.ccread.reader.domain.model.TocNode

data class TocRow(
    val key: String,
    val title: String,
    val depth: Int,
    val expandable: Boolean = false,
    val expanded: Boolean = true,
)

object ResplitMapping {
    fun map(
        @Suppress("UNUSED_PARAMETER") oldChapters: List<Chapter>,
        newChapters: List<Chapter>,
        oldCharPosition: Long,
    ): Pair<Int, Long> {
        if (newChapters.isEmpty()) return 0 to 0L
        val target = newChapters.lastOrNull { it.charStart <= oldCharPosition } ?: newChapters.first()
        val offset = (oldCharPosition - target.charStart).coerceIn(0L, target.charCount)
        return target.index to offset
    }
}

object ChapterNavigation {

    fun txtTarget(chapters: List<Chapter>, currentIndex: Int, delta: Int): Chapter? {
        val position = chapters.indexOfFirst { it.index == currentIndex }
        if (position < 0) return null
        return chapters.getOrNull(position + delta)
    }

    fun epubTarget(nodes: List<TocNode>, currentHref: String?, delta: Int): String? {
        if (currentHref == null) return null
        val hrefs = flattenHrefs(nodes)
        val position = indexOfHref(hrefs, currentHref)
        if (position < 0) return null
        return hrefs.getOrNull(position + delta)
    }

    private fun flattenHrefs(nodes: List<TocNode>): List<String> = nodes.flatMap { node ->
        (node.href?.let(::listOf).orEmpty()) + flattenHrefs(node.children)
    }

    private fun indexOfHref(hrefs: List<String>, current: String): Int {
        val exact = hrefs.indexOf(current)
        if (exact >= 0) return exact

        val target = current.substringBefore('#')
        val sameDocument = hrefs.indexOfFirst { it.substringBefore('#') == target }
        if (sameDocument >= 0) return sameDocument

        return hrefs.indexOfFirst {
            val other = it.substringBefore('#')
            other.isNotEmpty() && (target.endsWith(other) || other.endsWith(target))
        }
    }
}

object TocAncestors {

    fun of(
        nodes: List<com.ccread.reader.domain.model.TocNode>,
        href: String,
    ): Set<String> {
        val target = href.substringBefore('#')
        val path = find(nodes, target, emptyList()) ?: return emptySet()
        return path.toSet()
    }

    private fun find(
        nodes: List<com.ccread.reader.domain.model.TocNode>,
        target: String,
        path: List<String>,
    ): List<String>? {
        for (node in nodes) {
            val key = node.href ?: node.title
            val nodeHref = node.href?.substringBefore('#')
            if (nodeHref == target) return path
            val found = find(node.children, target, path + key)
            if (found != null) return found
        }
        return null
    }
}
