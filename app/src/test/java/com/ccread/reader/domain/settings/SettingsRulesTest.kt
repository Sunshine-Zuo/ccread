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
    fun `font color presets pair one to one with background presets`() {
        assertEquals(BackgroundPalette.presets.size, FontColorPalette.presets.size)
        assertEquals(BackgroundPalette.presets.map { it.text }, FontColorPalette.presets)
    }

    @Test
    fun `custom background resolves a contrasting text color`() {
        val dark = BackgroundPalette.resolve(BackgroundId.BEIGE, 0xFF101010.toInt())
        assertEquals(Color(0xFF101010), dark.background)
        assertEquals(Color(0xFFE8E8E8), dark.text)

        val light = BackgroundPalette.resolve(BackgroundId.BLACK, 0xFFFFFFFF.toInt())
        assertEquals(Color(0xFFFFFFFF), light.background)
        assertEquals(Color(0xFF1A1A1A), light.text)
    }

    @Test
    fun `following system brightness overrides manual value`() {
        assertEquals(-1f, BrightnessController.windowBrightness(true, 0.7f), 0.001f)
        assertEquals(0.7f, BrightnessController.windowBrightness(false, 0.7f), 0.001f)
        assertEquals(1f, BrightnessController.windowBrightness(false, 2f), 0.001f)
    }
}
