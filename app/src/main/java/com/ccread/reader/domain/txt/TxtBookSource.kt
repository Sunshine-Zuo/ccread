package com.ccread.reader.domain.txt

import com.ccread.reader.domain.model.Chapter
import java.nio.charset.Charset

class TxtBookSource(
    private val source: SeekableFile,
    private val chapters: List<Chapter>,
    private val charset: Charset,
    private val totalChars: Long,
) {

    fun chapterText(chapterIndex: Int): String {
        val chapter = chapters.firstOrNull { it.index == chapterIndex } ?: return ""
        val length = (chapter.endByte - chapter.startByte).coerceAtLeast(0).toInt()
        if (length == 0) return ""
        var text = String(source.readAt(chapter.startByte, length), charset)
        if (text.startsWith('﻿')) text = text.substring(1)
        return text
    }

    fun totalChars(): Long = totalChars
}
