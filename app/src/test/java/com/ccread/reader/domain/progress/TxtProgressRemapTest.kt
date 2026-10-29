package com.ccread.reader.domain.progress

import com.ccread.reader.domain.model.Chapter
import com.ccread.reader.domain.model.TxtProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TxtProgressRemapTest {

    private fun chapter(index: Int, charStart: Long, charCount: Long) =
        Chapter(1, index, "章$index", 0, 0, charStart, charCount)

    @Test
    fun `keeps percentage and maps to the new chapter`() {
        val chapters = listOf(chapter(0, 0, 200), chapter(1, 200, 800))
        val remapped = TxtProgressRemap.remap(TxtProgress(50.0, 0, 10), 1000, chapters)

        assertEquals(1, remapped?.chapterIndex)
        assertEquals(300L, remapped?.charOffsetInChapter)
        assertEquals(50.0, remapped?.percent ?: 0.0, 0.001)
    }

    @Test
    fun `missing progress stays missing and empty chapters return null`() {
        assertNull(TxtProgressRemap.remap(null, 1000, listOf(chapter(0, 0, 1000))))
        assertNull(TxtProgressRemap.remap(TxtProgress(10.0, 0, 0), 1000, emptyList()))
    }
}
