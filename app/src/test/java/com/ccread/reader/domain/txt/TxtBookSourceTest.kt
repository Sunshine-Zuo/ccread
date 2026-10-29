package com.ccread.reader.domain.txt

import com.ccread.reader.domain.model.Chapter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TxtBookSourceTest {

    @Test
    fun `reads the requested chapter by byte offsets`() {
        val text = "第一章 A\n正文一\n第二章 B\n正文二"
        val file = FakeSeekableFile(text.toByteArray())
        val index = TxtChapterSplitter.split(file, Charsets.UTF_8, SplitterParams(minChapterChars = 1))
        val chapters = index.chapters.mapIndexed { position, boundary ->
            Chapter(1, position, boundary.title, boundary.startByte, boundary.endByte, boundary.charStart, boundary.charCount)
        }
        val source = TxtBookSource(file, chapters, Charsets.UTF_8, index.totalChars)

        val second = source.chapterText(1)

        assertTrue(second.startsWith("第二章 B"))
        assertFalse(second.contains("正文一"))
        assertTrue(source.chapterText(1).contains("正文二"))
    }
}
