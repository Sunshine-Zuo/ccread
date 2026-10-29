package com.ccread.reader.domain.progress

import com.ccread.reader.domain.model.Chapter
import com.ccread.reader.domain.model.TxtProgress

object TxtProgressRemap {

    fun remap(progress: TxtProgress?, totalChars: Long, chapters: List<Chapter>): TxtProgress? {
        if (progress == null || chapters.isEmpty() || totalChars <= 0) return null
        val charPosition = (progress.percent.coerceIn(0.0, 100.0) / 100.0 * totalChars).toLong()
        val target = chapters.lastOrNull { it.charStart <= charPosition } ?: chapters.first()
        val offset = (charPosition - target.charStart).coerceIn(0L, target.charCount)
        return TxtProgress(
            percent = progress.percent,
            chapterIndex = target.index,
            charOffsetInChapter = offset,
        )
    }
}
