package com.ccread.reader.domain.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.ccread.reader.domain.model.BackgroundId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRulesTest {

    @Test
    fun `background palette has five readable pairs`() {
        val beige = BackgroundPalette.styleOf(BackgroundId.BEIGE)
        assertEquals(Color(0xFFF5ECD8), beige.background)
        assertEquals(Color(0xFF3B2F2F), beige.text)

        val black = BackgroundPalette.styleOf(BackgroundId.BLACK)
        assertEquals(Color(0xFF000000), black.background)
        assertEquals(Color(0xFFE8E8E8), black.text)

        assertEquals(5, BackgroundPalette.all.size)
    }

    @Test
    fun `dark backgrounds get light text`() {
        val darkGray = BackgroundPalette.styleOf(BackgroundId.DARK_GRAY)
        assertTrue(darkGray.text.luminance() > darkGray.background.luminance())
        val black = BackgroundPalette.styleOf(BackgroundId.BLACK)
        assertTrue(black.text.luminance() > black.background.luminance())
    }

    @Test
    fun `following system brightness overrides manual value`() {
        assertEquals(-1f, BrightnessController.windowBrightness(true, 0.7f), 0.001f)
        assertEquals(0.7f, BrightnessController.windowBrightness(false, 0.7f), 0.001f)
        assertEquals(1f, BrightnessController.windowBrightness(false, 2f), 0.001f)
    }
}
