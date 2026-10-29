package com.ccread.reader.data.epub

import com.ccread.reader.domain.model.BackgroundId
import com.ccread.reader.domain.model.PageMode
import com.ccread.reader.domain.model.ReadingSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpubStyleTest {

    @Test
    fun `font size scales from an 18sp baseline`() {
        assertEquals(1.0, EpubStyle.fontScale(18), 0.001)
        assertEquals(1.5, EpubStyle.fontScale(27), 0.001)
    }

    @Test
    fun `background is overridden and text color is untouched`() {
        val preferences = EpubStyle.preferences(
            ReadingSettings(backgroundId = BackgroundId.GREEN),
            PageMode.PAGED,
        )

        assertEquals(0xFFCCE8CF.toInt(), preferences.backgroundColor?.int)
        assertNull(preferences.textColor)
    }

    @Test
    fun `scroll mode maps to the scroll preference`() {
        val preferences = EpubStyle.preferences(ReadingSettings(), PageMode.SCROLL)
        assertTrue(preferences.scroll == true)
    }

    @Test
    fun `page margins scale the readium page gutter with the horizontal margin`() {
        assertEquals(1.2, EpubStyle.pageMarginFactor(24), 0.001)
        assertEquals(0.4, EpubStyle.pageMarginFactor(8), 0.001)
        assertEquals(2.4, EpubStyle.pageMarginFactor(48), 0.001)
    }

    @Test
    fun `preferences carry the horizontal margin as page margins`() {
        val preferences = EpubStyle.preferences(
            ReadingSettings(marginHorizontal = 24),
            PageMode.PAGED,
        )

        assertEquals(1.2, preferences.pageMargins ?: 0.0, 0.001)
    }
}
