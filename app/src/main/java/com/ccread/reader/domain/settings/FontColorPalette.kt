package com.ccread.reader.domain.settings

object FontColorPalette {

    /** 与背景预设一一对应，取每个背景的配套字色。 */
    val presets: List<Int> = BackgroundPalette.presets.map { it.text }
}
