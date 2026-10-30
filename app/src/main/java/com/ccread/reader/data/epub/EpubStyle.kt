package com.ccread.reader.data.epub

import androidx.compose.ui.graphics.toArgb
import com.ccread.reader.domain.model.PageMode
import com.ccread.reader.domain.model.ReadingSettings
import com.ccread.reader.domain.settings.BackgroundPalette
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.Color as ReadiumColor

object EpubStyle {

    private const val BASE_PAGE_GUTTER_DP = 20.0
    private const val WEB_VIEW_BASE_FONT_SIZE = 16.0

    /**
     * Readium 的字号是相对 WebView 根字号 16px 的百分比，而 TXT 用的是 sp。
     * 按 sp 的像素值（含系统字体缩放）换算，两种格式显示的字号才一致。
     */
    fun fontSizeFactor(fontSize: Int, fontScale: Float = 1f): Double =
        fontSize * fontScale / WEB_VIEW_BASE_FONT_SIZE

    fun backgroundColorArgb(settings: ReadingSettings): Int =
        settings.customBackgroundArgb
            ?: BackgroundPalette.styleOf(settings.backgroundId).background.toArgb()

    /**
     * Readium scales the default `--RS__pageGutter` (20px on phone-width viewports) by this
     * factor, so the result matches the horizontal margin used by the TXT reader.
     */
    fun pageMarginFactor(marginHorizontal: Int): Double = marginHorizontal / BASE_PAGE_GUTTER_DP

    fun preferences(
        settings: ReadingSettings,
        mode: PageMode,
        fontScale: Float = 1f,
    ): EpubPreferences = EpubPreferences(
        fontSize = fontSizeFactor(settings.fontSize, fontScale),
        backgroundColor = ReadiumColor(backgroundColorArgb(settings)),
        textColor = settings.textColorArgb?.let { ReadiumColor(it) },
        scroll = mode == PageMode.SCROLL,
        pageMargins = pageMarginFactor(settings.marginHorizontal),
    )
}
