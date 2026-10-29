package com.ccread.reader.ui.reader

import com.ccread.reader.domain.model.PageMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderInteractionTest {

    @Test
    fun `screen is split into three equal zones`() {
        assertEquals(TapZone.LEFT, ReaderInteraction.zoneFor(10f, 300f))
        assertEquals(TapZone.CENTER, ReaderInteraction.zoneFor(150f, 300f))
        assertEquals(TapZone.RIGHT, ReaderInteraction.zoneFor(290f, 300f))
    }

    @Test
    fun `paged mode left and right turn pages`() {
        assertEquals(ReaderAction.PreviousPage, ReaderInteraction.onTap(TapZone.LEFT, PageMode.PAGED, OverlayState()))
        assertEquals(ReaderAction.NextPage, ReaderInteraction.onTap(TapZone.RIGHT, PageMode.PAGED, OverlayState()))
    }

    @Test
    fun `scroll mode left and right do nothing`() {
        val overlays = OverlayState()
        assertEquals(ReaderAction.UpdateOverlays(overlays), ReaderInteraction.onTap(TapZone.LEFT, PageMode.SCROLL, overlays))
        assertEquals(ReaderAction.UpdateOverlays(overlays), ReaderInteraction.onTap(TapZone.RIGHT, PageMode.SCROLL, overlays))
    }

    @Test
    fun `center tap toggles the menu`() {
        val opened = ReaderInteraction.onTap(TapZone.CENTER, PageMode.PAGED, OverlayState()) as ReaderAction.UpdateOverlays
        assertTrue(opened.state.menu)

        val closed = ReaderInteraction.onTap(TapZone.CENTER, PageMode.PAGED, opened.state) as ReaderAction.UpdateOverlays
        assertFalse(closed.state.menu)
    }

    @Test
    fun `center tap closes open panels first`() {
        val overlays = OverlayState(toc = true)
        val result = ReaderInteraction.onTap(TapZone.CENTER, PageMode.PAGED, overlays) as ReaderAction.UpdateOverlays

        assertFalse(result.state.toc)
        assertTrue(result.state.menu)
    }

    @Test
    fun `back closes panels one at a time`() {
        val start = OverlayState(menu = true, toc = true, settings = true, brightness = true)

        val afterFirst = ReaderInteraction.onBack(start)
        assertFalse(afterFirst.brightness)
        assertTrue(afterFirst.settings)

        val afterSecond = ReaderInteraction.onBack(afterFirst)
        assertFalse(afterSecond.settings)
        assertTrue(afterSecond.toc)

        val afterThird = ReaderInteraction.onBack(afterSecond)
        assertFalse(afterThird.toc)
        assertTrue(afterThird.menu)

        val afterFourth = ReaderInteraction.onBack(afterThird)
        assertEquals(OverlayState(), afterFourth)
    }
}
