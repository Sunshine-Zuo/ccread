package com.ccread.reader.domain.progress

import com.ccread.reader.domain.model.Chapter
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressCalculatorTest {

    @Test
    fun `txt percent uses character offsets and clamps`() {
        assertEquals(50.0, ProgressCalculator.txtPercent(1000, 400, 100), 0.001)
        assertEquals(0.0, ProgressCalculator.txtPercent(1000, 0, -5), 0.001)
        assertEquals(0.0, ProgressCalculator.txtPercent(0, 0, 0), 0.001)
        assertEquals(100.0, ProgressCalculator.txtPercent(1000, 900, 500), 0.001)
    }

    @Test
    fun `position maps to chapter and offset`() {
        val chapters = listOf(
            Chapter(1, 0, "一", 0, 300, 0, 300),
            Chapter(1, 1, "二", 300, 700, 300, 400),
            Chapter(1, 2, "三", 700, 1000, 700, 300),
        )

        assertEquals(1 to 200L, ProgressCalculator.locate(chapters, 500))
        assertEquals(0 to 0L, ProgressCalculator.locate(chapters, -10))
        assertEquals(2 to 300L, ProgressCalculator.locate(chapters, 5000))
    }

    @Test
    fun `percent formatter always uses two decimals`() {
        assertEquals("12.34%", PercentFormatter.format(12.34))
        assertEquals("0.00%", PercentFormatter.format(0.0))
        assertEquals("100.00%", PercentFormatter.format(100.0))
    }
}
