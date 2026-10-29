package com.ccread.reader.data.epub

import androidx.compose.ui.graphics.toArgb
import com.ccread.reader.domain.model.BackgroundId
import com.ccread.reader.domain.model.PageMode
import com.ccread.reader.domain.model.ReadingSettings
import com.ccread.reader.domain.settings.BackgroundPalette
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.Color as ReadiumColor

object EpubStyle {

    private const val BASE_PAGE_GUTTER_DP = 20.0

    fun fontScale(fontSize: Int): Double = fontSize / 18.0

    fun backgroundColorArgb(id: BackgroundId): Int =
        BackgroundPalette.styleOf(id).background.toArgb()

    /**
     * Readium scales the default `--RS__pageGutter` (20px on phone-width viewports) by this
     * factor, so the result matches the horizontal margin used by the TXT reader.
     */
    fun pageMarginFactor(marginHorizontal: Int): Double = marginHorizontal / BASE_PAGE_GUTTER_DP

    fun preferences(settings: ReadingSettings, mode: PageMode): EpubPreferences = EpubPreferences(
        fontSize = fontScale(settings.fontSize),
        backgroundColor = ReadiumColor(backgroundColorArgb(settings.backgroundId)),
        scroll = mode == PageMode.SCROLL,
        pageMargins = pageMarginFactor(settings.marginHorizontal),
    )
}
