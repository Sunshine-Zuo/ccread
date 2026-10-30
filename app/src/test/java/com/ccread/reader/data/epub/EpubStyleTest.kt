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
    fun `font size matches txt sp using the webview 16px base`() {
        assertEquals(18.0 / 16.0, EpubStyle.fontSizeFactor(18), 0.001)
        assertEquals(27.0 / 16.0, EpubStyle.fontSizeFactor(27), 0.001)
    }

    @Test
    fun `system font scale is applied so epub matches txt`() {
        assertEquals(18.0 * 1.3 / 16.0, EpubStyle.fontSizeFactor(18, 1.3f), 0.001)
    }

    @Test
    fun `custom font color overrides the book color`() {
        val preferences = EpubStyle.preferences(
            ReadingSettings(textColorArgb = 0xFF112233.toInt()),
            PageMode.PAGED,
        )

        assertEquals(0xFF112233.toInt(), preferences.textColor?.int)
    }

    @Test
    fun `font color stays untouched when not set`() {
        val preferences = EpubStyle.preferences(ReadingSettings(), PageMode.PAGED)

        assertNull(preferences.textColor)
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
    fun `custom background overrides the preset background`() {
        val preferences = EpubStyle.preferences(
            ReadingSettings(customBackgroundArgb = 0xFF223344.toInt()),
            PageMode.PAGED,
        )

        assertEquals(0xFF223344.toInt(), preferences.backgroundColor?.int)
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
