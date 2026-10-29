package com.ccread.reader.ui.reader

import com.ccread.reader.domain.progress.PercentFormatter

object EpubProgressRules {
    fun clampProgression(value: Double): Double = value.coerceIn(0.0, 1.0)

    fun percentText(progression: Double): String =
        PercentFormatter.format(clampProgression(progression) * 100.0)
}
