package com.ccread.reader.domain.model

enum class PageMode { PAGED, SCROLL }

enum class BackgroundId { WHITE, BEIGE, GREEN, DARK_GRAY, BLACK }

data class ReadingSettings(
    val fontSize: Int = 18,
    val marginHorizontal: Int = 24,
    val lineSpacing: Float = 1.5f,
    val backgroundId: BackgroundId = BackgroundId.BEIGE,
    val customBackgroundArgb: Int? = null,
    val textColorArgb: Int? = null,
    val pageMode: PageMode = PageMode.PAGED,
    val followSystemBrightness: Boolean = true,
    val brightness: Float = 0.5f,
) {
    fun clamped(): ReadingSettings = copy(
        fontSize = fontSize.coerceIn(12, 36),
        marginHorizontal = marginHorizontal.coerceIn(8, 48),
        lineSpacing = lineSpacing.coerceIn(1.0f, 2.0f),
        brightness = brightness.coerceIn(0.0f, 1.0f),
    )
}
