package com.ccread.reader.ui.reader

import com.ccread.reader.domain.model.Chapter
import org.junit.Assert.assertEquals
import org.junit.Test

class TocViewModelTest {

    private fun chapter(index: Int, charStart: Long, charCount: Long) =
        Chapter(bookId = 1, index = index, title = "章$index", startByte = 0, endByte = 0, charStart = charStart, charCount = charCount)

    @Test
    fun `resplit keeps the character position`() {
        val old = listOf(chapter(0, 0, 500), chapter(1, 500, 500))
        val new = listOf(chapter(0, 0, 200), chapter(1, 200, 800))

        val (index, offset) = ResplitMapping.map(old, new, oldCharPosition = 600)

        assertEquals(1, index)
        assertEquals(400L, offset)
    }

    @Test
    fun `empty new toc returns zero`() {
        val old = listOf(chapter(0, 0, 500))
        assertEquals(0 to 0L, ResplitMapping.map(old, emptyList(), 300))
    }
}
