package com.ccread.reader.domain.txt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageMappingTest {

    @Test
    fun `swipe must exceed twenty percent of width`() {
        assertTrue(PageMapping.shouldFlip(dragPx = 61f, widthPx = 300f))
        assertFalse(PageMapping.shouldFlip(dragPx = 59f, widthPx = 300f))
        assertTrue(PageMapping.shouldFlip(dragPx = -61f, widthPx = 300f))
    }

    @Test
    fun `offset maps to its page`() {
        val pages = listOf(
            Page("a", 0, 10),
            Page("b", 10, 20),
            Page("c", 20, 30),
        )

        assertEquals(0, PageMapping.pageForOffset(pages, 0))
        assertEquals(1, PageMapping.pageForOffset(pages, 15))
        assertEquals(2, PageMapping.pageForOffset(pages, 999))
    }

    @Test
    fun `empty pages map to zero`() {
        assertEquals(0, PageMapping.pageForOffset(emptyList(), 5))
    }
}
