package com.ccread.reader.ui.reader

import com.ccread.reader.domain.model.Chapter
import com.ccread.reader.domain.model.TocNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChapterNavigationTest {

    private fun chapter(index: Int) = Chapter(
        bookId = 1,
        index = index,
        title = "第${index + 1}章",
        startByte = 0,
        endByte = 0,
        charStart = index * 100L,
        charCount = 100,
    )

    private val chapters = listOf(chapter(0), chapter(1), chapter(2))

    @Test
    fun `txt next moves to the following chapter`() {
        assertEquals(2, ChapterNavigation.txtTarget(chapters, 1, 1)?.index)
    }

    @Test
    fun `txt previous moves to the preceding chapter`() {
        assertEquals(0, ChapterNavigation.txtTarget(chapters, 1, -1)?.index)
    }

    @Test
    fun `txt navigation stops at the ends`() {
        assertNull(ChapterNavigation.txtTarget(chapters, 0, -1))
        assertNull(ChapterNavigation.txtTarget(chapters, 2, 1))
    }

    @Test
    fun `txt navigation returns null when the current chapter is unknown`() {
        assertNull(ChapterNavigation.txtTarget(chapters, 9, 1))
    }

    private val toc = listOf(
        TocNode(
            title = "第一卷",
            href = "v1.xhtml",
            children = listOf(
                TocNode("第一章", "c1.xhtml"),
                TocNode("第二章", "c2.xhtml#part2"),
            ),
        ),
        TocNode("第三章", "c3.xhtml"),
    )

    @Test
    fun `epub next follows the flattened toc order`() {
        assertEquals("c3.xhtml", ChapterNavigation.epubTarget(toc, "c2.xhtml", 1))
    }

    @Test
    fun `epub previous moves back through nested entries`() {
        assertEquals("c1.xhtml", ChapterNavigation.epubTarget(toc, "c2.xhtml", -1))
    }

    @Test
    fun `epub navigation matches hrefs with fragments`() {
        assertEquals("c2.xhtml#part2", ChapterNavigation.epubTarget(toc, "c1.xhtml#frag", 1))
        assertEquals("c2.xhtml#part2", ChapterNavigation.epubTarget(toc, "c2.xhtml#other", 0))
    }

    @Test
    fun `epub navigation stops at the ends`() {
        assertNull(ChapterNavigation.epubTarget(toc, "v1.xhtml", -1))
        assertNull(ChapterNavigation.epubTarget(toc, "c3.xhtml", 1))
    }

    @Test
    fun `epub navigation returns null without a current href`() {
        assertNull(ChapterNavigation.epubTarget(toc, null, 1))
    }
}
