package com.ccread.reader.domain.progress

import com.ccread.reader.domain.model.Chapter
import java.util.Locale

object ProgressCalculator {

    fun txtPercent(totalChars: Long, chapterCharStart: Long, offsetInChapter: Long): Double {
        if (totalChars <= 0) return 0.0
        val position = (chapterCharStart + offsetInChapter).coerceIn(0, totalChars)
        return position.toDouble() / totalChars * 100.0
    }

    fun locate(chapters: List<Chapter>, charPosition: Long): Pair<Int, Long> {
        if (chapters.isEmpty()) return 0 to 0L
        val target = chapters.lastOrNull { it.charStart <= charPosition } ?: chapters.first()
        val offset = (charPosition - target.charStart).coerceIn(0L, target.charCount)
        return target.index to offset
    }
}

object PercentFormatter {
    fun format(percent: Double): String = String.format(Locale.US, "%.2f%%", percent)
}
