package com.ccread.reader.domain.txt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TxtChapterSplitterTest {

    private fun split(text: String, params: SplitterParams = SplitterParams()): TxtIndex =
        TxtChapterSplitter.split(FakeSeekableFile(text.toByteArray()), Charsets.UTF_8, params)

    @Test
    fun `recognizes chapters and creates preface`() {
        val index = split("引子正文\n第一章 起点\n正文一\n第二章 归途\n正文二", SplitterParams(minChapterChars = 1))

        assertEquals(listOf("前言", "第一章 起点", "第二章 归途"), index.chapters.map { it.title })
    }

    @Test
    fun `recognizes chinese numerals and chapter keyword`() {
        val index = split("第十二章 风\n正文\nChapter 3 Home\n正文", SplitterParams(minChapterChars = 1))

        assertEquals(listOf("第十二章 风", "Chapter 3 Home"), index.chapters.map { it.title })
    }

    @Test
    fun `falls back to chunks when no chapter is found`() {
        val index = split("字".repeat(12_000), SplitterParams(fallbackChunkChars = 5_000))

        assertEquals(3, index.chapters.size)
        assertTrue(index.chapters.all { it.title.startsWith("第") && it.title.endsWith("部分") })
    }

    @Test
    fun `short chapters merge and long chapters split`() {
        val short = split(
            "第一章 A\n短\n第二章 B\n" + "长".repeat(600),
            SplitterParams(minChapterChars = 200),
        )
        assertEquals(2, short.chapters.size)

        val long = split(
            "第一章 A\n" + "长".repeat(450_000) + "\n第二章 B\n正文",
            SplitterParams(maxChapterChars = 200_000),
        )
        assertTrue(long.chapters.all { it.charCount <= 200_001 })
        assertTrue(long.chapters.first().title.startsWith("第一章 A"))
    }

    @Test
    fun `title longer than limit is treated as body`() {
        val index = split("第一章 " + "题".repeat(31) + "\n正文", SplitterParams(minChapterChars = 1))

        assertEquals(1, index.chapters.size)
        assertTrue(index.chapters.first().title.startsWith("第") && index.chapters.first().title.endsWith("部分"))
    }

    @Test
    fun `byte offsets locate the original chapter`() {
        val file = FakeSeekableFile("第一章 A\n正文甲\n第二章 B\n正文乙".toByteArray())
        val index = TxtChapterSplitter.split(file, Charsets.UTF_8, SplitterParams(minChapterChars = 1))

        val second = index.chapters[1]
        val text = file.readAt(second.startByte, (second.endByte - second.startByte).toInt())
            .toString(Charsets.UTF_8)

        assertTrue(text.startsWith("第二章 B"))
    }

    @Test
    fun `scanner never reads more than 64KB at once`() {
        val file = FakeSeekableFile(("第一章 A\n" + "长".repeat(200_000)).toByteArray())
        TxtChapterSplitter.split(file, Charsets.UTF_8)

        assertTrue(file.maxReadLength <= 65_536)
    }

    @Test
    fun `a very long single line does not exhaust memory`() {
        val index = split("长".repeat(400_000))

        assertTrue(index.chapters.isNotEmpty())
    }
}
