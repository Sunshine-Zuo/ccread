package com.ccread.reader.domain.txt

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.Locale

data class ChapterBoundary(
    val title: String,
    val startByte: Long,
    val endByte: Long,
    val charStart: Long,
    val charCount: Long,
)

data class TxtIndex(
    val charset: Charset,
    val totalChars: Long,
    val chapters: List<ChapterBoundary>,
)

data class SplitterParams(
    val minChapterChars: Long = 200,
    val maxChapterChars: Long = 200_000,
    val fallbackChunkChars: Long = 5_000,
    val maxTitleChars: Int = 30,
    val rules: List<ChapterRule> = BuiltInChapterRules.all.filter { it.enabled },
)

object TxtChapterSplitter {

    private const val PREFACE = "前言"
    private const val CONTINUATION = "（续）"
    private val HEADING_MARKER = Regex(
        """^(第[0-9零〇一二三四五六七八九十百千万两]+[章回节集篇部卷]|(?:Chapter|CHAPTER)\s+\d+)\s*""",
    )

    fun split(
        source: SeekableFile,
        charset: Charset,
        params: SplitterParams = SplitterParams(),
    ): TxtIndex {
        val chapters = mutableListOf<MutableChapter>()
        val fallback = mutableListOf<ChapterBoundary>()
        var preface: MutableChapter? = null
        var current: MutableChapter? = null
        var totalChars = 0L
        var headingCount = 0
        var fallbackChars = 0L
        var fallbackStartByte = 0L
        var fallbackStartChar = 0L
        var fallbackNumber = 0

        fun closeFallback(endByte: Long, endChar: Long) {
            if (fallbackChars > 0) {
                fallbackNumber++
                fallback += ChapterBoundary(
                    title = String.format(Locale.US, "第 %d 部分", fallbackNumber),
                    startByte = fallbackStartByte,
                    endByte = endByte,
                    charStart = fallbackStartChar,
                    charCount = fallbackChars,
                )
            }
            fallbackChars = 0
        }

        fun closeCurrent() {
            val chapter = current ?: return
            val previous = chapters.lastOrNull()
            if (chapter.charCount < params.minChapterChars &&
                chapter.title != PREFACE &&
                previous != null &&
                previous.title != PREFACE
            ) {
                previous.endByte = chapter.endByte
                previous.charCount += chapter.charCount
            } else {
                chapters += chapter
            }
            current = null
        }

        fun appendLine(line: Line, lineStartChar: Long) {
            val lineChars = line.text.length + 1L
            val chapter = current ?: return
            if (chapter.charCount > 0 && chapter.charCount + lineChars > params.maxChapterChars) {
                val root = chapter.title.removeSuffix(CONTINUATION)
                closeCurrent()
                current = MutableChapter(
                    title = root + CONTINUATION,
                    startByte = line.startByte,
                    endByte = line.endByte,
                    charStart = lineStartChar,
                    charCount = lineChars,
                )
            } else {
                chapter.endByte = line.endByte
                chapter.charCount += lineChars
            }
        }

        val reader = LineReader(source, charset)
        for (line in reader.lines()) {
            val lineStartChar = totalChars
            totalChars += line.text.length + 1L

            run {
                var offsetChars = 0
                var offsetBytes = line.startByte
                var remaining = line.text
                while (remaining.isNotEmpty()) {
                    if (fallbackChars == 0L) {
                        fallbackStartByte = offsetBytes
                        fallbackStartChar = lineStartChar + offsetChars
                    }
                    val room = (params.fallbackChunkChars - fallbackChars).coerceAtLeast(1)
                    val take = minOf(room, remaining.length.toLong()).toInt()
                    val slice = remaining.substring(0, take)
                    offsetBytes += byteLength(slice, charset)
                    fallbackChars += take
                    offsetChars += take
                    remaining = remaining.substring(take)
                    if (fallbackChars >= params.fallbackChunkChars) {
                        closeFallback(offsetBytes, lineStartChar + offsetChars)
                    }
                }
                fallbackChars += 1
                if (fallbackChars >= params.fallbackChunkChars) {
                    closeFallback(line.endByte, totalChars)
                }
            }

            val title = matchTitle(line.text, params)
            if (title != null) {
                headingCount++
                preface?.let {
                    chapters += it
                    preface = null
                }
                closeCurrent()
                current = MutableChapter(
                    title = title,
                    startByte = line.startByte,
                    endByte = line.endByte,
                    charStart = lineStartChar,
                    charCount = line.text.length + 1L,
                )
                continue
            }

            val chapter = current
            if (chapter == null) {
                val pre = preface ?: MutableChapter(
                    title = PREFACE,
                    startByte = line.startByte,
                    endByte = line.endByte,
                    charStart = lineStartChar,
                    charCount = 0,
                ).also { preface = it }
                pre.endByte = line.endByte
                pre.charCount += line.text.length + 1L
            } else {
                appendLine(line, lineStartChar)
            }
        }

        closeCurrent()
        if (fallbackChars > 0) closeFallback(source.sizeBytes, totalChars)

        val result = if (headingCount >= 2) {
            chapters.map { it.toBoundary() }
        } else {
            fallback
        }
        return TxtIndex(charset = charset, totalChars = totalChars, chapters = result)
    }

    private fun matchTitle(text: String, params: SplitterParams): String? {
        val raw = text.trim()
        if (raw.isEmpty()) return null
        val matched = params.rules.any { it.pattern.matches(raw) }
        if (!matched) return null
        val stripped = HEADING_MARKER.replace(raw, "").trim()
        val meaningful = if (stripped.isEmpty()) raw else stripped
        return if (meaningful.length > params.maxTitleChars) null else raw
    }

    private fun byteLength(text: String, charset: Charset): Int =
        if (text.isEmpty()) 0 else text.toByteArray(charset).size

    private data class MutableChapter(
        val title: String,
        val startByte: Long,
        var endByte: Long,
        val charStart: Long,
        var charCount: Long,
    ) {
        fun toBoundary() = ChapterBoundary(
            title = title,
            startByte = startByte,
            endByte = endByte,
            charStart = charStart,
            charCount = charCount,
        )
    }
}

private data class Line(
    val startByte: Long,
    val endByte: Long,
    val text: String,
)

private class LineReader(
    private val source: SeekableFile,
    private val charset: Charset,
    private val chunkSize: Int = 64 * 1024,
    private val maxLineBytes: Int = 64 * 1024,
) {
    private val utf16Le = charset.name().equals("UTF-16LE", ignoreCase = true)
    private val utf16Be = charset.name().equals("UTF-16BE", ignoreCase = true)

    fun lines(): Sequence<Line> = sequence {
        val buffer = ByteArrayOutputStream()
        val stream = ChunkStream(source, chunkSize)
        var lineStart = 0L
        var firstLine = true

        fun emit(endByte: Long): String {
            val text = decode(buffer.toByteArray(), firstLine)
            buffer.reset()
            firstLine = false
            lineStart = endByte
            return text
        }

        while (true) {
            val byte = stream.read() ?: break
            val isNewline = when {
                utf16Le -> byte == 0x0A && stream.peek() == 0x00
                utf16Be -> byte == 0x00 && stream.peek() == 0x0A
                else -> byte == 0x0A
            }

            if (isNewline) {
                if (utf16Le || utf16Be) stream.read()
                val end = stream.position
                yield(Line(startByte = lineStart, endByte = end, text = emit(end).trimEnd('\u0000')))
            } else {
                buffer.write(byte)
                if (buffer.size() >= maxLineBytes) {
                    val end = stream.position
                    yield(Line(startByte = lineStart, endByte = end, text = emit(end).trimEnd('\u0000')))
                }
            }
        }

        if (buffer.size() > 0) {
            val end = stream.position
            yield(Line(startByte = lineStart, endByte = end, text = decode(buffer.toByteArray(), firstLine)))
        }
    }

    private fun decode(bytes: ByteArray, first: Boolean): String {
        var text = String(bytes, charset)
        if (first && text.startsWith('﻿')) text = text.substring(1)
        return text
    }
}

private class ChunkStream(
    private val source: SeekableFile,
    private val chunkSize: Int,
) {
    private var chunk: ByteArray = ByteArray(0)
    private var index = 0
    private var chunkStart = 0L

    var position: Long = 0L
        private set

    fun peek(): Int {
        ensure()
        return if (index < chunk.size) chunk[index].toInt() and 0xFF else -1
    }

    fun read(): Int? {
        ensure()
        if (index >= chunk.size) return null
        val value = chunk[index++].toInt() and 0xFF
        position = chunkStart + index
        return value
    }

    private fun ensure() {
        if (index < chunk.size) return
        if (position >= source.sizeBytes) return
        chunkStart = position
        val length = minOf(chunkSize.toLong(), source.sizeBytes - position).toInt()
        chunk = if (length <= 0) ByteArray(0) else source.readAt(position, length)
        index = 0
        if (chunk.isEmpty()) position = source.sizeBytes
    }
}
