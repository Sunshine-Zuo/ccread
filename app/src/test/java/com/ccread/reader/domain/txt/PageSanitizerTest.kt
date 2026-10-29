package com.ccread.reader.domain.txt

import org.junit.Assert.assertEquals
import org.junit.Test

class PageSanitizerTest {

    @Test
    fun `drops whitespace-only pages`() {
        val pages = listOf(
            Page("第一页", 0, 3),
            Page("\n  \n", 3, 7),
        )

        assertEquals(listOf(Page("第一页", 0, 3)), PageSanitizer.sanitize(pages))
    }

    @Test
    fun `keeps one page when the whole chapter is blank`() {
        val pages = listOf(Page("\n\n", 0, 2))

        assertEquals(pages, PageSanitizer.sanitize(pages))
    }
}
