package com.ccread.reader.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class EpubProgressTest {

    @Test
    fun `progression is clamped and formatted with two decimals`() {
        assertEquals(0.0, EpubProgressRules.clampProgression(-0.1), 0.001)
        assertEquals(1.0, EpubProgressRules.clampProgression(1.5), 0.001)
        assertEquals("12.34%", EpubProgressRules.percentText(0.1234))
    }
}
