package com.ccread.reader.domain.txt

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import kotlin.math.abs

data class Page(
    val text: String,
    val startChar: Int,
    val endChar: Int,
)

interface TextPageBreaker {
    fun paginate(text: String): List<Page>
}

object PageSanitizer {
    fun sanitize(pages: List<Page>): List<Page> {
        val nonBlank = pages.filter { it.text.isNotBlank() }
        return if (nonBlank.isEmpty()) pages.take(1) else nonBlank
    }
}

object PageMapping {

    fun shouldFlip(dragPx: Float, widthPx: Float): Boolean =
        widthPx > 0f && abs(dragPx) > widthPx * 0.2f

    fun pageForOffset(pages: List<Page>, offset: Int): Int {
        if (pages.isEmpty()) return 0
        val index = pages.indexOfLast { it.startChar <= offset }
        return if (index < 0) 0 else index.coerceAtMost(pages.lastIndex)
    }
}

data class ScrollItem(val chapterIndex: Int, val charStart: Int)

object ScrollMapping {
    fun positionFor(items: List<ScrollItem>, firstVisibleItem: Int): Pair<Int, Int> {
        if (items.isEmpty()) return 0 to 0
        val item = items[firstVisibleItem.coerceIn(0, items.lastIndex)]
        return item.chapterIndex to item.charStart
    }
}

class ComposePageBreaker(
    private val measurer: TextMeasurer,
    private val widthPx: Int,
    private val heightPx: Int,
    private val style: TextStyle,
) : TextPageBreaker {

    override fun paginate(text: String): List<Page> {
        if (text.isEmpty()) return listOf(Page("", 0, 0))
        val pages = mutableListOf<Page>()
        var start = 0
        while (start < text.length) {
            val end = findPageEnd(text, start)
            val safeEnd = if (end <= start) (start + 1).coerceAtMost(text.length) else end
            pages += Page(
                text = text.substring(start, safeEnd),
                startChar = start,
                endChar = safeEnd,
            )
            start = safeEnd
        }
        return PageSanitizer.sanitize(pages)
    }

    private fun findPageEnd(text: String, start: Int): Int {
        var low = start + 1
        var high = text.length
        var best = start
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (fits(text, start, mid)) {
                best = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        if (best <= start) return start

        var cut = best
        if (cut < text.length && text[cut - 1].isHighSurrogate()) cut--
        return cut
    }

    private fun fits(text: String, start: Int, end: Int): Boolean {
        val result = measurer.measure(
            text = AnnotatedString(text.substring(start, end)),
            style = style,
            constraints = Constraints(maxWidth = widthPx.coerceAtLeast(1)),
        )
        return result.size.height <= heightPx
    }
}
