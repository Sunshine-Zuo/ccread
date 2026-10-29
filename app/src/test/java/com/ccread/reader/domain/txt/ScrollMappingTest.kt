package com.ccread.reader.domain.txt

import org.junit.Assert.assertEquals
import org.junit.Test

class ScrollMappingTest {

    @Test
    fun `first visible item maps to chapter and offset`() {
        val items = listOf(
            ScrollItem(0, 0),
            ScrollItem(0, 100),
            ScrollItem(0, 250),
            ScrollItem(1, 0),
        )

        assertEquals(0 to 250, ScrollMapping.positionFor(items, firstVisibleItem = 2))
        assertEquals(1 to 0, ScrollMapping.positionFor(items, firstVisibleItem = 3))
    }

    @Test
    fun `empty list returns zero`() {
        assertEquals(0 to 0, ScrollMapping.positionFor(emptyList(), 0))
    }

    @Test
    fun `out of range index clamps`() {
        val items = listOf(ScrollItem(2, 30))
        assertEquals(2 to 30, ScrollMapping.positionFor(items, 99))
    }
}
